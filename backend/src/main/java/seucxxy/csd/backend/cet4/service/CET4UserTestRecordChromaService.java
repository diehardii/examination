package seucxxy.csd.backend.cet4.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * CET4用户测试记录ChromaDB存储服务
 * 用于存储非客观题的答案和AI评分结果到ChromaDB
 */
@Service
public class CET4UserTestRecordChromaService {

    private static final Logger logger = LoggerFactory.getLogger(CET4UserTestRecordChromaService.class);
    private static final int EMBEDDING_DIMENSION = 384;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<String> answerCollectionIdCache = new AtomicReference<>();
    private final AtomicReference<String> gradeCollectionIdCache = new AtomicReference<>();

    @Value("${chromadb.base-url:http://localhost:8000/api/v2}")
    private String chromaBaseUrl;

    @Value("${chromadb.tenant:default_tenant}")
    private String tenant;

    @Value("${chromadb.database:default_database}")
    private String database;

    @Value("${chromadb.collection.user-test-record:user_test_record_cet4}")
    private String answerCollectionName;

    @Value("${chromadb.collection.user-test-grade:user_test_grade_record_cet4}")
    private String gradeCollectionName;

    @Autowired
    private seucxxy.csd.backend.common.mapper.SubjectsEnMapper subjectsEnMapper;

    public CET4UserTestRecordChromaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 存储学生非客观题的答案到ChromaDB
     * 
     * @param userAnswer 学生答案
     * @param subject 英语等级（CET4, CET6等）
     * @param testEnId 考试记录ID
     * @param userId 学生ID
     * @param userName 学生姓名
     * @param questionType 题目类型
     * @param examPaperEnId 试卷编号
     * @param examPaperEnName 试卷名称
     * @param partId 试卷第几部分
     * @param segmentId 第几部分第几段
     * @param score 本次得分
     */
    public void storeUserAnswer(
            String userAnswer,
            Integer subjectEnId,
            Long testEnId,
            Long userId,
            String userName,
            String questionType,
            Long examPaperEnId,
            String examPaperEnName,
            Integer partId,
            String segmentId,
            Double score) throws JsonProcessingException {
        
        // 生成answer_id: exam_paper_en_id + segment_id + test_en_id
        String answerId = examPaperEnId + "_" + segmentId + "_" + testEnId;
        String createdTime = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());

        // 构建metadata
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("subjectEnId", subjectEnId);
        metadata.put("test_en_id", testEnId);
        metadata.put("user_id", userId);
        metadata.put("user_name", userName);
        metadata.put("question_type", questionType);
        metadata.put("exam_paper_en_id", examPaperEnId);
        metadata.put("exam_paper_en_name", examPaperEnName);
        metadata.put("part_id", partId);
        metadata.put("segment_id", segmentId);
        metadata.put("score", score);
        metadata.put("answer_id", answerId);
        metadata.put("created_time", createdTime);

        // 查询 subjectEnName
        String subjectEnName = "";
        try {
            seucxxy.csd.backend.common.entity.SubjectsEn subjectEntity = subjectsEnMapper.findById(subjectEnId != null ? subjectEnId.longValue() : null);
            if (subjectEntity != null) {
                subjectEnName = subjectEntity.getSubjectEnName();
            }
        } catch (Exception ex) {
            
        }
        metadata.put("subjectEnName", subjectEnName);

        // 构建请求体
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("ids", Collections.singletonList(answerId));
        requestBody.put("documents", Collections.singletonList(userAnswer));
        requestBody.put("metadatas", Collections.singletonList(metadata));
        requestBody.put("embeddings", Collections.singletonList(generateDeterministicEmbedding(answerId)));

        // 发送到ChromaDB
        String collectionId = ensureAnswerCollectionId();
        String url = collectionAddUrl(collectionId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(URI.create(url), HttpMethod.POST, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            logger.warn("写入学生答案到 ChromaDB 失败，status={}, body={}",
                    response.getStatusCode(), response.getBody());
            throw new IllegalStateException("写入 ChromaDB 失败，状态码：" + response.getStatusCode());
        }
    }

    /**
     * 存储AI评分结果到ChromaDB
     * 
     * @param aiGradeResult AI评分结果（包含score, feedback, reasoning等）
     * @param subject 英语等级（CET4, CET6等）
     * @param testEnId 考试记录ID
     * @param userId 学生ID
     * @param userName 学生姓名
     * @param questionType 题目类型
     * @param examPaperEnId 试卷编号
     * @param examPaperEnName 试卷名称
     * @param partId 试卷第几部分
     * @param segmentId 第几部分第几段
     * @param score 本次得分
     */
    public void storeAIGradeResult(
            Map<String, Object> aiGradeResult,
            Integer subjectEnId,
            Long testEnId,
            Long userId,
            String userName,
            String questionType,
            Long examPaperEnId,
            String examPaperEnName,
            Integer partId,
            String segmentId,
            Double score) throws JsonProcessingException {
        
        // 生成grade_id: exam_paper_en_id + segment_id + test_en_id
        String gradeId = examPaperEnId + "_" + segmentId + "_" + testEnId;
        String createdTime = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());

