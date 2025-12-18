package seucxxy.csd.backend.cet4.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import seucxxy.csd.backend.cet4.entity.CET4ExamPaperEnCet4Structure;

@Mapper
public interface CET4ExamPaperEnCet4StructureMapper {

    /**
     * 根据 segment_id 查询结构信息
     */
    @Select("SELECT part_id as partId, section_id as sectionId, segment_id as segmentId, " +
            "number_of_questions as numberOfQuestions, score_per_question as scorePerQuestion, " +
            "segment_total_score as segmentTotalScore " +
            "FROM exam_paper_en_cet4_structure WHERE segment_id = #{segmentId}")
    CET4ExamPaperEnCet4Structure findBySegmentId(@Param("segmentId") String segmentId);
}
