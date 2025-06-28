package seucxxy.csd.backend.mapper;



import org.apache.ibatis.annotations.*;
import seucxxy.csd.backend.entity.UserDetail;

@Mapper
public interface UserDetailMapper {
    @Select("SELECT * FROM userDetail WHERE user_id = #{userId}")
    @Results({
            @Result(property = "userId", column = "user_id", id = true),
            @Result(property = "realName", column = "real_name", id = true),
            @Result(property = "identityId", column = "identity_id"),
            @Result(property = "gender", column = "gender"),
            @Result(property = "email", column = "email"),
            @Result(property = "address", column = "address"),
            @Result(property = "universityId", column = "university_id"),
            @Result(property = "departmentId", column = "department_id"),
            @Result(property = "occupation", column = "occupation")
    })
    UserDetail findByUserId(Long userId);

    @Insert("INSERT INTO userDetail VALUES (#{userId}, #{identityId}, #{gender}, " +
            "#{email}, #{address}, #{universityId}, #{departmentId}, #{occupation},#{realName})")
    int insert(UserDetail userDetail);

    @Update("UPDATE userDetail SET identity_id=#{identityId}, gender=#{gender}, " +
            "email=#{email}, address=#{address}, university_id=#{universityId}, " +
            "department_id=#{departmentId}, occupation=#{occupation} ,real_name = #{realName} WHERE user_id=#{userId}")
    int update(UserDetail userDetail);
}