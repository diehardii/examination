package seucxxy.csd.backend.common.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import seucxxy.csd.backend.common.service.RoleManService;
import seucxxy.csd.backend.common.service.UserService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/role-management")
public class RoleManController {

    private final UserService userService;
    private final RoleManService roleManService;

    public RoleManController(UserService userService, RoleManService roleManService) {
        this.userService = userService;
        this.roleManService = roleManService;
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsersWithRoles() {
        Map<String, Object> response = new HashMap<>();
        try {
            response.put("success", true);
            response.put("data", userService.getAllUsersWithRoles());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取用户列表失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/roles")
    public ResponseEntity<?> getAllRoles() {
        Map<String, Object> response = new HashMap<>();
        try {
            response.put("success", true);
            response.put("data", roleManService.getAllRoles());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取角色列表失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/update-role")
    public ResponseEntity<?> updateUserRole(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            String phone = (String) payload.get("phone");
            Integer roleId = Integer.parseInt(payload.get("roleId").toString());

            userService.updateUserRole(phone, roleId);

            response.put("success", true);
            response.put("message", "角色更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "角色更新失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}