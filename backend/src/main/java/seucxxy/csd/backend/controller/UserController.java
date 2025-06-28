package seucxxy.csd.backend.controller;



import jakarta.servlet.http.HttpSession;
import seucxxy.csd.backend.dto.UserDto;
import seucxxy.csd.backend.entity.User;
import seucxxy.csd.backend.mapper.UserMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserMapper userMapper;

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping("/me")
    public UserDto getCurrentUser(HttpSession session) {
     User user = (User) session.getAttribute("user");
         System.out.println(user.getUsername());

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setUsername(user.getUsername());
        userDto.setPhone(user.getPhone());
        if (user.getRole() != null) {
            userDto.setRoleName(user.getRole().getRoleName());
        }

        return userDto;
    }
}
