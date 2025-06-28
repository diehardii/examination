package seucxxy.csd.backend.mapper;

import org.apache.ibatis.annotations.*;
import seucxxy.csd.backend.entity.ExamPaper;
import seucxxy.csd.backend.entity.UserExamPaper;

import java.util.List;

@Mapper
public interface UserExamPaperMapper {
    @Select("SELECT * FROM user_exam_paper WHERE username = #{username}")
    List<UserExamPaper> findByUsername(String username);

    @Select("SELECT * FROM exam_paper WHERE id IN (SELECT exam_paper_id FROM user_exam_paper WHERE username = #{username})")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "examPaperName", column = "exam_paper_name"),
            @Result(property = "examPaperContent", column = "exam_paper_content"),
            @Result(property = "examPaperDifficulty", column = "exam_paper_difficulty"),
            @Result(property = "examPaperQuestionNumber", column = "exam_paper_question_number"),
            @Result(property = "examPaperSubject", column = "exam_paper_subject")
    })
    List<ExamPaper> findExamPapersByUsername(String username);

    @Insert("INSERT INTO user_exam_paper(exam_paper_id, username) VALUES(#{examPaperId}, #{username})")
    void insert(UserExamPaper userExamPaper);


    @Delete("DELETE FROM user_exam_paper WHERE username = #{username}")
    void deleteByUsername(String username);

    @Delete("DELETE FROM examinai.user_exam_paper WHERE exam_paper_id = #{examPaperId}")
    void deleteByExamPaperId(@Param("examPaperId") Long examPaperId);
}