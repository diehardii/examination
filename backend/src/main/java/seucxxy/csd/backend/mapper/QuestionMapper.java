package seucxxy.csd.backend.mapper;

import seucxxy.csd.backend.dto.QuestionWrongDTO;
import seucxxy.csd.backend.entity.Question;
import seucxxy.csd.backend.entity.SingleChoiceQuestion;
import seucxxy.csd.backend.entity.JudgeQuestion;
import org.apache.ibatis.annotations.*;

import java.util.List;


@Mapper
public interface QuestionMapper {

    @Select("SELECT * FROM question WHERE exam_paper_id = #{examPaperId} ORDER BY number")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "questionType", column = "question_type"),
            @Result(property = "type", column = "type"),
            @Result(property = "content", column = "content"),
            @Result(property = "number", column = "number"),
            @Result(property = "examPaperId", column = "exam_paper_id"),
            @Result(property = "correctAnswer", column = "correct_answer"),
            // 公共字段映射

            // 单选题特有字段映射（会被TypeDiscriminator处理）
            @Result(property = "optionA", column = "optiona"),
            @Result(property = "optionB", column = "optionb"),
            @Result(property = "optionC", column = "optionc"),
            @Result(property = "optionD", column = "optiond")
    })
    @TypeDiscriminator(
            column = "question_type",
            javaType = String.class,
            cases = {
                    @Case(value = "SINGLE_CHOICE", type = SingleChoiceQuestion.class),
                    @Case(value = "JUDGE", type = JudgeQuestion.class),

                    // 添加其他题型对应的子类...
            }
    )
    List<Question> findQuestionsByExamPaperId(Long examPaperId);

    @Select({
            "<script>",
            "SELECT id, question_type, type, content, number, exam_paper_id, correct_answer",
            "<if test='\"SINGLE_CHOICE\".equals(questionType)'>",  // 避免空指针
            ",optiona, optionb, optionc, optiond",
            "</if>",
            "FROM question WHERE exam_paper_id = #{examPaperId} ORDER BY number",
            "</script>"
    })
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "questionType", column = "question_type"),
            @Result(property = "type", column = "type"),
            @Result(property = "content", column = "content"),
            @Result(property = "number", column = "number"),
            @Result(property = "examPaperId", column = "exam_paper_id"),
            @Result(property = "correctAnswer", column = "correct_answer"),
            // 这些字段只有在SINGLE_CHOICE类型时才会被映射
            @Result(property = "optionA", column = "optiona"),
            @Result(property = "optionB", column = "optionb"),
            @Result(property = "optionC", column = "optionc"),
            @Result(property = "optionD", column = "optiond")
    })
    List<Question> findQuestionsByExamPaperIdOLd(Long examPaperId);

    @Insert({
            "<script>",
            "INSERT INTO question (question_type, type, content, number, exam_paper_id, ",
            "<if test='questionType == \"SINGLE_CHOICE\"'>",
            "optiona, optionb, optionc, optiond, ",
            "</if>",
            "correct_answer) ",
            "VALUES (#{questionType}, #{type}, #{content}, #{number}, #{examPaperId}, ",
            "<if test='questionType == \"SINGLE_CHOICE\"'>",
            "#{optionA}, #{optionB}, #{optionC}, #{optionD}, ",
            "</if>",
            "#{correctAnswer})",
            "</script>"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertQuestion(Question question);


    @Select("SELECT * FROM question WHERE exam_paper_id = #{examPaperId} AND number = #{number}")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "questionType", column = "question_type"),
            @Result(property = "type", column = "type"),
            @Result(property = "content", column = "content"),
            @Result(property = "number", column = "number"),
            @Result(property = "examPaperId", column = "exam_paper_id"),
            @Result(property = "correctAnswer", column = "correct_answer"),
            // 公共字段映射

            // 单选题特有字段映射（会被TypeDiscriminator处理）
            @Result(property = "optionA", column = "optiona"),
            @Result(property = "optionB", column = "optionb"),
            @Result(property = "optionC", column = "optionc"),
            @Result(property = "optionD", column = "optiond")
    })
    @TypeDiscriminator(
            column = "question_type",
            javaType = String.class,
            cases = {
                    @Case(value = "SINGLE_CHOICE", type = SingleChoiceQuestion.class),
                    @Case(value = "JUDGE", type = JudgeQuestion.class),

                    // 添加其他题型对应的子类...
            }
    )
    Question findQuestionByExamPaperIdAndNumber(@Param("examPaperId") Long examPaperId, @Param("number") int number);

    @Select("SELECT * FROM question " +
            "WHERE exam_paper_id = #{examPaperId} AND question_type = 'SINGLE_CHOICE'")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "content", column = "content"),
            @Result(property = "number", column = "number"),
            @Result(property = "correctAnswer", column = "correct_answer"),
            @Result(property = "optionA", column = "optiona"),
            @Result(property = "optionB", column = "optionb"),
            @Result(property = "optionC", column = "optionc"),
            @Result(property = "optionD", column = "optiond")
    })
    List<SingleChoiceQuestion> findSCByExamPaperId(Long examPaperId);

    /**
     * 根据试卷ID查询所有判断题
     *
     * @param examPaperId 试卷ID
     * @return 判断题列表
     */
    @Select("SELECT * FROM question " +
            "WHERE exam_paper_id = #{examPaperId} AND question_type = 'JUDGE'")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "content", column = "content"),
            @Result(property = "number", column = "number"),
            @Result(property = "correctAnswer", column = "correct_answer")
    })
    List<JudgeQuestion> findJQByExamPaperId(Long examPaperId);


    @Select("""
       SELECT q.id, ep.exam_paper_name,q.content,utrd.correct_answer,utrd.user_answer, q.number
       FROM   question q
        JOIN exam_paper ep ON q.exam_paper_id = ep.id
        JOIN user_test_record utr ON utr.exam_paper_id = ep.id
        JOIN user_test_record_detail utrd ON (
               utrd.test_id = utr.test_id
               AND utrd.question_number = q.number  
           )
           JOIN subjects s ON ep.exam_paper_subject = s.subject_name
       WHERE s.subject_id = #{subjectId} AND utr.user_id = #{userId}
           AND utrd.user_answer != utrd.correct_answer
    """)
    @Results({
            @Result(property = "questionId", column = "id"),
            @Result(property = "examPaperName", column = "exam_paper_name"),
            @Result(property = "content", column = "content"),
            @Result(property = "correctAnswer", column = "correct_answer"),
            @Result(property = "userAnswer", column = "user_answer"),
            @Result(property = "number", column = "number"),
    })
    List<QuestionWrongDTO> findWrongQuestionsBySubject(
            @Param("subjectId") int subjectId,
            @Param("userId") Long userId
    );

    @Select("""
    <script>
    SELECT * FROM question WHERE id IN
    <foreach item="id" collection="ids" open="(" separator="," close=")">
        #{id}
    </foreach>
    ORDER BY number
    </script>
""")
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "questionType", column = "question_type"),
            @Result(property = "type", column = "type"),
            @Result(property = "content", column = "content"),
            @Result(property = "number", column = "number"),
            @Result(property = "examPaperId", column = "exam_paper_id"),
            @Result(property = "correctAnswer", column = "correct_answer"),
            // 公共字段映射

            // 单选题特有字段映射
            @Result(property = "optionA", column = "optiona"),
            @Result(property = "optionB", column = "optionb"),
            @Result(property = "optionC", column = "optionc"),
            @Result(property = "optionD", column = "optiond")
    })
    @TypeDiscriminator(
            column = "question_type",
            javaType = String.class,
            cases = {
                    @Case(value = "SINGLE_CHOICE", type = SingleChoiceQuestion.class),
                    @Case(value = "JUDGE", type = JudgeQuestion.class),
                    // 添加其他题型对应的子类...
            }
    )
    List<Question> findQuestionsByIds(@Param("ids") List<Long> ids);

    @Delete("DELETE FROM examinai.question WHERE exam_paper_id = #{examPaperId}")
    void deleteByExamPaperId(@Param("examPaperId") Long examPaperId);
}