        // 构建metadata
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("subject", subjectEnId);
        metadata.put("test_en_id", testEnId);
        metadata.put("user_id", userId);
        metadata.put("user_name", userName);
        metadata.put("question_type", questionType);
        metadata.put("exam_paper_en_id", examPaperEnId);
        metadata.put("exam_paper_en_name", examPaperEnName);
        metadata.put("part_id", partId);
        metadata.put("segment_id", segmentId);
        metadata.put("score", score);
        metadata.put("grade_id", gradeId);
        metadata.put("created_time", createdTime);

        // 将AI评分结果转换为JSON字符串作为document
        String aiGradeResultJson = objectMapper.writeValueAsString(aiGradeResult);

        // 构建请求体
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("ids", Collections.singletonList(gradeId));
        requestBody.put("documents", Collections.singletonList(aiGradeResultJson));
        requestBody.put("metadatas", Collections.singletonList(metadata));
        requestBody.put("embeddings", Collections.singletonList(generateDeterministicEmbedding(gradeId)));

        // 发送到ChromaDB
        String collectionId = ensureGradeCollectionId();
        String url = collectionAddUrl(collectionId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(URI.create(url), HttpMethod.POST, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            logger.warn("写入AI评分结果到 ChromaDB 失败，status={}, body={}",
                    response.getStatusCode(), response.getBody());
            throw new IllegalStateException("写入 ChromaDB 失败，状态码：" + response.getStatusCode());
        }
    }

    /**
     * 确保答案集合存在并返回集合ID
     */
    private String ensureAnswerCollectionId() throws JsonProcessingException {
        String cached = answerCollectionIdCache.get();
        if (cached != null) {
            return cached;
        }
        synchronized (answerCollectionIdCache) {
            cached = answerCollectionIdCache.get();
            if (cached != null) {
                return cached;
            }
            String collectionId = fetchCollectionId(answerCollectionName);
            if (collectionId == null) {
                // 集合不存在，创建集合
                collectionId = createCollection(answerCollectionName, "CET4用户测试记录 - 学生答案");
            }
            answerCollectionIdCache.set(collectionId);
            return collectionId;
        }
    }

    /**
     * 确保评分集合存在并返回集合ID
     */
    private String ensureGradeCollectionId() throws JsonProcessingException {
        String cached = gradeCollectionIdCache.get();
        if (cached != null) {
            return cached;
        }
        synchronized (gradeCollectionIdCache) {
            cached = gradeCollectionIdCache.get();
            if (cached != null) {
                return cached;
            }
            String collectionId = fetchCollectionId(gradeCollectionName);
            if (collectionId == null) {
                // 集合不存在，创建集合
                collectionId = createCollection(gradeCollectionName, "CET4用户测试记录 - AI评分结果");
            }
            gradeCollectionIdCache.set(collectionId);
            return collectionId;
        }
    }

    /**
     * 获取集合ID
     */
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

