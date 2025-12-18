package seucxxy.csd.backend.cet4.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import seucxxy.csd.backend.cet4.entity.CET4ExamPaperEn;
import seucxxy.csd.backend.cet4.mapper.CET4EExamPaperEnMapper;
import seucxxy.csd.backend.cet4.mapper.CET4UserExamPaperEnMapper;

import java.util.List;

@Service
public class CET4ExamPaperEnService {
    
    private final CET4EExamPaperEnMapper examPaperEnMapper;
    private final CET4UserExamPaperEnMapper userExamPaperEnMapper;

    @Autowired
    public CET4ExamPaperEnService(CET4EExamPaperEnMapper examPaperEnMapper,
                                  CET4UserExamPaperEnMapper userExamPaperEnMapper) {
        this.examPaperEnMapper = examPaperEnMapper;
        this.userExamPaperEnMapper = userExamPaperEnMapper;
    }

    public CET4ExamPaperEn createExamPaper(CET4ExamPaperEn examPaperEn) {
        if (examPaperEn.getExamPaperEnName() == null || examPaperEn.getExamPaperEnName().trim().isEmpty()) {
            throw new IllegalArgumentException("试卷名称不能为空");
        }
        
        examPaperEnMapper.insert(examPaperEn);
        return examPaperEn;
    }

    public List<CET4ExamPaperEn> getAllExamPapers() {
        return examPaperEnMapper.selectAll();
    }

    public List<CET4ExamPaperEn> getExamPapersByUser(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return userExamPaperEnMapper.findExamPapersByUserId(userId);
    }
}

