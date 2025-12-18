package seucxxy.csd.backend.common.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import seucxxy.csd.backend.common.entity.Grade;
import seucxxy.csd.backend.common.service.GradeService;

import java.util.List;

@RestController
@RequestMapping("/api/grades")
public class GradeController {

    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @GetMapping
    public List<Grade> list(@RequestParam(value = "stageId", required = false) Integer stageId) {
        if (stageId != null) {
            return gradeService.listByStage(stageId);
        }
        return gradeService.listAll();
    }
}
