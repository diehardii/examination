package seucxxy.csd.backend.hs3.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * HS3 高考英语试卷生成公共服务
 * 
 * 主要功能：
 * 1. 调用Coze Workflow（8个参数版本）
 * 2. JSON解析与清理
 * 3. 日志记录
 */
@Service
public class HS3PaperGenerationCommonService {

    private static final Logger logger = LoggerFactory.getLogger(HS3PaperGenerationCommonService.class);

    // 高考试卷生成工作流ID（8个参数）
    private static final String HS3_COZE_WORKFLOW_ID = "7584246496888340520";
    private static final Path COZE_HTTP_LOG = Paths.get("logs", "hs3-coze-http.log");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Value("${coze.api.url}")
    private String cozeApiUrl;

    @Value("${coze.api.token.examinationAI}")
    private String cozeApiToken;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public HS3PaperGenerationCommonService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 调用Coze工作流生成题目（带重试机制）
     * 
     * 高考版本的8个参数：
     * - inputExamPaperSamp: 样本试卷内容
     * - topic: 主题
     * - partName: 大题名称（如"听力", "阅读理解"等）
     * - partNumber: 大题序号
     * - sectionName: 小节名称（如"对话", "短文阅读"等）
     * - sectionNumber: 小节序号
     * - segmentNumber: 片段序号
     * - questionNumberStart: 起始题号
     */
    public Map<String, Object> callCozeWorkflow(String topic,
                                                 String inputExamPaperSamp,
                                                 String partName,
                                                 String partNumber,
                                                 String sectionName,
                                                 String sectionNumber,
                                                 String segmentNumber,
                                                 String questionNumberStart) throws Exception {
        int maxAttempts = 10;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Map<String, Object> result = callCozeWorkflowOnce(
                        topic, inputExamPaperSamp, partName, partNumber,
                        sectionName, sectionNumber, segmentNumber, questionNumberStart, attempt);
                
                // 检查output是否为空
                Object output = result.get("output");
                if (output != null) {
                    return result;
                }
                
                // output为空，继续重试
                System.out.println("[HS3 Coze] ⚠️ output为空，继续重试 (attempt " + attempt + "/" + maxAttempts + ")");
                
            } catch (Exception e) {
                lastException = e;
                System.out.println("[HS3 Coze] ❌ 第" + attempt + "次调用失败: " + e.getMessage());
                
                if (attempt < maxAttempts) {
                    Thread.sleep(2000);
                }
            }
        }

        throw new RuntimeException("Coze工作流调用失败，已重试" + maxAttempts + "次", lastException);
    }

    /**
     * 单次调用Coze工作流
     */
    private Map<String, Object> callCozeWorkflowOnce(String topic,
                                                      String inputExamPaperSamp,
                                                      String partName,
                                                      String partNumber,
                                                      String sectionName,
                                                      String sectionNumber,
                                                      String segmentNumber,
                                                      String questionNumberStart,
                                                      int attemptNumber) throws Exception {
        // 参数校验
        if (inputExamPaperSamp == null || inputExamPaperSamp.isBlank()) {
            throw new IllegalArgumentException("inputExamPaperSamp 不能为空");
        }
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic 不能为空");
        }

        // ==================== 控制台：开始生成（简化输出） ====================
        // 构建segment_name用于显示
        String segmentName = buildSegmentName(partName, partNumber, sectionName, sectionNumber, segmentNumber);
        System.out.println("[HS3 Coze] 🚀 开始生成: " + segmentName + " (第" + attemptNumber + "次)");

        // 构建请求参数（8个参数）
        Map<String, String> params = new HashMap<>();
        params.put("inputExamPaperSamp", inputExamPaperSamp);
        params.put("topic", topic);
        params.put("partName", partName != null ? partName : "");
        params.put("partNumber", partNumber != null ? partNumber : "1");
        params.put("sectionName", sectionName != null ? sectionName : "");
        params.put("sectionNumber", sectionNumber != null ? sectionNumber : "1");
        params.put("segmentNumber", segmentNumber != null ? segmentNumber : "1");
        params.put("questionNumberStart", questionNumberStart != null ? questionNumberStart : "1");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("workflow_id", HS3_COZE_WORKFLOW_ID);
        requestBody.put("parameters", params);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + cozeApiToken);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        // 记录请求开始时间
        long startTime = System.currentTimeMillis();

        // 发送POST请求
        ResponseEntity<String> response = restTemplate.postForEntity(cozeApiUrl, entity, String.class);
        
        long elapsed = System.currentTimeMillis() - startTime;

        // 记录完整HTTP日志到文件（仅写作部分）
        writeFullHttpLog(topic, partName, sectionName, segmentNumber, attemptNumber, 
                cozeApiUrl, jsonBody, headers, response, elapsed);

        // 解析响应
        Map<String, Object> result = parseCozeResponse(response);
        
        // ==================== 控制台：简化输出（只显示segment_name和返回字符数） ====================
        Object output = result.get("output");
        if (output != null) {
            String outputStr = output.toString();
            System.out.println("[HS3 Coze] ✅ " + segmentName + " 完成, 返回 " + outputStr.length() + " 字符, 耗时 " + elapsed + "ms");
        } else {
            System.out.println("[HS3 Coze] ⚠️ " + segmentName + " 输出为空, 耗时 " + elapsed + "ms");
        }

        return result;
    }

    /**
     * 解析Coze工作流响应
     */
    private Map<String, Object> parseCozeResponse(ResponseEntity<String> response) throws Exception {
        String responseBody = response.getBody();
        if (responseBody == null || responseBody.isEmpty()) {
            throw new RuntimeException("Coze工作流返回的响应体为空");
        }

        JsonNode root = objectMapper.readTree(responseBody);

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

        String dataDecoded = dataNode.asText();

        // 解析 JSON
        JsonNode dataJsonNode = objectMapper.readTree(dataDecoded);

        // 提取 output 字段
        JsonNode outputNode = dataJsonNode.get("output");
        if (outputNode == null || outputNode.isMissingNode()) {
            throw new RuntimeException("Coze工作流返回的output字段缺失");
        }

        if (!outputNode.isTextual()) {
            throw new RuntimeException("Coze工作流返回的output字段不是字符串，实际类型: " + outputNode.getNodeType());
        }

        String outputStr = outputNode.asText();
        if (outputStr.isEmpty()) {
            throw new RuntimeException("Coze工作流返回的output字段为空字符串");
        }

        // 检查是否为 "wrong JSON"
        if ("wrong JSON".equalsIgnoreCase(outputStr.trim())) {
            throw new RuntimeException("Coze工作流返回格式错误: wrong JSON");
        }

        // 修复 JSON 字符串
        String fixedStr = fixJsonLineBreaks(outputStr);

        // 尝试解析修复后的JSON
        JsonNode outputJsonNode;
        try {
            outputJsonNode = objectMapper.readTree(fixedStr);
        } catch (Exception e) {
            // JSON解析失败，抛出异常触发重试
            logger.warn("[HS3 Coze] JSON解析失败，将触发重试: {}", e.getMessage());
            throw new RuntimeException("Coze返回的JSON格式错误，需要重试: " + e.getMessage(), e);
        }

        // 提取answers
        List<String> answers = new ArrayList<>();
        if (outputJsonNode.has("answers")) {
            JsonNode answersNode = outputJsonNode.get("answers");
            if (answersNode.isArray()) {
                for (JsonNode answerNode : answersNode) {
                    answers.add(answerNode.asText());
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("output", fixedStr);
        result.put("answers", answers);
        result.put("rawResponse", responseBody);
        return result;
    }

    /**
     * 修复JSON字符串中的换行符问题
     */
    private String fixJsonLineBreaks(String input) {
        if (input == null) {
            return null;
        }
        
        // 移除可能的BOM
        if (input.startsWith("\uFEFF")) {
            input = input.substring(1);
        }
        
        // 清理markdown代码块标记
        input = input.replaceAll("^```json\\s*", "")
                     .replaceAll("^```\\s*", "")
                     .replaceAll("\\s*```$", "");
        
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            if (escaped) {
                result.append(c);
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                escaped = true;
                result.append(c);
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                result.append(c);
                continue;
            }
            
            if (inString) {
                // 在字符串内部，将真实换行转为转义序列
                if (c == '\n') {
                    result.append("\\n");
                } else if (c == '\r') {
                    result.append("\\r");
                } else if (c == '\t') {
                    result.append("\\t");
                } else {
                    result.append(c);
                }
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }

    /**
     * 构建segment名称用于显示
     */
    private String buildSegmentName(String partName, String partNumber, String sectionName, 
                                     String sectionNumber, String segmentNumber) {
        StringBuilder sb = new StringBuilder();
        if (partName != null && !partName.isEmpty()) {
            sb.append(partName);
        }
        if (sectionName != null && !sectionName.isEmpty()) {
            if (sb.length() > 0) sb.append("-");
            sb.append(sectionName);
        }
        if (segmentNumber != null && !segmentNumber.isEmpty()) {
            sb.append("(").append(segmentNumber).append(")");
        }
        return sb.length() > 0 ? sb.toString() : "segment-" + segmentNumber;
    }

    /**
     * 写入完整HTTP日志到文件（仅写作部分）
     * 只有partName包含"写作"时才记录完整日志
     */
    private void writeFullHttpLog(String topic, String partName, String sectionName, 
                                   String segmentNumber, int attemptNumber,
                                   String url, String requestBody, HttpHeaders requestHeaders,
                                   ResponseEntity<String> response, long elapsed) {
        // 只记录"写作"部分的完整日志
        if (partName == null || !partName.contains("写作")) {
            return;
        }
        
        try {
            Files.createDirectories(COZE_HTTP_LOG.getParent());
            
            StringBuilder log = new StringBuilder();
            String timestamp = LocalDateTime.now().format(TS_FMT);
            
            log.append("\n\n");
            log.append("╔══════════════════════════════════════════════════════════════════════════════╗\n");
            log.append("║                 HS3 COZE 写作部分 HTTP 请求/响应日志                          ║\n");
            log.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
            log.append("║ 时间: ").append(timestamp).append("\n");
            log.append("║ 尝试次数: ").append(attemptNumber).append("\n");
            log.append("║ 耗时: ").append(elapsed).append("ms\n");
            log.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
            log.append("║ 参数信息:\n");
            log.append("║   - topic: ").append(topic).append("\n");
            log.append("║   - partName: ").append(partName).append("\n");
            log.append("║   - sectionName: ").append(sectionName).append("\n");
            log.append("║   - segmentNumber: ").append(segmentNumber).append("\n");
            log.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
            
            // HTTP请求部分
            log.append("║ ▼▼▼ HTTP REQUEST (完整输入) ▼▼▼\n");
            log.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
            log.append("║ URL: ").append(url).append("\n");
            log.append("║ Method: POST\n");
            log.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
            log.append("║ Request Body (完整):\n");
            log.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
            log.append(requestBody).append("\n");
            log.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
            
            // HTTP响应部分
            log.append("║ ▼▼▼ HTTP RESPONSE (完整输出) ▼▼▼\n");
            log.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
            log.append("║ Status: ").append(response.getStatusCode().value()).append("\n");
            log.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
            log.append("║ Response Body (完整):\n");
            log.append("╠══════════════════════════════════════════════════════════════════════════════╣\n");
            String responseBody = response.getBody();
            log.append(responseBody != null ? responseBody : "<empty>").append("\n");
            log.append("╚══════════════════════════════════════════════════════════════════════════════╝\n");
            
            Files.writeString(COZE_HTTP_LOG, log.toString(), 
                    java.nio.file.StandardOpenOption.CREATE, 
                    java.nio.file.StandardOpenOption.APPEND);
            
            System.out.println("[HS3 Coze] 📝 写作部分日志已写入: " + COZE_HTTP_LOG.toAbsolutePath());
            
        } catch (Exception e) {
            logger.warn("[HS3 Coze] 写入HTTP日志失败: {}", e.getMessage());
        }
    }
}
