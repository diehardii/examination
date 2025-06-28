package seucxxy.csd.backend.service;



import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import seucxxy.csd.backend.entity.User;
import seucxxy.csd.backend.mapper.UserMapper;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;


    public User getUserService(String username){
        User user = userMapper.findByUsername(username);
        return user;
    }
    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userMapper.findByUsername(username); // 直接返回User或null
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        String role = user.getRole() != null ?
                user.getRole().getRoleName() :
                "USER";

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(role)
                .build();
    }
}
