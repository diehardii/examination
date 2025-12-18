package seucxxy.csd.backend.hs3.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import seucxxy.csd.backend.hs3.service.HS3ListeningAudioService;
import seucxxy.csd.backend.common.entity.User;
import seucxxy.csd.backend.common.util.UserSessionUtil;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/hs3/audio")
@CrossOrigin(origins = "http://localhost:5002", allowCredentials = "true")
public class HS3AudioController {
    
    @Autowired
    private HS3ListeningAudioService listeningAudioService;

    /**
     * 为高考听力生成完整音频（逐段调用Coze，最后合并）
     */
    @PostMapping("/generate-listening-full")
    public ResponseEntity<Map<String, Object>> generateFullListeningAudio(
            @RequestBody Map<String, Object> request,
            HttpSession session) {
        // 检查用户登录状态
        User user = UserSessionUtil.getCurrentUser(session);
        if (user == null) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "用户未登录");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResult);
        }

        try {
            // 从请求中获取所有segments数据
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> segments = (List<Map<String, Object>>) request.get("segments");
            
            if (segments == null || segments.isEmpty()) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("error", "未提供segments数据");
                return ResponseEntity.badRequest().body(errorResult);
            }
            
            // 调用服务生成完整音频
            String audioUrl = listeningAudioService.generateFullListeningAudio(segments);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("audio_url", audioUrl);
            result.put("message", "听力音频生成成功");
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "音频生成失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }
}
