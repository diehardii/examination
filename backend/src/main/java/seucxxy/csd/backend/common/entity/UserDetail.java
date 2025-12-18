package seucxxy.csd.backend.common.entity;


import lombok.Data;

@Data
public class UserDetail {
    private Long userId;
    private String realName;
    private String identityId;
    private String gender;
    private String email;
    private String address;
    private Integer universityId;
    private Integer departmentId;
    private String occupation;
    private int subjectId;

    // 关联对象
    private University university;
    private Department department;
}