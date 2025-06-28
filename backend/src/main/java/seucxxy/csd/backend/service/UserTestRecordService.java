package seucxxy.csd.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seucxxy.csd.backend.entity.*;
import seucxxy.csd.backend.mapper.ExamPaperMapper;
import seucxxy.csd.backend.mapper.UserTestRecordDetailMapper;
import seucxxy.csd.backend.mapper.UserTestRecordMapper;

import java.util.List;
import java.util.Map;

@Service
public class UserTestRecordService {

    @Autowired
    private ExamPaperMapper examPaperMapper;
    @Autowired
    private UserTestRecordMapper userTestRecordMapper;

    @Autowired
    private UserTestRecordDetailMapper userTestRecordDetailMapper;

    public void saveTestRecord(UserTestRecord record, List<Question> questions,
                               Map<Integer, String> singleChoiceAnswers,
                               Map<Integer, String> judgeAnswers) {
        // 保存主记录
        userTestRecordMapper.insertUserTestRecord(record);

        // 保存详细记录
        for (Question q : questions) {
            UserTestRecordDetail detail = new UserTestRecordDetail();
            detail.setTestId(record.getTestId());
            detail.setQuestionNumber(q.getNumber());
            detail.setQuestionType(q instanceof SingleChoiceQuestion ? "SINGLE_CHOICE" : "JUDGE");
            detail.setCorrectAnswer(q.getCorrectAnswer());

            if (q instanceof SingleChoiceQuestion) {
                detail.setUserAnswer(singleChoiceAnswers.get(q.getNumber()));
                System.out.println("userAnswer"+singleChoiceAnswers.get(q.getNumber()));
            } else if (q instanceof JudgeQuestion) {
                detail.setUserAnswer(judgeAnswers.get(q.getNumber()));
                System.out.println("userAnswer"+judgeAnswers.get(q.getNumber()));
            }

            System.out.println("detail"+detail.getUserAnswer());
            userTestRecordDetailMapper.insertUserTestRecordDetail(detail);
        }
    }

    public List<UserTestRecord> getUserTestRecordsByUsername(String username) {
        List<UserTestRecord> records = userTestRecordMapper.findUserTestRecordsByUsernameOrderByTestTimeDesc(username);

        // 2. 为每条记录加载关联的ExamPaper
        records.forEach(record -> {
            if (record.getExamPaperId() != null) {
                ExamPaper paper = examPaperMapper.findExamPaperById(record.getExamPaperId());
                record.setExamPaper(paper);
            }
        });

        return records;
    }


    public List<UserTestRecord> getUserTestRecordsByUsernameAndSubject(String username, String subject) {

        List<UserTestRecord> records = userTestRecordMapper.findUserTestRecordsByUsernameAndSubject(username, subject);


        return records;
    }



}



