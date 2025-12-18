package seucxxy.csd.backend.common.service.impl;



import org.springframework.stereotype.Service;

import seucxxy.csd.backend.common.dto.UserExamPaperDTO;
import seucxxy.csd.backend.common.entity.User;
import seucxxy.csd.backend.common.mapper.UserMapper;
import seucxxy.csd.backend.common.service.UserService;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }


    @Override
    public User getUserById(long id) {
        return userMapper.findUserById(id);
    }
    @Override
    public List<User> getAllUsersWithRoles() {
        return userMapper.findAllUsersWithRoles();
    }

    @Override
    public void updateUserRole(String phone, Integer roleId) {
        userMapper.updateUserRole(phone, roleId);
    }

    @Override
    public List<UserExamPaperDTO> getUsersByExamPaper(Long examPaperId) {
        List<UserExamPaperDTO> users = userMapper.findUsersByExamPaperId(examPaperId);
        // 注意：testCount功能已废弃，因为UserTestRecordMapper已被删除
        // 如需此功能，请使用CET4模块的UserTestRecordEnMapper
        users.forEach(user -> {
            user.setTestCount(0); // 默认值
        });
        return users;
    }

}