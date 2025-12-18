package seucxxy.csd.backend.common.service;



import org.springframework.stereotype.Service;

import seucxxy.csd.backend.common.dto.UserInfoDTO;
import seucxxy.csd.backend.common.entity.Student;
import seucxxy.csd.backend.common.entity.User;
import seucxxy.csd.backend.common.mapper.StudentMapper;
import seucxxy.csd.backend.common.mapper.UserMapper;

@Service
public class UserDetailService {
    private final UserMapper userMapper;
    private final StudentMapper studentMapper;

    public UserDetailService(UserMapper userMapper, StudentMapper studentMapper) {
        this.userMapper = userMapper;
        this.studentMapper = studentMapper;
    }

    public UserInfoDTO getUserInfoDTO(Long userId) {
        User user = userMapper.findUserById(userId);
        if (user == null) {
            return null;
        }

        UserInfoDTO userInfoDTO = new UserInfoDTO();
        userInfoDTO.setUserId(user.getId());
        userInfoDTO.setUsername(user.getUsername());
        userInfoDTO.setRealName(user.getRealName());
        userInfoDTO.setGender(user.getGender());
        userInfoDTO.setAddress(user.getAddress());
        userInfoDTO.setIdentityId(user.getIdentityId());
        userInfoDTO.setEmail(user.getEmail());
        userInfoDTO.setPhoneNumber(user.getPhone());
        userInfoDTO.setRoleId(user.getRoleId());
        
        // 如果是学生角色(roleId=2)，查询学生信息
        if (user.getRoleId() != null && user.getRoleId() == 2) {
            Student student = studentMapper.findById(userId.intValue());
            if (student != null) {
                userInfoDTO.setStudentNumber(student.getStudentNumber());
                userInfoDTO.setStageId(student.getStageId());
                userInfoDTO.setGradeId(student.getGradeId());
                userInfoDTO.setClassId(student.getClassId());
                userInfoDTO.setStageName(student.getStageName());
                userInfoDTO.setGradeName(student.getGradeName());
                userInfoDTO.setClassCode(student.getClassCode());
            }
        }
        
        return userInfoDTO;
    }

    public void saveDetail(UserInfoDTO userInfoDTO, long userId) {
        User existing = userMapper.findUserById(userId);
        if (existing == null) {
            throw new RuntimeException("用户不存在");
        }

        User toUpdate = new User();
        toUpdate.setId(userId);
        toUpdate.setUsername(userInfoDTO.getUsername() != null ? userInfoDTO.getUsername() : existing.getUsername());
        toUpdate.setPhone(userInfoDTO.getPhoneNumber());
        toUpdate.setIdentityId(userInfoDTO.getIdentityId());
        toUpdate.setGender(userInfoDTO.getGender());
        toUpdate.setEmail(userInfoDTO.getEmail());
        toUpdate.setAddress(userInfoDTO.getAddress());
        toUpdate.setRealName(userInfoDTO.getRealName());

        userMapper.updateUserProfile(toUpdate);
        
        // 如果是学生角色(roleId=2)，处理学生信息
        if (existing.getRoleId() != null && existing.getRoleId() == 2) {
            boolean studentExists = studentMapper.existsById(Long.valueOf(userId).intValue());
            
            Student student = new Student();
            student.setStudentId(Long.valueOf(userId).intValue());
            student.setStudentNumber(userInfoDTO.getStudentNumber());
            student.setStageId(userInfoDTO.getStageId());
            student.setGradeId(userInfoDTO.getGradeId());
            student.setClassId(userInfoDTO.getClassId());
            
            if (studentExists) {
                // 更新学生信息
                studentMapper.updateClassInfo(student);
                // 如果学号也需要更新，需要单独处理
                if (userInfoDTO.getStudentNumber() != null) {
                    studentMapper.updateStudentNumber(student);
                }
            } else {
                // 插入学生信息
                if (userInfoDTO.getStudentNumber() != null && 
                    userInfoDTO.getStageId() != null && 
                    userInfoDTO.getGradeId() != null && 
                    userInfoDTO.getClassId() != null) {
                    studentMapper.insert(student);
                }
            }
        }
    }

    public java.util.List<User> getAllUsersWithRoles() {
        return userMapper.findAllUsersWithRoles();
    }
}
