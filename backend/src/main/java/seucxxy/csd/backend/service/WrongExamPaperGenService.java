package seucxxy.csd.backend.service;

import seucxxy.csd.backend.dto.QuestionWrongDTO;
import seucxxy.csd.backend.entity.*;

import org.springframework.stereotype.Service;
import seucxxy.csd.backend.mapper.*;

import java.util.List;

@Service
public class WrongExamPaperGenService {

    private final QuestionMapper questionMapper;
    private final SubjectMapper subjectMapper;
    private final ExamPaperMapper examPaperMapper;
    private final UserMapper userMapper;
    private final UserExamPaperMapper userExamPaperMapper ;

    public WrongExamPaperGenService(QuestionMapper questionMapper,
                                    SubjectMapper subjectMapper,
                                    ExamPaperMapper examPaperMapper,
                                    UserMapper userMapper,
                                    UserExamPaperMapper userExamPaperMapper) {
        this.questionMapper = questionMapper;
        this.subjectMapper = subjectMapper;
        this.examPaperMapper = examPaperMapper;
        this.userMapper = userMapper;
        this.userExamPaperMapper = userExamPaperMapper;
    }




    public List<QuestionWrongDTO> getWrongQuestionsBySubject(int subjectId, Long userId) {
        return questionMapper.findWrongQuestionsBySubject(subjectId, userId);
    }


    public ExamPaper generateWrongQuestionPaper(
            String paperName,
            int subjectId,
            Long userId,
            List<Long> questionIds) {

        System.out.println(questionIds);
        // 1. 创建新试卷
        ExamPaper paper = new ExamPaper();
        paper.setExamPaperName(paperName);
        paper.setExamPaperSubject(subjectMapper.findSubjectById(subjectId).getSubjectName());
        paper.setExamPaperQuestionNumber(questionIds.size());
        paper.setExamPaperContent(paperName);
        paper.setExamPaperDifficulty("自定义");

        examPaperMapper.insertExamPaper(paper);

        // 2. 添加题目到试卷
        List<Question> questions = questionMapper.findQuestionsByIds(questionIds);
        System.out.println(questions);
        for (Question q : questions) {

            q.setExamPaperId(paper.getId());
            System.out.println(paper.getId());
            questionMapper.insertQuestion(q);
        }

        // 3. 关联用户和试卷
        UserExamPaper userPaper = new UserExamPaper();
        userPaper.setExamPaperId(paper.getId());
        userPaper.setUsername(userMapper.findUserById(userId).getUsername());
        userExamPaperMapper.insert(userPaper);

        return paper;
    }
}