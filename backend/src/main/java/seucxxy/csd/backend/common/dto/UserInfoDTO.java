package seucxxy.csd.backend.common.dto;


import lombok.Data;

@Data
public class UserInfoDTO {
    private Long userId;
    private String realName;
    private String identityId;
    private String gender;
    private String phoneNumber;
    private String email;
    private String address;
    private String username;
    
    // 用户角色ID
    private Integer roleId;
    
    // 学生相关字段 (仅当 roleId = 2 时有效)
    private String studentNumber;    // 学号
    private Integer stageId;         // 学段ID
    private Integer gradeId;         // 年级ID
    private Integer classId;         // 班级ID
    
    // 学段、年级、班级显示名称（用于前端展示）
    private String stageName;
    private String gradeName;
    private String classCode;
}
