package seucxxy.csd.backend.service;

import seucxxy.csd.backend.entity.Department;
import seucxxy.csd.backend.mapper.DepartmentMapper;
import org.springframework.stereotype.Service;
import java.util.List;


@Service
public class DepartmentService {




        private final DepartmentMapper departmentMapper;

        public DepartmentService(DepartmentMapper departmentMapper) {
            this.departmentMapper = departmentMapper;
        }


        public List<Department> getAllDepartments() {
            return departmentMapper.findAll();
        }


        public List<Department> getDepartmentsByUniversityId(Integer universityId) {
            return departmentMapper.findByUniversityId(universityId);
        }


        public Department createDepartment(Department department) {
            departmentMapper.insert(department);
            return department; // 返回带ID的实体
        }


    public List<Department> getDepartmentsByUniversity(Integer universityId) {
        return departmentMapper.findByUniversityId(universityId);
    }




    public Department updateDepartment(Department department) {
        departmentMapper.update(department);
        return department;
    }


    public void deleteDepartment(Integer id) {
        departmentMapper.delete(id);
    }


    public void deleteByUniversityId(Integer universityId) {
        departmentMapper.deleteByUniversityId(universityId);
    }
    }