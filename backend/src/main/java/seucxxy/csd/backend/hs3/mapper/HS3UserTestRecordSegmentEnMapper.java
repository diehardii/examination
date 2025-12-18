package seucxxy.csd.backend.hs3.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Result;

import seucxxy.csd.backend.hs3.entity.HS3UserTestRecordSegmentEn;

import java.util.List;

@Mapper
public interface HS3UserTestRecordSegmentEnMapper {

    @Insert("INSERT INTO user_test_record_segment_en(segment_id, question_type, score, correct_answers_number, number_of_questions, correct_answers_percent, test_en_id) " +
            "VALUES(#{segmentId}, #{questionType}, #{score}, #{correctAnswersNumber}, #{numberOfQuestions}, #{correctAnswersPercent}, #{testEnId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(HS3UserTestRecordSegmentEn record);

    @Select("SELECT * FROM user_test_record_segment_en WHERE test_en_id = #{testEnId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "segmentId", column = "segment_id"),
            @Result(property = "questionType", column = "question_type"),
            @Result(property = "score", column = "score"),
            @Result(property = "correctAnswersNumber", column = "correct_answers_number"),
            @Result(property = "numberOfQuestions", column = "number_of_questions"),
            @Result(property = "correctAnswersPercent", column = "correct_answers_percent"),
            @Result(property = "testEnId", column = "test_en_id")
    })
    List<HS3UserTestRecordSegmentEn> findByTestEnId(@Param("testEnId") Long testEnId);
}
