package seucxxy.csd.backend.common.service;

import seucxxy.csd.backend.common.dto.UserExamPaperDTO;
import seucxxy.csd.backend.common.entity.User;

import java.util.List;

public interface UserService {

    User getUserById(long id);
    List<User> getAllUsersWithRoles();
    void updateUserRole(String phone, Integer roleId);
    public List<UserExamPaperDTO> getUsersByExamPaper(Long examPaperId);
}

