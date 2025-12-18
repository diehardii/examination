package seucxxy.csd.backend.common.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seucxxy.csd.backend.common.dto.TeacherClassesRequest;
import seucxxy.csd.backend.common.dto.TeacherProfileDto;
import seucxxy.csd.backend.common.dto.TeacherStageRequest;
import seucxxy.csd.backend.common.entity.Teacher;
import seucxxy.csd.backend.common.service.TeacherService;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @GetMapping
    public List<TeacherProfileDto> listTeachers() {
        return teacherService.listTeachers();
    }

    @GetMapping("/{teacherId}/stages/{stageId}/classes")
    public List<Integer> getTeacherClassesByStage(@PathVariable Long teacherId, @PathVariable Integer stageId) {
        return teacherService.getTeacherClassesByStage(teacherId, stageId);
    }

    @PostMapping("/{teacherId}/stages/{stageId}/classes")
    public void assignClassesByStage(
            @PathVariable Long teacherId, 
            @PathVariable Integer stageId,
            @RequestBody TeacherClassesRequest request) {
        teacherService.assignClassesByStage(teacherId, stageId, request.getClassIds());
    }

    @GetMapping("/{teacherId}/assignments")
    public java.util.Map<Integer, List<Integer>> getTeacherAllAssignments(@PathVariable Long teacherId) {
        return teacherService.getTeacherAllAssignments(teacherId);
    }
}
