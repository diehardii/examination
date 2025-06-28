package seucxxy.csd.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import seucxxy.csd.backend.entity.*;
import seucxxy.csd.backend.mapper.*;

import java.util.*;

@Service
public class PaperAnaService {
    private final UserTestRecordMapper userTestRecordMapper;
    private final QuestionMapper questionMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UserTestRecordDetailMapper userTestRecordDetailMapper;

    public PaperAnaService(UserTestRecordMapper userTestRecordMapper,
                           QuestionMapper questionMapper,
                           RestTemplateBuilder restTemplateBuilder,
                           ObjectMapper objectMapper,
                           UserTestRecordDetailMapper userTestRecordDetailMapper) {
        this.userTestRecordMapper = userTestRecordMapper;
        this.questionMapper = questionMapper;
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
        this.userTestRecordDetailMapper = userTestRecordDetailMapper;
    }


    public String analyzeSubject(String username, String subject) {
        String examResultsJson = getExamResultsJson(username, subject);
        return callCozeAnalysisApi(examResultsJson);
    }

    private String getExamResultsJson(String username, String subject) {
        List<Map<String, Object>> examResults = new ArrayList<>();

        List<UserTestRecord> records = userTestRecordMapper.findUserTestRecordsByUsernameAndSubject(username, subject);

        for (UserTestRecord record : records) {
            // 获取单选题
            List<SingleChoiceQuestion> singleChoiceQuestions =
                    questionMapper.findSCByExamPaperId(record.getExamPaper().getId());

            for (SingleChoiceQuestion question : singleChoiceQuestions) {
                Map<String, Object> questionData = new HashMap<>();
                questionData.put("question", question.getContent());

                List<Map<String, String>> options = new ArrayList<>();
                options.add(Map.of("label", "A", "text", question.getOptionA()));
                options.add(Map.of("label", "B", "text", question.getOptionB()));
                options.add(Map.of("label", "C", "text", question.getOptionC()));
                options.add(Map.of("label", "D", "text", question.getOptionD()));

                questionData.put("options", options);
                questionData.put("correctAnswer", question.getCorrectAnswer());

                // 查找用户答案
                Optional<UserTestRecordDetail> detail = userTestRecordDetailMapper.findUserTestRecordDetailsByTestId(record.getTestId()).stream()
                        .filter(d -> d.getQuestionNumber() == question.getNumber() &&
                                "SINGLE_CHOICE".equals(d.getQuestionType()))
                        .findFirst();

                questionData.put("userAnswer", detail.map(UserTestRecordDetail::getUserAnswer).orElse(""));

                examResults.add(questionData);
            }

            // 获取判断题
            List<JudgeQuestion> judgeQuestions =
                    questionMapper.findJQByExamPaperId(record.getExamPaper().getId());

            for (JudgeQuestion question : judgeQuestions) {
                Map<String, Object> questionData = new HashMap<>();
                questionData.put("question", question.getContent());
                questionData.put("correctAnswer", question.getCorrectAnswer());

                Optional<UserTestRecordDetail> detail = userTestRecordDetailMapper.findUserTestRecordDetailsByTestId(record.getTestId()).stream()
                        .filter(d -> d.getQuestionNumber() == question.getNumber() &&
                                "JUDGE".equals(d.getQuestionType()))
                        .findFirst();

                questionData.put("userAnswer", detail.map(UserTestRecordDetail::getUserAnswer).orElse(""));

                examResults.add(questionData);
            }
        }

        try {
            return objectMapper.writeValueAsString(examResults);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize exam results", e);
        }
    }

    private String callCozeAnalysisApi(String examResultsJson) {
        // 清理JSON字符串
        String cleanJson = examResultsJson
                .replaceAll("\\s+", "")
                .replaceAll("\"", "")
                .replaceAll("[{}[\\\\]]", "");

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("workflow_id", "7506799894930407458");

        Map<String, String> parameters = new HashMap<>();
        parameters.put("examResult", cleanJson);
        requestBody.put("parameters", parameters);

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer pat_OTmQLzBzAYHoTtGkWskQAeKgC9uQ1X5PvbawypMLZmfyX0PmiK7kgaThbKtAJpDN");

        // 发送请求
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "https://api.coze.cn/v1/workflow/run",
                HttpMethod.POST,
                entity,
                String.class
        );

        // 解析响应
        try {
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            String dataValue = rootNode.path("data").asText();

            // 提取分析结果
            return dataValue.replaceAll(".*\"analysis\":\"", "")
                    .replaceAll("\"}$", "")
                    .replace("\\n", "<br/>");
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse Coze API response", e);
        }
    }
}