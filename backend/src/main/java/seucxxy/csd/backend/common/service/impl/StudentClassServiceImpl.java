package seucxxy.csd.backend.common.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seucxxy.csd.backend.common.dto.ClassStudentDto;
import seucxxy.csd.backend.common.dto.StudentClassDto;
import seucxxy.csd.backend.common.entity.Student;
import seucxxy.csd.backend.common.mapper.ClassMapper;
import seucxxy.csd.backend.common.mapper.StudentMapper;
import seucxxy.csd.backend.common.service.StudentClassService;

@Service
@Transactional
public class StudentClassServiceImpl implements StudentClassService {

    private final StudentMapper studentMapper;
    private final ClassMapper classMapper;

    public StudentClassServiceImpl(StudentMapper studentMapper, ClassMapper classMapper) {
        this.studentMapper = studentMapper;
        this.classMapper = classMapper;
    }

    @Override
    public List<StudentClassDto> listAllStudents() {
        List<Student> students = studentMapper.findAllStudents();
        return students.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    public List<StudentClassDto> listUnassignedStudents() {
        List<Student> students = studentMapper.findUnassignedStudents();
        return students.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    public StudentClassDto getStudentClassInfo(Integer studentId) {
        Student student = studentMapper.findById(studentId);
        if (student == null) {
            throw new IllegalArgumentException("Student not found: " + studentId);
        }
        return convertToDto(student);
    }

    @Override
    public void assignClassToStudent(Integer studentId, Integer classId) {
        // 验证班级存在
        var clazz = classMapper.findById(classId);
        if (clazz == null) {
            throw new IllegalArgumentException("Class not found: " + classId);
        }

        // 检查学生记录是否存在
        if (studentMapper.existsById(studentId)) {
            // 更新现有记录
            Student student = new Student();
            student.setStudentId(studentId);
            student.setStageId(clazz.getStageId());
            student.setGradeId(clazz.getGradeId());
            student.setClassId(classId);
            studentMapper.updateClassInfo(student);
        } else {
            // 创建新记录
            Student student = new Student();
            student.setStudentId(studentId);
            student.setStudentNumber("STU" + studentId); // 生成学号
            student.setStageId(clazz.getStageId());
            student.setGradeId(clazz.getGradeId());
            student.setClassId(classId);
            studentMapper.insert(student);
        }
    }

    @Override
    public ClassStudentDto getClassStudents(Integer classId) {
        var clazz = classMapper.findById(classId);
        if (clazz == null) {
            throw new IllegalArgumentException("Class not found: " + classId);
        }

        ClassStudentDto dto = new ClassStudentDto();
        dto.setClassId(classId);
        dto.setClassCode(clazz.getClassCode());
        dto.setGradeName(clazz.getGradeName());
        dto.setStageName(clazz.getStageName());

        List<Student> students = studentMapper.findByClassId(classId);
        dto.setStudentCount(students.size());

        List<ClassStudentDto.StudentInfo> studentInfos = students.stream().map(s -> {
            ClassStudentDto.StudentInfo info = new ClassStudentDto.StudentInfo();
            info.setStudentId(Long.valueOf(s.getStudentId()));
            info.setStudentName(s.getStudentName());
            info.setUsername(s.getUsername());
            info.setEmail(s.getEmail());
            return info;
        }).collect(Collectors.toList());

        dto.setStudents(studentInfos);
        return dto;
    }

    @Override
    public List<ClassStudentDto> listClassesWithStudentCount() {
        List<seucxxy.csd.backend.common.entity.ClassInfo> classes = classMapper.findAll();
        
        return classes.stream().map(clazz -> {
            ClassStudentDto dto = new ClassStudentDto();
            dto.setClassId(clazz.getClassId());
            dto.setClassCode(clazz.getClassCode());
            dto.setGradeName(clazz.getGradeName());
            dto.setStageName(clazz.getStageName());
            dto.setStudentCount(studentMapper.countByClassId(clazz.getClassId()));
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public void batchAssignStudentsToClass(Integer classId, List<Integer> studentIds) {
        var clazz = classMapper.findById(classId);
        if (clazz == null) {
            throw new IllegalArgumentException("Class not found: " + classId);
        }

        List<Student> studentsToUpdate = new ArrayList<>();
        for (Integer studentId : studentIds) {
            Student student = new Student();
            student.setStudentId(studentId);
            student.setStageId(clazz.getStageId());
            student.setGradeId(clazz.getGradeId());
            student.setClassId(classId);
            studentsToUpdate.add(student);
        }

        if (!studentsToUpdate.isEmpty()) {
            studentMapper.batchUpdateClassInfo(studentsToUpdate);
        }
    }

    @Override
    public void removeStudentFromClass(Integer studentId) {
        studentMapper.deleteById(studentId);
    }

    private StudentClassDto convertToDto(Student student) {
        StudentClassDto dto = new StudentClassDto();
        dto.setStudentId(student.getStudentId());
        dto.setStudentName(student.getStudentName());
        dto.setStudentNumber(student.getStudentNumber());
        dto.setUsername(student.getUsername());
        dto.setEmail(student.getEmail());
        dto.setPhone(student.getPhone());
        dto.setClassId(student.getClassId());
        dto.setClassCode(student.getClassCode());
        dto.setGradeId(student.getGradeId());
        dto.setGradeName(student.getGradeName());
        dto.setStageId(student.getStageId());
        dto.setStageName(student.getStageName());
        return dto;
    }
}
