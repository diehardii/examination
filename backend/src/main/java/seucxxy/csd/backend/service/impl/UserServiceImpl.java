package seucxxy.csd.backend.service.impl;



import org.springframework.stereotype.Service;
import seucxxy.csd.backend.dto.UserExamPaperDTO;
import seucxxy.csd.backend.entity.User;
import seucxxy.csd.backend.mapper.UserExamPaperMapper;
import seucxxy.csd.backend.mapper.UserMapper;
import seucxxy.csd.backend.mapper.UserTestRecordMapper;
import seucxxy.csd.backend.service.UserService;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserTestRecordMapper   userTestRecordMapper;

    public UserServiceImpl(UserMapper userMapper,
                           UserTestRecordMapper userTestRecordMapper) {
        this.userMapper = userMapper;
        this.userTestRecordMapper = userTestRecordMapper;
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
        users.forEach(user -> {
            int testCount = userTestRecordMapper.countByUserIdAndExamPaperId(user.getUserId(), examPaperId);
            user.setTestCount(testCount);
        });
        return users;
    }

}