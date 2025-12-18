package seucxxy.csd.backend.cet4.mapper;

import org.apache.ibatis.annotations.*;
import seucxxy.csd.backend.cet4.entity.CET4UserTestRecordEn;
import seucxxy.csd.backend.cet4.entity.CET4ExamPaperEn;

import java.util.List;

@Mapper
public interface CET4UserTestRecordEnMapper {
    
    @Insert("INSERT INTO user_test_record_en (correct_number, test_en_score, test_en_time, exam_paper_en_id, user_id) " +
            "VALUES (#{correctNumber}, #{testEnScore}, #{testEnTime}, #{examPaperEnId}, #{userId})")
    @Options(useGeneratedKeys = true, keyProperty = "testEnId")
    void insertUserTestRecordEn(CET4UserTestRecordEn record);

    @Select("SELECT * FROM user_test_record_en WHERE test_en_id = #{testEnId}")
    @Results({
            @Result(property = "testEnId", column = "test_en_id"),
            @Result(property = "correctNumber", column = "correct_number"),
            @Result(property = "testEnScore", column = "test_en_score"),
            @Result(property = "testEnTime", column = "test_en_time"),
            @Result(property = "examPaperEnId", column = "exam_paper_en_id"),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "examPaperEn", column = "exam_paper_en_id",
                    one = @One(select = "seucxxy.csd.backend.cet4.mapper.ExamPaperEnMapper.getExamPaperEnById"))
    })
    CET4UserTestRecordEn getUserTestRecordEnById(@Param("testEnId") Long testEnId);

    @Select("SELECT * FROM user_test_record_en WHERE user_id = #{userId} AND exam_paper_en_id = #{examPaperEnId} ORDER BY test_en_time DESC")
    @Results({
            @Result(property = "testEnId", column = "test_en_id"),
            @Result(property = "correctNumber", column = "correct_number"),
            @Result(property = "testEnScore", column = "test_en_score"),
            @Result(property = "testEnTime", column = "test_en_time"),
            @Result(property = "examPaperEnId", column = "exam_paper_en_id"),
            @Result(property = "userId", column = "user_id")
    })
    List<CET4UserTestRecordEn> getUserTestRecordEnsByExamPaperEnId(@Param("userId") Long userId, @Param("examPaperEnId") Long examPaperEnId);

    @Select("SELECT * FROM user_test_record_en WHERE user_id = #{userId} ORDER BY test_en_time DESC LIMIT #{limit}")
    @Results({
            @Result(property = "testEnId", column = "test_en_id"),
            @Result(property = "correctNumber", column = "correct_number"),
            @Result(property = "testEnScore", column = "test_en_score"),
            @Result(property = "testEnTime", column = "test_en_time"),
            @Result(property = "examPaperEnId", column = "exam_paper_en_id"),
            @Result(property = "userId", column = "user_id")
    })
    List<CET4UserTestRecordEn> findRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    @Update("UPDATE user_test_record_en SET correct_number = #{correctNumber}, test_en_score = #{testEnScore} WHERE test_en_id = #{testEnId}")
    void updateUserTestRecordEn(CET4UserTestRecordEn record);
}
