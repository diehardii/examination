package seucxxy.csd.backend.common.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import seucxxy.csd.backend.common.entity.Grade;

import java.util.List;

@Mapper
public interface GradeMapper {

    @Select("SELECT * FROM grades ORDER BY stage_id, grade_level")
    List<Grade> findAll();

    @Select("SELECT * FROM grades WHERE stage_id = #{stageId} ORDER BY grade_level")
    List<Grade> findByStageId(@Param("stageId") Integer stageId);
}
