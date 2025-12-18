package seucxxy.csd.backend.common.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;

import seucxxy.csd.backend.common.entity.Subject;



@Mapper
public interface SubjectMapper {
    List<Subject> findAllSubjects();
    Subject findSubjectBySubjectName(String name);
    List<String> findSubjectsByUsername(String username);
    List<String> findSubjectsByStudentId(Long studentId);
    Subject findSubjectById(int id);
    void insertSubject(Subject  subject);
    void updateSubject(@Param("subjectId") int subjectId, @Param("newSubject") Subject newSubject);
    void deleteSubject(int subjectId);
    List<String> findSubjectsByTeacherId(Long teacherId);
}