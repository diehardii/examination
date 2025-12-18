package seucxxy.csd.backend.common.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import seucxxy.csd.backend.common.entity.Teacher;

@Mapper
public interface TeacherMapper {

    // 查询教师的所有学段（去重）
    @Select("SELECT DISTINCT ta.stage_id, es.display_name AS stageName " +
            "FROM teaching_assignments ta " +
            "LEFT JOIN education_stages es ON ta.stage_id = es.stage_id " +
            "WHERE ta.teacher_id = #{teacherId}")
    List<Teacher> findStagesByTeacherId(Long teacherId);

    // 查询教师在某个学段下的所有班级
    @Select("SELECT ta.assignment_id, ta.teacher_id, ta.stage_id, ta.class_id, " +
            "es.display_name AS stageName, c.class_code AS classCode " +
            "FROM teaching_assignments ta " +
            "LEFT JOIN education_stages es ON ta.stage_id = es.stage_id " +
            "LEFT JOIN classes c ON ta.class_id = c.class_id " +
            "WHERE ta.teacher_id = #{teacherId} AND ta.stage_id = #{stageId}")
    List<Teacher> findClassesByTeacherAndStage(@Param("teacherId") Long teacherId, @Param("stageId") Integer stageId);

    // 查询教师的所有班级ID
    @Select("SELECT class_id FROM teaching_assignments WHERE teacher_id = #{teacherId}")
    List<Integer> findClassIds(Long teacherId);

    // 查询教师在某个学段下的班级ID
    @Select("SELECT class_id FROM teaching_assignments WHERE teacher_id = #{teacherId} AND stage_id = #{stageId}")
    List<Integer> findClassIdsByStage(@Param("teacherId") Long teacherId, @Param("stageId") Integer stageId);

    // 删除教师的所有班级分配
    @Delete("DELETE FROM teaching_assignments WHERE teacher_id = #{teacherId}")
    int deleteAllAssignments(Long teacherId);

    // 删除教师在某个学段下的所有班级分配
    @Delete("DELETE FROM teaching_assignments WHERE teacher_id = #{teacherId} AND stage_id = #{stageId}")
    int deleteAssignmentsByStage(@Param("teacherId") Long teacherId, @Param("stageId") Integer stageId);

    // 批量插入教师班级分配
    @Insert({"<script>",
            "INSERT INTO teaching_assignments (teacher_id, stage_id, class_id) VALUES",
            "<foreach collection='assignments' item='item' separator=','>",
            "(#{item.teacherId}, #{item.stageId}, #{item.classId})",
            "</foreach>",
            "</script>"})
    int insertAssignments(@Param("assignments") List<Teacher> assignments);

    // 统计教师在各学段的班级数量
    @Select("SELECT stage_id, COUNT(DISTINCT class_id) as count " +
            "FROM teaching_assignments " +
            "WHERE teacher_id = #{teacherId} " +
            "GROUP BY stage_id")
    List<java.util.Map<String, Object>> countClassesByStage(Long teacherId);
}
