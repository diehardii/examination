package seucxxy.csd.backend.hs3.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import seucxxy.csd.backend.hs3.service.HS3PaperGenService;
import seucxxy.csd.backend.common.util.UserSessionUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * HS3 高考英语试卷生成控制器
 */
@RestController
@RequestMapping("/api/hs3/paper-gen")
@CrossOrigin(origins = {"http://localhost:5002", "http://localhost:5003"}, allowCredentials = "true")
public class HS3PaperGenController {

    private final HS3PaperGenService paperGenService;

    @Autowired
    public HS3PaperGenController(HS3PaperGenService paperGenService) {
        this.paperGenService = paperGenService;
    }

    /**
     * 生成高考英语试卷（同步模式）
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generatePaper(@RequestBody Map<String, String> request,
                                                            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = UserSessionUtil.getCurrentUserId(session);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "用户未登录");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String subjectEn = request.getOrDefault("subjectEn", "高考");
            String source = request.getOrDefault("exam_paper_en_source", "AIfromreal");

            // 调用服务生成试卷
            Map<String, Object> result = paperGenService.generatePaper(subjectEn, source, userId);
            
            response.put("success", true);
            response.put("examPaperEnId", result.get("examPaperEnId"));
            response.put("segments", result.get("segments"));
            response.put("exam_paper_en_source", result.get("exam_paper_en_source"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("生成高考试卷失败: " + e.getMessage());
            e.printStackTrace();
            if ("用户未登录".equals(e.getMessage())) {
                response.put("success", false);
                response.put("message", "用户未登录");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            response.put("success", false);
            response.put("message", "生成试卷失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 存储segment到ChromaDB
     */
    @PostMapping("/store-segment")
    public ResponseEntity<Map<String, Object>> storeSegment(@RequestBody Map<String, Object> request,
                                                           HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = UserSessionUtil.getCurrentUserId(session);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "用户未登录");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String examPaperId = (String) request.get("examPaperId");
            String examPaperName = (String) request.get("examPaperName");
            String subject = (String) request.get("subject");
            @SuppressWarnings("unchecked")
            Map<String, Object> segment = (Map<String, Object>) request.get("segment");
            String examPaperEnSource = (String) request.get("examPaperEnSource");

            paperGenService.storeSegmentToChroma(examPaperId, examPaperName, subject, segment, examPaperEnSource);

            response.put("success", true);
            response.put("message", "存储成功");
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
