package seucxxy.csd.backend.cet4.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CET4TopicMapper {
    @Select("SELECT topic FROM topics ORDER BY id")
    List<String> selectAllTopics();
}

