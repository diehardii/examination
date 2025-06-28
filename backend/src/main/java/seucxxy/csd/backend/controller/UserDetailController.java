package seucxxy.csd.backend.controller;


import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import seucxxy.csd.backend.entity.User;
import seucxxy.csd.backend.entity.UserDetail;
import seucxxy.csd.backend.dto.UserInfoDTO;
import seucxxy.csd.backend.service.UserDetailService;

@RestController
@RequestMapping("/api/user-detail")
public class UserDetailController {
    private final UserDetailService userDetailService;

    public UserDetailController(UserDetailService userDetailService) {
        this.userDetailService = userDetailService;
    }

    @GetMapping
    public UserInfoDTO getUserInfoDTO(@RequestParam Long userId) {
        return userDetailService.getUserInfoDTO(userId);
    }

    @PostMapping
    public String saveDetail(@RequestBody UserInfoDTO userInfoDTO,
                             HttpSession session) {
        User user = (User) session.getAttribute("user");
        userDetailService.saveDetail(userInfoDTO,user.getId());
        return "保存成功";
    }
}
