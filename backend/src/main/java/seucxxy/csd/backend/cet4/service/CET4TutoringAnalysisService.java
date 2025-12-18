package seucxxy.csd.backend.cet4.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import seucxxy.csd.backend.cet4.dto.CET4SectionAChromaRecord;
import seucxxy.csd.backend.cet4.entity.CET4ExamPaperEn;
import seucxxy.csd.backend.cet4.entity.CET4UserTestRecordDetailEn;
import seucxxy.csd.backend.cet4.entity.CET4UserTestRecordEn;
import seucxxy.csd.backend.cet4.mapper.CET4EExamPaperEnMapper;
import seucxxy.csd.backend.cet4.mapper.CET4UserTestRecordDetailEnMapper;
import seucxxy.csd.backend.cet4.mapper.CET4UserTestRecordEnMapper;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CET4TutoringAnalysisService {

    private static final int EMBEDDING_DIMENSION = 384;
    private static final int DEFAULT_LIMIT = 5;

    private final RestTemplate restTemplate;
    private final CET4ChromaEngExamPaperService chromaEngExamPaperService;
    private final CET4UserTestRecordEnMapper userTestRecordEnMapper;
    private final CET4UserTestRecordDetailEnMapper userTestRecordDetailEnMapper;
    private final CET4EExamPaperEnMapper examPaperEnMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AtomicReference<String> tutoringCollectionIdCache = new AtomicReference<>();

    @Value("${coze.api.url}")
    private String cozeApiUrl;

    @Value("${coze.api.token.examinationAI}")
    private String cozeApiToken;

    @Value("${coze.workflow.tutoring-analysis:7580326569463201834}")
    private String tutoringWorkflowId;

    @Value("${chromadb.base-url:http://localhost:8000/api/v2}")
    private String chromaBaseUrl;

    @Value("${chromadb.tenant:default_tenant}")
    private String tenant;

    @Value("${chromadb.database:default_database}")
    private String database;

    @Value("${chromadb.collection.tutoring:tutoring_content_en_cet4}")
    private String tutoringCollectionName;

    @Async("paperAnalysisTaskExecutor")
    public void triggerHomepageAnalysis(Long userId) {
        if (userId == null) {
            log.warn("[TutoringAnalysis] 用户未登录，跳过分析");
            return;
        }
        try {
            runAnalysis(userId);
        } catch (Exception e) {
            log.warn("[TutoringAnalysis] 用户 {} 分析失败: {}", userId, e.getMessage(), e);
        }
    }

    public void runAnalysis(Long userId) throws Exception {
        List<CET4UserTestRecordEn> records = userTestRecordEnMapper.findRecentByUserId(userId, DEFAULT_LIMIT);
        if (records == null || records.isEmpty()) {
            log.info("[TutoringAnalysis] 用户 {} 最近无考试记录，跳过分析", userId);
            return;
        }

        List<Map<String, Object>> examPaperEns = new ArrayList<>();
        List<Long> testIds = new ArrayList<>();

        for (CET4UserTestRecordEn record : records) {
            Map<String, Object> examEntry = buildExamEntry(record);
            if (examEntry != null && !examEntry.isEmpty()) {
                examPaperEns.add(examEntry);
                testIds.add(record.getTestEnId());
            }
        }

        if (examPaperEns.isEmpty()) {
            log.info("[TutoringAnalysis] 用户 {} 没有可用的试卷数据，跳过分析", userId);
            return;
        }

        ObjectNode inputNode = objectMapper.createObjectNode();
        inputNode.put("userId", userId);
        inputNode.set("examPaperEns", objectMapper.valueToTree(examPaperEns));

        log.info("[TutoringAnalysis] 开始调用 workflow={}, userId={}, examPaperCount={}", tutoringWorkflowId, userId, examPaperEns.size());
        String cozeResponse = callWorkflow(inputNode);
        log.info("[TutoringAnalysis] workflow 调用完成, userId={}, responseLength={}", userId, cozeResponse == null ? 0 : cozeResponse.length());
        String analysisContent = extractAnalysisContent(cozeResponse);
        if (analysisContent == null || analysisContent.isBlank()) {
            log.warn("[TutoringAnalysis] workflow 返回为空，跳过存储");
            return;
        }
        storeAnalysisResult(userId, analysisContent, testIds);
    }

    private Map<String, Object> buildExamEntry(CET4UserTestRecordEn record) {
        try {
                if (record.getExamPaperEnId() == null) {
                log.warn("[TutoringAnalysis] testEnId={} 无 examPaperEnId，跳过", record.getTestEnId());
                return Collections.emptyMap();
                }

                CET4ExamPaperEn examPaper = examPaperEnMapper.getExamPaperEnById(record.getExamPaperEnId());

                // 只用试卷ID在 Chroma 中查询，避免名称不一致导致漏查
                List<CET4SectionAChromaRecord> units = chromaEngExamPaperService.fetchExamPaperUnits(
                    String.valueOf(record.getExamPaperEnId()),
                    null
                );
            List<Map<String, Object>> questions = units.stream()
                    .map(this::mapChromaUnit)
                    .collect(Collectors.toList());

            List<CET4UserTestRecordDetailEn> details = userTestRecordDetailEnMapper.getUserTestRecordDetailsEnByTestEnId(record.getTestEnId());
            if (details == null) {
                details = Collections.emptyList();
            }
            Map<String, Object> subjective = new LinkedHashMap<>();
            List<Map<String, Object>> objective = new ArrayList<>();
            for (CET4UserTestRecordDetailEn detail : details) {
                Map<String, Object> detailMap = new LinkedHashMap<>();
                detailMap.put("segmentId", detail.getSegmentId());
                detailMap.put("questionsEnNumber", detail.getQuestionsEnNumber());
                detailMap.put("questionsType", detail.getQuestionsType());
                detailMap.put("userAnswer", detail.getUserAnswer());
                detailMap.put("correctAnswer", detail.getCorrectAnswer());

                if (isSubjective(detail.getQuestionsType())) {
                    if (isWriting(detail.getQuestionsType())) {
                        subjective.put("writing", detailMap);
                    } else {
                        subjective.put("translation", detailMap);
                    }
                } else {
                    objective.add(detailMap);
                }
            }

            Map<String, Object> examEntry = new LinkedHashMap<>();
            examEntry.put("testEnId", record.getTestEnId());
            examEntry.put("examPaperEnId", record.getExamPaperEnId());
            examEntry.put("examPaperEnName", examPaper != null ? examPaper.getExamPaperEnName() : null);
            examEntry.put("examPaperEnSource", examPaper != null ? examPaper.getExamPaperEnSource() : null);
            examEntry.put("score", record.getTestEnScore());
            examEntry.put("correctNumber", record.getCorrectNumber());
            examEntry.put("testEnTime", record.getTestEnTime() == null ? null : record.getTestEnTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            examEntry.put("questions", questions);
            examEntry.put("objective", objective);
            examEntry.put("subjective", subjective);
            return examEntry;
        } catch (Exception ex) {
            log.warn("[TutoringAnalysis] 构建试卷数据失败 testEnId={}: {}", record.getTestEnId(), ex.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> mapChromaUnit(CET4SectionAChromaRecord record) {
        Map<String, Object> question = new LinkedHashMap<>();
        Map<String, Object> metadata = new LinkedHashMap<>(record.metadata());
        Object document = parseDocument(record.document());
        question.put("segmentId", metadata.getOrDefault("segment_id", metadata.get("segmentId")));
        question.put("questionType", metadata.getOrDefault("question_type", metadata.get("questionType")));
        question.put("partId", metadata.getOrDefault("part_id", metadata.get("partId")));
        question.put("document", document);
        question.put("metadata", metadata);
        return question;
    }

    private Object parseDocument(String document) {
        if (document == null || document.isBlank()) {
            return "";
        }
        try {
            return objectMapper.readValue(document, Object.class);
        } catch (Exception e) {
            return document;
        }
    }

    private boolean isSubjective(String type) {
        if (type == null) return false;
        String t = type.trim().toLowerCase();
        return t.contains("writing") || t.contains("translation") || "写作".equals(type) || "翻译".equals(type);
    }

    private boolean isWriting(String type) {
        if (type == null) return false;
        String t = type.trim().toLowerCase();
        return t.contains("writing") || "写作".equals(type);
    }

    private String callWorkflow(JsonNode inputNode) throws JsonProcessingException {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("input", inputNode);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("workflow_id", tutoringWorkflowId);
        payload.put("parameters", params);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + cozeApiToken);

        String body = objectMapper.writeValueAsString(payload);
        log.info("[TutoringAnalysis] 即将调用 Coze workflow={}, payloadLength={}", tutoringWorkflowId, body.length());
        ResponseEntity<String> response = restTemplate.exchange(cozeApiUrl, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        log.info("[TutoringAnalysis] Coze 调用完成 workflow={}, status={}, bodyLength={}", tutoringWorkflowId, response.getStatusCodeValue(), response.getBody() == null ? 0 : response.getBody().length());
        return response.getBody();
    }

    private String extractAnalysisContent(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode dataNode = root.path("data");
            JsonNode outputNode = dataNode.path("output");
            if (!outputNode.isMissingNode() && !outputNode.isNull()) {
                return outputNode.isTextual() ? outputNode.asText() : objectMapper.writeValueAsString(outputNode);
            }
            JsonNode rootOutput = root.get("output");
            if (rootOutput != null && !rootOutput.isNull()) {
                return rootOutput.isTextual() ? rootOutput.asText() : objectMapper.writeValueAsString(rootOutput);
            }
            return body;
        } catch (Exception e) {
            return body;
        }
    }

    private void storeAnalysisResult(Long userId, String analysisContent, List<Long> testIds) throws JsonProcessingException {
        String collectionId = ensureTutoringCollectionId();
        String url = collectionAddUrl(collectionId);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("user_id", userId);
        // Chroma metadata 要求简单标量，改为逗号分隔字符串避免反序列化错误
        metadata.put("test_en_ids", testIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        metadata.put("created_time", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()));
        metadata.put("type", "analysis");

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("ids", List.of(userId + "analysis"));
        requestBody.put("documents", List.of(analysisContent));
        requestBody.put("metadatas", List.of(metadata));
        requestBody.put("embeddings", List.of(generateDeterministicEmbedding(userId + "analysis")));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        log.info("[TutoringAnalysis] 即将写入 Chroma 分析结果，userId={}, collection={}, collectionId={}, payloadLength={}",
            userId, tutoringCollectionName, collectionId, jsonBody.length());
        ResponseEntity<String> response = restTemplate.exchange(URI.create(url), HttpMethod.POST, new HttpEntity<>(jsonBody, headers), String.class);
        log.info("[TutoringAnalysis] 已写入 Chroma 分析结果，userId={}, collection={}, collectionId={}, status={}, bodyLength={}, metadata={}",
            userId, tutoringCollectionName, collectionId, response.getStatusCodeValue(), response.getBody() == null ? 0 : response.getBody().length(), metadata);
    }

    private String ensureTutoringCollectionId() throws JsonProcessingException {
        String cached = tutoringCollectionIdCache.get();
        if (cached != null) {
            return cached;
        }
        synchronized (tutoringCollectionIdCache) {
            cached = tutoringCollectionIdCache.get();
            if (cached != null) {
                return cached;
            }
            String collectionId = fetchCollectionId(tutoringCollectionName);
            if (collectionId == null) {
                collectionId = createCollection(tutoringCollectionName, "CET4 tutoring analysis content");
            }
            tutoringCollectionIdCache.set(collectionId);
            return collectionId;
        }
    }

    private String fetchCollectionId(String collectionName) throws JsonProcessingException {
        String url = chromaBaseUrl + "/tenants/" + tenant + "/databases/" + database + "/collections";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }
            JsonNode collections = objectMapper.readTree(response.getBody());
            if (collections.isArray()) {
                for (JsonNode collection : collections) {
                    if (collectionName.equals(collection.get("name").asText())) {
                        return collection.get("id").asText();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取 ChromaDB 集合失败: {}", e.getMessage());
        }
        return null;
    }

    private String createCollection(String collectionName, String description) throws JsonProcessingException {
        String url = chromaBaseUrl + "/tenants/" + tenant + "/databases/" + database + "/collections";

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("name", collectionName);
        requestBody.put("metadata", Collections.singletonMap("description", description));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("创建 ChromaDB 集合失败: " + collectionName);
        }

        JsonNode responseNode = objectMapper.readTree(response.getBody());
        String collectionId = responseNode.get("id").asText();
        log.info("创建 ChromaDB 集合成功: {} (id: {})", collectionName, collectionId);
        return collectionId;
    }

    private String collectionAddUrl(String collectionId) {
        return chromaBaseUrl + "/tenants/" + tenant + "/databases/" + database +
                "/collections/" + collectionId + "/add";
    }

    private List<Float> generateDeterministicEmbedding(String id) {
        List<Float> embedding = new ArrayList<>(EMBEDDING_DIMENSION);
        java.util.Random random = new java.util.Random(Objects.hashCode(id));
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            embedding.add(random.nextFloat());
        }
        return embedding;
    }
}
