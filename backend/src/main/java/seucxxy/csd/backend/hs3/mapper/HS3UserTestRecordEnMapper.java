package seucxxy.csd.backend.hs3.mapper;

import org.apache.ibatis.annotations.*;
import seucxxy.csd.backend.hs3.entity.HS3UserTestRecordEn;

import java.util.List;

@Mapper
public interface HS3UserTestRecordEnMapper {
    
    @Insert("INSERT INTO user_test_record_en (correct_number, test_en_score, test_en_time, exam_paper_en_id, user_id) " +
            "VALUES (#{correctNumber}, #{testEnScore}, #{testEnTime}, #{examPaperEnId}, #{userId})")
    @Options(useGeneratedKeys = true, keyProperty = "testEnId")
    void insertUserTestRecordEn(HS3UserTestRecordEn record);

    @Select("SELECT * FROM user_test_record_en WHERE test_en_id = #{testEnId}")
    @Results({
            @Result(property = "testEnId", column = "test_en_id"),
            @Result(property = "correctNumber", column = "correct_number"),
            @Result(property = "testEnScore", column = "test_en_score"),
            @Result(property = "testEnTime", column = "test_en_time"),
            @Result(property = "examPaperEnId", column = "exam_paper_en_id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "examPaperEn", column = "exam_paper_en_id",
                    one = @One(select = "seucxxy.csd.backend.hs3.mapper.HS3ExamPaperEnMapper.getExamPaperEnById"))
    })
    HS3UserTestRecordEn getUserTestRecordEnById(@Param("testEnId") Long testEnId);

    @Update("UPDATE user_test_record_en SET correct_number = #{correctNumber}, test_en_score = #{testEnScore} WHERE test_en_id = #{testEnId}")
    void updateUserTestRecordEn(HS3UserTestRecordEn record);
}
