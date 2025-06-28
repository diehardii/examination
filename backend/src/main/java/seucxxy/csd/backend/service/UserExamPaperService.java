package seucxxy.csd.backend.service;

import seucxxy.csd.backend.entity.*;
import seucxxy.csd.backend.mapper.*;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;

@Service
public class UserExamPaperService {
    private final ExamPaperMapper examPaperMapper;
    private final UserExamPaperMapper userExamPaperMapper;
    private final RestTemplate restTemplate;



    public UserExamPaperService(ExamPaperMapper examPaperMapper,
                                UserExamPaperMapper userExamPaperMapper,
                                RestTemplate restTemplate) {
        this.examPaperMapper = examPaperMapper;
        this.userExamPaperMapper = userExamPaperMapper;
        this.restTemplate = restTemplate;
    }


    public List<ExamPaper> getUserExamPapers(String username) {
        return userExamPaperMapper.findExamPapersByUsername(username);
    }
    public void saveUserExamPapers(String username, List<Long> examPaperIds) {
        // 先删除用户原有的试卷关系
        userExamPaperMapper.deleteByUsername(username);

        // 添加新的试卷关系
        examPaperIds.forEach(examPaperId -> {
            UserExamPaper userExamPaper = new UserExamPaper();
            userExamPaper.setExamPaperId(examPaperId);
            userExamPaper.setUsername(username);
            userExamPaperMapper.insert(userExamPaper);
        });
    }

}