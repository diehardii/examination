package seucxxy.csd.backend.common.mapper;



import org.apache.ibatis.annotations.*;

import seucxxy.csd.backend.common.entity.Department;
import seucxxy.csd.backend.common.entity.University;

import java.util.List;

@Mapper
public interface DepartmentMapper {

    @Select("SELECT * FROM department")
    @Results({
            @Result(property = "departmentId", column = "department_id", id = true),
            @Result(property = "departmentName", column = "department_name"),
            @Result(property = "universityId", column = "university_id")
    })
    List<Department> findAll();

    @Select("SELECT * FROM department")
    @Results({
            @Result(property = "departmentId", column = "department_id", id = true),
            @Result(property = "departmentName", column = "department_name"),
            @Result(property = "universityId", column = "university_id"),
            @Result(property = "university", column = "university_id",
                    one = @One(select = "com.examinai.mapper.UniversityMapper.findById"))
    })
    List<Department> findAllWithUniversity();


    @Select("SELECT * FROM department WHERE department_id = #{id}")
    Department findById(Integer id);

    @Select("SELECT * FROM department WHERE university_id = #{universityId}")
    @Results({
            @Result(property = "departmentId", column = "department_id", id = true),
            @Result(property = "departmentName", column = "department_name"),
            @Result(property = "universityId", column = "university_id")
    })
    List<Department> findByUniversityId(Integer universityId);

    @Insert("INSERT INTO department(university_id, department_name) VALUES(#{universityId}, #{departmentName})")
    @Options(useGeneratedKeys = true, keyProperty = "departmentId")
    int insert(Department department);

    @Update("UPDATE department SET university_id = #{universityId}, department_name = #{departmentName} " +
            "WHERE department_id = #{departmentId}")
    int update(Department department);

    @Delete("DELETE FROM department WHERE department_id = #{id}")
    int delete(Integer id);



    @Delete("DELETE FROM department WHERE university_id = #{universityId}")
    void deleteByUniversityId(Integer universityId);
}
