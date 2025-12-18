package seucxxy.csd.backend.common.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seucxxy.csd.backend.common.entity.EducationStage;
import seucxxy.csd.backend.common.service.EducationStageService;

import java.util.List;

@RestController
@RequestMapping("/api/education-stages")
public class EducationStageController {

    private final EducationStageService educationStageService;

    public EducationStageController(EducationStageService educationStageService) {
        this.educationStageService = educationStageService;
    }

    @GetMapping
    public List<EducationStage> list() {
        return educationStageService.listAll();
    }
}
