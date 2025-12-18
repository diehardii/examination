package seucxxy.csd.backend.hs3.controller;

import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import seucxxy.csd.backend.common.entity.User;
import seucxxy.csd.backend.common.util.UserSessionUtil;
import seucxxy.csd.backend.hs3.entity.HS3UserTestRecordEn;
import seucxxy.csd.backend.hs3.mapper.HS3UserTestRecordEnMapper;
import seucxxy.csd.backend.hs3.service.HS3PaperTestService;

import java.util.HashMap;
import java.util.Map;

/**
 * HS3考试提交和结果查看Controller
 */
@RestController
@RequestMapping("/api/hs3/test")
public class HS3TestController {

    private static final Logger logger = LoggerFactory.getLogger(HS3TestController.class);

    private final HS3PaperTestService paperTestService;
    private final HS3UserTestRecordEnMapper userTestRecordEnMapper;

    @Autowired
    public HS3TestController(
            HS3PaperTestService paperTestService,
            HS3UserTestRecordEnMapper userTestRecordEnMapper) {
        this.paperTestService = paperTestService;
        this.userTestRecordEnMapper = userTestRecordEnMapper;
    }

    /**
     * 提交答案
     * POST /api/hs3/test/submit
     * Body: {
     *   "examPaperEnId": 123,
     *   "answers": {
     *     "listening": {"segment_id-questionNumber": "A", ...},
     *     "reading": {...},
     *     "cloze": {...},
     *     "grammar": {...},
     *     "writing": {"segment_id": "essay text..."},
     *     "application": {"segment_id": "letter text..."}
     *   }
     * }
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitAnswer(@RequestBody Map<String, Object> request, HttpSession session) {
        try {
            User user = UserSessionUtil.getCurrentUser(session);
            if (user == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "用户未登录");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            Map<String, Object> result = paperTestService.submitAnswerEn(request, user);
            if (Boolean.TRUE.equals(result.get("success"))) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
        } catch (Exception e) {
            logger.error("[HS3-Test] 提交答案失败: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "提交失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 获取考试结果详情（包含原题内容，类似CET4）
     * GET /api/hs3/test/result-details/{testEnId}
     */
    @GetMapping("/result-details/{testEnId}")
    public ResponseEntity<?> getTestResultDetails(@PathVariable Long testEnId, HttpSession session) {
        try {
            User user = UserSessionUtil.getCurrentUser(session);
            if (user == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "用户未登录");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            // 获取考试记录（用于权限验证）
            HS3UserTestRecordEn testRecord = userTestRecordEnMapper.getUserTestRecordEnById(testEnId);
            if (testRecord == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "考试记录不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            // 验证是否是当前用户的考试记录
            if (!testRecord.getUserId().equals(user.getId())) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "无权查看此考试记录");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            // 调用service获取详情（包含原题）
            Map<String, Object> result = paperTestService.getTestRecordEnDetails(testEnId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[HS3-Test] 获取考试结果详情失败: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "获取考试结果详情失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
