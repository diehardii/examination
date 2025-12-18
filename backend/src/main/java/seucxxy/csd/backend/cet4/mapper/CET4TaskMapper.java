package seucxxy.csd.backend.cet4.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import seucxxy.csd.backend.cet4.entity.CET4Task;

import java.util.List;

@Mapper
public interface CET4TaskMapper {

    @Insert("INSERT INTO cet4_paper_gen_task " +
            "(user_id, task_type, async_mode, status, progress, message, exam_paper_en_source, payload_json, requested_total) " +
            "VALUES(#{userId}, #{taskType}, #{asyncMode}, #{status}, #{progress}, #{message}, #{examPaperEnSource}, #{payloadJson}, #{requestedTotal})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CET4Task task);

    @Select("SELECT id, user_id AS userId, task_type AS taskType, async_mode AS asyncMode, status, progress, message, " +
            "exam_paper_en_source AS examPaperEnSource, payload_json AS payloadJson, result_json AS resultJson, exam_paper_en_id AS examPaperEnId, " +
            "requested_total AS requestedTotal, generated_count AS generatedCount, failed_count AS failedCount, failed_types_json AS failedTypesJson, " +
            "created_at AS createdAt, updated_at AS updatedAt, completed_at AS completedAt " +
            "FROM cet4_paper_gen_task WHERE id = #{id}")
    CET4Task findById(@Param("id") Long id);

    @Select("SELECT id, user_id AS userId, task_type AS taskType, async_mode AS asyncMode, status, progress, message, " +
            "exam_paper_en_source AS examPaperEnSource, payload_json AS payloadJson, result_json AS resultJson, exam_paper_en_id AS examPaperEnId, " +
            "requested_total AS requestedTotal, generated_count AS generatedCount, failed_count AS failedCount, failed_types_json AS failedTypesJson, " +
            "created_at AS createdAt, updated_at AS updatedAt, completed_at AS completedAt " +
            "FROM cet4_paper_gen_task WHERE user_id = #{userId} AND task_type = #{taskType} AND async_mode = TRUE " +
            "ORDER BY CASE WHEN status IN ('SUCCEEDED','FAILED') THEN 0 ELSE 1 END, completed_at DESC, updated_at DESC LIMIT #{limit}")
    List<CET4Task> findRecentByUser(@Param("userId") Long userId, @Param("taskType") String taskType, @Param("limit") int limit);

    @Update("UPDATE cet4_paper_gen_task SET status = 'RUNNING', progress = 5, message = NULL, updated_at = NOW() WHERE id = #{id}")
    int markRunning(@Param("id") Long id);

    @Update("UPDATE cet4_paper_gen_task SET progress = #{progress}, message = COALESCE(#{message}, message), updated_at = NOW() WHERE id = #{id}")
    int updateProgress(@Param("id") Long id, @Param("progress") int progress, @Param("message") String message);

    @Update("UPDATE cet4_paper_gen_task SET status = 'SUCCEEDED', progress = 100, message = #{message}, exam_paper_en_id = #{examPaperEnId}, " +
            "result_json = #{resultJson}, completed_at = NOW(), updated_at = NOW() WHERE id = #{id}")
    int markPaperSuccess(@Param("id") Long id,
                         @Param("examPaperEnId") Long examPaperEnId,
                         @Param("resultJson") String resultJson,
                         @Param("message") String message);

    @Update("UPDATE cet4_paper_gen_task SET status = 'SUCCEEDED', progress = 100, message = #{message}, generated_count = #{generatedCount}, " +
            "failed_count = #{failedCount}, failed_types_json = #{failedTypesJson}, result_json = #{resultJson}, completed_at = NOW(), updated_at = NOW() WHERE id = #{id}")
    int markIntensiveSuccess(@Param("id") Long id,
                             @Param("generatedCount") Integer generatedCount,
                             @Param("failedCount") Integer failedCount,
                             @Param("failedTypesJson") String failedTypesJson,
                             @Param("resultJson") String resultJson,
                             @Param("message") String message);

    @Update("UPDATE cet4_paper_gen_task SET status = 'FAILED', progress = 100, message = #{message}, completed_at = NOW(), updated_at = NOW() WHERE id = #{id}")
    int markFailed(@Param("id") Long id, @Param("message") String message);

    @Delete("DELETE FROM cet4_paper_gen_task WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUser(@Param("id") Long id, @Param("userId") Long userId);
}
