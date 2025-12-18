package seucxxy.csd.backend.cet4.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import seucxxy.csd.backend.cet4.service.CET4ListeningAudioService;
import seucxxy.csd.backend.common.entity.User;
import seucxxy.csd.backend.common.util.UserSessionUtil;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/audio")
@CrossOrigin(origins = "http://localhost:5002", allowCredentials = "true")
public class CET4AudioController {

    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private CET4ListeningAudioService listeningAudioService;
    
    // 更新为新的Python服务地址
    private static final String AUDIO_SERVICE_URL = "http://localhost:5000/api/audio";

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkAudioService() {
        try {
            String url = AUDIO_SERVICE_URL + "/health";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(response.getBody());
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "error");
                errorResponse.put("message", "音频服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
            }
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "无法连接到音频服务: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }
    }

    @PostMapping("/synthesize-combined")
    public ResponseEntity<Map<String, Object>> synthesizeCombined(
            @RequestBody Map<String, Object> request,
            HttpSession session) {
        
        // 检查用户登录状态
        User user = UserSessionUtil.getCurrentUser(session);

        try {
            String url = AUDIO_SERVICE_URL + "/synthesize-combined";
            
            // 转发请求到音频服务
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
                );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                // 如果成功，转换音频URL为绝对路径
                if (Boolean.TRUE.equals(responseBody.get("success")) && responseBody.containsKey("audio_url")) {
                    String audioUrl = (String) responseBody.get("audio_url");
                    if (audioUrl != null && audioUrl.startsWith("/audio/")) {
                        responseBody.put("audio_url", AUDIO_SERVICE_URL + audioUrl);
                    }
                }
                
                return ResponseEntity.ok(responseBody);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "error", "音频生成服务错误"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "音频生成失败: " + e.getMessage()));
        }
    }

    @PostMapping("/synthesize")
    public ResponseEntity<Map<String, Object>> synthesize(
            @RequestBody Map<String, Object> request,
            HttpSession session) {
        
        // 检查用户登录状态
        User user = UserSessionUtil.getCurrentUser(session);

        try {
            String url = AUDIO_SERVICE_URL + "/synthesize";
            
            // 转发请求到音频服务
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
                );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                
                // 转换音频URL
                Object resultsObj = responseBody.get("results");
                if (Boolean.TRUE.equals(responseBody.get("success")) && resultsObj instanceof List<?>) {
                    for (Object item : (List<?>) resultsObj) {
                        if (!(item instanceof Map)) {
                            continue;
                        }
                        @SuppressWarnings("unchecked")
                        Map<String, Object> result = (Map<String, Object>) item;
                        if (result.containsKey("url")) {
                            String url1 = (String) result.get("url");
                            if (url1 != null && url1.startsWith("/audio/")) {
                                result.put("url", AUDIO_SERVICE_URL + url1);
                            }
                        }
                    }
                }
                
                return ResponseEntity.ok(responseBody);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "error", "音频生成服务错误"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "音频生成失败: " + e.getMessage()));
        }
    }

    @GetMapping("/audio/{filename}")
    public ResponseEntity<byte[]> getAudioFile(@PathVariable String filename, HttpSession session) {
        // 检查用户登录状态
        User user = UserSessionUtil.getCurrentUser(session);

        try {
            String url = AUDIO_SERVICE_URL + "/audio/" + filename;
            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                return new ResponseEntity<>(response.getBody(), headers, HttpStatus.OK);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 为多个听力segment生成完整音频（逐段调用Coze，最后合并）
     */
    @PostMapping("/generate-listening-full")
    public ResponseEntity<Map<String, Object>> generateFullListeningAudio(
            @RequestBody Map<String, Object> request,
            HttpSession session) {
        // 检查用户登录状态
        User user = UserSessionUtil.getCurrentUser(session);

        try {
            // 从请求中获取所有segments数据
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> segments = (List<Map<String, Object>>) request.get("segments");
            // 新增：获取英文试卷来源标识字符串
            String examPaperEnSource = (String) request.getOrDefault("exam_paper_en_source", "");
            
            if (segments == null || segments.isEmpty()) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("success", false);
                errorResult.put("error", "未提供segments数据");
                return ResponseEntity.badRequest().body(errorResult);
            }
            
            // 调用服务生成完整音频（传入 exam_paper_en_source）
            String audioUrl = listeningAudioService.generateFullListeningAudio(segments, examPaperEnSource);
            
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