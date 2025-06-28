package seucxxy.csd.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import seucxxy.csd.backend.entity.ExamPaper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ExamPaperMapper {

    @Select("SELECT * FROM exam_paper WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "examPaperName", column = "exam_paper_name"),
            @Result(property = "examPaperContent", column = "exam_paper_content"),
            @Result(property = "examPaperDifficulty", column = "exam_paper_difficulty"),
            @Result(property = "examPaperQuestionNumber", column = "exam_paper_question_number"),
            @Result(property = "examPaperSubject", column = "exam_paper_subject")
    })
    ExamPaper findExamPaperById(Long id);

    @Select("SELECT * FROM exam_paper WHERE exam_paper_name = #{name}")
    ExamPaper findExamPaperByName(String name);

    @Insert("INSERT INTO exam_paper(exam_paper_name, exam_paper_content, exam_paper_difficulty, " +
            "exam_paper_question_number, exam_paper_subject) " +
            "VALUES(#{examPaperName}, #{examPaperContent}, #{examPaperDifficulty}, " +
            "#{examPaperQuestionNumber}, #{examPaperSubject})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertExamPaper(ExamPaper examPaper);


    @Select("SELECT * FROM exam_paper WHERE id NOT IN (SELECT exam_paper_id FROM user_exam_paper WHERE username = #{username})")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "examPaperName", column = "exam_paper_name"),
            @Result(property = "examPaperContent", column = "exam_paper_content"),
            @Result(property = "examPaperDifficulty", column = "exam_paper_difficulty"),
            @Result(property = "examPaperQuestionNumber", column = "exam_paper_question_number"),
            @Result(property = "examPaperSubject", column = "exam_paper_subject")
    })
    List<ExamPaper> findAvailableExamPapers(String username);


    @Select("SELECT * FROM exam_paper WHERE id = #{id}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "examPaperName", column = "exam_paper_name"),
            @Result(property = "examPaperContent", column = "exam_paper_content"),
            @Result(property = "examPaperDifficulty", column = "exam_paper_difficulty"),
            @Result(property = "examPaperQuestionNumber", column = "exam_paper_question_number"),
            @Result(property = "examPaperSubject", column = "exam_paper_subject"),
            @Result(property = "questions", column = "id",
                    many = @Many(select = "seucxxy.csd.backend.mapper.QuestionMapper.findQuestionsByExamPaperId"))
    })
    ExamPaper findExamPaperByIdWithQuestions(Long id);


    @Select("SELECT id, exam_paper_name as examPaperName, exam_paper_content as examPaperContent, " +
            "exam_paper_subject as examPaperSubject, exam_paper_difficulty as examPaperDifficulty " +
            "FROM examinai.exam_paper")
    List<ExamPaper> findAll();

    @Delete("DELETE FROM examinai.exam_paper WHERE id = #{examPaperId}")
    void deleteById(@Param("examPaperId") Long examPaperId);
}