package seucxxy.csd.backend.hs3.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * HS3高考英语试卷ChromaDB存储服务
 * 用于将解析后的试卷段落存储到ChromaDB的eng_exam_papers_hs3集合
 */
@Service("hs3ChromaEngExamPaperService")
public class HS3ChromaEngExamPaperService {

    private static final Logger logger = LoggerFactory.getLogger(HS3ChromaEngExamPaperService.class);
    private static final int EMBEDDING_DIMENSION = 384;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<String> collectionIdCache = new AtomicReference<>();

    @Value("${chromadb.base-url:http://localhost:8000/api/v2}")
    private String chromaBaseUrl;

    @Value("${chromadb.tenant:default_tenant}")
    private String tenant;

    @Value("${chromadb.database:default_database}")
    private String database;

    @Value("${chromadb.collection.eng-exam-papers-hs3:eng_exam_papers_hs3}")
    private String collectionName;

    public HS3ChromaEngExamPaperService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 存储高考试卷的所有大题到ChromaDB
     * @param examPaperId 试卷ID
     * @param examPaperName 试卷名称
     * @param subject 科目（当前为"高考"）
     * @param examPaperSource 试卷来源：real/AIfromreal/AIfromself/AIfromWrongBank
     * @param segments 大题列表（从Coze解析的JSON对象）
     * @throws JsonProcessingException 
     */
    public void storeExamPaperSegments(
            @NotNull Long examPaperId,
            @NotNull String examPaperName,
            @NotNull String subject,
            @NotNull String examPaperSource,
            @NotNull List<Map<String, Object>> segments) throws JsonProcessingException {
        
        if (segments == null || segments.isEmpty()) {
            throw new IllegalArgumentException("大题列表不能为空");
        }

        String createdTime = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());
        
        List<String> ids = new ArrayList<>();
        List<String> documents = new ArrayList<>();
        List<Map<String, Object>> metadatas = new ArrayList<>();
        List<List<Float>> embeddings = new ArrayList<>();

        for (Map<String, Object> segment : segments) {
            // 提取segment信息
            String partName = firstStringValue(segment, "partName", "part_name");
            Integer partNumber = firstIntValue(segment, "partNumber", "part_number", 1);
            String sectionName = firstStringValue(segment, "sectionName", "section_name");
            Integer sectionNumber = firstIntValue(segment, "sectionNumber", "section_number", 1);
            String segmentName = firstStringValue(segment, "segmentName", "segment_name");
            Integer segmentNumber = firstIntValue(segment, "segmentNumber", "segment_number", 1);
            String topic = getStringValue(segment, "topic", "unknown topic");
            
            // 构建metadata - 只使用segment_name，不使用segment_id
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("subject", subject);
            metadata.put("part_name", partName);
            metadata.put("part_number", partNumber);
            metadata.put("section_name", sectionName);
            metadata.put("section_number", sectionNumber);
            metadata.put("segment_name", segmentName);
            metadata.put("segment_number", segmentNumber);
            metadata.put("topic", topic);
            metadata.put("exam_paper_en_id", examPaperId.intValue());
            metadata.put("exam_paper_en_source", normalizeExamPaperSource(examPaperSource));
            metadata.put("exam_paper_en_name", examPaperName);
            metadata.put("created_time", createdTime);

            // 使用 examPaperId + segmentName 作为唯一ID，避免不同试卷相同segment_name覆盖
            String uniqueId = examPaperId + segmentName;
            ids.add(uniqueId);
            
            // 构建document - 存储完整的Coze output，排除前端额外添加的字段
            Map<String, Object> documentContent = new LinkedHashMap<>();
            // 需要排除的前端额外添加的字段（驼峰命名的冗余字段和其他非Coze输出字段）
            Set<String> excludeKeys = Set.of(
                "partName", "partNumber", "sectionName", "sectionNumber", 
                "segmentName", "segmentNumber",  // 驼峰命名的冗余字段
                "exam_paper_en_source", "original_document", "output", 
                "segment_index", "answers"  // 前端添加的其他字段
            );
            for (Map.Entry<String, Object> entry : segment.entrySet()) {
                if (!excludeKeys.contains(entry.getKey())) {
                    documentContent.put(entry.getKey(), entry.getValue());
                }
            }
            
            documents.add(objectMapper.writeValueAsString(documentContent));
            metadatas.add(metadata);
            embeddings.add(generateDeterministicEmbedding(uniqueId));

            logger.info("准备存储大题 - id: {}, segment_name: {}, topic: {}", 
                    uniqueId, segmentName, topic);
        }

        // 批量存储到ChromaDB
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("ids", ids);
        requestBody.put("documents", documents);
        requestBody.put("metadatas", metadatas);
        requestBody.put("embeddings", embeddings);

