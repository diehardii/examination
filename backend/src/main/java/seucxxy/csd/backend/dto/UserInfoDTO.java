package seucxxy.csd.backend.dto;


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
    private Integer universityId;
    private Integer departmentId;
    private String occupation;
}
