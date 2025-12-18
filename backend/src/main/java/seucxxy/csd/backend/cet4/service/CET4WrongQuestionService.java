package seucxxy.csd.backend.cet4.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import seucxxy.csd.backend.cet4.dto.CET4WrongQuestionRecordDTO;
import seucxxy.csd.backend.cet4.entity.CET4UserTestRecordDetailEn;
import seucxxy.csd.backend.cet4.mapper.CET4UserTestRecordDetailEnMapper;
import seucxxy.csd.backend.cet4.mapper.CET4UserTestRecordEnMapper;
import seucxxy.csd.backend.cet4.mapper.CET4UserTestRecordSegmentEnMapper;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 错题服务
 * 提供错题查询功能
 */
@Service
public class CET4WrongQuestionService {

    private static final Logger logger = LoggerFactory.getLogger(CET4WrongQuestionService.class);

    private final CET4UserTestRecordSegmentEnMapper segmentMapper;
    private final CET4UserTestRecordDetailEnMapper detailMapper;
    private final CET4UserTestRecordEnMapper testRecordMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    // Collection ID缓存
    private final AtomicReference<String> collectionIdCache = new AtomicReference<>();
    private final AtomicReference<String> answerCollectionIdCache = new AtomicReference<>();
    private final AtomicReference<String> gradeCollectionIdCache = new AtomicReference<>();

    @Value("${chromadb.base-url:http://localhost:8000/api/v2}")
    private String chromaBaseUrl;

    @Value("${chromadb.tenant:default_tenant}")
    private String chromaTenant;

    @Value("${chromadb.database:default_database}")
    private String chromaDatabase;

    @Value("${chromadb.collection.eng-exam-papers:eng_exam_papers_cet4}")
    private String chromaCollectionName;
    
    @Value("${chromadb.collection.user-test-record:user_test_record_cet4}")
    private String answerCollectionName;
    
    @Value("${chromadb.collection.user-test-grade-record:user_test_grade_record_cet4}")
    private String gradeCollectionName;

