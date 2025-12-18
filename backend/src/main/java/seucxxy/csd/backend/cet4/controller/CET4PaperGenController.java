package seucxxy.csd.backend.cet4.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import seucxxy.csd.backend.cet4.service.CET4PaperGenService;
import seucxxy.csd.backend.common.util.UserSessionUtil;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cet4/paper-gen")
@CrossOrigin(origins = {"http://localhost:5002", "http://localhost:5003"}, allowCredentials = "true")
public class CET4PaperGenController {

    private final CET4PaperGenService paperGenService;

    @Autowired
    public CET4PaperGenController(CET4PaperGenService paperGenService) {
        this.paperGenService = paperGenService;
    }

    @PostMapping("/coze/generate-single")
    public ResponseEntity<Map<String, Object>> generateSingleWithCoze(@RequestBody Map<String, String> request,
                                                                       HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = UserSessionUtil.getCurrentUserId(session);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "用户未登录");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            String examTopic = request.get("examTopic");
            String inputExamPaperSamp = request.get("inputExamPaperSamp");
            String examPaperEnSource = request.get("examPaperEnSource");
            String segmentIdSelf = request.get("segmentIdSelf");

            Map<String, Object> result = paperGenService.generateSingleQuestionDirect(
                    examTopic,
                    inputExamPaperSamp,
                    examPaperEnSource,
                    segmentIdSelf
            );

            response.put("success", true);
            response.put("data", result.get("output"));
            response.put("answers", result.get("answers"));
            response.put("rawResponse", result.get("rawResponse"));
            response.put("examTopic", examTopic);
            response.put("exam_paper_en_source", examPaperEnSource);
            response.put("segment_id", segmentIdSelf);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generatePaper(@RequestBody Map<String, String> request,
                                                            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            String examPaperEnName = request.get("examPaperEnName");
            String examPaperEnDesc = request.get("examPaperEnDesc");
            String subjectEn = request.get("subjectEn");
            String source = request.getOrDefault("exam_paper_en_source", "AIfromreal");
            Long userId = UserSessionUtil.getCurrentUserId(session);
            // 调用带来源的生成方法
            Map<String, Object> result = paperGenService.generatePaper(examPaperEnName, examPaperEnDesc, subjectEn, source, userId);
            response.put("success", true);
            response.put("examPaperEnId", result.get("examPaperEnId"));
            response.put("units", result.get("units"));
            response.put("exam_paper_en_source", result.get("exam_paper_en_source"));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("生成试卷失败: " + e.getMessage());
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
     * 并发生成接口（兼容原 /paper-gen/concurrent/generate 路由，内部已统一使用并行实现）。
     */
    @PostMapping("/concurrent/generate")
    public ResponseEntity<Map<String, Object>> generatePaperConcurrent(@RequestBody Map<String, String> request,
                                                                       HttpSession session) {
        return generatePaper(request, session);
    }

    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> savePaperMeta(@RequestBody Map<String, String> request,
                                                            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long userId = UserSessionUtil.getCurrentUserId(session);
            String name = request.get("examPaperEnName");
            String desc = request.get("examPaperEnDesc");
            String subjectEn = request.getOrDefault("subjectEn", "CET4");
            String source = request.getOrDefault("exam_paper_en_source", "AIfromreal");
            Long examPaperEnId = paperGenService.createExamPaperRecord(name, desc, subjectEn, source, userId);
            response.put("success", true);
            response.put("examPaperEnId", examPaperEnId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception ex) {
            response.put("success", false);
            response.put("message", ex.getMessage());
            if ("用户未登录".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
