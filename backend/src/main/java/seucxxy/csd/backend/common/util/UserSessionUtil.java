package seucxxy.csd.backend.common.util;

import jakarta.servlet.http.HttpSession;
import seucxxy.csd.backend.common.entity.User;

/**
 * 用户会话验证工具类
 * 统一处理从 HttpSession 中获取用户对象的空值检查
 */
public class UserSessionUtil {

    /**
     * 从会话中获取当前用户
     * @param session HTTP会话
     * @return 当前用户
     * @throws RuntimeException 当用户未登录时抛出异常
     */
    public static User getCurrentUser(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        return user;
    }

    /**
     * 检查用户是否已登录
     * @param session HTTP会话
     * @return true表示已登录，false表示未登录
     */
    public static boolean isUserLoggedIn(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return user != null;
    }

    /**
     * 获取当前用户ID
     * @param session HTTP会话
     * @return 用户ID
     * @throws RuntimeException 当用户未登录时抛出异常
     */
    public static Long getCurrentUserId(HttpSession session) {
        User user = getCurrentUser(session);
        return user.getId();
    }

    /**
     * 获取当前用户名
     * @param session HTTP会话
     * @return 用户名
     * @throws RuntimeException 当用户未登录时抛出异常
     */
    public static String getCurrentUsername(HttpSession session) {
        User user = getCurrentUser(session);
        return user.getUsername();
    }

    /**
     * 获取当前用户角色ID
     * @param session HTTP会话
     * @return 角色ID
     * @throws RuntimeException 当用户未登录时抛出异常
     */
    public static Integer getCurrentUserRoleId(HttpSession session) {
        User user = getCurrentUser(session);
        return user.getRoleId();
    }

    /**
     * 检查当前用户是否为管理员
     * @param session HTTP会话
     * @return true表示是管理员
     */
    public static boolean isCurrentUserAdmin(HttpSession session) {
        try {
            return getCurrentUserRoleId(session) == 1;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 检查当前用户是否为学生
     * @param session HTTP会话
     * @return true表示是学生
     */
    public static boolean isCurrentUserStudent(HttpSession session) {
        try {
            return getCurrentUserRoleId(session) == 2;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 检查当前用户是否为教师
     * @param session HTTP会话
     * @return true表示是教师
     */
    public static boolean isCurrentUserTeacher(HttpSession session) {
        try {
            return getCurrentUserRoleId(session) == 3;
        } catch (RuntimeException e) {
            return false;
        }
    }
}