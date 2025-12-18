package seucxxy.csd.backend.hs3.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * HS3 Coze AI评分服务
 * 用于评分写作和语法填空等非客观题
 */
@Service
public class HS3CozeAIGradingService {

    private static final Logger logger = LoggerFactory.getLogger(HS3CozeAIGradingService.class);

    @Value("${coze.api.token.examinationAI}")
    private String cozeApiToken;

    // 工作流ID: 7583918944659734543
    private static final String HS3_GRADING_WORKFLOW_ID = "7583918944659734543";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public HS3CozeAIGradingService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 调用Coze工作流进行AI评分
     * @param segment 原题segment（完整的JSON对象字符串）
     * @param userAnswer 学生答案
     * @return 评分结果（包含user_score、grade_base、user_answer_ana）
     */
    public Map<String, Object> gradeSubjectiveQuestion(String segment, String userAnswer) {
        logger.info("[HS3-CozeGrade] 开始调用评分任务");
        try {
            // 调用Coze工作流
            String url = "https://api.coze.cn/v1/workflow/run";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + cozeApiToken);
            headers.set("Content-Type", "application/json");

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("segment", segment);
            parameters.put("user_answer", userAnswer);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("workflow_id", HS3_GRADING_WORKFLOW_ID);
            requestBody.put("parameters", parameters);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            // 解析响应
            JsonNode responseJson = objectMapper.readTree(response.getBody());

            if (responseJson.has("code") && responseJson.get("code").asInt() == 0) {
                JsonNode dataNode = responseJson.path("data");
                if (dataNode.isMissingNode() || dataNode.isNull()) {
                    throw new RuntimeException("Coze工作流返回的data字段缺失");
                }

                // 兼容 data 既可能是字符串(内含JSON)也可能是对象
                if (dataNode.isTextual()) {
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
                    parsedOutput = outputNode;
                } else if (outputNode.isTextual()) {
                    String outputStr = outputNode.asText();
                    if (outputStr.isEmpty()) {
                        throw new RuntimeException("Coze工作流返回的output字段为空字符串");
                    }
                    // 去掉可能的markdown代码块标记
                    outputStr = outputStr.replaceAll("```json\\n?", "").replaceAll("```\\n?", "").trim();
                    try {
                        parsedOutput = objectMapper.readTree(outputStr);
                    } catch (Exception e) {
                        logger.error("JSON解析失败，尝试处理转义字符");
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
                result.put("user_score", parsedOutput.path("user_score").asInt(0));
                result.put("grade_base", parsedOutput.path("grade_base").asText(""));
                result.put("user_answer_ana", parsedOutput.path("user_answer_ana").asText(""));

                logger.info("[HS3-CozeGrade] 评分成功, 得分: {}", result.get("user_score"));
                return result;
            } else {
                String errorMsg = responseJson.path("msg").asText("未知错误");
                throw new RuntimeException("Coze API调用失败: " + errorMsg);
            }
        } catch (Exception e) {
            logger.error("AI评分失败: {}", e.getMessage(), e);

            // 返回默认评分结果
            Map<String, Object> defaultResult = new HashMap<>();
            defaultResult.put("user_score", 0);
            defaultResult.put("grade_base", "AI评分失败：" + e.getMessage());
            defaultResult.put("user_answer_ana", "系统错误，无法完成评分");

            return defaultResult;
        } finally {
            logger.info("[HS3-CozeGrade] 评分任务结束");
        }
    }
}
