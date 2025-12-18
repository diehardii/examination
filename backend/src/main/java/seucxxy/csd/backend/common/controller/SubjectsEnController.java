package seucxxy.csd.backend.common.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import seucxxy.csd.backend.common.entity.SubjectsEn;
import seucxxy.csd.backend.common.service.SubjectsEnService;

import java.util.List;

@RestController
@RequestMapping("/api/subjects-en")
@CrossOrigin(origins = {"http://localhost:5002", "http://localhost:5003"})
public class SubjectsEnController {
    
    private static final Logger logger = LoggerFactory.getLogger(SubjectsEnController.class);
    
    @Autowired
    private SubjectsEnService subjectsEnService;
    
    @GetMapping("/list")
    public ResponseEntity<List<SubjectsEn>> getAllSubjects() {
        System.out.println("========== 收到获取科目列表请求 ==========");
        List<SubjectsEn> subjects = subjectsEnService.getAllSubjects();
        System.out.println("查询到 " + (subjects != null ? subjects.size() : 0) + " 条科目数据");
        if (subjects != null && !subjects.isEmpty()) {
            System.out.println("第一条数据: id=" + subjects.get(0).getId() + ", name=" + subjects.get(0).getSubjectEnName());
        }
        System.out.println("========================================");
        return ResponseEntity.ok(subjects);
    }
    
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        try {
            // 测试数据库连接
            subjectsEnService.deleteAllSubjects();
            
            // 插入一条测试数据
            subjectsEnService.initTestData();
            
            // 查询数据
            List<SubjectsEn> subjects = subjectsEnService.getAllSubjects();
            return ResponseEntity.ok("测试成功，插入数据条数: " + subjects.size());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("测试失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/init-data")
    public ResponseEntity<String> initData() {
        try {
            // 先清空表
            subjectsEnService.deleteAllSubjects();
            
            // 插入测试数据
            subjectsEnService.initTestData();
            return ResponseEntity.ok("测试数据初始化成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("初始化失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/name/{name}")
    public ResponseEntity<SubjectsEn> getSubjectByName(@PathVariable String name) {
        SubjectsEn subject = subjectsEnService.getSubjectByName(name);
        if (subject != null) {
            return ResponseEntity.ok(subject);
        }
        return ResponseEntity.notFound().build();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<SubjectsEn> getSubjectById(@PathVariable Long id) {
        SubjectsEn subject = subjectsEnService.getSubjectById(id);
        if (subject != null) {
            return ResponseEntity.ok(subject);
        }
        return ResponseEntity.notFound().build();
    }
}