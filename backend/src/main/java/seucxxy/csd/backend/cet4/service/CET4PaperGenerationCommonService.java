package seucxxy.csd.backend.cet4.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import seucxxy.csd.backend.cet4.dto.CET4SectionAChromaRecord;
import seucxxy.csd.backend.cet4.entity.CET4UserExamPaperEn;
import seucxxy.csd.backend.cet4.mapper.CET4TopicMapper;
import seucxxy.csd.backend.cet4.mapper.CET4UserExamPaperEnMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * CET4试卷生成公共服务
 * 包含 CET4IntensiveTrainService 和 CET4PaperGenService 共用的方法
 * 
 * 主要功能：
 * 1. Topic 管理（随机选择主题）
 * 2. ChromaDB 查询（样本文档获取）
 * 3. Coze Workflow 调用
 * 4. JSON 处理（清理转义引号、修复换行符）
 * 5. 日志记录
 */
@Service
public class CET4PaperGenerationCommonService {

    private static final Logger logger = LoggerFactory.getLogger(CET4PaperGenerationCommonService.class);

    private final CET4TopicMapper topicMapper;
    private final CET4ChromaEngExamPaperService chromaService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();
    private final CET4UserExamPaperEnMapper userExamPaperEnMapper;
    private final CET4PythonDeepSeekService pythonDeepSeekService;

    // Coze工作流配置
    private static final String COZE_WORKFLOW_ID = "7577681716829126656";
    private static final Path COZE_HTTP_LOG = Paths.get("logs", "coze-http.log");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Value("${coze.api.url}")
    private String cozeApiUrl;

    @Value("${coze.api.token.examinationAI}")
    private String cozeApiToken;

    @Autowired
    public CET4PaperGenerationCommonService(CET4TopicMapper topicMapper,
                                           CET4ChromaEngExamPaperService chromaService,
                                           RestTemplate restTemplate,
                                           CET4UserExamPaperEnMapper userExamPaperEnMapper,
                                           CET4PythonDeepSeekService pythonDeepSeekService) {
        this.topicMapper = topicMapper;
        this.chromaService = chromaService;
        this.restTemplate = restTemplate;
        this.userExamPaperEnMapper = userExamPaperEnMapper;
        this.pythonDeepSeekService = pythonDeepSeekService;
        this.objectMapper = new ObjectMapper();
    }

    // ==================== 用户-试卷绑定 ====================

    /**
     * 建立用户与试卷的归属关系，避免重复插入
     *
     * @param userId 当前用户ID
     * @param examPaperEnId 新生成的试卷ID
     */
    public void bindExamPaperToUser(Long userId, Long examPaperEnId) {
        if (userId == null || examPaperEnId == null) {
            return;
        }

        try {
            int existing = userExamPaperEnMapper.countByUserAndPaper(examPaperEnId, userId);
            if (existing > 0) {
                return;
            }
            CET4UserExamPaperEn relation = new CET4UserExamPaperEn(examPaperEnId, userId);
            userExamPaperEnMapper.insert(relation);
        } catch (Exception e) {
        }
    }

    // ==================== Topic 管理 ====================

    /**
     * 从数据库随机选择一个topic
     * 
     * @return 随机选中的主题
     * @throws RuntimeException 如果数据库中没有主题数据
     */
    public String getRandomTopic() {
        List<String> allTopics = topicMapper.selectAllTopics();
        if (allTopics == null || allTopics.isEmpty()) {
            throw new RuntimeException("数据库topics表中没有数据,无法生成题目");
        }
        return allTopics.get(random.nextInt(allTopics.size()));
    }

    /**
     * 获取所有主题并打乱顺序
     * 
     * @return 打乱顺序后的主题列表
     * @throws RuntimeException 如果数据库中没有主题数据
     */
    public List<String> getAllTopicsShuffled() {
        List<String> allTopics = topicMapper.selectAllTopics();
        if (allTopics == null || allTopics.isEmpty()) {
            throw new RuntimeException("topics表中没有数据");
        }
        Collections.shuffle(allTopics);
        return allTopics;
    }

    // ==================== ChromaDB 查询 ====================

