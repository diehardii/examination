package seucxxy.csd.backend.cet4.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seucxxy.csd.backend.cet4.service.CET4TutoringAnalysisService;
import seucxxy.csd.backend.common.util.UserSessionUtil;

import java.util.Map;

@RestController
@RequestMapping("/api/cet4/tutoring-analysis")
@CrossOrigin(origins = {"http://localhost:5002", "http://localhost:5003", "http://localhost:5173"}, allowCredentials = "true")
public class CET4TutoringAnalysisController {

    private final CET4TutoringAnalysisService tutoringAnalysisService;

    public CET4TutoringAnalysisController(CET4TutoringAnalysisService tutoringAnalysisService) {
        this.tutoringAnalysisService = tutoringAnalysisService;
    }

    @PostMapping("/homepage")
    public ResponseEntity<Map<String, Object>> triggerHomepageAnalysis(HttpSession session) {
        try {
            Long userId = UserSessionUtil.getCurrentUserId(session);
            tutoringAnalysisService.triggerHomepageAnalysis(userId);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("success", true));
        } catch (Exception e) {
            if ("用户未登录".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", e.getMessage()
                ));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
