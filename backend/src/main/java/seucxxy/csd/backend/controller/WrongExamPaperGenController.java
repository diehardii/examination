package seucxxy.csd.backend.controller;



import jakarta.servlet.http.HttpSession;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import seucxxy.csd.backend.dto.QuestionWrongDTO;
import seucxxy.csd.backend.entity.*;
import seucxxy.csd.backend.mapper.UserExamPaperMapper;
import seucxxy.csd.backend.service.WrongExamPaperGenService;
import java.util.List;

@RestController
@RequestMapping("/api/wrong-exam-paper")
@Transactional
public class WrongExamPaperGenController {

    private final WrongExamPaperGenService wrongExamPaperGenService;


    public WrongExamPaperGenController(WrongExamPaperGenService wrongExamPaperGenService) {
        this.wrongExamPaperGenService = wrongExamPaperGenService;

    }



    @GetMapping("/{subjectId}/{userId}")
    public List<QuestionWrongDTO> getWrongQuestionsBySubject(
            @PathVariable int subjectId,
            @PathVariable Long userId) {
        System.out.println(wrongExamPaperGenService.getWrongQuestionsBySubject(subjectId, userId));
        return wrongExamPaperGenService.getWrongQuestionsBySubject(subjectId, userId);
    }

    @PostMapping("/generate-wrong-exam-paper")
    public ExamPaper generateWrongQuestionPaper(
            @RequestBody WrongExamPaperGenRequest request) {

        return  wrongExamPaperGenService.generateWrongQuestionPaper(
                request.getPaperName(),
                request.getSubjectId(),
                request.getUserId(),
                request.getQuestionIds()
        );


    }
}


