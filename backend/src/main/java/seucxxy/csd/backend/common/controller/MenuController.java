package seucxxy.csd.backend.common.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import seucxxy.csd.backend.common.dto.MenuItemDto;
import seucxxy.csd.backend.common.entity.User;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    @GetMapping
    public List<MenuItemDto> getMenuItems(HttpSession session) {

        User user = (User) session.getAttribute("user");
        
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }

        List<MenuItemDto> menuItems = new ArrayList<>();

        if (user.getRoleId() == 1) { // 管理员菜单
            menuItems.add(new MenuItemDto("用户管理", "/user-man", "👨‍💼"));
            menuItems.add(new MenuItemDto("系统管理", "/system-man", "⚙️"));
        } else if(user.getRoleId() == 2) { // 学生菜单

            menuItems.add(new MenuItemDto("试卷生成", "/paper-gen", "🖨️"));
            menuItems.add(new MenuItemDto("试卷考试", "/paper-test", "🧰"));
            menuItems.add(new MenuItemDto("用户管理", "/user-man", "👨‍💼"));
            menuItems.add(new MenuItemDto("强化训练", "/intensive-train", "💪"));

        }else if(user.getRoleId() == 3) { // 老师菜单

            menuItems.add(new MenuItemDto("试卷生成", "/paper-gen", "🖨️"));
            menuItems.add(new MenuItemDto("试卷考试", "/paper-test", "🧰"));
            menuItems.add(new MenuItemDto("强化训练", "/intensive-train", "💪"));

           }
        return menuItems;
    }
}