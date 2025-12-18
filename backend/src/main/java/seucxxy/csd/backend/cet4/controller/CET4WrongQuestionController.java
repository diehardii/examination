package seucxxy.csd.backend.cet4.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import seucxxy.csd.backend.cet4.dto.CET4WrongQuestionRecordDTO;
import seucxxy.csd.backend.cet4.service.CET4WrongQuestionService;
import seucxxy.csd.backend.common.entity.User;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 错题答疑Controller
 * 提供错题列表查询接口
 */
@RestController
@RequestMapping("/api/cet4/wrong-questions")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5002", "http://localhost:5003", "http://localhost:3000", "http://localhost:8081"}, allowCredentials = "true")
public class CET4WrongQuestionController {

    private static final Logger logger = LoggerFactory.getLogger(CET4WrongQuestionController.class);

    private final CET4WrongQuestionService wrongQuestionService;

    public CET4WrongQuestionController(CET4WrongQuestionService wrongQuestionService) {
        this.wrongQuestionService = wrongQuestionService;
    }

    /**
     * 获取当前用户的错题列表（支持分页）
     * 
     * @param sortBy 排序依据: time(时间) 或 percent(正确率)
     * @param sortOrder 排序顺序: ASC(正序) 或 DESC(倒序)
     * @param page 页码（从1开始，默认为1）
     * @param pageSize 每页大小（默认为10）
     * @param session HTTP会话，用于获取当前用户
     * @return 错题列表（包含分页信息）
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getWrongQuestionsList(
            @RequestParam(defaultValue = "time") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 从session获取当前用户
            User currentUser = (User) session.getAttribute("user");
            
            if (currentUser == null) {
                logger.warn("[WrongQuestionList] 未登录用户访问");
                response.put("success", false);
                response.put("message", "用户未登录");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            Long userId = currentUser.getId();
            logger.info("[WrongQuestionList] userId={}, sortBy={}, sortOrder={}, page={}, pageSize={}", 
                    userId, sortBy, sortOrder, page, pageSize);
            
            // 验证分页参数
            if (page < 1) page = 1;
            if (pageSize < 1) pageSize = 10;
            if (pageSize > 100) pageSize = 100; // 限制最大每页数量
            
            // 查询总数
            int total = wrongQuestionService.countWrongQuestions(userId);
            
            // 分页查询错题列表
            List<CET4WrongQuestionRecordDTO> wrongQuestions = wrongQuestionService.getWrongQuestionsWithPagination(
                    userId, sortBy, sortOrder, page, pageSize);
            
            // 计算总页数
            int totalPages = (int) Math.ceil((double) total / pageSize);
            
            logger.info("[WrongQuestionList] 返回 {} 条记录，共 {} 条，{} 页", 
                    wrongQuestions != null ? wrongQuestions.size() : 0, total, totalPages);
            
            response.put("success", true);
            response.put("data", wrongQuestions);
            response.put("total", total);
            response.put("page", page);
            response.put("pageSize", pageSize);
            response.put("totalPages", totalPages);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[WrongQuestionList] 获取错题列表失败", e);
            
            response.put("success", false);
            response.put("message", "获取错题列表失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取错题详情
     * 
     * @param testEnId 考试ID
     * @param segmentId 段落ID
     * @param session HTTP会话
     * @return 错题详情
     */
    @GetMapping("/detail")
    public ResponseEntity<Map<String, Object>> getWrongQuestionDetail(
            @RequestParam Long testEnId,
            @RequestParam String segmentId,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 从session获取当前用户
            User currentUser = (User) session.getAttribute("user");
            if (currentUser == null) {
                logger.warn("[WrongQuestionDetail] 未登录用户访问");
                response.put("success", false);
                response.put("message", "用户未登录");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            logger.info("[WrongQuestionDetail] userId={}, testEnId={}, segmentId={}", currentUser.getId(), testEnId, segmentId);
            
            // 调用Service获取错题详情
            Map<String, Object> detail = wrongQuestionService.getWrongQuestionDetail(
                    testEnId, segmentId, currentUser.getId());
            
            if (detail != null) {
                logger.info("[WrongQuestionDetail] questionContent keys={}, userAnswers={}, correctAnswers={}, hasUserAnswer={} hasAiScore={}",
                        detail.get("questionContent") instanceof Map ? ((Map<?, ?>) detail.get("questionContent")).keySet() : detail.get("questionContent"),
                        detail.get("userAnswers") instanceof Map ? ((Map<?, ?>) detail.get("userAnswers")).size() : detail.get("userAnswers"),
                        detail.get("correctAnswers") instanceof Map ? ((Map<?, ?>) detail.get("correctAnswers")).size() : detail.get("correctAnswers"),
                        detail.get("userAnswer") != null,
                        detail.get("aiScore") != null);
            }

            response.put("success", true);
            response.put("data", detail);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[WrongQuestionDetail] 获取错题详情失败", e);
            
            response.put("success", false);
            response.put("message", "获取错题详情失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
