package seucxxy.csd.backend.common.service;

import seucxxy.csd.backend.common.entity.Subject;
import seucxxy.csd.backend.common.mapper.SubjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubjectService {
    private final SubjectMapper subjectMapper;

    @Autowired
    public SubjectService(SubjectMapper subjectMapper) {
        this.subjectMapper = subjectMapper;
    }

    public List<Subject> getAllSubjects() {
        return subjectMapper.findAllSubjects();
    }

    public List<String> getSubjectsForUser(String username) {
        return subjectMapper.findSubjectsByUsername(username);
    }

    public List<String> getSubjectsForStudent(Long studentId) {
        return subjectMapper.findSubjectsByStudentId(studentId);
    }

    public Subject getSubjectBySubjectName(String subjectName) {
        return subjectMapper.findSubjectBySubjectName(subjectName);
    }

    public void addSubject(Subject  subject) {
        subjectMapper.insertSubject(subject);
    }

    public void updateSubject(int subjectId, Subject newSubject) {
        subjectMapper.updateSubject(subjectId, newSubject);
    }

    public void deleteSubject(int subjectId) {
        subjectMapper.deleteSubject(subjectId);
    }

    public List<String> getSubjectsForTeacher(Long teacherId) {
        return subjectMapper.findSubjectsByTeacherId(teacherId);
    }
}