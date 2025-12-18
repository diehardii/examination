package seucxxy.csd.backend.cet4.controller;



import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import seucxxy.csd.backend.cet4.service.CET4PaperTestService;
import seucxxy.csd.backend.common.entity.User;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/api/exam-paper", "/api/cet4/exam-paper"})
@CrossOrigin(origins = {"http://localhost:5002", "http://localhost:5003"}, allowCredentials = "true")
public class CET4PaperTestController {
    @Autowired
    private CET4PaperTestService paperTestService;

    // 试卷列表（英文 CET4）
    @GetMapping("/papers-en")
    public ResponseEntity<?> getExamPaperEnList() {
        return paperTestService.getExamPaperEnList();
    }

    // 提交答案（英文试卷）添加迁移日志
    @PostMapping("/submit-answer-en")
    public Map<String, Object> submitAnswerEn(@RequestBody Map<String, Object> request, HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            User user = (User) session.getAttribute("user");
            if (user == null) {
                result.put("success", false);
                result.put("message", "用户未登录");
                return result;
            }
            return paperTestService.submitAnswerEn(request, user);
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.trim().isEmpty()) {
                errorMsg = "提交答案失败，后端发生未知异常";
            }
            result.put("success", false);
            result.put("message", errorMsg);
        }
        return result;
    }

    // 获取测试结果详情（英文试卷）添加迁移日志
    @GetMapping("/test-record-en-details/{testEnId}")
    public Map<String, Object> getTestRecordEnDetails(@PathVariable Long testEnId) {
        return paperTestService.getTestRecordEnDetails(testEnId);
    }
}
