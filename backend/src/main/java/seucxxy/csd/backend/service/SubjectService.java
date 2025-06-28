package seucxxy.csd.backend.service;

import seucxxy.csd.backend.entity.Subject;
import seucxxy.csd.backend.mapper.SubjectMapper;
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
}