package seucxxy.csd.backend.common.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import seucxxy.csd.backend.common.entity.User;
import seucxxy.csd.backend.common.mapper.UserTeacherMapper;
import seucxxy.csd.backend.common.service.UserTeacherService;

import java.util.List;

@Service
public class UserTeacherServiceImpl implements UserTeacherService {
    
    @Autowired
    private UserTeacherMapper userTeacherMapper;
    
    @Override
    public List<User> getStudentsByTeacherId(Long teacherId) {
        return userTeacherMapper.findStudentsByTeacherId(teacherId);
    }
    
    @Override
    @Transactional
    public void saveTeacherStudentRelation(Long teacherId, List<Long> studentIds) {
        // 先删除该教师的所有学生关系
        userTeacherMapper.deleteByTeacherId(teacherId);
        
        // 如果有新的学生关系，则批量插入
        if (studentIds != null && !studentIds.isEmpty()) {
            userTeacherMapper.batchInsert(teacherId, studentIds);
        }
    }
} 