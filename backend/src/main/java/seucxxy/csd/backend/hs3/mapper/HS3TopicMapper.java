package seucxxy.csd.backend.hs3.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * HS3 Topic Mapper
 * 
 * 从关系数据库读取topics数组
 */
@Mapper
public interface HS3TopicMapper {
    
    /**
     * 获取所有主题
     */
    @Select("SELECT topic FROM topics ORDER BY id")
    List<String> selectAllTopics();
}
