package seucxxy.csd.backend.controller;



import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import seucxxy.csd.backend.dto.UserExamPaperDTO;
import seucxxy.csd.backend.entity.ExamPaper;

import seucxxy.csd.backend.service.ExamPaperService;
import seucxxy.csd.backend.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/paper-clear")
public class PaperClearController {

    private final ExamPaperService examPaperService;
    private final UserService userService;

    public PaperClearController(ExamPaperService examPaperService,
                                UserService userService) {
        this.examPaperService = examPaperService;
        this.userService = userService;
    }

    @GetMapping
    public List<ExamPaper> getAllExamPapers() {
        return examPaperService.getAllExamPapers();
    }

    @GetMapping("/{examPaperId}/users")
    public List<UserExamPaperDTO> getUsersByExamPaper(@PathVariable Long examPaperId) {
        return userService.getUsersByExamPaper(examPaperId);
    }

    @DeleteMapping("/{examPaperId}")
    public ResponseEntity<String> deleteExamPaper(@PathVariable Long examPaperId) {
        try {
            examPaperService.deleteExamPaperCascade(examPaperId);
            return ResponseEntity.ok("试卷删除成功");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("删除试卷失败: " + e.getMessage());
        }
    }
}
