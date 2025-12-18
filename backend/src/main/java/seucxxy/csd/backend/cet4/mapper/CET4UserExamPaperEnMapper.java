package seucxxy.csd.backend.cet4.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import seucxxy.csd.backend.cet4.entity.CET4ExamPaperEn;
import seucxxy.csd.backend.cet4.entity.CET4UserExamPaperEn;

import java.util.List;

@Mapper
public interface CET4UserExamPaperEnMapper {

    @Insert("INSERT INTO user_exam_paper_en (exam_paper_en_id, user_id) VALUES (#{examPaperEnId}, #{userId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(CET4UserExamPaperEn relation);

    @Select("SELECT COUNT(1) FROM user_exam_paper_en WHERE exam_paper_en_id = #{examPaperEnId} AND user_id = #{userId}")
    int countByUserAndPaper(@Param("examPaperEnId") Long examPaperEnId, @Param("userId") Long userId);

    @Select("SELECT e.id, e.exam_paper_en_name AS examPaperEnName, e.exam_paper_en_desc AS examPaperEnDesc, " +
            "e.exam_paper_en_subject AS examPaperEnSubject, e.exam_paper_en_source AS examPaperEnSource " +
            "FROM exam_paper_en e INNER JOIN user_exam_paper_en ue ON e.id = ue.exam_paper_en_id " +
            "WHERE ue.user_id = #{userId} ORDER BY ue.id DESC")
    List<CET4ExamPaperEn> findExamPapersByUserId(@Param("userId") Long userId);
}
