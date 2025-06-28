package seucxxy.csd.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import seucxxy.csd.backend.entity.ExamPaper;
import seucxxy.csd.backend.entity.Subject;
import seucxxy.csd.backend.service.ExamPaperService;
import seucxxy.csd.backend.service.SubjectService;

import java.util.List;
import java.util.Map;

// PaperGenController.java
@RestController
@RequestMapping("/api/exam-paper")
public class PaperGenController {
    private final ExamPaperService examPaperService;
    private final SubjectService subjectService;

    @Autowired
    public PaperGenController(ExamPaperService examPaperService,
                              SubjectService subjectService) {
        this.examPaperService = examPaperService;
        this.subjectService = subjectService;
    }

    @GetMapping("/subjects")
    public ResponseEntity<List<Subject>> getAllSubjects() {
        List<Subject> subjects = subjectService.getAllSubjects();
        return ResponseEntity.ok(subjects);
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateExamPaper(@RequestBody PaperGenRequest request) {
        try {
            ExamPaper paper = examPaperService.generateExamPaper(
                    request.getName(),
                    request.getContent(),
                    request.getDifficulty(),
                    request.getSubject(),
                    request.getSingleChoiceNumber(),
                    request.getJudgeNumber()
            );
            return ResponseEntity.ok(paper);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "生成试卷时出错: " + e.getMessage())
            );
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExamPaper> getExamPaper(@PathVariable Long id) {
        ExamPaper examPaper = examPaperService.getExamPaperByIdWithQuestions(id);
        return ResponseEntity.ok(examPaper);
    }
}

