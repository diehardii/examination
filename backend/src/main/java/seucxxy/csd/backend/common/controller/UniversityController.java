package seucxxy.csd.backend.common.controller;



import seucxxy.csd.backend.common.entity.University;
import seucxxy.csd.backend.common.service.UniversityService;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/universities")
public class UniversityController {

    private final UniversityService universityService;

    public UniversityController(UniversityService universityService) {
        this.universityService = universityService;
    }

    @GetMapping
    public List<University> getAllUniversities() {
        return universityService.getAllUniversities();
    }

    @GetMapping("/{id}")
    public University getUniversityById(@PathVariable Integer id) {
        return universityService.getUniversityById(id);
    }

    @PostMapping
    public University createUniversity(@RequestBody University university) {
        return universityService.createUniversity(university);
    }


    @PutMapping("/{id}")
    public University updateUniversity(@PathVariable Integer id, @RequestBody University university) {
        university.setUniversityId(id);
        return universityService.updateUniversity(university);
    }

    @DeleteMapping("/{id}")
    public void deleteUniversity(@PathVariable Integer id) {
        universityService.deleteUniversity(id);
    }
}
