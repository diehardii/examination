package seucxxy.csd.backend.common.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import seucxxy.csd.backend.common.entity.User;
import seucxxy.csd.backend.common.service.UserTeacherService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher-student")
@CrossOrigin(origins = "*")
public class UserTeacherController {
    
    @Autowired
    private UserTeacherService userTeacherService;
    
    // 根据教师ID获取学生列表
    @GetMapping("/teacher/{teacherId}/students")
    public ResponseEntity<List<User>> getStudentsByTeacherId(@PathVariable Long teacherId) {
        try {
            List<User> students = userTeacherService.getStudentsByTeacherId(teacherId);
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 保存教师学生关系
    @PostMapping("/save")
    public ResponseEntity<String> saveTeacherStudentRelation(@RequestBody Map<String, Object> request) {
        try {
            Long teacherId = Long.valueOf(request.get("teacherId").toString());
            @SuppressWarnings("unchecked")
            List<Long> studentIds = (List<Long>) request.get("studentIds");
            
            userTeacherService.saveTeacherStudentRelation(teacherId, studentIds);
            return ResponseEntity.ok("保存成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("保存失败: " + e.getMessage());
        }
    }
} 