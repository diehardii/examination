package seucxxy.csd.backend.common.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import seucxxy.csd.backend.common.dto.ClassStudentDto;
import seucxxy.csd.backend.common.dto.StudentClassDto;
import seucxxy.csd.backend.common.service.StudentClassService;

@RestController
@RequestMapping("/api/student-class")
@CrossOrigin(origins = "*")
public class StudentClassController {
    
    private final StudentClassService studentClassService;

    public StudentClassController(StudentClassService studentClassService) {
        this.studentClassService = studentClassService;
    }
    
    // 获取所有学生及其班级信息
    @GetMapping("/students")
    public ResponseEntity<?> listAllStudents() {
        try {
            List<StudentClassDto> students = studentClassService.listAllStudents();
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("获取学生列表失败: " + e.getMessage());
        }
    }
    
    // 获取未分配班级的学生
    @GetMapping("/students/unassigned")
    public ResponseEntity<List<StudentClassDto>> listUnassignedStudents() {
        try {
            List<StudentClassDto> students = studentClassService.listUnassignedStudents();
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 获取学生的班级信息
    @GetMapping("/students/{studentId}")
    public ResponseEntity<StudentClassDto> getStudentClassInfo(@PathVariable Integer studentId) {
        try {
            StudentClassDto student = studentClassService.getStudentClassInfo(studentId);
            return ResponseEntity.ok(student);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 为学生分配班级
    @PostMapping("/students/{studentId}/assign")
    public ResponseEntity<String> assignClassToStudent(
            @PathVariable Integer studentId,
            @RequestBody Map<String, Integer> request) {
        try {
            Integer classId = request.get("classId");
            studentClassService.assignClassToStudent(studentId, classId);
            return ResponseEntity.ok("分配成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("分配失败: " + e.getMessage());
        }
    }
    
    // 获取所有班级及其学生统计
    @GetMapping("/classes")
    public ResponseEntity<?> listClassesWithStudentCount() {
        try {
            List<ClassStudentDto> classes = studentClassService.listClassesWithStudentCount();
            return ResponseEntity.ok(classes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("获取班级列表失败: " + e.getMessage());
        }
    }
    
    // 获取班级的学生列表
    @GetMapping("/classes/{classId}/students")
    public ResponseEntity<ClassStudentDto> getClassStudents(@PathVariable Integer classId) {
        try {
            ClassStudentDto result = studentClassService.getClassStudents(classId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // 批量为学生分配班级
    @PostMapping("/classes/{classId}/assign-students")
    public ResponseEntity<String> batchAssignStudentsToClass(
            @PathVariable Integer classId,
            @RequestBody Map<String, List<Integer>> request) {
        try {
            List<Integer> studentIds = request.get("studentIds");
            studentClassService.batchAssignStudentsToClass(classId, studentIds);
            return ResponseEntity.ok("批量分配成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("批量分配失败: " + e.getMessage());
        }
    }
    
    // 移除学生的班级分配
    @DeleteMapping("/students/{studentId}/remove")
    public ResponseEntity<String> removeStudentFromClass(@PathVariable Integer studentId) {
        try {
            studentClassService.removeStudentFromClass(studentId);
            return ResponseEntity.ok("移除成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("移除失败: " + e.getMessage());
        }
    }
}
