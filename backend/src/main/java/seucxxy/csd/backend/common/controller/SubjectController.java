package seucxxy.csd.backend.common.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import seucxxy.csd.backend.common.entity.Subject;
import seucxxy.csd.backend.common.service.SubjectService;

import java.util.List;

@RestController
@RequestMapping("/api/subject")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }


    @PostMapping("/")
    public ResponseEntity<?> addSubject(@RequestBody Subject subject) {
        try {
            subjectService.addSubject(subject);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{oldName}")
    public ResponseEntity<?> updateSubject(
            @PathVariable String oldName,
            @RequestBody Subject newSubject) {
        try {
            subjectService.updateSubject(subjectService.getSubjectBySubjectName(oldName).getSubjectId(), newSubject);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{subjectName}")
    public ResponseEntity<?> deleteSubject(@PathVariable String subjectName) {
        try {
            subjectService.deleteSubject(subjectService.getSubjectBySubjectName(subjectName).getSubjectId());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<String>> getSubjectsForTeacher(@PathVariable Long teacherId) {
        try {
            List<String> subjects = subjectService.getSubjectsForTeacher(teacherId);
            return ResponseEntity.ok(subjects);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/name/{subjectName}")
    public ResponseEntity<Subject> getSubjectByName(@PathVariable String subjectName) {
        try {
            Subject subject = subjectService.getSubjectBySubjectName(subjectName);
            return ResponseEntity.ok(subject);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/subjects")
    public ResponseEntity<List<Subject>> getAllSubjects() {
        try {
            List<Subject> subjects = subjectService.getAllSubjects();
            return ResponseEntity.ok(subjects);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/user-subjects")
    public ResponseEntity<List<String>> getUserSubjects() {
        try {
            // 简单使用 SecurityContext 获取当前用户名
            String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
            List<String> subjects = subjectService.getSubjectsForUser(username);
            return ResponseEntity.ok(subjects);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

}