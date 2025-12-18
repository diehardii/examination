package seucxxy.csd.backend.common.mapper;

import org.apache.ibatis.annotations.*;

import seucxxy.csd.backend.common.entity.User;
import seucxxy.csd.backend.common.entity.UserTeacher;

import java.util.List;

@Mapper
public interface UserTeacherMapper {
    
    // 根据教师ID获取学生列表
    @Select("SELECT u.* FROM users u " +
            "INNER JOIN user_teacher ut ON u.id = ut.student_id " +
            "WHERE ut.teacher_id = #{teacherId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "username", column = "username"),
            @Result(property = "password", column = "password"),
            @Result(property = "phone", column = "phone"),
            @Result(property = "roleId", column = "role_id"),
            @Result(property = "realName", column = "real_name"),
            @Result(property = "email", column = "email"),
            @Result(property = "identityId", column = "identity_id"),
            @Result(property = "gender", column = "gender"),
            @Result(property = "address", column = "address")
    })
    List<User> findStudentsByTeacherId(Long teacherId);
    
    // 删除教师的所有学生关系
    @Delete("DELETE FROM user_teacher WHERE teacher_id = #{teacherId}")
    int deleteByTeacherId(Long teacherId);
    
    // 批量插入教师学生关系
    @Insert("<script>" +
            "INSERT INTO user_teacher (student_id, teacher_id) VALUES " +
            "<foreach collection='studentIds' item='studentId' separator=','>" +
            "(#{studentId}, #{teacherId})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("teacherId") Long teacherId, @Param("studentIds") List<Long> studentIds);
    
    // 检查关系是否存在
    @Select("SELECT COUNT(*) FROM user_teacher WHERE teacher_id = #{teacherId} AND student_id = #{studentId}")
    int existsByTeacherIdAndStudentId(@Param("teacherId") Long teacherId, @Param("studentId") Long studentId);
} 