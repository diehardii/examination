package seucxxy.csd.backend.mapper;

import org.apache.ibatis.annotations.*;
import seucxxy.csd.backend.entity.ExamPaper;
import seucxxy.csd.backend.entity.UserTestRecord;

import java.util.List;

@Mapper
public interface UserTestRecordMapper {
    @Insert("INSERT INTO user_test_record (correct_number, test_score, test_time, exam_paper_id, user_id) " +
            "VALUES (#{correctNumber}, #{testScore}, #{testTime}, #{examPaperId}, #{userId})")
    @Options(useGeneratedKeys = true, keyProperty = "testId")
    void insertUserTestRecord(UserTestRecord record);


    @Select("SELECT * FROM user_test_record WHERE user_id = (SELECT id FROM users WHERE username = #{username}) ORDER BY test_time DESC")
    @Results({
            @Result(property = "testId", column = "test_id"),
            @Result(property = "correctNumber", column = "correct_number"),
            @Result(property = "testScore", column = "test_score"),
            @Result(property = "testTime", column = "test_time"),
            @Result(property = "examPaperId", column = "exam_paper_id"),
            @Result(property = "userId", column = "user_id")
    })
    List<UserTestRecord> findUserTestRecordsByUsernameOrderByTestTimeDesc(String username);



    @Select("SELECT * FROM user_test_record WHERE test_id = #{testId} AND user_id = #{Id}")
    @Results({
            @Result(property = "testId", column = "test_id"),
            @Result(property = "correctNumber", column = "correct_number"),
            @Result(property = "testScore", column = "test_score"),
            @Result(property = "testTime", column = "test_time"),
            @Result(property = "examPaperId", column = "exam_paper_id"),
            @Result(property = "userId", column = "user_id")
    })
   UserTestRecord findUserTestRecordByTestIdAndUserId(@Param("testId") Long testId, @Param("Id") Long Id);


        @Select("SELECT * FROM user_test_record WHERE user_id = (SELECT id FROM users WHERE username = #{username}) " +
                "AND exam_paper_id IN (SELECT id FROM exam_paper WHERE exam_paper_subject = #{subject}) " +
                "ORDER BY test_time DESC")
        @Results({
                @Result(property = "testId", column = "test_id"),
                @Result(property = "correctNumber", column = "correct_number"),
                @Result(property = "testScore", column = "test_score"),
                @Result(property = "testTime", column = "test_time"),
                @Result(property = "examPaperId", column = "exam_paper_id"),
                @Result(property = "userId", column = "user_id"),
                @Result(property = "examPaper", column = "exam_paper_id",
                        one = @One(select = "seucxxy.csd.backend.mapper.ExamPaperMapper.findExamPaperById"))
        })
        List<UserTestRecord> findUserTestRecordsByUsernameAndSubject(@Param("username") String username,
                                                      @Param("subject") String subject);


    @Select("SELECT utr.*, ep.* FROM user_test_record utr " +
            "LEFT JOIN exam_paper ep ON utr.exam_paper_id = ep.id " +
            "WHERE utr.user_id = #{userId} AND utr.exam_paper_id = #{examPaperId} " +
            "ORDER BY utr.test_time DESC")
    @Results(id = "userTestRecordResultMap", value = {
            @Result(property = "testId", column = "test_id", id = true),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "testTime", column = "test_time"),
            @Result(property = "testScore", column = "test_score"),
            @Result(property = "correctNumber", column = "correct_number"),


            // 映射 examPaper 对象
            @Result(
                    property = "examPaper", // UserTestRecord 中的属性名
                    column = "exam_paper_id", // 关联字段
                    javaType = ExamPaper.class, // 关联的类
                    one = @One(select = "seucxxy.csd.backend.mapper.ExamPaperMapper.findExamPaperById") // 调用另一个查询
            )
    })
    List<UserTestRecord> findByUserIdAndExamPaperId(
            @Param("userId") long userId,
            @Param("examPaperId") long examPaperId
    );

    @Select("SELECT * FROM user_test_record WHERE test_id = #{id}")
    @Results(id = "userTestRecordMap", value = {
            @Result(property = "testId", column = "test_id", id = true),
            @Result(property = "userId", column = "user_id"),
            @Result(property = "examPaperId", column = "exam_paper_id"),
            @Result(property = "examPaperName", column = "exam_paper_name"),
            @Result(property = "testTime", column = "test_time"),
            @Result(property = "testScore", column = "test_score"),
            @Result(property = "correctNumber", column = "correct_number"),
            @Result(property = "totalNumber", column = "total_number"),
            // 如果有其他字段继续添加...
    })
    UserTestRecord findByTestId(@Param("id") long id);

        @Select("SELECT COUNT(*) FROM examinai.user_test_record " +
                "WHERE user_id = #{userId} AND exam_paper_id = #{examPaperId}")
        int countByUserIdAndExamPaperId(@Param("userId") Long userId,
                                        @Param("examPaperId") Long examPaperId);

    @Delete("DELETE FROM examinai.user_test_record WHERE exam_paper_id = #{examPaperId}")
    void deleteByExamPaperId(@Param("examPaperId") Long examPaperId);
}


