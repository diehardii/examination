package seucxxy.csd.backend.cet4.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Coze AI评分服务
 * 用于评分写作和翻译等主观题
 */
@Service
public class CET4CozeAIGradingService {

    @Value("${coze.api.token.examinationAI}")
    private String cozeApiToken;

    @Value("${coze.api.workflow.grading.id:7574604525890256931}")
    private String workflowId;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public CET4CozeAIGradingService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 调用Coze工作流进行AI评分
     * @param question 题目信息（包含题干、参考答案等）
     * @param answer 学生答案
     * @return 评分结果（包含分数、反馈、分析等）
     */
    public Map<String, Object> gradeSubjectiveQuestion(Map<String, Object> question, String answer) {
        System.out.println("[CozeGrade] 开始调用评分任务");
        try {
            // 构建输入参数
            Map<String, Object> input = new HashMap<>();
            input.put("question", question);
            input.put("answer", answer);

            // 调用Coze工作流
            String url = "https://api.coze.cn/v1/workflow/run";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + cozeApiToken);
            headers.set("Content-Type", "application/json");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("workflow_id", workflowId);
            requestBody.put("parameters", input);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            // 解析响应
            JsonNode responseJson = objectMapper.readTree(response.getBody());

            if (responseJson.has("code") && responseJson.get("code").asInt() == 0) {
                JsonNode dataNode = responseJson.path("data");
                if (dataNode.isMissingNode() || dataNode.isNull()) {
                    throw new RuntimeException("Coze工作流返回的data字段缺失");
                }

                // 兼容 data 既可能是字符串(内含JSON)也可能是对象 的两种返回格式
                if (dataNode.isTextual()) {
                    // 老格式: data 是一个JSON字符串
                    String dataStr = dataNode.asText();
                    if (dataStr.isEmpty()) {
                        throw new RuntimeException("Coze工作流返回的data字段为空文本");
                    }
                    try {
                        dataNode = objectMapper.readTree(dataStr);
                    } catch (Exception e) {
                        throw new RuntimeException("解析data字符串为JSON失败: " + e.getMessage());
                    }
                }
                if (!dataNode.isObject()) {
                    throw new RuntimeException("Coze工作流返回的data字段不是对象");
                }

                // 兼容 output 既可能是对象也可能是字符串(内含JSON)
                JsonNode outputNode = dataNode.get("output");
                if (outputNode == null || outputNode.isMissingNode()) {
                    throw new RuntimeException("Coze工作流返回的output字段缺失");
                }

                JsonNode parsedOutput;
                if (outputNode.isObject() || outputNode.isArray()) {
                    // 新格式: 直接就是结构化JSON对象
                    parsedOutput = outputNode;
                } else if (outputNode.isTextual()) {
                    // 老格式: output是JSON字符串
                    String outputStr = outputNode.asText();

                    if (outputStr.isEmpty()) {
                        throw new RuntimeException("Coze工作流返回的output字段为空字符串");
                    }
                    try {
                        parsedOutput = objectMapper.readTree(outputStr);
                    } catch (Exception e) {
                        // 如果直接解析失败，尝试处理转义字符
                        System.err.println("JSON解析失败，尝试处理转义字符");
                        String unescapedOutput = outputStr.replace("\\\"", "\"")
                                                       .replace("\\\\", "\\");
                        try {
                            parsedOutput = objectMapper.readTree(unescapedOutput);
                        } catch (Exception e2) {
                            throw new RuntimeException("Coze工作流返回的output字段JSON格式错误: " + e2.getMessage());
                        }
                    }
                } else {
                    throw new RuntimeException("Coze工作流返回的output字段类型不支持: " + outputNode.getNodeType());
                }

                // 提取评分结果
                Map<String, Object> result = new HashMap<>();
                result.put("score", parsedOutput.path("score").asInt(0));
                result.put("feedback", parsedOutput.path("feedback").asText(""));
                result.put("reasoning", parsedOutput.path("reasoning").asText(""));
                result.put("type", parsedOutput.path("type").asText(""));

                if (parsedOutput.has("word_count")) {
                    result.put("word_count", parsedOutput.path("word_count").asInt(0));
                }

                return result;
            } else {
                String errorMsg = responseJson.path("msg").asText("未知错误");
                throw new RuntimeException("Coze API调用失败: " + errorMsg);
            }
        } catch (Exception e) {
            System.err.println("AI评分失败: " + e.getMessage());
            e.printStackTrace();

            // 返回默认评分结果
            Map<String, Object> defaultResult = new HashMap<>();
            defaultResult.put("score", 0);
            defaultResult.put("feedback", "AI评分失败：" + e.getMessage());
            defaultResult.put("reasoning", "系统错误，无法完成评分");
            defaultResult.put("type", "error");

            return defaultResult;
        } finally {
            System.out.println("[CozeGrade] 评分任务结束");
        }
    }
}
