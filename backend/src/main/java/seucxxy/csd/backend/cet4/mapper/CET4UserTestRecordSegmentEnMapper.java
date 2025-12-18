package seucxxy.csd.backend.cet4.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Result;

import seucxxy.csd.backend.cet4.entity.CET4UserTestRecordSegmentEn;
import seucxxy.csd.backend.cet4.dto.CET4WrongQuestionRecordDTO;

import java.util.List;

@Mapper
public interface CET4UserTestRecordSegmentEnMapper {

    @Insert("INSERT INTO user_test_record_segment_en(segment_id, question_type, score, correct_answers_number, number_of_questions, correct_answers_percent, test_en_id) " +
            "VALUES(#{segmentId}, #{questionType}, #{score}, #{correctAnswersNumber}, #{numberOfQuestions}, #{correctAnswersPercent}, #{testEnId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CET4UserTestRecordSegmentEn record);

    // 可选：按测试记录查询
    List<CET4UserTestRecordSegmentEn> findByTestEnId(@Param("testEnId") Long testEnId);

    /**
     * 查询用户的错题记录列表
     * 按时间排序
     */
    @Select("SELECT " +
            "s.test_en_id, " +
            "s.segment_id, " +
            "s.question_type, " +
            "s.correct_answers_number, " +
            "s.number_of_questions, " +
            "s.correct_answers_percent, " +
            "s.score, " +
            "r.test_en_time, " +
            "p.exam_paper_en_name " +
            "FROM user_test_record_segment_en s " +
            "INNER JOIN user_test_record_en r ON s.test_en_id = r.test_en_id " +
            "INNER JOIN exam_paper_en p ON r.exam_paper_en_id = p.id " +
            "WHERE r.user_id = #{userId} " +
            "AND s.correct_answers_percent < 100 " +
            "ORDER BY r.test_en_time ${sortOrder}")
    @Results({
            @Result(property = "testEnId", column = "test_en_id"),
            @Result(property = "segmentId", column = "segment_id"),
            @Result(property = "questionType", column = "question_type"),
            @Result(property = "correctAnswersNumber", column = "correct_answers_number"),
            @Result(property = "numberOfQuestions", column = "number_of_questions"),
            @Result(property = "correctAnswersPercent", column = "correct_answers_percent"),
            @Result(property = "score", column = "score"),
            @Result(property = "testEnTime", column = "test_en_time"),
            @Result(property = "examPaperEnName", column = "exam_paper_en_name")
    })
    List<CET4WrongQuestionRecordDTO> findWrongQuestionsByUserIdOrderByTime(
            @Param("userId") Long userId,
            @Param("sortOrder") String sortOrder);

    /**
     * 查询用户的错题记录列表
     * 按正确率排序
     */
    @Select("SELECT " +
            "s.test_en_id, " +
            "s.segment_id, " +
            "s.question_type, " +
            "s.correct_answers_number, " +
            "s.number_of_questions, " +
            "s.correct_answers_percent, " +
            "s.score, " +
            "r.test_en_time, " +
            "p.exam_paper_en_name " +
            "FROM user_test_record_segment_en s " +
            "INNER JOIN user_test_record_en r ON s.test_en_id = r.test_en_id " +
            "INNER JOIN exam_paper_en p ON r.exam_paper_en_id = p.id " +
            "WHERE r.user_id = #{userId} " +
            "AND s.correct_answers_percent < 100 " +
            "ORDER BY s.correct_answers_percent ${sortOrder}, r.test_en_time DESC")
    @Results({
            @Result(property = "testEnId", column = "test_en_id"),
            @Result(property = "segmentId", column = "segment_id"),
            @Result(property = "questionType", column = "question_type"),
            @Result(property = "correctAnswersNumber", column = "correct_answers_number"),
            @Result(property = "numberOfQuestions", column = "number_of_questions"),
            @Result(property = "correctAnswersPercent", column = "correct_answers_percent"),
            @Result(property = "score", column = "score"),
            @Result(property = "testEnTime", column = "test_en_time"),
            @Result(property = "examPaperEnName", column = "exam_paper_en_name")
    })
    List<CET4WrongQuestionRecordDTO> findWrongQuestionsByUserIdOrderByPercent(
            @Param("userId") Long userId,
            @Param("sortOrder") String sortOrder);

    /**
     * 统计用户错题总数
     */
    @Select("SELECT COUNT(*) " +
            "FROM user_test_record_segment_en s " +
            "INNER JOIN user_test_record_en r ON s.test_en_id = r.test_en_id " +
            "WHERE r.user_id = #{userId} " +
            "AND s.correct_answers_percent < 100")
    int countWrongQuestionsByUserId(@Param("userId") Long userId);

    /**
     * 分页查询用户的错题记录列表（按时间排序）
     */
    @Select("SELECT " +
            "s.test_en_id, " +
            "s.segment_id, " +
            "s.question_type, " +
            "s.correct_answers_number, " +
            "s.number_of_questions, " +
            "s.correct_answers_percent, " +
            "s.score, " +
            "r.test_en_time, " +
            "p.exam_paper_en_name " +
            "FROM user_test_record_segment_en s " +
            "INNER JOIN user_test_record_en r ON s.test_en_id = r.test_en_id " +
            "INNER JOIN exam_paper_en p ON r.exam_paper_en_id = p.id " +
            "WHERE r.user_id = #{userId} " +
            "AND s.correct_answers_percent < 100 " +
            "ORDER BY r.test_en_time ${sortOrder} " +
            "LIMIT #{pageSize} OFFSET #{offset}")
    @Results({
            @Result(property = "testEnId", column = "test_en_id"),
            @Result(property = "segmentId", column = "segment_id"),
            @Result(property = "questionType", column = "question_type"),
            @Result(property = "correctAnswersNumber", column = "correct_answers_number"),
            @Result(property = "numberOfQuestions", column = "number_of_questions"),
            @Result(property = "correctAnswersPercent", column = "correct_answers_percent"),
            @Result(property = "score", column = "score"),
            @Result(property = "testEnTime", column = "test_en_time"),
            @Result(property = "examPaperEnName", column = "exam_paper_en_name")
    })
    List<CET4WrongQuestionRecordDTO> findWrongQuestionsByUserIdOrderByTimeWithPagination(
            @Param("userId") Long userId,
            @Param("sortOrder") String sortOrder,
            @Param("pageSize") int pageSize,
            @Param("offset") int offset);

    /**
     * 分页查询用户的错题记录列表（按正确率排序）
     */
    @Select("SELECT " +
            "s.test_en_id, " +
            "s.segment_id, " +
            "s.question_type, " +
            "s.correct_answers_number, " +
            "s.number_of_questions, " +
            "s.correct_answers_percent, " +
            "s.score, " +
            "r.test_en_time, " +
            "p.exam_paper_en_name " +
            "FROM user_test_record_segment_en s " +
            "INNER JOIN user_test_record_en r ON s.test_en_id = r.test_en_id " +
            "INNER JOIN exam_paper_en p ON r.exam_paper_en_id = p.id " +
            "WHERE r.user_id = #{userId} " +
            "AND s.correct_answers_percent < 100 " +
            "ORDER BY s.correct_answers_percent ${sortOrder}, r.test_en_time DESC " +
            "LIMIT #{pageSize} OFFSET #{offset}")
    @Results({
            @Result(property = "testEnId", column = "test_en_id"),
            @Result(property = "segmentId", column = "segment_id"),
            @Result(property = "questionType", column = "question_type"),
            @Result(property = "correctAnswersNumber", column = "correct_answers_number"),
            @Result(property = "numberOfQuestions", column = "number_of_questions"),
            @Result(property = "correctAnswersPercent", column = "correct_answers_percent"),
            @Result(property = "score", column = "score"),
            @Result(property = "testEnTime", column = "test_en_time"),
            @Result(property = "examPaperEnName", column = "exam_paper_en_name")
    })
    List<CET4WrongQuestionRecordDTO> findWrongQuestionsByUserIdOrderByPercentWithPagination(
            @Param("userId") Long userId,
            @Param("sortOrder") String sortOrder,
            @Param("pageSize") int pageSize,
            @Param("offset") int offset);
}
