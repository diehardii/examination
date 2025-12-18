package seucxxy.csd.backend.cet4.mapper;

import org.apache.ibatis.annotations.*;
import seucxxy.csd.backend.cet4.entity.CET4UserTestRecordDetailEn;

import java.util.List;

@Mapper
public interface CET4UserTestRecordDetailEnMapper {
    
    @Insert("INSERT INTO user_test_record_detail_en (correct_answer, questions_en_number, segment_id, questions_type, user_answer, test_en_id) " +
            "VALUES (#{correctAnswer}, #{questionsEnNumber}, #{segmentId}, #{questionsType}, #{userAnswer}, #{testEnId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertUserTestRecordDetailEn(CET4UserTestRecordDetailEn detail);

    @Select("SELECT * FROM user_test_record_detail_en WHERE test_en_id = #{testEnId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "correctAnswer", column = "correct_answer"),
            @Result(property = "questionsEnNumber", column = "questions_en_number"),
            @Result(property = "segmentId", column = "segment_id"),
            @Result(property = "questionsType", column = "questions_type"),
            @Result(property = "userAnswer", column = "user_answer"),
            @Result(property = "testEnId", column = "test_en_id")
    })
    List<CET4UserTestRecordDetailEn> getUserTestRecordDetailsEnByTestEnId(@Param("testEnId") Long testEnId);

    @Select("SELECT * FROM user_test_record_detail_en WHERE test_en_id = #{testEnId} AND questions_en_number = #{questionsEnNumber}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "correctAnswer", column = "correct_answer"),
            @Result(property = "questionsEnNumber", column = "questions_en_number"),
            @Result(property = "segmentId", column = "segment_id"),
            @Result(property = "questionsType", column = "questions_type"),
            @Result(property = "userAnswer", column = "user_answer"),
            @Result(property = "testEnId", column = "test_en_id")
    })
    CET4UserTestRecordDetailEn getUserTestRecordDetailEn(@Param("testEnId") Long testEnId, @Param("questionsEnNumber") int questionsEnNumber);
    
    /**
     * 查询某个用户某个题型的错题(答案不一致的记录)
     * 注意: Writing和Translation题型不需要查错题
     */
    @Select("SELECT d.* FROM user_test_record_detail_en d " +
            "INNER JOIN user_test_record_en r ON d.test_en_id = r.test_en_id " +
            "WHERE r.user_id = #{userId} " +
            "AND d.questions_type = #{questionType} " +
            "AND d.correct_answer != d.user_answer " +
            "ORDER BY RAND() " +
            "LIMIT 1")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "correctAnswer", column = "correct_answer"),
            @Result(property = "questionsEnNumber", column = "questions_en_number"),
            @Result(property = "segmentId", column = "segment_id"),
            @Result(property = "questionsType", column = "questions_type"),
            @Result(property = "userAnswer", column = "user_answer"),
            @Result(property = "testEnId", column = "test_en_id")
    })
    CET4UserTestRecordDetailEn getRandomWrongQuestion(@Param("userId") Long userId, @Param("questionType") String questionType);
}
