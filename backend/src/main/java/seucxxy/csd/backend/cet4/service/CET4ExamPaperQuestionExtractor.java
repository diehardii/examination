package seucxxy.csd.backend.cet4.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CET4ExamPaperQuestionExtractor {

    /**
     * 从JSON文档解析题目列表。
     */
    public List<Map<String, Object>> parseQuestionsFromDocument(
            JsonNode docNode,
            Map<String, Object> metadata,
            String questionType,
            String document) {
        List<Map<String, Object>> questions = new ArrayList<>();

        try {
            if (questionType == null || questionType.isEmpty()) {
                return questions;
            }

            String type = questionType.trim();

            // 主观题：仅一题，无标准答案
            if (type.equals("Writing") || type.equals("Translation")) {
                Map<String, Object> question = new HashMap<>();
                question.put("number", 1);
                question.put("type", type);
                question.put("correctAnswer", "");
                questions.add(question);
                return questions;
            }

            JsonNode answersNode = null;

            switch (type) {
                case "BlankedCloze":
                    answersNode = docNode.at("/reading_comprehension/section_a/answers");
                    break;
                case "Matching":
                    answersNode = docNode.at("/reading_comprehension/section_b/answers");
                    break;
                case "ReadingPassage":
                    answersNode = docNode.get("answers");
                    break;
                case "NewsReport":
                case "Conversation":
                case "ListeningPassage":
                    answersNode = docNode.get("answers");
                    break;
                default:
                    return questions;
            }

            if (answersNode != null && answersNode.isArray()) {
                for (JsonNode answer : answersNode) {
                    String questionNumberStr = getTextValue(answer, "question_number", "");
                    String correctAnswer = getTextValue(answer, "answer", "");

                    if (!questionNumberStr.isEmpty() && !correctAnswer.isEmpty()) {
                        Map<String, Object> question = new HashMap<>();
                        question.put("number", parseQuestionNumber(questionNumberStr));
                        question.put("type", type);
                        question.put("correctAnswer", correctAnswer);
                        questions.add(question);
                    }
                }
            }

        } catch (Exception e) {
            // 忽略单题解析异常，尽可能返回其他题目
        }

        return questions;
    }

    /**
     * 解析题号字符串为整数。
     */
    public int parseQuestionNumber(String questionNumber) {
        if (questionNumber == null || questionNumber.isEmpty()) {
            return 0;
        }
        try {
            String digits = questionNumber.replaceAll("\\D+", "");
            if (digits.isEmpty()) {
                return 0;
            }
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 安全获取JsonNode中的文本值。
     */
    public String getTextValue(JsonNode node, String key, String defaultValue) {
        if (node == null || key == null) return defaultValue;
        JsonNode valueNode = node.get(key);
        if (valueNode == null || valueNode.isNull()) return defaultValue;
        String text = valueNode.asText("").trim();
        return text.isEmpty() ? defaultValue : text;
    }
}
