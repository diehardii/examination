package seucxxy.csd.backend.controller;


import org.springframework.web.bind.annotation.*;
import seucxxy.csd.backend.entity.ExamPaper;
import seucxxy.csd.backend.service.ExamPaperService;
import seucxxy.csd.backend.service.UserExamPaperService;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/exam-paper")
public class PaperManController {

    private final ExamPaperService examPaperService;
    private final UserExamPaperService userExamPaperService;

    public PaperManController(ExamPaperService examPaperService,
                              UserExamPaperService userExamPaperService) {
        this.examPaperService = examPaperService;
        this.userExamPaperService = userExamPaperService;
    }

    @GetMapping("/available")
    public ResponseEntity<List<ExamPaper>> getAvailableExamPapers(@RequestParam String username) {
        System.out.println(username);
        List<ExamPaper> papers = examPaperService.getAvailableExamPapers(username);
        System.out.println(papers);
        return ResponseEntity.ok(papers);
    }

    @GetMapping("/user")
    public ResponseEntity<List<ExamPaper>> getUserExamPapers(@RequestParam String username) {
        List<ExamPaper> papers = userExamPaperService.getUserExamPapers(username);
        return ResponseEntity.ok(papers);
    }

    @PostMapping("/save")
    public ResponseEntity<Void> saveUserPapers(
            @RequestParam String username,
            @RequestBody List<Long> examPaperIds) {

        userExamPaperService.saveUserExamPapers(username, examPaperIds);
        return ResponseEntity.ok().build();
    }
}
