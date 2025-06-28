package seucxxy.csd.backend.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seucxxy.csd.backend.entity.*;
import seucxxy.csd.backend.mapper.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class PaperTestService {

    @Autowired
    private UserExamPaperMapper userExamPaperMapper;

    @Autowired
    private ExamPaperMapper examPaperMapper;

    @Autowired
    private UserTestRecordMapper userTestRecordMapper;

    @Autowired
    private UserTestRecordDetailMapper userTestRecordDetailMapper;

    @Autowired
    private QuestionMapper questionMapper;



    public List<UserTestRecord> getUserTestRecordsByExamPaperId(Long userId, Long examPaperId) {
        return userTestRecordMapper.findByUserIdAndExamPaperId(userId, examPaperId);
    }

    public Map<String, Object> getTestRecordDetails(Long testId) {
        Map<String, Object> result = new HashMap<>();

        // 获取测试记录
        UserTestRecord testRecord = userTestRecordMapper.findByTestId(testId);
        result.put("testRecord", testRecord);

        // 获取试卷信息
        ExamPaper examPaper = examPaperMapper.findExamPaperById(testRecord.getExamPaperId());
        result.put("examPaper", examPaper);

        // 获取测试详情
        List<UserTestRecordDetail> details = userTestRecordDetailMapper.findUserTestRecordDetailsByTestId(testId);
        result.put("details", details);

        // 获取问题信息
        List<Question> questions = questionMapper.findQuestionsByExamPaperId(testRecord.getExamPaperId());
        result.put("questions", questions);

        // 处理为TestResult组件需要的数据格式
        Map<String, Object> testResult = new HashMap<>();
        testResult.put("examPaper", examPaper);
        testResult.put("total", examPaper.getExamPaperQuestionNumber());
        testResult.put("correctNumber", testRecord.getCorrectNumber());
        testResult.put("testScore", testRecord.getTestScore());

        Map<Integer, String> singleChoiceUserAnswers = new HashMap<>();
        Map<Integer, String> singleChoiceCorrectAnswers = new HashMap<>();
        Map<Integer, String> judgeUserAnswers = new HashMap<>();
        Map<Integer, String> judgeCorrectAnswers = new HashMap<>();

        for (UserTestRecordDetail detail : details) {
            if ("SINGLE_CHOICE".equals(detail.getQuestionType())) {
                singleChoiceUserAnswers.put(detail.getQuestionNumber(), detail.getUserAnswer());
                singleChoiceCorrectAnswers.put(detail.getQuestionNumber(), detail.getCorrectAnswer());
            } else if ("JUDGE".equals(detail.getQuestionType())) {
                judgeUserAnswers.put(detail.getQuestionNumber(), detail.getUserAnswer());
                judgeCorrectAnswers.put(detail.getQuestionNumber(), detail.getCorrectAnswer());
            }
        }

        testResult.put("singleChoiceUserAnswers", singleChoiceUserAnswers);
        testResult.put("singleChoiceCorrectAnswers", singleChoiceCorrectAnswers);
        testResult.put("judgeUserAnswers", judgeUserAnswers);
        testResult.put("judgeCorrectAnswers", judgeCorrectAnswers);

        result.put("testResult", testResult);

        return result;
    }
}