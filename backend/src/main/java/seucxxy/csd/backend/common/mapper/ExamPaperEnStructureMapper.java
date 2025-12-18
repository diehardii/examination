package seucxxy.csd.backend.common.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import seucxxy.csd.backend.common.dto.QuestionTypeScoreConf;

import java.util.List;

@Mapper
public interface ExamPaperEnStructureMapper {
    /**
     * 获取所有题型（去重）
     */
    @Select("SELECT DISTINCT question_type FROM exam_paper_en_structure")
    List<String> listQuestionTypes();

    /**
     * 兼容旧接口：仅按题型查询（不区分学科）。
     */
    @Select("SELECT count_of_question_type AS countOfQuestionType, " +
            "number_of_question_type_questions AS numberOfQuestionTypeQuestions, " +
            "score_of_question_type AS scoreOfQuestionType " +
            "FROM exam_paper_en_structure WHERE question_type = #{questionType} LIMIT 1")
    QuestionTypeScoreConf getConfByQuestionType(String questionType);

    /**
     * 新接口：按 (subject_en_id, question_type) 查询，避免跨学科串用配置。
     */
    @Select("SELECT count_of_question_type AS countOfQuestionType, " +
            "number_of_question_type_questions AS numberOfQuestionTypeQuestions, " +
            "score_of_question_type AS scoreOfQuestionType " +
            "FROM exam_paper_en_structure WHERE subject_en_id = #{subjectEnId} AND question_type = #{questionType} LIMIT 1")
    QuestionTypeScoreConf getConfBySubjectAndQuestionType(@Param("subjectEnId") Integer subjectEnId,
                                                          @Param("questionType") String questionType);

    /**
     * 根据题型反查一个可用的 subject_en_id（用于无法从上下文直接获得学科ID时的兜底解析）。
     */
    @Select("SELECT subject_en_id FROM exam_paper_en_structure WHERE question_type = #{questionType} LIMIT 1")
    Integer findSubjectEnIdByQuestionType(@Param("questionType") String questionType);
}