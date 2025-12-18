package seucxxy.csd.backend.common.service;

import java.util.List;

import seucxxy.csd.backend.common.entity.User;

public interface UserTeacherService {
    
    // 根据教师ID获取学生列表
    List<User> getStudentsByTeacherId(Long teacherId);
    
    // 保存教师学生关系
    void saveTeacherStudentRelation(Long teacherId, List<Long> studentIds);
} 