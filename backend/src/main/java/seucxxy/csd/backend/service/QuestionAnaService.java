package seucxxy.csd.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import seucxxy.csd.backend.entity.*;
import seucxxy.csd.backend.exception.ResourceNotFoundException;
import seucxxy.csd.backend.mapper.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuestionAnaService {

    @Autowired
    private UserTestRecordMapper userTestRecordMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private UserTestRecordDetailMapper userTestRecordDetailMapper;

    @Autowired
    private ExamPaperMapper examPaperMapper;

    @Value("${coze.api.url}")
    private String cozeApiUrl;

    @Value("${coze.api.token.ana}")
    private String cozeApiToken;

    public List<UserTestRecordDetail> getUserTestRecordDetail(long userTestId) {
        return userTestRecordDetailMapper.findUserTestRecordDetailsByTestId(userTestId);
    }

    public List<Question> getQuestionsForUserTestRecord(Long userTestRecordId, Long userId) {
        UserTestRecord record = userTestRecordMapper.findUserTestRecordByTestIdAndUserId(userTestRecordId, userId);
        if (record == null) {
            throw new ResourceNotFoundException("Test record not found");
        }
        return questionMapper.findQuestionsByExamPaperId(record.getExamPaperId());
    }

    public Map<String, Object> getQuestionDetails(Long userTestRecordId, int questionNumber, Long userId) {
        UserTestRecord record = userTestRecordMapper.findUserTestRecordByTestIdAndUserId(userTestRecordId, userId);
        if (record == null) {
            throw new ResourceNotFoundException("Test record not found");
        }

        Question question = questionMapper.findQuestionByExamPaperIdAndNumber(record.getExamPaperId(), questionNumber);
        if (question == null) {
            throw new ResourceNotFoundException("Question not found");
        }

        UserTestRecordDetail detail = userTestRecordDetailMapper.findUserTestRecordDeailByUserTestIdAndQuestionNumber(
                userTestRecordId, questionNumber);
        if (detail == null) {
            throw new ResourceNotFoundException("User answer not found");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("question", question);
        result.put("detail", detail);

        if (question instanceof SingleChoiceQuestion) {
            SingleChoiceQuestion scq = (SingleChoiceQuestion) question;
            Map<String, String> options = new HashMap<>();
            options.put("A", scq.getOptionA());
            options.put("B", scq.getOptionB());
            options.put("C", scq.getOptionC());
            options.put("D", scq.getOptionD());
            result.put("options", options);
        }

        return result;
    }

    public String analyzeQuestion(Question question, String userAnswer, String correctAnswer) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> questionMap = new HashMap<>();
        questionMap.put("question", question.getContent());
        questionMap.put("correctAnswer", correctAnswer);
        questionMap.put("userAnswer", userAnswer);

        String questionJson = objectMapper.writeValueAsString(questionMap);
        String jsonInputString = "{\"workflow_id\": \"7507949205416706100\", \"parameters\": {\"question\": " + questionJson + "}}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(cozeApiUrl))
                .header("Content-Type", "application/json; utf-8")
                .header("Authorization", "Bearer " + cozeApiToken)
                .POST(HttpRequest.BodyPublishers.ofString(jsonInputString))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode rootNode = objectMapper.readTree(response.body());
            String dataValue = rootNode.path("data").asText();

            return dataValue.replaceAll(".*\"analysis\":\"", "")
                    .replaceAll("\"}$", "")
                    .replace("\\n", "<br/>");
        } catch (Exception e) {
            throw new RuntimeException("Failed to analyze question", e);
        }
    }

    public List<Map<String, Object>> getQuestionsWithAnswers(Long userTestRecordId, Long userId) {
        // 1. 获取考试记录的所有题目（包含具体子类信息）
        List<Question> questions = getQuestionsForUserTestRecord(userTestRecordId, userId);

        // 2. 获取用户的答题详情
        List<UserTestRecordDetail> details = getUserTestRecordDetail(userTestRecordId);

        // 3. 合并数据
        return questions.stream().map(q -> {
            UserTestRecordDetail detail = details.stream()
                    .filter(d -> d.getQuestionNumber() == q.getNumber())
                    .findFirst()
                    .orElse(null);

            Map<String, Object> item = new HashMap<>();
            item.put("number", q.getNumber());
            item.put("content", q.getContent());
            item.put("correctAnswer", q.getCorrectAnswer());
            item.put("userAnswer", detail != null ? detail.getUserAnswer() : "未作答");
            item.put("type", q.getType());
            item.put("questionType", q.getQuestionType());

            return item;
        }).collect(Collectors.toList());
    }
}