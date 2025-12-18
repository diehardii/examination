package seucxxy.csd.backend.common.controller;


import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import seucxxy.csd.backend.common.dto.UserInfoDTO;
import seucxxy.csd.backend.common.entity.User;
import seucxxy.csd.backend.common.service.UserDetailService;

@RestController
@RequestMapping("/api/user-detail")
public class UserDetailController {
    private final UserDetailService userDetailService;

    public UserDetailController(UserDetailService userDetailService) {
        this.userDetailService = userDetailService;
    }

    @GetMapping
    public UserInfoDTO getUserInfoDTO(@RequestParam(required = false) Long userId,
                                      HttpSession session) {
        Long targetUserId = userId;
        if (targetUserId == null) {
            User sessionUser = (User) session.getAttribute("user");
            if (sessionUser != null) {
                targetUserId = sessionUser.getId();
            }
        }
        if (targetUserId == null) {
            throw new RuntimeException("用户未登录");
        }
        return userDetailService.getUserInfoDTO(targetUserId);
    }

    @PostMapping
    public String saveDetail(@RequestBody UserInfoDTO userInfoDTO,
                             HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        userDetailService.saveDetail(userInfoDTO,user.getId());
        return "保存成功";
    }

    @GetMapping("/all")
    public java.util.List<User> getAllUsersWithRoles() {
        return userDetailService.getAllUsersWithRoles();
    }
}
