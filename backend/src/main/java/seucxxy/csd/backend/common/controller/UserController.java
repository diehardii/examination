package seucxxy.csd.backend.common.controller;



import jakarta.servlet.http.HttpSession;
import seucxxy.csd.backend.common.dto.UserDto;
import seucxxy.csd.backend.common.entity.User;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/me")
    public UserDto getCurrentUser(HttpSession session) {
     User user = (User) session.getAttribute("user");

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
