package seucxxy.csd.backend.hs3.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import seucxxy.csd.backend.hs3.entity.HS3ExamPaperEn;

import java.util.List;

@Mapper
public interface HS3ExamPaperEnMapper {
    @Insert("INSERT INTO exam_paper_en(exam_paper_en_name, exam_paper_en_desc, exam_paper_en_subject, exam_paper_en_source) " +
            "VALUES(#{examPaperEnName}, #{examPaperEnDesc}, #{examPaperEnSubject}, #{examPaperEnSource})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(HS3ExamPaperEn examPaperEn);

    @Select("SELECT id, exam_paper_en_name as examPaperEnName, exam_paper_en_desc as examPaperEnDesc, " +
            "exam_paper_en_subject as examPaperEnSubject, exam_paper_en_source as examPaperEnSource FROM exam_paper_en ORDER BY id DESC")
    List<HS3ExamPaperEn> selectAll();
    
    @Select("SELECT id, exam_paper_en_name as examPaperEnName, exam_paper_en_desc as examPaperEnDesc, " +
            "exam_paper_en_subject as examPaperEnSubject, exam_paper_en_source as examPaperEnSource FROM exam_paper_en WHERE id = #{id}")
    HS3ExamPaperEn getExamPaperEnById(@Param("id") Long id);

    @Select("SELECT id FROM exam_paper_en WHERE exam_paper_en_subject = #{subject} AND exam_paper_en_source = #{source}")
    List<Long> findIdsBySubjectAndSource(@Param("subject") String subject, @Param("source") String source);
}