    /**
     * 从ChromaDB根据试卷ID和题型获取题目单元
     * 注意：此方法需要由调用方提供具体实现，因为 CET4IntensiveTrainService 和 CET4PaperGenService
     * 使用不同的方式与 ChromaDB 交互
     * 
     * @param examPaperId 试卷ID
     * @param questionType 题型
     * @return 题目单元列表
     */
    public List<Map<String, Object>> fetchChromaUnitsByPaperIdAndType(String examPaperId, String questionType) {
        return queryExistingUnits(examPaperId, questionType);
    }

    /**
     * 从ChromaDB获取指定试卷的指定segment的文档
     * 
     * @param examPaperId 试卷ID
     * @param segmentId 片段ID
     * @param questionType 题型
     * @return 文档内容（JSON字符串）
     */
    public String getDocumentFromChroma(String examPaperId, String segmentId, String questionType) {
        try {
            List<CET4SectionAChromaRecord> records = chromaService.fetchExamPaperUnits(examPaperId, null);
            if (records == null) return null;
            
            for (CET4SectionAChromaRecord rec : records) {
                Map<String, Object> meta = rec.metadata();
                Object seg = meta.get("segment_id");
                Object qt = meta.get("question_type");
                String segStr = seg == null ? "" : String.valueOf(seg);
                String qtStr = qt == null ? "" : String.valueOf(qt);
                
                if (segStr.equals(segmentId) && (qtStr.equalsIgnoreCase(questionType) || qtStr.contains(questionType))) {
                    return rec.document();
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 查询某题型下已有的Unit
     * 
     * @param examPaperId 试卷ID
     * @param questionType 题型
     * @return Unit列表
     */
    public List<Map<String, Object>> queryExistingUnits(String examPaperId, String questionType) {
        try {
            var allUnits = chromaService.fetchExamPaperUnits(examPaperId, null);
            if (allUnits == null) return Collections.emptyList();
            
            List<Map<String, Object>> list = new ArrayList<>();
            for (var rec : allUnits) {
                Map<String, Object> meta = rec.metadata();
                Object qt = meta.get("question_type");
                String qtStr = qt == null ? "" : String.valueOf(qt);
                if (qtStr.equalsIgnoreCase(questionType) || qtStr.contains(questionType)) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("document", rec.document());
                    row.putAll(meta);
                    list.add(row);
                }
            }
            return list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // queryExamPaperIdsBySubject 和 queryChromaDBByPaperId 方法
    // 已由 CET4PaperGenService 独立实现，因为它们使用了 SimpleChromaRequest
    // 的特殊方法，这些方法在当前版本的 ChromaDB 服务中可能不可用

    // ==================== Coze Workflow 调用 ====================

    /**
     * 调用Coze工作流生成题目（带重试机制和降级）
     * 
     * @param examTopic 考试主题
     * @param inputExamPaperSamp 输入的样本试卷
     * @param examPaperEnSource 试卷来源（如 "AIfromreal", "AIfromself"）
     * @param segmentIdSelf 片段ID
     * @return 包含 output 和 answers 的结果Map
     * @throws Exception 所有重试失败后抛出异常
     */
    public Map<String, Object> callCozeWorkflow(String examTopic, String inputExamPaperSamp, 
                                                String examPaperEnSource, String segmentIdSelf) throws Exception {
        int maxAttempts = 10;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                logger.info("coze开始解析 segment_id={} (第{}次)", segmentIdSelf, attempt);
                Map<String, Object> result = callCozeWorkflowOnce(examTopic, inputExamPaperSamp, 
                                                                  examPaperEnSource, segmentIdSelf, attempt);
                logger.info("coze解析结束 segment_id={}", segmentIdSelf);
                
                // 检查output是否为空
                Object output = result.get("output");
                if (output != null) {
                    // output不为空，返回结果
                    return result;
                }
                
                // output为空，记录日志并继续重试
                
            } catch (Exception e) {
                lastException = e;
                
                if (attempt < maxAttempts) {
                    Thread.sleep(2000);
                }
            }
        }

        // 所有Coze重试都失败，调用DeepSeek作为保底方案
        logger.warn("Coze调用{}次均失败，降级使用DeepSeek生成 segment_id={}", maxAttempts, segmentIdSelf);
        try {
            Map<String, Object> deepSeekResult = pythonDeepSeekService.callPythonDeepSeek(
                    examTopic, inputExamPaperSamp, examPaperEnSource, segmentIdSelf);
            logger.info("DeepSeek降级生成成功 segment_id={}", segmentIdSelf);
            return deepSeekResult;
        } catch (Exception deepSeekEx) {
            logger.error("DeepSeek降级也失败 segment_id={}: {}", segmentIdSelf, deepSeekEx.getMessage());
            throw new RuntimeException("生成题目失败：Coze(重试" + maxAttempts + "次)和DeepSeek均不可用", deepSeekEx);
        }
    }

    /**
     * 单次调用Coze工作流
     * 
     * @param examTopic 考试主题
     * @param inputExamPaperSamp 输入的样本试卷
     * @param examPaperEnSource 试卷来源
     * @param segmentIdSelf 片段ID
     * @param attemptNumber 尝试次数
     * @return 包含 output、answers 和 rawResponse 的结果Map
     * @throws Exception 调用失败时抛出异常
     */
    private Map<String, Object> callCozeWorkflowOnce(String examTopic, String inputExamPaperSamp,
                                                     String examPaperEnSource, String segmentIdSelf,
                                                     int attemptNumber) throws Exception {
        // 简单前置校验，避免传空触发必填校验失败
        if (examTopic == null || examTopic.isBlank()) {
            throw new IllegalArgumentException("examTopic 不能为空");
        }
        if (inputExamPaperSamp == null || inputExamPaperSamp.isBlank()) {
            throw new IllegalArgumentException("inputExamPaperSamp 不能为空");
        }
        if (examPaperEnSource == null || examPaperEnSource.isBlank()) {
            throw new IllegalArgumentException("examPaperEnSource 不能为空");
        }
        if (segmentIdSelf == null || segmentIdSelf.isBlank()) {
            throw new IllegalArgumentException("segmentIdSelf 不能为空");
        }

        // 构建请求体（四个参数均为字符串）
        Map<String, String> params = new HashMap<>();
        // Coze 工作流必填参数（按需求使用驼峰命名）
        params.put("examTopic", examTopic);
        params.put("inputExamPaperSamp", inputExamPaperSamp);
        params.put("examPaperEnSource", examPaperEnSource);
        params.put("segmentIdSelf", segmentIdSelf);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("workflow_id", COZE_WORKFLOW_ID);
        requestBody.put("parameters", params);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        // 打印请求体方便排查缺失字段

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + cozeApiToken);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        // 调用前打印原始 HTTP 请求（token 部分打码）
        String maskedToken = cozeApiToken == null ? "null" : cozeApiToken.substring(0, Math.min(6, cozeApiToken.length())) + "***";

        // 发送POST请求
        logger.info("开始调用coze segment_id={}", segmentIdSelf);
        @SuppressWarnings("null")
        ResponseEntity<String> response = restTemplate.postForEntity(cozeApiUrl, entity, String.class);
        logger.info("调用结束coze segment_id={} status={}", segmentIdSelf, response.getStatusCodeValue());

        // 记录HTTP日志
        writeCozeHttpLog(examTopic, inputExamPaperSamp, examPaperEnSource, segmentIdSelf, jsonBody, response);

        // 解析并处理响应
        return runCozeWorkflow(response);
    }

    /**
     * 解析Coze工作流返回的响应
     * 
     * @param response HTTP响应
     * @return 包含 output、answers 和 rawResponse 的结果Map
     * @throws RuntimeException 解析失败时抛出异常
     */
    private Map<String, Object> runCozeWorkflow(ResponseEntity<String> response) throws RuntimeException {
        String responseBody = response.getBody();
        if (responseBody == null || responseBody.isEmpty()) {
            throw new RuntimeException("Coze工作流返回的响应体为空");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            throw new RuntimeException("解析Coze响应失败: " + e.getMessage(), e);
        }

        // 检查响应状态码
        int code = root.path("code").asInt(-1);
        if (code != 0) {
            String msg = root.path("msg").asText("未知错误");
            throw new RuntimeException("Coze工作流执行失败，code=" + code + ", msg=" + msg);
        }

        // 提取 data 字段
        JsonNode dataNode = root.path("data");
        if (dataNode.isMissingNode() || !dataNode.isTextual()) {
            throw new RuntimeException("Coze工作流返回的data字段缺失或格式错误");
        }

        // data 字段直接是 JSON 字符串（无需 Base64 解码）
        String dataDecoded = dataNode.asText();

        // 解析 JSON
        JsonNode dataJsonNode;
        try {
            dataJsonNode = objectMapper.readTree(dataDecoded);
        } catch (Exception e) {
            throw new RuntimeException("解析data JSON失败: " + e.getMessage(), e);
        }

        // 提取 output 字段（必需）
        JsonNode outputNode = dataJsonNode.get("output");
        if (outputNode == null || outputNode.isMissingNode()) {
            throw new RuntimeException("Coze工作流返回的output字段缺失（output为空需要重试）");
        }

        // 只处理字符串类型的 output
        if (!outputNode.isTextual()) {
            throw new RuntimeException("Coze工作流返回的output字段不是字符串，实际类型: " + outputNode.getNodeType() +
                "。请确保 Coze 工作流返回 JSON 字符串。");
        }

        String outputStr = outputNode.asText();
        if (outputStr.isEmpty()) {
            throw new RuntimeException("Coze工作流返回的output字段为空字符串（output为空需要重试）");
        }

        // 检查是否为 "wrong JSON"
        if ("wrong JSON".equalsIgnoreCase(outputStr.trim())) {
            throw new RuntimeException("Coze工作流返回格式错误: wrong JSON（需要重试）");
        }

        // 修复 JSON 字符串：将字段值内的真实换行符转义为 \n
        String fixedStr = fixJsonLineBreaks(outputStr);

        // 解析 JSON
        JsonNode parsedOutput;
        try {
            parsedOutput = objectMapper.readTree(fixedStr);
        } catch (Exception e) {
            throw new RuntimeException("解析output的JSON字符串失败: " + e.getMessage() +
                "。请检查 Coze 工作流返回的数据格式是否正确。", e);
        }

        // 检查解析后是否为空
        if (parsedOutput == null || (parsedOutput.isObject() && parsedOutput.size() == 0) ||
            (parsedOutput.isArray() && parsedOutput.size() == 0)) {
            throw new RuntimeException("Coze工作流返回的output解析后为空（output为空需要重试）");
        }

        // 清理转义引号：重新序列化 JsonNode 以确保 JSON 格式正确且无多余转义
        JsonNode cleanedOutput = parsedOutput;
        try {
            String outputStrClean = objectMapper.writeValueAsString(parsedOutput);
            cleanedOutput = objectMapper.readTree(outputStrClean);
        } catch (Exception e) {
        }

        // 提取答案
        Map<String, String> answers = new HashMap<>();
        JsonNode answersNode = findAnswersNode(cleanedOutput);
        if (answersNode != null && answersNode.isArray()) {
            for (JsonNode answerItem : answersNode) {
                if (answerItem.isObject()) {
                    String questionNumber = answerItem.path("question_number").asText("");
                    String answer = answerItem.path("answer").asText("");
                    if (!questionNumber.isEmpty() && !answer.isEmpty()) {
                        answers.put(questionNumber, answer);
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("output", cleanedOutput);
        result.put("answers", answers);
        result.put("rawResponse", response.getBody());

        return result;
    }

    // ==================== JSON 处理 ====================

    /**
     * 清理 JSON 字符串中的转义引号问题
     * 将 \" 替换为普通引号 "，使 JSON 字符串更加清晰易读
     * 
     * @param jsonStr 原始 JSON 字符串
     * @return 清理后的 JSON 字符串
     */
    public String cleanEscapedQuotes(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return jsonStr;
        }

        // 使用 JsonNode 重新序列化（推荐，保证 JSON 有效性）
        try {
            JsonNode node = objectMapper.readTree(jsonStr);
            // 使用 ObjectMapper 重新序列化，会自动清理不必要的转义
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return jsonStr;
        }
    }

    /**
     * 修复 JSON 字符串中字段值内的真实换行符
     * 
     * 问题：Coze 返回的 JSON 字符串中，某些字段值（如 passage_content, option_content）
     * 可能包含未转义的换行符，导致 JSON 解析失败。
     * 
     * 解决：智能识别并转义字段值内的换行符，同时保留 JSON 结构的换行符
     * 
     * 策略：
     * 1. 在双引号内的换行符（字段值内）→ 转义为 \n
     * 2. 在双引号外的换行符（JSON 结构）→ 保持不变
     * 
     * @param jsonStr 原始 JSON 字符串
     * @return 修复后的 JSON 字符串
     */
    public String fixJsonLineBreaks(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) {
            return jsonStr;
        }

        StringBuilder result = new StringBuilder();
        boolean inString = false;  // 是否在字符串字面量内
        boolean escaped = false;   // 前一个字符是否是转义符 \

        for (int i = 0; i < jsonStr.length(); i++) {
            char ch = jsonStr.charAt(i);

            if (escaped) {
                // 如果前一个字符是转义符，当前字符无论是什么都直接添加
                result.append(ch);
                escaped = false;
                continue;
            }

            if (ch == '\\') {
                // 遇到转义符，标记并添加
                result.append(ch);
                escaped = true;
                continue;
            }

            if (ch == '"') {
                // 遇到双引号，切换字符串内/外状态
                result.append(ch);
                inString = !inString;
                continue;
            }

            if (inString && (ch == '\n' || ch == '\r')) {
                // 在字符串内遇到换行符，转义为 \n（只保留 \n，忽略 \r）
                if (ch == '\n') {
                    result.append("\\n");
                }
                // \r 直接忽略
            } else {
                // 其他情况直接添加
                result.append(ch);
            }
        }

        return result.toString();
    }

    /**
     * 递归查找 JSON 中的 answers 节点
     * 
     * @param node 要搜索的 JSON 节点
     * @return 找到的 answers 节点，如果没找到则返回 null
     */
    private JsonNode findAnswersNode(JsonNode node) {
        if (node == null) {
            return null;
        }

        // 直接检查当前节点是否有 "answers" 字段
        if (node.has("answers")) {
            return node.get("answers");
        }

        // 如果是对象，遍历所有字段
        if (node.isObject()) {
            @SuppressWarnings("deprecation")
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode found = findAnswersNode(field.getValue());
                if (found != null) {
                    return found;
                }
            }
        }

        // 如果是数组，遍历所有元素
        if (node.isArray()) {
            for (JsonNode item : node) {
                JsonNode found = findAnswersNode(item);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    // ==================== 日志记录 ====================

    /**
     * 记录Coze HTTP调用的详细日志到文件
     * 
     * @param examTopic 考试主题
     * @param inputExamPaperSamp 输入样本
     * @param response HTTP响应
     */
    private void writeCozeHttpLog(String examTopic,
                                  String inputExamPaperSamp,
                                  String examPaperEnSource,
                                  String segmentIdSelf,
                                  String requestBody,
                                  ResponseEntity<String> response) {
        try {
            // 确保日志目录存在
            Path logDir = COZE_HTTP_LOG.getParent();
            if (logDir != null && !Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }

            StringBuilder logEntry = new StringBuilder();
            logEntry.append("\n========================================\n");
            logEntry.append("时间: ").append(LocalDateTime.now().format(TS_FMT)).append("\n");
            logEntry.append("========================================\n");
                logEntry.append("【请求参数】\n");
                logEntry.append("examTopic: ").append(examTopic).append("\n");
                logEntry.append("inputExamPaperSamp前200字符: ")
                    .append(inputExamPaperSamp.substring(0, Math.min(200, inputExamPaperSamp.length())))
                    .append("\n");
                logEntry.append("examPaperEnSource: ").append(examPaperEnSource).append("\n");
                logEntry.append("segmentIdSelf: ").append(segmentIdSelf).append("\n");
                logEntry.append("请求体: ").append(requestBody).append("\n");
            logEntry.append("----------------------------------------\n");
            logEntry.append("【响应】\n");
            logEntry.append("HTTP状态码: ").append(response.getStatusCode()).append("\n");
            logEntry.append("响应体: ").append(response.getBody()).append("\n");
            logEntry.append("========================================\n\n");

            Files.write(COZE_HTTP_LOG, logEntry.toString().getBytes(StandardCharsets.UTF_8),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {
            System.err.println("写入Coze HTTP日志失败: " + e.getMessage());
        }
    }

    // ==================== Getter 方法 ====================

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public Random getRandom() {
        return random;
    }
}
