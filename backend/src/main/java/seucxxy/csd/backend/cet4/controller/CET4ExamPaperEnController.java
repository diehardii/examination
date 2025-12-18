package seucxxy.csd.backend.cet4.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import seucxxy.csd.backend.cet4.entity.CET4ExamPaperEn;
import seucxxy.csd.backend.cet4.service.CET4ExamPaperEnService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cet4/exam-paper-en")
@CrossOrigin(origins = {"http://localhost:5002", "http://localhost:5003"}, allowCredentials = "true")
public class CET4ExamPaperEnController {

    private final CET4ExamPaperEnService examPaperEnService;

    public CET4ExamPaperEnController(CET4ExamPaperEnService examPaperEnService) {
        this.examPaperEnService = examPaperEnService;
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getAllCET4ExamPapers() {
        Map<String, Object> response = new HashMap<>();
        try {
            List<CET4ExamPaperEn> allPapers = examPaperEnService.getAllExamPapers();
            
            // 过滤出 CET4 科目的试卷
            List<CET4ExamPaperEn> cet4Papers = allPapers.stream()
                    .filter(paper -> "CET4".equals(paper.getExamPaperEnSubject()))
                    .collect(Collectors.toList());
            
            response.put("success", true);
            response.put("data", cet4Papers);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("获取试卷列表失败: " + e.getMessage());
            e.printStackTrace();
            
            response.put("success", false);
            response.put("message", "获取试卷列表失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/")
    public ResponseEntity<Map<String, Object>> createCET4ExamPaper(@RequestBody CET4ExamPaperEn examPaperEn) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 确保科目是 CET4
            if (examPaperEn.getExamPaperEnSubject() == null || examPaperEn.getExamPaperEnSubject().isEmpty()) {
                examPaperEn.setExamPaperEnSubject("CET4");
            }
            
            CET4ExamPaperEn created = examPaperEnService.createExamPaper(examPaperEn);
            response.put("success", true);
            response.put("id", created.getId());
            response.put("examPaperEn", created);
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            System.err.println("创建失败: " + e.getMessage());
            
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (DataIntegrityViolationException e) {
            // 处理唯一约束违反（试卷名称重复）
            String message = e.getMessage();
            if (message != null && (message.contains("Duplicate entry") || message.contains("UNIQUE constraint"))) {
                response.put("success", false);
                response.put("message", "试卷名称已存在，请使用其他名称");
            } else {
                response.put("success", false);
                response.put("message", "数据库约束违反：" + e.getMessage());
            }
            
            System.err.println("数据库约束违反: " + message);
            
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            System.err.println("创建失败: " + e.getMessage());
            e.printStackTrace();
            
            response.put("success", false);
            response.put("message", "创建试卷失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
