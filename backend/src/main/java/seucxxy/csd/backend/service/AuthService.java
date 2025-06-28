package seucxxy.csd.backend.service;




import seucxxy.csd.backend.dto.LoginRequest;
import seucxxy.csd.backend.dto.RegisterRequest;
import seucxxy.csd.backend.entity.User;

public interface AuthService {
    User login(LoginRequest loginRequest);
    void register(RegisterRequest registerRequest);
    boolean checkUsernameExists(String username);
    boolean checkPhoneExists(String phone);
    public String changeUserPassword(Long userId, String newPassword);
}
