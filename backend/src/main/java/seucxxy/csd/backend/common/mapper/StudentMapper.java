package seucxxy.csd.backend.common.mapper;

import java.util.List;
import org.apache.ibatis.annotations.*;
import seucxxy.csd.backend.common.entity.Student;

@Mapper
public interface StudentMapper {

    // 查询所有学生及其班级信息
    @Select("SELECT s.student_id, s.student_number, s.stage_id, s.grade_id, s.class_id, " +
            "u.real_name AS studentName, u.username, u.email, u.phone, " +
            "es.display_name AS stageName, g.grade_name AS gradeName, c.class_code AS classCode " +
            "FROM students s " +
            "LEFT JOIN users u ON s.student_id = u.id " +
            "LEFT JOIN education_stages es ON s.stage_id = es.stage_id " +
            "LEFT JOIN grades g ON s.grade_id = g.grade_id " +
            "LEFT JOIN classes c ON s.class_id = c.class_id " +
            "WHERE u.role_id = 3 OR (u.role_id IS NULL)")
    List<Student> findAllStudents();

    // 根据学生ID查询学生信息
    @Select("SELECT s.student_id, s.student_number, s.stage_id, s.grade_id, s.class_id, " +
            "u.real_name AS studentName, u.username, u.email, u.phone, " +
            "es.display_name AS stageName, g.grade_name AS gradeName, c.class_code AS classCode " +
            "FROM students s " +
            "LEFT JOIN users u ON s.student_id = u.id " +
            "LEFT JOIN education_stages es ON s.stage_id = es.stage_id " +
            "LEFT JOIN grades g ON s.grade_id = g.grade_id " +
            "LEFT JOIN classes c ON s.class_id = c.class_id " +
            "WHERE s.student_id = #{studentId}")
    Student findById(Integer studentId);

    // 根据班级ID查询学生列表
    @Select("SELECT s.student_id, s.student_number, s.stage_id, s.grade_id, s.class_id, " +
            "u.real_name AS studentName, u.username, u.email, u.phone " +
            "FROM students s " +
            "LEFT JOIN users u ON s.student_id = u.id " +
            "WHERE s.class_id = #{classId}")
    List<Student> findByClassId(Integer classId);

    // 根据年级ID查询学生列表
    @Select("SELECT s.student_id, s.student_number, s.stage_id, s.grade_id, s.class_id, " +
            "u.real_name AS studentName, u.username, u.email " +
            "FROM students s " +
            "LEFT JOIN users u ON s.student_id = u.id " +
            "WHERE s.grade_id = #{gradeId}")
    List<Student> findByGradeId(Integer gradeId);

    // 根据学段ID查询学生列表
    @Select("SELECT s.student_id, s.student_number, s.stage_id, s.grade_id, s.class_id, " +
            "u.real_name AS studentName, u.username, u.email " +
            "FROM students s " +
            "LEFT JOIN users u ON s.student_id = u.id " +
            "WHERE s.stage_id = #{stageId}")
    List<Student> findByStageId(Integer stageId);

    // 查询没有班级分配的学生
    @Select("SELECT u.id AS studentId, u.real_name AS studentName, u.username, u.email " +
            "FROM users u " +
            "WHERE u.role_id = 3 AND u.id NOT IN (SELECT student_id FROM students)")
    List<Student> findUnassignedStudents();

    // 统计班级的学生数量
    @Select("SELECT COUNT(*) FROM students WHERE class_id = #{classId}")
    int countByClassId(Integer classId);

    // 统计年级的学生数量
    @Select("SELECT COUNT(*) FROM students WHERE grade_id = #{gradeId}")
    int countByGradeId(Integer gradeId);

    // 插入学生记录
    @Insert("INSERT INTO students (student_id, student_number, stage_id, grade_id, class_id) " +
            "VALUES (#{studentId}, #{studentNumber}, #{stageId}, #{gradeId}, #{classId})")
    int insert(Student student);

    // 更新学生的班级信息
    @Update("UPDATE students SET stage_id = #{stageId}, grade_id = #{gradeId}, class_id = #{classId} " +
            "WHERE student_id = #{studentId}")
    int updateClassInfo(Student student);

    // 更新学生学号
    @Update("UPDATE students SET student_number = #{studentNumber} WHERE student_id = #{studentId}")
    int updateStudentNumber(Student student);

    // 批量更新学生的班级
    @Update({"<script>",
            "<foreach collection='students' item='item' separator=';'>",
            "UPDATE students SET stage_id = #{item.stageId}, grade_id = #{item.gradeId}, class_id = #{item.classId} ",
            "WHERE student_id = #{item.studentId}",
            "</foreach>",
            "</script>"})
    int batchUpdateClassInfo(@Param("students") List<Student> students);

    // 删除学生记录
    @Delete("DELETE FROM students WHERE student_id = #{studentId}")
    int deleteById(Integer studentId);

    // 检查学生是否存在
    @Select("SELECT COUNT(*) > 0 FROM students WHERE student_id = #{studentId}")
    boolean existsById(Integer studentId);
}
