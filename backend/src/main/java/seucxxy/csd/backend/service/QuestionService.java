package seucxxy.csd.backend.service;

import org.springframework.stereotype.Service;
import seucxxy.csd.backend.entity.*;
import seucxxy.csd.backend.mapper.*;

import java.util.List;


@Service
public class QuestionService {

    private final QuestionMapper questionMapper;

    public QuestionService(QuestionMapper questionMapper) {
           this.questionMapper = questionMapper;
    }

      public List<Question> getQuestionsByExamPaperId(long examPaperId) {
        return questionMapper.findQuestionsByExamPaperId(examPaperId);
    }

    public Question getQuestionByExamPaperIdAndNumber(long examPaperId, int number) {
        return questionMapper.findQuestionByExamPaperIdAndNumber(examPaperId, number);
    }


}