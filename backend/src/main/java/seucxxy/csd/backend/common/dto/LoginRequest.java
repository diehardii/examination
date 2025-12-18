package seucxxy.csd.backend.common.dto;



import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}