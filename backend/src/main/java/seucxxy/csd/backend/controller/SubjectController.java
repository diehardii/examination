package seucxxy.csd.backend.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import seucxxy.csd.backend.entity.Subject;
import seucxxy.csd.backend.service.SubjectService;

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
            System.out.println("Name"+subject.getSubjectName());
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
            System.out.println("测试开始了");
            System.out.println("Name"+newSubject.getSubjectName());
            System.out.println("oldName"+oldName);
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
}