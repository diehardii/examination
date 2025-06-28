package seucxxy.csd.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import seucxxy.csd.backend.entity.Subject;
import java.util.List;
import org.apache.ibatis.annotations.Param;



@Mapper
public interface SubjectMapper {
    List<Subject> findAllSubjects();
    Subject findSubjectBySubjectName(String name);
    List<String> findSubjectsByUsername(String username);
    Subject findSubjectById(int id);
    void insertSubject(Subject  subject);
    void updateSubject(@Param("subjectId") int subjectId, @Param("newSubject") Subject newSubject);
    void deleteSubject(int subjectId);
}