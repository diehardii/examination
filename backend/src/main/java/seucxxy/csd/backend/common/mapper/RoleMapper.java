package seucxxy.csd.backend.common.mapper;




import org.apache.ibatis.annotations.*;

import seucxxy.csd.backend.common.entity.Role;

import java.util.List;

@Mapper
public interface RoleMapper {
    Role findById(@Param("roleId") Integer roleId);
    Role findByName(@Param("roleName") String roleName);
    List<Role> findAllRoles();


}
