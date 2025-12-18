package seucxxy.csd.backend.cet4.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cet4/knowledge-manager")
@CrossOrigin(origins = {"http://localhost:5002", "http://localhost:5003"}, allowCredentials = "true")
@RequiredArgsConstructor
public class CET4KnowledgeManagerController {

    @PostMapping("/set-subject")
    public ResponseEntity<Map<String, Object>> setSubject(
            @RequestBody Map<String, String> request,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        try {
            String subject = request.get("subject");
            if (subject == null || subject.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "科目不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            session.setAttribute("subject", subject);
            response.put("success", true);
            response.put("message", "科目设置成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("设置科目失败: " + e.getMessage());
            e.printStackTrace();
            
            response.put("success", false);
            response.put("message", "设置科目失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/get-subject")
    public ResponseEntity<Map<String, Object>> getSubject(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        try {
            String subject = (String) session.getAttribute("subject");
            
            // 如果没有设置科目，默认返回 CET4
            if (subject == null || subject.isEmpty()) {
                subject = "CET4";
            }
            
            response.put("success", true);
            response.put("subject", subject);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("获取科目失败: " + e.getMessage());
            e.printStackTrace();
            
            response.put("success", false);
            response.put("message", "获取科目失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
