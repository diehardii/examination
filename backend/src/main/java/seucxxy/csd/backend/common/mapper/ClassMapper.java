package seucxxy.csd.backend.common.mapper;

import org.apache.ibatis.annotations.*;
import seucxxy.csd.backend.common.entity.ClassInfo;

import java.util.List;

@Mapper
public interface ClassMapper {

    @Select("SELECT c.*, es.display_name AS stageName, g.grade_name AS gradeName, d.department_name AS departmentName " +
            "FROM classes c " +
            "LEFT JOIN education_stages es ON c.stage_id = es.stage_id " +
            "LEFT JOIN grades g ON c.grade_id = g.grade_id " +
            "LEFT JOIN department d ON c.department_id = d.department_id")
    List<ClassInfo> findAll();

    @Select("SELECT c.*, es.display_name AS stageName, g.grade_name AS gradeName, d.department_name AS departmentName " +
            "FROM classes c " +
            "LEFT JOIN education_stages es ON c.stage_id = es.stage_id " +
            "LEFT JOIN grades g ON c.grade_id = g.grade_id " +
            "LEFT JOIN department d ON c.department_id = d.department_id " +
            "WHERE c.stage_id = #{stageId}")
    List<ClassInfo> findByStage(Integer stageId);

    @Insert("INSERT INTO classes (stage_id, grade_id, department_id, class_code) " +
            "VALUES (#{stageId}, #{gradeId}, #{departmentId}, #{classCode})")
    @Options(useGeneratedKeys = true, keyProperty = "classId")
    int insert(ClassInfo classInfo);

    @Update("UPDATE classes SET stage_id=#{stageId}, grade_id=#{gradeId}, " +
            "department_id=#{departmentId}, class_code=#{classCode} WHERE class_id=#{classId}")
    int update(ClassInfo classInfo);

    @Delete("DELETE FROM classes WHERE class_id = #{classId}")
    int delete(Integer classId);

    @Select({"<script>",
            "SELECT COUNT(*) FROM classes WHERE stage_id = #{stageId} AND class_id IN",
            "<foreach collection='classIds' item='cid' open='(' separator=',' close=')'>",
            "#{cid}",
            "</foreach>",
            "</script>"})
    int countByStage(@Param("stageId") Integer stageId, @Param("classIds") List<Integer> classIds);

    @Select("SELECT c.*, es.display_name AS stageName, g.grade_name AS gradeName, d.department_name AS departmentName " +
            "FROM classes c " +
            "LEFT JOIN education_stages es ON c.stage_id = es.stage_id " +
            "LEFT JOIN grades g ON c.grade_id = g.grade_id " +
            "LEFT JOIN department d ON c.department_id = d.department_id " +
            "WHERE c.class_id = #{classId}")
    ClassInfo findById(Integer classId);
}
