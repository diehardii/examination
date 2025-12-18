package seucxxy.csd.backend.common.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import seucxxy.csd.backend.common.entity.EducationStage;

import java.util.List;

@Mapper
public interface EducationStageMapper {

    @Select("SELECT * FROM education_stages ORDER BY stage_id")
    List<EducationStage> findAll();

    @Select("SELECT * FROM education_stages WHERE stage_id = #{stageId}")
    EducationStage findById(Integer stageId);

    @Select("SELECT COUNT(1) FROM education_stages WHERE stage_id = #{stageId}")
    int existsById(Integer stageId);
}
