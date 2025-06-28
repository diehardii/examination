package seucxxy.csd.backend.mapper;

import org.apache.ibatis.annotations.*;
import seucxxy.csd.backend.entity.UserTestRecordDetail;

import java.util.List;

@Mapper
public interface UserTestRecordDetailMapper {
    @Insert("INSERT INTO user_test_record_detail (correct_answer, question_number, question_type, user_answer, test_id) " +
            "VALUES (#{correctAnswer}, #{questionNumber}, #{questionType}, #{userAnswer}, #{testId})")
    void insertUserTestRecordDetail(UserTestRecordDetail detail);


    @Select("SELECT * FROM user_test_record_detail WHERE test_id = #{testId} AND question_number = #{questionNumber}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "correctAnswer", column = "correct_answer"),
            @Result(property = "questionNumber", column = "question_number"),
            @Result(property = "questionType", column = "question_type"),
            @Result(property = "userAnswer", column = "user_answer"),
            @Result(property = "testId", column = "test_id")
    })
    UserTestRecordDetail findUserTestRecordDeailByUserTestIdAndQuestionNumber(
            @Param("testId") Long testId,
            @Param("questionNumber") int questionNumber);

    @Select("SELECT * FROM user_test_record_detail WHERE test_id = #{testId}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "correctAnswer", column = "correct_answer"),
            @Result(property = "questionNumber", column = "question_number"),
            @Result(property = "questionType", column = "question_type"),
            @Result(property = "userAnswer", column = "user_answer"),
            @Result(property = "testId", column = "test_id")
    })
    List<UserTestRecordDetail> findUserTestRecordDetailsByTestId(@Param("testId") Long testId);

    @Delete("DELETE FROM examinai.user_test_record_detail " +
            "WHERE test_id IN (SELECT test_id FROM examinai.user_test_record WHERE exam_paper_id = #{examPaperId})")
    void deleteDetailsByExamPaperId(@Param("examPaperId") Long examPaperId);
}