        logger.info("========== 准备发送HS3试卷到ChromaDB ==========");
        logger.info("试卷ID: {}, 试卷名称: {}, 大题数量: {}", examPaperId, examPaperName, ids.size());
        logger.info("================================================");

        String collectionId = ensureCollectionId();
        String url = collectionAddUrl(collectionId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        // 最多尝试10次，失败则抛错返回前端，提示智能体平台不可用
        final int maxAttempts = 10;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            long start = System.currentTimeMillis();
            try {
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
                long elapsed = System.currentTimeMillis() - start;

                if (!response.getStatusCode().is2xxSuccessful()) {
                    logger.error("HS3试卷存储到ChromaDB失败，attempt={}，status={}，body={}，耗时={}ms", 
                            attempt, response.getStatusCode(), safeBody(response), elapsed);
                    throw new RuntimeException("ChromaDB 存储失败: " + response.getStatusCode());
                }

                logger.info("HS3试卷成功存储到ChromaDB，共 {} 个大题，耗时={}ms，status={}，bodyLen={}，attempt={}",
                        ids.size(), elapsed, response.getStatusCodeValue(), 
                        response.getBody() == null ? 0 : response.getBody().length(), attempt);
                return; // 成功即返回
            } catch (Exception ex) {
                long elapsed = System.currentTimeMillis() - start;
                logger.error("HS3试卷存储到ChromaDB异常，attempt={}，耗时={}ms，url={}，error={}", 
                        attempt, elapsed, url, ex.getMessage(), ex);
                if (attempt == maxAttempts) {
                    // 达到最大重试次数仍失败，抛出明确异常供前端提示
                    throw new RuntimeException("智能体平台不可用，已重试" + maxAttempts + "次仍失败", ex);
                }
                try {
                    Thread.sleep(1000L * attempt); // 线性退避，避免频繁打满
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("智能体平台不可用，重试被中断", ie);
                }
            }
        }
    }

    private String safeBody(ResponseEntity<String> response) {
        if (response == null) return "<null response>";
        String body = response.getBody();
        if (body == null) return "<empty body>";
        return body.length() > 500 ? body.substring(0, 500) + "..." : body;
    }

    /**
     * 确保集合存在，返回集合ID
     */
    private String ensureCollectionId() throws JsonProcessingException {
        String cached = collectionIdCache.get();
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        // 1. 检查集合是否存在
        String listUrl = collectionsBaseUrl();
        ResponseEntity<String> listResponse = restTemplate.getForEntity(listUrl, String.class);
        
        if (listResponse.getStatusCode().is2xxSuccessful() && listResponse.getBody() != null) {
            JsonNode collections = objectMapper.readTree(listResponse.getBody());
            if (collections.isArray()) {
                for (JsonNode col : collections) {
                    String name = col.path("name").asText("");
                    if (collectionName.equals(name)) {
                        String id = col.path("id").asText("");
                        if (!id.isEmpty()) {
                            collectionIdCache.set(id);
                            logger.info("找到现有集合: {}, id: {}", collectionName, id);
                            return id;
                        }
                    }
                }
            }
        }

        // 2. 创建集合
        logger.info("集合 {} 不存在，开始创建...", collectionName);
        Map<String, Object> createBody = new LinkedHashMap<>();
        createBody.put("name", collectionName);
        
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("description", "HS3高考英语试卷存储集合");
        createBody.put("metadata", metadata);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = objectMapper.writeValueAsString(createBody);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        ResponseEntity<String> createResponse = restTemplate.postForEntity(listUrl, entity, String.class);
        if (!createResponse.getStatusCode().is2xxSuccessful() || createResponse.getBody() == null) {
            throw new RuntimeException("创建集合失败");
        }

        JsonNode created = objectMapper.readTree(createResponse.getBody());
        String id = created.path("id").asText("");
        if (id.isEmpty()) {
            throw new RuntimeException("创建集合后未返回ID");
        }

        collectionIdCache.set(id);
        logger.info("集合创建成功: {}, id: {}", collectionName, id);
        return id;
    }

    /**
     * 生成确定性的embedding向量（用于测试/占位）
     */
    private List<Float> generateDeterministicEmbedding(String text) {
        List<Float> embedding = new ArrayList<>(EMBEDDING_DIMENSION);
        int hash = text.hashCode();
        Random random = new Random(hash);
        
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            embedding.add(random.nextFloat() * 2 - 1);
        }
        
        // 归一化
        double sum = 0.0;
        for (Float val : embedding) {
            sum += val * val;
        }
        double norm = Math.sqrt(sum);
        if (norm > 0) {
            for (int i = 0; i < embedding.size(); i++) {
                embedding.set(i, (float) (embedding.get(i) / norm));
            }
        }
        
        return embedding;
    }

    private String collectionsBaseUrl() {
        // 与 CET4 一致：使用租户+数据库前缀，避免 404 (/collections 根路径无效)
        return String.format("%s/tenants/%s/databases/%s/collections",
                trimTrailingSlash(Objects.requireNonNull(chromaBaseUrl, "chromadb.base-url 未配置")),
                Objects.requireNonNull(tenant, "chromadb.tenant 未配置"),
                Objects.requireNonNull(database, "chromadb.database 未配置"));
    }

    private String collectionAddUrl(String collectionId) {
        return collectionsBaseUrl() + "/" + collectionId + "/add";
    }

    private String collectionGetUrl(String collectionId) {
        return collectionsBaseUrl() + "/" + collectionId + "/get";
    }

    /**
     * 根据试卷ID或名称从 ChromaDB 获取已存储的 HS3 试卷大题
     * 返回的列表元素包含 document (原始JSON字符串) 和 metadata
     */
    public List<Map<String, Object>> fetchExamPaperSegments(String examPaperId, String examPaperName) throws JsonProcessingException {
        String normalizedId = normalizeString(examPaperId);
        String normalizedName = normalizeString(examPaperName);
        if (normalizedId.isEmpty() && normalizedName.isEmpty()) {
            throw new IllegalArgumentException("试卷编号和试卷名称至少提供一个");
        }

        String collectionId = ensureCollectionId();
        String url = collectionGetUrl(collectionId);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("limit", 1000);
        // Chroma v2 支持 documents/metadatas/embeddings/distances/uris
        requestBody.put("include", List.of("documents", "metadatas"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(URI.create(url), HttpMethod.POST, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            logger.warn("从 ChromaDB 查询 HS3 试卷单元失败，status={}", response.getStatusCode());
            return Collections.emptyList();
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode documents = root.path("documents");
        JsonNode metadatas = root.path("metadatas");
        if (!documents.isArray() || !metadatas.isArray()) {
            return Collections.emptyList();
        }

        int size = Math.min(documents.size(), metadatas.size());

        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            JsonNode metaNode = metadatas.get(i);
            if (metaNode == null || !metaNode.isObject()) {
                continue;
            }

            String metaExamId = extractExamPaperId(metaNode);
            String metaExamName = getTextValue(metaNode, "exam_paper_en_name");

            boolean matches = false;
            if (!normalizedId.isEmpty() && normalizedId.equals(metaExamId)) {
                matches = true;
            }
            if (!normalizedName.isEmpty() && normalizedName.equals(metaExamName)) {
                matches = true;
            }

            if (!matches) {
                continue;
            }

            Map<String, Object> record = new LinkedHashMap<>();
            record.put("document", documents.get(i).asText(""));
            Map<String, Object> metadata = jsonNodeToMap(metaNode);
            record.put("metadata", metadata);
            records.add(record);
        }

        return records;
    }

    private String extractExamPaperId(JsonNode metaNode) {
        JsonNode examIdNode = metaNode.get("exam_paper_en_id");
        if (examIdNode == null) {
            examIdNode = metaNode.get("exam_paper_id");
        }
        if (examIdNode == null) {
            return "";
        }
        if (examIdNode.isNumber()) {
            return String.valueOf(examIdNode.asInt());
        }
        return examIdNode.asText("");
    }

    private String getTextValue(JsonNode node, String key) {
        if (node == null || !node.has(key)) return "";
        return node.get(key).asText("");
    }

    private Map<String, Object> jsonNodeToMap(JsonNode node) {
        if (node == null || node.isNull()) return Collections.emptyMap();
        return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString().trim();
    }

    private String firstStringValue(Map<String, Object> map, String primaryKey, String fallbackKey) {
        String val = getStringValue(map, primaryKey, "");
        if (val != null && !val.isEmpty()) {
            return val;
        }
        return getStringValue(map, fallbackKey, "");
    }

    private Integer getIntValue(Map<String, Object> map, String key, Integer defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            logger.warn("无法解析整数字段 {}: {}", key, value);
            return defaultValue;
        }
    }

    private Integer firstIntValue(Map<String, Object> map, String primaryKey, String fallbackKey, Integer defaultValue) {
        Integer val = getIntValue(map, primaryKey, null);
        if (val != null) {
            return val;
        }
        return getIntValue(map, fallbackKey, defaultValue);
    }

    private String normalizeExamPaperSource(String source) {
        String s = normalizeString(source);
        if (s.isEmpty()) return "real";
        if ("real".equalsIgnoreCase(s)) return "real";
        if ("AIfromreal".equalsIgnoreCase(s)) return "AIfromreal";
        if ("AIfromself".equalsIgnoreCase(s)) return "AIfromself";
        if ("AIfromWrongBank".equalsIgnoreCase(s)) return "AIfromWrongBank";
        return s;
    }

    private String normalizeString(String input) {
        return input == null ? "" : input.trim();
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return null;
        }
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
