package seucxxy.csd.backend.common.controller;

import org.springframework.web.bind.annotation.*;
import seucxxy.csd.backend.common.entity.ClassInfo;
import seucxxy.csd.backend.common.service.ClassesService;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
public class ClassesController {

    private final ClassesService classesService;

    public ClassesController(ClassesService classesService) {
        this.classesService = classesService;
    }

    @GetMapping
    public List<ClassInfo> list() {
        return classesService.listAll();
    }

    @GetMapping("/by-stage/{stageId}")
    public List<ClassInfo> listByStage(@PathVariable Integer stageId) {
        return classesService.listByStage(stageId);
    }

    @PostMapping
    public ClassInfo create(@RequestBody ClassInfo classInfo) {
        return classesService.create(classInfo);
    }

    @PutMapping
    public void update(@RequestBody ClassInfo classInfo) {
        if (classInfo.getClassId() == null) {
            throw new IllegalArgumentException("classId is required for update");
        }
        classesService.update(classInfo);
    }

    @DeleteMapping("/{classId}")
    public void delete(@PathVariable Integer classId) {
        classesService.delete(classId);
    }
}
