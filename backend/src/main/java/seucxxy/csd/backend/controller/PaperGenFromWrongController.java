package seucxxy.csd.backend.controller;



import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import seucxxy.csd.backend.dto.PaperGenFromWrongRequest;
import seucxxy.csd.backend.entity.ExamPaper;
import seucxxy.csd.backend.entity.User;
import seucxxy.csd.backend.entity.UserExamPaper;
import seucxxy.csd.backend.mapper.UserExamPaperMapper;
import seucxxy.csd.backend.service.PaperGenFromWrongService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/exam-paper")
public class PaperGenFromWrongController {

    private final PaperGenFromWrongService paperGenFromWrongService;
    private final UserExamPaperMapper userExamPaperMapper;

    public PaperGenFromWrongController(PaperGenFromWrongService paperGenFromWrongService,
                                       UserExamPaperMapper userExamPaperMapper) {
        this.paperGenFromWrongService = paperGenFromWrongService;
        this.userExamPaperMapper = userExamPaperMapper;
    }

    @PostMapping("/generate-from-wrong")
    public ResponseEntity<?> generateFromWrong(@RequestBody PaperGenFromWrongRequest request,
                                               HttpSession session) {
        try {
            System.out.println("request");
            System.out.println(request);
            ExamPaper paper = paperGenFromWrongService.PaperGenFromWrongApi(
                    request.getExamPaperId(),
                    request.getName(),
                    "根据错题生成试卷",
                    "自定义", // 固定难度
                    request.getSubject(),
                    request.getSingleChoiceNumber(),
                    request.getJudgeNumber()
            );
            User user = (User) session.getAttribute("user");

            UserExamPaper userExamPaper = new UserExamPaper();
            userExamPaper.setExamPaperId(paper.getId());
            userExamPaper.setUsername(user.getUsername());

            userExamPaperMapper.insert(userExamPaper);

            return ResponseEntity.ok(paper);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .header("X-Error-Message", e.getMessage())
                    .build();

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .header("X-Error-Message", "服务器内部错误")
                    .build();
        }
    }
}
