package seucxxy.csd.backend.cet4.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import seucxxy.csd.backend.cet4.entity.CET4PaperAnalysisTask;

import java.util.List;

@Mapper
public interface CET4PaperAnalysisTaskMapper {

    @Insert("INSERT INTO cet4_paper_analysis_task (user_id, status, progress, message, file_name, source_type, exam_paper_en_source, raw_text, segments_json, structured_json) " +
            "VALUES(#{userId}, #{status}, #{progress}, #{message}, #{fileName}, #{sourceType}, #{examPaperEnSource}, #{rawText}, #{segmentsJson}, #{structuredJson})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CET4PaperAnalysisTask task);

    @Select("SELECT id, user_id AS userId, status, progress, message, file_name AS fileName, source_type AS sourceType, exam_paper_en_source AS examPaperEnSource, " +
            "raw_text AS rawText, segments_json AS segmentsJson, structured_json AS structuredJson, created_at AS createdAt, updated_at AS updatedAt, completed_at AS completedAt " +
            "FROM cet4_paper_analysis_task WHERE id = #{id}")
    CET4PaperAnalysisTask findById(@Param("id") Long id);

    @Select("SELECT id, user_id AS userId, status, progress, message, file_name AS fileName, source_type AS sourceType, exam_paper_en_source AS examPaperEnSource, " +
            "raw_text AS rawText, segments_json AS segmentsJson, structured_json AS structuredJson, created_at AS createdAt, updated_at AS updatedAt, completed_at AS completedAt " +
            "FROM cet4_paper_analysis_task WHERE user_id = #{userId} " +
            "ORDER BY CASE WHEN status IN ('SUCCEEDED','FAILED') THEN 0 ELSE 1 END, completed_at DESC, updated_at DESC LIMIT #{limit}")
    List<CET4PaperAnalysisTask> findRecentByUser(@Param("userId") Long userId, @Param("limit") int limit);

    @Update("UPDATE cet4_paper_analysis_task SET status = 'RUNNING', progress = 5, message = NULL, updated_at = NOW() WHERE id = #{id}")
    int markRunning(@Param("id") Long id);

    @Update("UPDATE cet4_paper_analysis_task SET progress = #{progress}, message = COALESCE(#{message}, message), updated_at = NOW() WHERE id = #{id}")
    int updateProgress(@Param("id") Long id, @Param("progress") int progress, @Param("message") String message);

    @Update("UPDATE cet4_paper_analysis_task SET status = 'SUCCEEDED', progress = 100, message = #{message}, structured_json = #{structuredJson}, " +
            "completed_at = NOW(), updated_at = NOW() WHERE id = #{id}")
    int markSuccess(@Param("id") Long id, @Param("structuredJson") String structuredJson, @Param("message") String message);

    @Update("UPDATE cet4_paper_analysis_task SET status = 'FAILED', progress = 100, message = #{message}, completed_at = NOW(), updated_at = NOW() WHERE id = #{id}")
    int markFailed(@Param("id") Long id, @Param("message") String message);

    @Delete("DELETE FROM cet4_paper_analysis_task WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);
}
