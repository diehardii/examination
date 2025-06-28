package seucxxy.csd.backend.mapper;


import org.apache.ibatis.annotations.*;
import seucxxy.csd.backend.entity.Department;
import seucxxy.csd.backend.entity.University;
import java.util.List;

@Mapper
public interface UniversityMapper {

    @Select("SELECT * FROM university WHERE university_id = #{id}")
    University findById(Integer id);

    @Select("SELECT * FROM university")
    @Results({
            @Result(property = "universityId", column = "university_id", id = true),
            @Result(property = "universityName", column = "university_name")
    })
    List<University> findAll();

    @Select("SELECT * FROM university")
    @Results(id = "universityResultMap", value = {
            @Result(property = "universityId", column = "university_id", id = true),
            @Result(property = "universityName", column = "university_name"),
            @Result(property = "departments", column = "university_id",
                    many = @Many(select = "com.examinai.mapper.DepartmentMapper.findByUniversityId"))
    })
    List<University> findAllWithDepartments();

    // 更新大学信息
    @Update("UPDATE university SET " +
            "university_name = #{universityName} " +
            "WHERE university_id = #{universityId}")
       void updateUniversity(University university);

    // 删除大学
    @Delete("DELETE FROM university WHERE university_id = #{id}")
    void deleteUniversity(@Param("id") Integer id);

    @Insert("INSERT INTO university(university_name) VALUES(#{universityName})")
    @Options(useGeneratedKeys = true, keyProperty = "universityId")
    int insert(University university);



    // 关联查询示例（N+1查询问题需注意）
    @Select("SELECT * FROM department WHERE university_id = #{universityId}")
    List<Department> findDepartmentsByUniversityId(Integer universityId);
}