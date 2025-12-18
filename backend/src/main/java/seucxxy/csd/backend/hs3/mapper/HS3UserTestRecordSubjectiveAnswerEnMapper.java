package seucxxy.csd.backend.hs3.mapper;

import org.apache.ibatis.annotations.*;
import seucxxy.csd.backend.hs3.entity.HS3UserTestRecordSubjectiveAnswerEn;

import java.util.List;

@Mapper
public interface HS3UserTestRecordSubjectiveAnswerEnMapper {
    
    @Insert("INSERT INTO user_test_record_subjective_answer_en (test_en_id, segment_name, user_answer, user_answer_grade) " +
            "VALUES (#{testEnId}, #{segmentName}, #{userAnswer}, #{userAnswerGrade})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(HS3UserTestRecordSubjectiveAnswerEn answer);

    @Select("SELECT * FROM user_test_record_subjective_answer_en WHERE test_en_id = #{testEnId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "testEnId", column = "test_en_id"),
            @Result(property = "segmentName", column = "segment_name"),
            @Result(property = "userAnswer", column = "user_answer"),
            @Result(property = "userAnswerGrade", column = "user_answer_grade")
    })
    List<HS3UserTestRecordSubjectiveAnswerEn> findByTestEnId(@Param("testEnId") Long testEnId);
}