    @Autowired
    public CET4WrongQuestionService(
            CET4UserTestRecordSegmentEnMapper segmentMapper,
            CET4UserTestRecordDetailEnMapper detailMapper,
            CET4UserTestRecordEnMapper testRecordMapper,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.segmentMapper = segmentMapper;
        this.detailMapper = detailMapper;
        this.testRecordMapper = testRecordMapper;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 获取用户错题列表
     * 
     * @param userId 用户ID
     * @param sortBy 排序依据: "time" 或 "percent"
     * @param sortOrder 排序顺序: "ASC" 或 "DESC"
     * @return 错题记录列表
     */
    public List<CET4WrongQuestionRecordDTO> getWrongQuestions(Long userId, String sortBy, String sortOrder) {
        // 验证sortOrder参数
        if (!"ASC".equalsIgnoreCase(sortOrder) && !"DESC".equalsIgnoreCase(sortOrder)) {
            sortOrder = "DESC"; // 默认倒序
        }

        List<CET4WrongQuestionRecordDTO> result;
        
        if ("time".equalsIgnoreCase(sortBy)) {
            result = segmentMapper.findWrongQuestionsByUserIdOrderByTime(userId, sortOrder);
        } else if ("percent".equalsIgnoreCase(sortBy)) {
            result = segmentMapper.findWrongQuestionsByUserIdOrderByPercent(userId, sortOrder);
        } else {
            result = segmentMapper.findWrongQuestionsByUserIdOrderByTime(userId, "DESC");
        }
        
        return result;
    }

    /**
     * 获取用户错题总数
     * 
     * @param userId 用户ID
     * @return 错题总数
     */
    public int countWrongQuestions(Long userId) {
        return segmentMapper.countWrongQuestionsByUserId(userId);
    }

    /**
     * 分页获取用户错题列表
     * 
     * @param userId 用户ID
     * @param sortBy 排序依据: "time" 或 "percent"
     * @param sortOrder 排序顺序: "ASC" 或 "DESC"
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @return 错题记录列表
     */
    public List<CET4WrongQuestionRecordDTO> getWrongQuestionsWithPagination(
            Long userId, String sortBy, String sortOrder, int page, int pageSize) {
        // 验证sortOrder参数
        if (!"ASC".equalsIgnoreCase(sortOrder) && !"DESC".equalsIgnoreCase(sortOrder)) {
            sortOrder = "DESC"; // 默认倒序
        }
        
        // 计算偏移量
        int offset = (page - 1) * pageSize;
        
        List<CET4WrongQuestionRecordDTO> result;
        
        if ("time".equalsIgnoreCase(sortBy)) {
            result = segmentMapper.findWrongQuestionsByUserIdOrderByTimeWithPagination(
                    userId, sortOrder, pageSize, offset);
        } else if ("percent".equalsIgnoreCase(sortBy)) {
            result = segmentMapper.findWrongQuestionsByUserIdOrderByPercentWithPagination(
                    userId, sortOrder, pageSize, offset);
        } else {
            result = segmentMapper.findWrongQuestionsByUserIdOrderByTimeWithPagination(
                    userId, "DESC", pageSize, offset);
        }
        
        return result;
    }

    /**
     * 获取错题详情
     * 从ChromaDB获取题目内容，从数据库或ChromaDB获取用户答案
     * 
     * @param testEnId 考试ID
     * @param segmentId 段落ID
     * @param userId 用户ID
     * @return 错题详情
     */
    public Map<String, Object> getWrongQuestionDetail(Long testEnId, String segmentId, Long userId) throws Exception {
        Map<String, Object> result = new HashMap<>();

        try {
            logger.info("开始查询错题详情 - testEnId: {}, segmentId: {}, userId: {}", testEnId, segmentId, userId);
            
            // 1. 获取考试记录，得到exam_paper_en_id
            var testRecord = testRecordMapper.getUserTestRecordEnById(testEnId);
            if (testRecord == null) {
                logger.error("找不到考试记录: {}", testEnId);
                throw new RuntimeException("找不到考试记录: " + testEnId);
            }
            Long examPaperEnId = testRecord.getExamPaperEnId();
            logger.info("获取到考试试卷ID: {}", examPaperEnId);

            // 2. 从ChromaDB查询题目内容
            Map<String, Object> questionContent = queryQuestionFromChroma(examPaperEnId, segmentId);
            if (questionContent == null) {
                logger.error("从ChromaDB查询题目内容返回null");
                throw new RuntimeException("无法从ChromaDB获取题目内容");
            }
            result.put("questionContent", questionContent);
            logger.info("查询到题目内容: {}", questionContent.keySet());

            // 3. 判断题型，从不同的数据源获取用户答案
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) questionContent.get("metadata");
            String questionType = metadata != null ? (String) metadata.get("question_type") : null;
            logger.info("题目类型: {}", questionType);
        
            if ("Writing".equals(questionType) || "Translation".equals(questionType)) {
                // 主观题：从ChromaDB的user_test_record_cet4集合查询学生答案和AI评分
                logger.info("识别为主观题，查询ChromaDB答案");
                Map<String, Object> subjectiveData = querySubjectiveAnswerFromChroma(testEnId, examPaperEnId, segmentId);
                result.put("userAnswer", subjectiveData.get("userAnswer"));
                result.put("aiScore", subjectiveData.get("score"));
                result.put("aiFeedback", subjectiveData.get("feedback"));
                result.put("aiReasoning", subjectiveData.get("reasoning"));
                result.put("userAnswers", new HashMap<>()); // 主观题没有按题号的答案映射
                result.put("correctAnswers", new HashMap<>());
            } else {
                // 客观题：从数据库user_test_record_detail_en表查询答案
                logger.info("识别为客观题，从数据库查询答案");
                List<CET4UserTestRecordDetailEn> userAnswers = detailMapper.getUserTestRecordDetailsEnByTestEnId(testEnId);
                logger.info("从数据库查询到 {} 条答案记录", userAnswers != null ? userAnswers.size() : 0);
                
                // 筛选出该segment的答案
                Map<String, String> answerMap = new HashMap<>();
                Map<String, String> correctAnswerMap = new HashMap<>();
                if (userAnswers != null) {
                    for (CET4UserTestRecordDetailEn detail : userAnswers) {
                        if (segmentId.equals(detail.getSegmentId())) {
                            String questionNumber = String.valueOf(detail.getQuestionsEnNumber());
                            answerMap.put(questionNumber, detail.getUserAnswer());
                            if (detail.getCorrectAnswer() != null && !detail.getCorrectAnswer().isEmpty()) {
                                correctAnswerMap.put(questionNumber, detail.getCorrectAnswer());
                            }
                        }
                    }
                }
                logger.info("筛选出该segment的 {} 道题目答案", answerMap.size());
                result.put("userAnswers", answerMap);
                result.put("correctAnswers", correctAnswerMap);
            }

            logger.info("✅ 错题详情查询完成");
            return result;
            
        } catch (Exception e) {
            logger.error("❌ 查询错题详情失败: testEnId={}, segmentId={}, error={}", 
                testEnId, segmentId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 从ChromaDB查询题目内容
     */
    private Map<String, Object> queryQuestionFromChroma(Long examPaperEnId, String segmentId) throws Exception {
        logger.info("从ChromaDB查询题目 - examPaperEnId: {}, segmentId: {}", examPaperEnId, segmentId);

        // 1. 获取collection ID
        String collectionId = ensureCollectionId();
        
        // 2. 构建查询URL - 使用正确的API路径
        String getUrl = chromaBaseUrl + "/tenants/" + chromaTenant + "/databases/" + chromaDatabase 
                + "/collections/" + collectionId + "/get";

        // 3. 构建查询请求体
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("limit", 1);
        requestBody.put("include", Arrays.asList("documents", "metadatas"));

        // where条件：exam_paper_en_id 和 segment_id
        // ChromaDB的$and需要包含多个独立的条件对象
        Map<String, Object> condition1 = new LinkedHashMap<>();
        condition1.put("exam_paper_en_id", examPaperEnId.intValue());
        
        Map<String, Object> condition2 = new LinkedHashMap<>();
        condition2.put("segment_id", segmentId);
        
        Map<String, Object> whereClause = new LinkedHashMap<>();
        whereClause.put("$and", Arrays.asList(condition1, condition2));
        requestBody.put("where", whereClause);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        logger.debug("ChromaDB查询URL: {}", getUrl);
        logger.debug("查询条件: {}", jsonBody);

        ResponseEntity<String> response = restTemplate.exchange(
                URI.create(getUrl),
                HttpMethod.POST,
                entity,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            logger.error("从ChromaDB查询失败，状态码: {}", response.getStatusCode());
            throw new RuntimeException("从ChromaDB查询失败");
        }

        // 解析响应
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode documents = root.path("documents");
        JsonNode metadatas = root.path("metadatas");

        if (!documents.isArray() || documents.size() == 0) {
            logger.warn("ChromaDB中找不到对应的题目: examPaperEnId={}, segmentId={}", examPaperEnId, segmentId);
            throw new RuntimeException("ChromaDB中找不到对应的题目");
        }

        // 解析document内容（JSON格式）
        String documentStr = documents.get(0).asText();
        Map<String, Object> documentData = objectMapper.readValue(
            documentStr,
            new TypeReference<Map<String, Object>>() {}
        );

        // 添加metadata信息
        if (metadatas.isArray() && metadatas.size() > 0) {
            JsonNode metadata = metadatas.get(0);
            documentData.put("metadata", objectMapper.convertValue(metadata, Map.class));
        }

        logger.info("✅ ChromaDB查询成功");
        return documentData;
    }
    
    /**
     * 确保collection存在并返回ID
     */
    private String ensureCollectionId() throws Exception {
        String cached = collectionIdCache.get();
        if (cached != null) {
            return cached;
        }
        
        synchronized (collectionIdCache) {
            cached = collectionIdCache.get();
            if (cached != null) {
                return cached;
            }
            
            String collectionId = fetchCollectionId();
            if (collectionId == null) {
                throw new RuntimeException("ChromaDB集合不存在: " + chromaCollectionName);
            }
            
            collectionIdCache.set(collectionId);
            return collectionId;
        }
    }
    
    /**
     * 获取collection ID
     */
    private String fetchCollectionId() throws Exception {
        String url = chromaBaseUrl + "/tenants/" + chromaTenant + "/databases/" + chromaDatabase + "/collections";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(URI.create(url), HttpMethod.GET, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }
            
            JsonNode collections = objectMapper.readTree(response.getBody());
            if (collections.isArray()) {
                for (JsonNode collection : collections) {
                    if (chromaCollectionName.equals(collection.get("name").asText())) {
                        return collection.get("id").asText();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("获取ChromaDB集合失败: {}", e.getMessage(), e);
            throw e;
        }
        return null;
    }
    
    /**
     * 从ChromaDB查询主观题(写作/翻译)的学生答案和AI评分
     */
    private Map<String, Object> querySubjectiveAnswerFromChroma(Long testEnId, Long examPaperEnId, String segmentId) throws Exception {
        Map<String, Object> result = new HashMap<>();
        result.put("userAnswer", "");
        result.put("score", null);
        result.put("feedback", "");
        result.put("reasoning", "");
        
        logger.info("从ChromaDB查询主观题答案 - testEnId: {}, examPaperEnId: {}, segmentId: {}", testEnId, examPaperEnId, segmentId);
        
        try {
            // 1. 从user_test_record_cet4集合查询学生答案
            String answerCollectionId = fetchAnswerCollectionId();
            if (answerCollectionId != null) {
                String userAnswer = queryUserAnswerFromChroma(answerCollectionId, testEnId, examPaperEnId, segmentId);
                if (userAnswer != null) {
                    result.put("userAnswer", userAnswer);
                    logger.info("✅ 查询到学生答案,长度: {}", userAnswer.length());
                }
            }
            
            // 2. 从user_test_grade_record_cet4集合查询AI评分
            String gradeCollectionId = fetchGradeCollectionId();
            if (gradeCollectionId != null) {
                Map<String, Object> aiGrade = queryAIGradeFromChroma(gradeCollectionId, testEnId, examPaperEnId, segmentId);
                if (aiGrade != null) {
                    result.putAll(aiGrade);
                    logger.info("✅ 查询到AI评分: {}", aiGrade.get("score"));
                }
            }
            
        } catch (Exception e) {
            logger.error("查询主观题答案失败: {}", e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * 获取答案集合ID
     */
    private String fetchAnswerCollectionId() throws Exception {
        return fetchCollectionIdByName("user_test_record_cet4");
    }
    
    /**
     * 获取评分集合ID
     */
    private String fetchGradeCollectionId() throws Exception {
        return fetchCollectionIdByName("user_test_grade_record_cet4");
    }
    
    /**
     * 根据集合名称获取集合ID
     */
    private String fetchCollectionIdByName(String collectionName) throws Exception {
        String url = chromaBaseUrl + "/tenants/" + chromaTenant + "/databases/" + chromaDatabase + "/collections";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(URI.create(url), HttpMethod.GET, entity, String.class);
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
        return null;
    }
    
    /**
     * 从ChromaDB查询学生答案
     */
    private String queryUserAnswerFromChroma(String collectionId, Long testEnId, Long examPaperEnId, String segmentId) throws Exception {
        String getUrl = chromaBaseUrl + "/tenants/" + chromaTenant + "/databases/" + chromaDatabase 
                + "/collections/" + collectionId + "/get";
        
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("limit", 1);
        requestBody.put("include", Arrays.asList("documents", "metadatas"));
        
        // where条件
        Map<String, Object> cond1 = new LinkedHashMap<>();
        cond1.put("test_en_id", testEnId.intValue());
        
        Map<String, Object> cond2 = new LinkedHashMap<>();
        cond2.put("exam_paper_en_id", examPaperEnId.intValue());
        
        Map<String, Object> cond3 = new LinkedHashMap<>();
        cond3.put("segment_id", segmentId);
        
        Map<String, Object> whereClause = new LinkedHashMap<>();
        whereClause.put("$and", Arrays.asList(cond1, cond2, cond3));
        requestBody.put("where", whereClause);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        
        logger.debug("查询学生答案URL: {}", getUrl);
        logger.debug("查询条件: {}", jsonBody);
        
        ResponseEntity<String> response = restTemplate.exchange(URI.create(getUrl), HttpMethod.POST, entity, String.class);
        
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return null;
        }
        
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode documents = root.path("documents");
        
        if (documents.isArray() && documents.size() > 0) {
            return documents.get(0).asText();
        }
        
        return null;
    }
    
    /**
     * 从ChromaDB查询AI评分结果
     */
    private Map<String, Object> queryAIGradeFromChroma(String collectionId, Long testEnId, Long examPaperEnId, String segmentId) throws Exception {
        String getUrl = chromaBaseUrl + "/tenants/" + chromaTenant + "/databases/" + chromaDatabase 
                + "/collections/" + collectionId + "/get";
        
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("limit", 1);
        requestBody.put("include", Arrays.asList("documents", "metadatas"));
        
        // where条件
        Map<String, Object> cond1 = new LinkedHashMap<>();
        cond1.put("test_en_id", testEnId.intValue());
        
        Map<String, Object> cond2 = new LinkedHashMap<>();
        cond2.put("exam_paper_en_id", examPaperEnId.intValue());
        
        Map<String, Object> cond3 = new LinkedHashMap<>();
        cond3.put("segment_id", segmentId);
        
        Map<String, Object> whereClause = new LinkedHashMap<>();
        whereClause.put("$and", Arrays.asList(cond1, cond2, cond3));
        requestBody.put("where", whereClause);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        
        logger.debug("查询AI评分URL: {}", getUrl);
        logger.debug("查询条件: {}", jsonBody);
        
        ResponseEntity<String> response = restTemplate.exchange(URI.create(getUrl), HttpMethod.POST, entity, String.class);
        
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return null;
        }
        
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode documents = root.path("documents");
        
        if (documents.isArray() && documents.size() > 0) {
            String aiGradeJson = documents.get(0).asText();
            if (aiGradeJson != null && !aiGradeJson.isEmpty()) {
                return objectMapper.readValue(
                    aiGradeJson,
                    new TypeReference<Map<String, Object>>() {}
                );
            }
        }
        
        return null;
    }
}
