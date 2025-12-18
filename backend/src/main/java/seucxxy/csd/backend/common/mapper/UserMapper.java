package seucxxy.csd.backend.common.mapper;


import org.apache.ibatis.annotations.*;

import seucxxy.csd.backend.common.dto.UserExamPaperDTO;
import seucxxy.csd.backend.common.entity.User;

import java.util.List;

@Mapper
public interface UserMapper {
    User findByUsername(@Param("username") String username);
    User findByPhone(@Param("phone") String phone);
    User findUserById(@Param("id") long id);
    void insertUser(User user);
    List<User> findAllUsersWithRoles();
    List<User> findUsersByRoleName(@Param("roleName") String roleName);
    int updateUserRole(@Param("phone") String phone, @Param("roleId") Integer roleId);
    void updatePassword(@Param("id") Long id, @Param("newPassword") String newPassword);
    void updatePhone(@Param("id") Long id, @Param("phoneNumber") String phoneNumber);
    int updateUserProfile(User user);
    List<UserExamPaperDTO> findUsersByExamPaperId(Long examPaperId);
}