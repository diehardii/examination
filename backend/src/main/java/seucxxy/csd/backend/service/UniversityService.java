package seucxxy.csd.backend.service;



import org.springframework.stereotype.Service;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import seucxxy.csd.backend.entity.University;
import seucxxy.csd.backend.mapper.DepartmentMapper;
import seucxxy.csd.backend.mapper.UniversityMapper;
import seucxxy.csd.backend.service.DepartmentService;



@Service
public class UniversityService {

        private final UniversityMapper universityMapper;
        private final DepartmentMapper departmentMapper;

        public UniversityService(UniversityMapper universityMapper,
                                 DepartmentMapper departmentMapper) {
            this.universityMapper = universityMapper;
            this.departmentMapper = departmentMapper;
        }


        public List<University> getAllUniversities() {
            return universityMapper.findAll();
        }


        public University getUniversityById(Integer id) {
            return universityMapper.findById(id);
        }


        public University createUniversity(University university) {
            System.out.println(university.getUniversityId()+university.getUniversityName());
            universityMapper.insert(university);
            return university; // 返回带ID的实体
        }


    public University updateUniversity(University university) {
        universityMapper.updateUniversity(university);
        return university;
    }

    @Transactional
    public void deleteUniversity(Integer id) {
        // 先删除所有关联的院系
        departmentMapper.deleteByUniversityId(id);
        // 再删除大学
        universityMapper.deleteUniversity(id);
    }

}