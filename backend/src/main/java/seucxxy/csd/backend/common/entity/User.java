package seucxxy.csd.backend.common.entity;



import lombok.Data;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String phone;
    private Integer roleId;

    private String identityId;
    private String gender;
    private String email;
    private String address;
    private String realName;

    // 关联的角色对象
    private Role role;
}