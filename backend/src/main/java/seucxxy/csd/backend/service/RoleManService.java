package seucxxy.csd.backend.service;


import org.springframework.stereotype.Service;
import seucxxy.csd.backend.entity.Role;
import seucxxy.csd.backend.mapper.RoleMapper;


import java.util.List;

@Service
public class RoleManService  {

    private final RoleMapper roleMapper;

    public RoleManService(RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    public List<Role> getAllRoles() {
        return roleMapper.findAllRoles();
    }
}