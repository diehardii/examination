package seucxxy.csd.backend.mapper;




import org.apache.ibatis.annotations.*;
import seucxxy.csd.backend.entity.Role;

import java.util.List;

@Mapper
public interface RoleMapper {
    Role findById(@Param("roleId") Integer roleId);
    Role findByName(@Param("roleName") String roleName);
    List<Role> findAllRoles();


}
