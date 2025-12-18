package seucxxy.csd.backend.hs3.service;

import com.fasterxml.jackson.core.type.TypeReference;
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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * HS3 高考英语 Coze 工作流调用服务
 * 
 * 调用 work_flow_id = 7583193741231128582 的试卷解析智能体
 * 输入：inputFile（试卷文本）、topics（主题数组）
 * 输出：结构化的试卷JSON数组
 */
@Service
public class HS3CozeWorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(HS3CozeWorkflowService.class);

    // 高考英语试卷解析工作流ID
    private static final String HS3_PAPER_ANALYSIS_WORKFLOW_ID = "7583193741231128582";
    
    // 各部分期望的segment数量（根据Neo4j图数据库结构定义）
    private static final Map<String, Integer> EXPECTED_SEGMENT_COUNTS = Map.of(
        "听力", 10,          // 第一节5个对话 + 第二节5个对话/独白
        "阅读理解", 5,       // 第一节4篇阅读 + 第二节1个七选五
        "语言知识运用", 2,   // 第一节1个完形填空 + 第二节1个语法填空
        "写作", 2            // 第一节1个应用文 + 第二节1个读后续写
    );

    @Value("${coze.api.url}")
    private String cozeApiUrl;

    @Value("${coze.api.token.examinationAI}")
    private String cozeApiToken;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public HS3CozeWorkflowService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 调用Coze试卷解析工作流（原方法，不分段）
     * 
     * @param inputFile 试卷文本内容
     * @param topics 主题数组，如 ["Environmental Protection", "Climate Change"]
     * @return 解析后的试卷结构化数据（JSON数组）
     * @throws Exception 调用失败时抛出异常
     */
    public List<Map<String, Object>> parsePaperWithCoze(String inputFile, List<String> topics) throws Exception {
        return parsePaperWithCoze(inputFile, topics, null);
    }
    
    /**
     * 调用Coze试卷解析工作流（支持分段处理）
     * 
     * @param inputFile 试卷文本内容
     * @param topics 主题数组，如 ["Environmental Protection", "Climate Change"]
     * @param partName 分段名称（如 "Part One"、"Part Two"），如果为null则处理整个试卷
     * @return 解析后的试卷结构化数据（JSON数组）
     * @throws Exception 调用失败时抛出异常
     */
    public List<Map<String, Object>> parsePaperWithCoze(String inputFile, List<String> topics, String partName) throws Exception {
        if (inputFile == null || inputFile.isBlank()) {
            throw new IllegalArgumentException("inputFile 不能为空");
        }

        // 将topics转换为JSON字符串
        String topicsJson = objectMapper.writeValueAsString(topics != null ? topics : Collections.emptyList());
        
        if (partName != null && !partName.isEmpty()) {
            logger.info("[HS3 Coze] 开始调用试卷解析工作流(分段模式), workflow_id={}, partName={}, 试卷长度={}, topics数量={}", 
                    HS3_PAPER_ANALYSIS_WORKFLOW_ID, partName, inputFile.length(), topics != null ? topics.size() : 0);
        } else {
            logger.info("[HS3 Coze] 开始调用试卷解析工作流, workflow_id={}, 试卷长度={}, topics数量={}", 
                    HS3_PAPER_ANALYSIS_WORKFLOW_ID, inputFile.length(), topics != null ? topics.size() : 0);
        }

        int maxAttempts = 10;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                List<Map<String, Object>> result = callCozeWorkflowOnce(inputFile, topicsJson, attempt, partName);

                // 验证segment数量是否符合预期（根据图数据库结构）
                String segmentCountCheckResult = checkSegmentCount(result, partName);
                if (segmentCountCheckResult != null) {
                    String partInfo = partName != null ? " (Part: " + partName + ")" : "";
                    logger.warn("[HS3 Coze] segment数量检查不通过{}: {}，立即重试 (attempt {}/{})", 
                            partInfo, segmentCountCheckResult, attempt, maxAttempts);
                    if (attempt < maxAttempts) {
                        // 立即重试，不等待
                        continue;
                    }
                    throw new RuntimeException(segmentCountCheckResult + "，已重试" + maxAttempts + "次");
                }

                // 检查七选五、完形填空、语法填空的option_content是否为空
                String optionCheckResult = checkOptionContent(result);
                if (optionCheckResult != null) {
                    String partInfo = partName != null ? " (Part: " + partName + ")" : "";
                    logger.warn("[HS3 Coze] option_content检查不通过{}: {}，立即重试 (attempt {}/{})", 
                            partInfo, optionCheckResult, attempt, maxAttempts);
                    if (attempt < maxAttempts) {
                        // 立即重试，不等待
                        continue;
                    }
                    throw new RuntimeException("智能体平台不可用：" + optionCheckResult + "，已重试" + maxAttempts + "次");
                }

                logger.info("[HS3 Coze] 试卷解析成功{}, 返回 {} 个片段", 
                        partName != null ? " (Part: " + partName + ")" : "", result != null ? result.size() : 0);
                return result;
            } catch (Exception e) {
                lastException = e;
                logger.warn("[HS3 Coze] 第{}次调用失败{}: {}", attempt, 
                        partName != null ? " (Part: " + partName + ")" : "", e.getMessage());
                // 立即重试，不等待
            }
        }

        throw new RuntimeException("Coze试卷解析失败" + (partName != null ? " (Part: " + partName + ")" : "") + 
                "，已重试" + maxAttempts + "次", lastException);
    }
    
    /**
     * 检查segment数量是否符合预期（根据Neo4j图数据库结构）
     * @param segments 解析后的segments列表
     * @param partName 部分名称
     * @return 如果检查通过返回null，否则返回错误信息
     */
    private String checkSegmentCount(List<Map<String, Object>> segments, String partName) {
        if (partName == null || partName.isEmpty()) {
            return null; // 不指定partName时不检查
        }
        
        // 查找匹配的期望数量
        Integer expectedCount = null;
        for (Map.Entry<String, Integer> entry : EXPECTED_SEGMENT_COUNTS.entrySet()) {
            if (partName.contains(entry.getKey())) {
                expectedCount = entry.getValue();
                break;
            }
        }
        
        if (expectedCount == null) {
            logger.debug("[HS3 Coze] 未找到 {} 的期望segment数量配置，跳过检查", partName);
            return null; // 未配置期望数量，不检查
        }
        
        int actualCount = segments != null ? segments.size() : 0;
        
        if (actualCount != expectedCount) {
            logger.warn("[HS3 Coze] {} segment数量不符合预期: 期望={}, 实际={}", partName, expectedCount, actualCount);
            return String.format("%s 解析结果不完整：期望 %d 个segment，实际 %d 个", partName, expectedCount, actualCount);
        }
        
        logger.info("[HS3 Coze] {} segment数量验证通过: {}", partName, actualCount);
        return null; // 检查通过
    }

    /**
     * 检查七选五、完形填空的option_content是否为空字符串
     * @param segments 解析后的segments列表
     * @return 如果检查通过返回null，否则返回错误信息
     */
    private String checkOptionContent(List<Map<String, Object>> segments) {
        if (segments == null || segments.isEmpty()) {
            return null;
        }
        
        // 需要检查option_content的section类型
        List<String> sectionsToCheck = List.of("七选五", "完形填空");
        
        for (Map<String, Object> segment : segments) {
            String sectionName = (String) segment.get("section_name");
            if (sectionName == null) {
                continue;
            }
            
            // 检查是否是需要验证的section类型
            boolean needCheck = sectionsToCheck.stream().anyMatch(sectionName::contains);
            if (!needCheck) {
                continue;
            }
            
            // 获取question_and_options字段
            Object questionAndOptions = segment.get("question_and_options");
            if (questionAndOptions == null) {
                // 不检查字段是否存在，跳过
                continue;
            }
            
            // 检查每个题目的option_content
            if (questionAndOptions instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> questions = (List<Map<String, Object>>) questionAndOptions;
                for (Map<String, Object> question : questions) {
                    Object options = question.get("options");
                    if (options == null) {
                        // 不检查字段是否存在，跳过
                        continue;
                    }
                    if (options instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> optionList = (List<Map<String, Object>>) options;
                        for (Map<String, Object> option : optionList) {
                            Object optionContent = option.get("option_content");
                            // 只检查option_content存在且为空字符串的情况
                            if (optionContent != null && optionContent.toString().trim().isEmpty()) {
                                String questionNumber = String.valueOf(question.get("question_number"));
                                logger.warn("[HS3 Coze] section_name={}, 题号={} 的option_content为空字符串", sectionName, questionNumber);
                                return sectionName + " 第" + questionNumber + "题选项内容为空";
                            }
                        }
                    }
                }
            }
        }
        
        return null; // 检查通过
    }

    /**
     * 单次调用Coze工作流（支持分段处理）
     */
    private List<Map<String, Object>> callCozeWorkflowOnce(String inputFile, String topicsJson, int attemptNumber, String partName) throws Exception {
        // 构建请求参数
        Map<String, String> params = new HashMap<>();
        params.put("inputFile", inputFile);
        params.put("topics", topicsJson);
        
        // 如果指定了partName，添加到参数中
        if (partName != null && !partName.isEmpty()) {
            params.put("partName", partName);
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("workflow_id", HS3_PAPER_ANALYSIS_WORKFLOW_ID);
        requestBody.put("parameters", params);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + cozeApiToken);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        String partInfo = partName != null && !partName.isEmpty() ? " (Part: " + partName + ")" : "";
        
        logger.info("[HS3 Coze] 开始解析{}, 第{}次尝试, body长度={}", 
                partInfo, attemptNumber, jsonBody.length());

        // 发送POST请求
        ResponseEntity<String> response = restTemplate.postForEntity(cozeApiUrl, entity, String.class);

        logger.info("[HS3 Coze] 解析完成{}, HTTP状态={}", partInfo, response.getStatusCodeValue());

        return parseCozeResponse(response, partName);
    }

    /**
     * 解析Coze响应
     */
    private List<Map<String, Object>> parseCozeResponse(ResponseEntity<String> response, String partName) throws Exception {
        String responseBody = response.getBody();
        if (responseBody == null || responseBody.isEmpty()) {
            throw new RuntimeException("Coze工作流返回的响应体为空");
        }

        String partInfo = partName != null && !partName.isEmpty() ? " (Part: " + partName + ")" : "";
        
        // 【调试】打印Coze HTTP原始响应（简化版，不打印响应体内容）
        logger.info("[HS3 Coze] HTTP响应{}: 状态码={}, 响应体长度={} 字符", partInfo, response.getStatusCodeValue(), responseBody.length());
        
        // 写入完整内容到专门的日志文件
        writeCozeResponseToFile(response.getStatusCodeValue(), responseBody, partName);

        JsonNode root = objectMapper.readTree(responseBody);

        // 检查响应状态码
        int code = root.path("code").asInt(-1);
        if (code != 0) {
            String msg = root.path("msg").asText("未知错误");
            throw new RuntimeException("Coze工作流执行失败，code=" + code + ", msg=" + msg);
        }

        // 提取 data 字段
        JsonNode dataNode = root.path("data");
        if (dataNode.isMissingNode()) {
            throw new RuntimeException("Coze工作流返回的data字段缺失");
        }

        String dataStr = dataNode.isTextual() ? dataNode.asText() : dataNode.toString();

        // 解析data JSON
        JsonNode dataJsonNode = objectMapper.readTree(dataStr);

        // 提取 output 字段
        JsonNode outputNode = dataJsonNode.path("output");
        if (outputNode.isMissingNode()) {
            throw new RuntimeException("Coze工作流返回的output字段缺失");
        }

        String outputStr;
        if (outputNode.isTextual()) {
            outputStr = outputNode.asText();
        } else if (outputNode.isArray()) {
            outputStr = outputNode.toString();
        } else {
            outputStr = outputNode.toString();
        }

        if (outputStr.isEmpty()) {
            throw new RuntimeException("Coze工作流返回的output为空");
        }

        // 修复可能的JSON格式问题
        outputStr = fixJsonString(outputStr);
        
        // 【调试】打印解析后的JSON结构
        logger.info("[HS3 Coze] ========== 解析后的JSON结构{} ==========", partInfo);
        try {
            // 格式化输出JSON便于查看
            Object json = objectMapper.readValue(outputStr, Object.class);
            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
            logger.info("[HS3 Coze] 解析后的output JSON:\n{}", prettyJson);
        } catch (Exception e) {
            logger.info("[HS3 Coze] 解析后的output JSON (原始):\n{}", outputStr);
        }
        logger.info("[HS3 Coze] ========== 解析后的JSON结构结束 ==========");

        // 根据最新提示词格式，Coze返回的是一个只包含 segments 键的JSON对象：
        // {
        //   "segments": [...]  // 大题数组
        // }
        // 每个segment包含：part_number, section_number, segment_number, segment_name等
        
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            // 首先尝试解析为JSON对象
            JsonNode outputJsonNode = objectMapper.readTree(outputStr);
            
            if (outputJsonNode.isObject()) {
                // 新格式：只有segments键的对象
                JsonNode segmentsNode = outputJsonNode.path("segments");
                
                if (segmentsNode.isArray()) {
                    logger.info("[HS3 Coze] 检测到新格式（只有segments键），开始提取segments...");
                    
                    for (JsonNode segmentNode : segmentsNode) {
                        Map<String, Object> segment = objectMapper.convertValue(segmentNode, new TypeReference<Map<String, Object>>() {});
                        result.add(segment);
                    }
                    logger.info("[HS3 Coze] 成功提取 {} 个segments", result.size());
                    
                    // 打印第一个segment的基本信息
                    if (!result.isEmpty()) {
                        Map<String, Object> firstSeg = result.get(0);
                        logger.info("[HS3 Coze] 第一个segment信息: segment_name={}, part_number={}, section_number={}, segment_number={}", 
                            firstSeg.get("segment_name"), firstSeg.get("part_number"), 
                            firstSeg.get("section_number"), firstSeg.get("segment_number"));
                    }
                } else {
                    logger.warn("[HS3 Coze] segments字段不是数组或缺失");
                }
                
            } else if (outputJsonNode.isArray()) {
                // 如果是数组格式（旧格式），直接解析
                logger.info("[HS3 Coze] 检测到旧格式（JSON数组），直接解析...");
                result = objectMapper.readValue(outputStr, new TypeReference<List<Map<String, Object>>>() {});
                logger.info("[HS3 Coze] 成功解析 {} 个元素", result.size());
            } else {
                throw new RuntimeException("Coze返回的output既不是对象也不是数组");
            }
            
        } catch (Exception parseException) {
            logger.error("[HS3 Coze] 解析output JSON失败: {}", parseException.getMessage());
            logger.debug("[HS3 Coze] output内容: {}", outputStr.substring(0, Math.min(500, outputStr.length())));
            throw new RuntimeException("解析Coze返回的JSON失败: " + parseException.getMessage(), parseException);
        }

        return result;
    }

    /**
     * 修复JSON字符串中的格式问题
     */
    private String fixJsonString(String json) {
        if (json == null) return null;
        
        // 移除可能的BOM
        json = json.trim();
        if (json.startsWith("\ufeff")) {
            json = json.substring(1);
        }
        
        // 修复转义问题
        json = json.replace("\\\\n", "\\n")
                   .replace("\\\\r", "\\r")
                   .replace("\\\\t", "\\t");
        
        return json;
    }
    
    /**
     * 将Coze请求的HTTP原始信息写入专门的日志文件
     * 【调试模式】暂时禁用
     * 
     * @param url 请求URL
     * @param requestBody 请求体内容
     * @param partName 分段名称（可选）
     */
    private void writeCozeRequestToFile(String url, String requestBody, String partName) {
        // 【调试模式】暂时禁用写入日志文件
    }
    
    /**
     * 将Coze返回的HTTP原始响应和解析后的JSON写入cozeout.txt文件
     * 
     * @param statusCode HTTP状态码
     * @param responseBody 响应体内容
     * @param partName 分段名称（可选）
     */
    private void writeCozeResponseToFile(int statusCode, String responseBody, String partName) {
        try {
            Path filePath = Paths.get("cozeout.txt");
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String partInfo = partName != null && !partName.isEmpty() ? " (Part: " + partName + ")" : "";
            
            StringBuilder content = new StringBuilder();
            content.append("\n").append("=".repeat(80)).append("\n");
            content.append("[").append(timestamp).append("]").append(partInfo).append("\n");
            content.append("=".repeat(80)).append("\n\n");
            
            // 1. 写入HTTP原始响应
            content.append("【1. Coze HTTP 原始响应】\n");
            content.append("-".repeat(40)).append("\n");
            content.append("HTTP状态码: ").append(statusCode).append("\n");
            content.append("响应体:\n").append(responseBody).append("\n\n");
            
            // 2. 尝试解析并格式化JSON
            content.append("【2. 解析后的JSON结构】\n");
            content.append("-".repeat(40)).append("\n");
            try {
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode dataNode = root.path("data");
                if (!dataNode.isMissingNode()) {
                    String dataStr = dataNode.isTextual() ? dataNode.asText() : dataNode.toString();
                    JsonNode dataJsonNode = objectMapper.readTree(dataStr);
                    JsonNode outputNode = dataJsonNode.path("output");
                    if (!outputNode.isMissingNode()) {
                        String outputStr = outputNode.isTextual() ? outputNode.asText() : outputNode.toString();
                        outputStr = fixJsonString(outputStr);
                        // 格式化输出JSON
                        try {
                            Object json = objectMapper.readValue(outputStr, Object.class);
                            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
                            content.append(prettyJson);
                        } catch (Exception e) {
                            content.append(outputStr);
                        }
                    } else {
                        content.append("output字段缺失");
                    }
                } else {
                    content.append("data字段缺失");
                }
            } catch (Exception e) {
                content.append("解析失败: ").append(e.getMessage());
            }
            content.append("\n\n");
            
            // 追加写入文件
            Files.writeString(filePath, content.toString(), 
                    java.nio.file.StandardOpenOption.CREATE, 
                    java.nio.file.StandardOpenOption.APPEND);
            
            logger.info("[HS3 Coze] 已将响应写入 cozeout.txt{}", partInfo);
        } catch (Exception e) {
            logger.warn("[HS3 Coze] 写入cozeout.txt失败: {}", e.getMessage());
        }
    }
}
