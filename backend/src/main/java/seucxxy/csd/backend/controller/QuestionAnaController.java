package seucxxy.csd.backend.controller;

import org.springframework.web.bind.annotation.*;
import seucxxy.csd.backend.entity.*;
import seucxxy.csd.backend.service.QuestionAnaService;
import seucxxy.csd.backend.service.UserService;
import seucxxy.csd.backend.service.UserTestRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/question-analysis")
public class QuestionAnaController {

    @Autowired
    private QuestionAnaService questionAnaService;

    @Autowired
    private UserTestRecordService userTestRecordService;

    @Autowired
    private UserService userService;

    @GetMapping("/user-test-records/{userId}")
    public ResponseEntity<List<UserTestRecord>> getUserTestRecords(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        List<UserTestRecord> records = userTestRecordService.getUserTestRecordsByUsername(user.getUsername());
        return ResponseEntity.ok(records);
    }

    @GetMapping("/questions-with-answers/{testRecordId}/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getQuestionsWithAnswers(
            @PathVariable Long testRecordId,
            @PathVariable Long userId) {
        List<Map<String, Object>> result = questionAnaService.getQuestionsWithAnswers(testRecordId, userId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeQuestion(
            @RequestParam Long userTestRecordId,
            @RequestParam int questionNumber,
            @RequestParam Long userId) {
        try {
            Map<String, Object> details = questionAnaService.getQuestionDetails(
                    userTestRecordId, questionNumber, userId);

            Question question = (Question) details.get("question");
            UserTestRecordDetail detail = (UserTestRecordDetail) details.get("detail");

            String analysisResult = questionAnaService.analyzeQuestion(
                    question, detail.getUserAnswer(), detail.getCorrectAnswer());

            Map<String, Object> response = new HashMap<>();
            response.put("analysisResult", analysisResult);

            // 添加题目信息
            response.put("questionNumber", String.valueOf(question.getNumber()));
            response.put("questionContent", question.getContent());
            response.put("userAnswer", detail.getUserAnswer());
            response.put("correctAnswer", detail.getCorrectAnswer());

            if (details.containsKey("options")) {

                response.put("options", details.get("options"));
                System.out.println("options"+details.get("options").toString());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}