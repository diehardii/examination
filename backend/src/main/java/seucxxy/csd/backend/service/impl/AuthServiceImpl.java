package seucxxy.csd.backend.service.impl;



import org.springframework.transaction.annotation.Transactional;
import seucxxy.csd.backend.dto.LoginRequest;
import seucxxy.csd.backend.dto.RegisterRequest;
import seucxxy.csd.backend.entity.User;
import seucxxy.csd.backend.mapper.UserMapper;
import seucxxy.csd.backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User login(LoginRequest loginRequest) {
        User user = userMapper.findByUsername(loginRequest.getUsername());
        if (user != null && passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return user;
        }
        return null;
    }

    @Override
    public void register(RegisterRequest registerRequest) {
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPhone(registerRequest.getPhone());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRoleId(2); // 默认角色为学生

        userMapper.insertUser(user);
    }

    @Override
    public boolean checkUsernameExists(String username) {
        return userMapper.findByUsername(username) != null;
    }

    @Override
    public boolean checkPhoneExists(String phone) {
        return userMapper.findByPhone(phone) != null;
    }


    @Override
    public String changeUserPassword(Long userId, String newPassword) {
        String encodedPassword = passwordEncoder.encode(newPassword);
        userMapper.updatePassword(userId, encodedPassword);
        return encodedPassword;
    }
}
