package seucxxy.csd.backend.controller;



import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import seucxxy.csd.backend.entity.*;
import seucxxy.csd.backend.service.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exam-paper")
public class PaperTestController {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;
    @Autowired
    private ExamPaperService examPaperService;
    @Autowired
    private UserExamPaperService userExamPaperService;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private UserTestRecordService userTestRecordService;

    @Autowired
    private PaperTestService paperTestService;

    @GetMapping("/papers")
    public List<ExamPaper> getUserPapers() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userExamPaperService.getUserExamPapers(username);
    }

    @GetMapping("/question-answer/{paperId}")
    public Map<String, Object> getPaperQuestions(@PathVariable Long paperId) {
        System.out.println("paper id: " + paperId);
        ExamPaper examPaper = examPaperService.getExamPaperByIdWithQuestions(paperId);
        System.out.println(examPaper);
        List<Question> questions = questionService.getQuestionsByExamPaperId(paperId);
        System.out.println(questions);
        Map<String, Object> response = new HashMap<>();
        response.put("examPaper", examPaper);
        response.put("questions", questions);
        return response;
    }

    @PostMapping("/submit-answer")
    public Map<String, Object> submitAnswer(@RequestBody Map<String, Object> request, HttpSession session) {

        User user = (User) session.getAttribute("user");
        System.out.println(user.getUsername());

        Long examPaperId = Long.parseLong(request.get("examPaperId").toString());
        Map<String, String> answers = (Map<String, String>) request.get("answers");


        ExamPaper examPaper = examPaperService.getExamPaperByIdWithQuestions(examPaperId);
        System.out.println(examPaper.getExamPaperName()+examPaper.getExamPaperDifficulty());
        List<Question> questions = questionService.getQuestionsByExamPaperId(examPaperId);
        examPaper.setQuestions(questions);

        Map<Integer, String> singleChoiceUserAnswers = new HashMap<>();
        Map<Integer, String> judgeUserAnswers = new HashMap<>();
        Map<Integer, String> singleChoiceCorrectAnswers = new HashMap<>();
        Map<Integer, String> judgeCorrectAnswers = new HashMap<>();

        int correctNumber = 0;
        int total = questions.size();

        for (Question q : questions) {
            String userAnswer = null;
            if (q instanceof SingleChoiceQuestion) {
                userAnswer = answers.get("SC_" + q.getNumber());
                singleChoiceUserAnswers.put(q.getNumber(), userAnswer);
                singleChoiceCorrectAnswers.put(q.getNumber(), q.getCorrectAnswer());
                if (q.getCorrectAnswer().equals(userAnswer)) {
                    correctNumber++;
                }
            } else if (q instanceof JudgeQuestion) {
                userAnswer = answers.get("JQ_" + q.getNumber());
                if (userAnswer.equals("T"))
                    userAnswer = "正确";
                else if (userAnswer.equals("F"))
                    userAnswer = "错误";

                judgeUserAnswers.put(q.getNumber(), userAnswer);
                judgeCorrectAnswers.put(q.getNumber(), q.getCorrectAnswer());
                if (q.getCorrectAnswer().equals(userAnswer)) {
                    correctNumber++;
                }
            }
        }

        int testScore = (int) ((correctNumber * 100.0) / total);

        UserTestRecord record = new UserTestRecord();
        record.setCorrectNumber(correctNumber);
        record.setTestScore(testScore);
        record.setTestTime(LocalDateTime.now());
        record.setExamPaperId(examPaperId);
        record.setUserId(user.getId());

        userTestRecordService.saveTestRecord(record, questions, singleChoiceUserAnswers, judgeUserAnswers);

        Map<String, Object> result = new HashMap<>();
        result.put("examPaper", examPaper);
        result.put("total", total);
        result.put("correctNumber", correctNumber);
        result.put("testScore", testScore);
        result.put("singleChoiceUserAnswers", singleChoiceUserAnswers);
        result.put("judgeUserAnswers", judgeUserAnswers);
        result.put("singleChoiceCorrectAnswers", singleChoiceCorrectAnswers);
        result.put("judgeCorrectAnswers", judgeCorrectAnswers);

        return result;
    }


    @GetMapping("/test-records/{userId}/{examPaperId}")
    public List<UserTestRecord> getUserTestRecordsByExamPaperId(@PathVariable Long userId, @PathVariable Long examPaperId) {
        List<UserTestRecord> userTestRecords = new ArrayList<>();
        userTestRecords = paperTestService.getUserTestRecordsByExamPaperId(userId, examPaperId);


        return userTestRecords;
    }

    @GetMapping("/test-record-details/{testId}")
    public Map<String, Object> getTestRecordDetails(@PathVariable Long testId) {

        Map<String, Object> result = new HashMap<>();
       result = paperTestService.getTestRecordDetails(testId);

       return result;
    }
}