            com.fasterxml.jackson.databind.JsonNode collections = objectMapper.readTree(response.getBody());
            if (collections.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode collection : collections) {
                    if (collectionName.equals(collection.get("name").asText())) {
                        return collection.get("id").asText();
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("获取 ChromaDB 集合失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 创建新集合
     */
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

        com.fasterxml.jackson.databind.JsonNode responseNode = objectMapper.readTree(response.getBody());
        String collectionId = responseNode.get("id").asText();
        logger.info("创建 ChromaDB 集合成功: {} (id: {})", collectionName, collectionId);
        return collectionId;
    }

    /**
     * 构建添加数据的URL
     */
    private String collectionAddUrl(String collectionId) {
        return chromaBaseUrl + "/tenants/" + tenant + "/databases/" + database + 
               "/collections/" + collectionId + "/add";
    }

    /**
     * 生成确定性的嵌入向量（基于ID的哈希）
     */
    private List<Float> generateDeterministicEmbedding(String id) {
        List<Float> embedding = new ArrayList<>(EMBEDDING_DIMENSION);
        Random random = new Random(id.hashCode());
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            embedding.add(random.nextFloat());
        }
        return embedding;
    }
    
    /**
     * 从ChromaDB查询学生答案和AI评分结果
     * 
     * @param testEnId 考试记录ID
     * @param examPaperEnId 试卷ID
     * @return Map<segmentId, Map<"userAnswer"|"aiScore"|"aiFeedback"|"aiReasoning", Object>>
     */
    public Map<String, Map<String, Object>> fetchUserAnswersAndGrades(Long testEnId, Long examPaperEnId) {
        Map<String, Map<String, Object>> result = new HashMap<>();
        
        try {
            // 查询学生答案
            Map<String, String> userAnswers = fetchUserAnswersByTestId(testEnId, examPaperEnId);
            
            // 查询AI评分结果
            Map<String, Map<String, Object>> aiGrades = fetchAIGradesByTestId(testEnId, examPaperEnId);
            
            // 合并结果
            for (String segmentId : userAnswers.keySet()) {
                Map<String, Object> data = new HashMap<>();
                data.put("userAnswer", userAnswers.get(segmentId));
                
                if (aiGrades.containsKey(segmentId)) {
                    data.putAll(aiGrades.get(segmentId));
                }
                
                result.put(segmentId, data);
            }
            
        } catch (Exception e) {
            logger.error("从ChromaDB查询学生答案和AI评分失败: {}", e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * 从ChromaDB查询学生答案
     */
    private Map<String, String> fetchUserAnswersByTestId(Long testEnId, Long examPaperEnId) throws Exception {
        Map<String, String> answers = new HashMap<>();
        
        String collectionId = ensureAnswerCollectionId();
        String url = collectionGetUrl(collectionId);
        
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("limit", 1000);
        requestBody.put("include", List.of("documents", "metadatas"));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(URI.create(url), HttpMethod.POST, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return answers;
        }
        
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response.getBody());
        com.fasterxml.jackson.databind.JsonNode documents = root.path("documents");
        com.fasterxml.jackson.databind.JsonNode metadatas = root.path("metadatas");
        
        if (!documents.isArray() || !metadatas.isArray()) {
            return answers;
        }
        
        for (int i = 0; i < Math.min(documents.size(), metadatas.size()); i++) {
            com.fasterxml.jackson.databind.JsonNode metaNode = metadatas.get(i);
            
            // 过滤:只返回匹配testEnId和examPaperEnId的记录
            Long metaTestEnId = metaNode.path("test_en_id").asLong(0L);
            Long metaExamPaperEnId = metaNode.path("exam_paper_en_id").asLong(0L);
            
            if (metaTestEnId.equals(testEnId) && metaExamPaperEnId.equals(examPaperEnId)) {
                String segmentId = metaNode.path("segment_id").asText("");
                String userAnswer = documents.get(i).asText("");
                
                if (!segmentId.isEmpty()) {
                    answers.put(segmentId, userAnswer);
                }
            }
        }
        
        return answers;
    }
    
    /**
     * 从ChromaDB查询AI评分结果
     */
    private Map<String, Map<String, Object>> fetchAIGradesByTestId(Long testEnId, Long examPaperEnId) throws Exception {
        Map<String, Map<String, Object>> grades = new HashMap<>();
        
        String collectionId = ensureGradeCollectionId();
        String url = collectionGetUrl(collectionId);
        
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("limit", 1000);
        requestBody.put("include", List.of("documents", "metadatas"));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(URI.create(url), HttpMethod.POST, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return grades;
        }
        
        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response.getBody());
        com.fasterxml.jackson.databind.JsonNode documents = root.path("documents");
        com.fasterxml.jackson.databind.JsonNode metadatas = root.path("metadatas");
        
        if (!documents.isArray() || !metadatas.isArray()) {
            return grades;
        }
        
        for (int i = 0; i < Math.min(documents.size(), metadatas.size()); i++) {
            com.fasterxml.jackson.databind.JsonNode metaNode = metadatas.get(i);
            
            // 过滤:只返回匹配testEnId和examPaperEnId的记录
            Long metaTestEnId = metaNode.path("test_en_id").asLong(0L);
            Long metaExamPaperEnId = metaNode.path("exam_paper_en_id").asLong(0L);
            
            if (metaTestEnId.equals(testEnId) && metaExamPaperEnId.equals(examPaperEnId)) {
                String segmentId = metaNode.path("segment_id").asText("");
                String aiGradeJson = documents.get(i).asText("");
                
                if (!segmentId.isEmpty() && !aiGradeJson.isEmpty()) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> aiGradeData = objectMapper.readValue(aiGradeJson, Map.class);
                        grades.put(segmentId, aiGradeData);
                    } catch (Exception e) {
                        logger.warn("解析AI评分JSON失败: {}", e.getMessage());
                    }
                }
            }
        }
        
        return grades;
    }
    
    /**
     * 构建查询数据的URL
     */
    private String collectionGetUrl(String collectionId) {
        return chromaBaseUrl + "/tenants/" + tenant + "/databases/" + database + 
               "/collections/" + collectionId + "/get";
    }
}
