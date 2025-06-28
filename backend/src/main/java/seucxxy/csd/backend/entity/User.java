package seucxxy.csd.backend.entity;



import lombok.Data;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String phone;
    private Integer roleId;

    // 关联的角色对象
    private Role role;
}