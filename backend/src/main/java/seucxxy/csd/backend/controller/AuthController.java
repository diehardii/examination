package seucxxy.csd.backend.controller;


import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import seucxxy.csd.backend.dto.ChangePasswordRequest;
import seucxxy.csd.backend.dto.LoginRequest;
import seucxxy.csd.backend.dto.RegisterRequest;
import seucxxy.csd.backend.dto.UserDto;
import seucxxy.csd.backend.entity.Role;
import seucxxy.csd.backend.entity.User;
import seucxxy.csd.backend.mapper.RoleMapper;
import seucxxy.csd.backend.mapper.UserMapper;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seucxxy.csd.backend.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {


    @Autowired
    private AuthService authService;

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
                          UserMapper userMapper,
                          RoleMapper roleMapper,
                          PasswordEncoder passwordEncoder) {

        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public UserDto login(@RequestBody LoginRequest loginRequest, HttpSession session) {
        System.out.println(loginRequest);
        User user = authService.login(loginRequest);
        session.setAttribute("user", user);
        System.out.println(loginRequest+"get"+user.getUsername());
        return convertToDto(user);
    }

    @PostMapping("/register")
    public UserDto register(@RequestBody RegisterRequest registerRequest) {
        // 检查用户名是否已存在
        if (userMapper.findByUsername(registerRequest.getUsername()) != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查手机号是否已存在
        if (userMapper.findByPhone(registerRequest.getPhone()) != null) {
            throw new RuntimeException("手机号已存在");
        }

        // 设置默认角色为普通用户
        Role userRole = roleMapper.findByName("USER");
        if (userRole == null) {
            throw new RuntimeException("默认角色未配置");
        }

        // 创建新用户
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setPhone(registerRequest.getPhone());
        user.setRoleId(userRole.getRoleId());

        userMapper.insertUser(user);

        // 返回用户信息
        User savedUser = userMapper.findByUsername(user.getUsername());
        return convertToDto(savedUser);
    }

    private UserDto convertToDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setPhone(user.getPhone());
        if (user.getRole() != null) {
            userDto.setRoleName(user.getRole().getRoleName());
        }
        return userDto;
    }


    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest changePasswordRequest, HttpSession session) {
        try {
            // 从session中获取当前用户
            User currentUser = (User) session.getAttribute("user");
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            // 验证当前密码是否正确
            if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), currentUser.getPassword())) {
                return ResponseEntity.badRequest().body("当前密码不正确");
            }

            // 验证新密码和确认密码是否一致
            if (!changePasswordRequest.getNewPassword().equals(changePasswordRequest.getConfirmPassword())) {
                return ResponseEntity.badRequest().body("新密码和确认密码不一致");
            }

            String encodedNewPassword = authService.changeUserPassword(currentUser.getId(), changePasswordRequest.getNewPassword());
            // 更新session中的用户信息
            currentUser.setPassword(encodedNewPassword);
            session.setAttribute("user", currentUser);

            return ResponseEntity.ok().body("密码修改成功");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("密码修改失败: " + e.getMessage());
        }
    }
}