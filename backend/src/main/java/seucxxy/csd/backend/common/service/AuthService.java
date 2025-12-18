package seucxxy.csd.backend.common.service;




import seucxxy.csd.backend.common.dto.LoginRequest;
import seucxxy.csd.backend.common.dto.RegisterRequest;
import seucxxy.csd.backend.common.entity.User;

public interface AuthService {
    User login(LoginRequest loginRequest);
    void register(RegisterRequest registerRequest);
    boolean checkUsernameExists(String username);
    boolean checkPhoneExists(String phone);
    public String changeUserPassword(Long userId, String newPassword);
}
