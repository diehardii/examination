package seucxxy.csd.backend.common.controller;


import seucxxy.csd.backend.common.entity.Department;
import seucxxy.csd.backend.common.service.DepartmentService;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    public List<Department> getAllDepartments() {
        return departmentService.getAllDepartments();
    }

    @GetMapping("/by-university/{universityId}")
    public List<Department> getByUniversityId(@PathVariable Integer universityId) {
        return departmentService.getDepartmentsByUniversityId(universityId);
    }

    @PostMapping
    public Department createDepartment(@RequestBody Department department) {
        return departmentService.createDepartment(department);

    }

    @PutMapping("/{id}")
    public Department updateDepartment(@PathVariable Integer id, @RequestBody Department department) {
        department.setDepartmentId(id);
        return departmentService.updateDepartment(department);
    }

    @DeleteMapping("/{id}")
    public void deleteDepartment(@PathVariable Integer id) {
        departmentService.deleteDepartment(id);
    }
}