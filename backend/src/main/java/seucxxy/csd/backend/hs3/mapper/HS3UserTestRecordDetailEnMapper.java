package seucxxy.csd.backend.hs3.mapper;

import org.apache.ibatis.annotations.*;
import seucxxy.csd.backend.hs3.entity.HS3UserTestRecordDetailEn;

import java.util.List;

@Mapper
public interface HS3UserTestRecordDetailEnMapper {
    
    @Insert("INSERT INTO user_test_record_detail_en (correct_answer, questions_en_number, segment_id, questions_type, user_answer, test_en_id) " +
            "VALUES (#{correctAnswer}, #{questionsEnNumber}, #{segmentId}, #{questionsType}, #{userAnswer}, #{testEnId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertUserTestRecordDetailEn(HS3UserTestRecordDetailEn detail);

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
    List<HS3UserTestRecordDetailEn> getUserTestRecordDetailsEnByTestEnId(@Param("testEnId") Long testEnId);
}
