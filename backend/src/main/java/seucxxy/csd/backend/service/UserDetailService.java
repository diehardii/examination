package seucxxy.csd.backend.service;



import org.springframework.stereotype.Service;
import seucxxy.csd.backend.dto.UserInfoDTO;
import seucxxy.csd.backend.entity.User;
import seucxxy.csd.backend.entity.UserDetail;
import seucxxy.csd.backend.mapper.UserDetailMapper;
import seucxxy.csd.backend.mapper.UserMapper;

@Service
public class UserDetailService {
    private final UserDetailMapper userDetailMapper;
    private final UserMapper userMapper;

    public UserDetailService(UserDetailMapper userDetailMapper,
                             UserMapper userMapper) {
        this.userDetailMapper = userDetailMapper;
        this.userMapper = userMapper;
    }

    public UserDetail getDetail(Long userId) {
        return userDetailMapper.findByUserId(userId);
    }

    public UserInfoDTO getUserInfoDTO(Long userId)
    {
        UserInfoDTO userInfoDTO = new UserInfoDTO();
        UserDetail userDetail = userDetailMapper.findByUserId(userId);
        userInfoDTO.setRealName(userDetail.getRealName());
        userInfoDTO.setGender(userDetail.getGender());
        userInfoDTO.setAddress(userDetail.getAddress());
        userInfoDTO.setOccupation(userDetail.getOccupation());
        userInfoDTO.setDepartmentId(userDetail.getDepartmentId());
        userInfoDTO.setUniversityId(userDetail.getUniversityId());
        userInfoDTO.setIdentityId(userDetail.getIdentityId());
        userInfoDTO.setEmail(userDetail.getEmail());
        userInfoDTO.setPhoneNumber(userMapper.findUserById(userId).getPhone());
        return userInfoDTO;
    }

    public void saveDetail(UserInfoDTO userInfoDTO,long userId) {
        UserDetail userDetail = new UserDetail();
        userDetail.setUserId(userId);
        userDetail.setRealName(userInfoDTO.getRealName());
        userDetail.setIdentityId(userInfoDTO.getIdentityId());
        userDetail.setUniversityId(userInfoDTO.getUniversityId());
        userDetail.setDepartmentId(userInfoDTO.getDepartmentId());
        userDetail.setAddress(userInfoDTO.getAddress());
        userDetail.setEmail(userInfoDTO.getEmail());
        userDetail.setGender(userInfoDTO.getGender());
        userDetail.setOccupation(userInfoDTO.getOccupation());
        userDetail.setRealName(userInfoDTO.getRealName());
        userMapper.updatePhone(userInfoDTO.getUserId(), userInfoDTO.getPhoneNumber());
        if (userDetailMapper.findByUserId(userId) == null) {
            userDetailMapper.insert(userDetail);
        } else {
            userDetailMapper.update(userDetail);
        }
    }
}
