package seucxxy.csd.backend.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import seucxxy.csd.backend.dto.MenuItemDto;
import seucxxy.csd.backend.entity.User;
import seucxxy.csd.backend.mapper.UserMapper;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private final UserMapper userMapper;

    public MenuController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping
    public List<MenuItemDto> getMenuItems(HttpSession session) {

        User user = (User) session.getAttribute("user");

        List<MenuItemDto> menuItems = new ArrayList<>();

        if (user.getRoleId() == 1) { // 管理员菜单
            menuItems.add(new MenuItemDto("试卷生成", "/paper-gen", "📄"));
            menuItems.add(new MenuItemDto("试卷管理", "/paper-man", "📚"));
            menuItems.add(new MenuItemDto("试卷考试", "/paper-test", "🧰"));
            menuItems.add(new MenuItemDto("试卷分析", "/paper-ana", "📊"));
            menuItems.add(new MenuItemDto("试题分析", "/question-ana", "🔍"));
            menuItems.add(new MenuItemDto("错题管理", "/wrong-question-man", "📌"));
        } else { // 普通用户菜单
            menuItems.add(new MenuItemDto("试卷管理", "/paper-man", "📚"));
            menuItems.add(new MenuItemDto("试卷考试", "/paper-test", "🧰"));
            menuItems.add(new MenuItemDto("试卷分析", "/paper-ana", "📊"));
            menuItems.add(new MenuItemDto("试题分析", "/question-ana", "🔍"));
            menuItems.add(new MenuItemDto("错题管理", "/wrong-question-man", "📌"));
            menuItems.add(new MenuItemDto("用户管理", "/user-man", "👨‍💼"));
        }

        return menuItems;
    }
}