package seucxxy.csd.backend.common.service;

import java.util.List;
import seucxxy.csd.backend.common.dto.ClassStudentDto;
import seucxxy.csd.backend.common.dto.StudentClassDto;

public interface StudentClassService {
    /**
     * 获取所有学生及其班级信息
     */
    List<StudentClassDto> listAllStudents();
    
    /**
     * 获取未分配班级的学生列表
     */
    List<StudentClassDto> listUnassignedStudents();
    
    /**
     * 获取学生的班级信息
     */
    StudentClassDto getStudentClassInfo(Integer studentId);
    
    /**
     * 为学生分配班级
     */
    void assignClassToStudent(Integer studentId, Integer classId);
    
    /**
     * 获取班级的学生列表
     */
    ClassStudentDto getClassStudents(Integer classId);
    
    /**
     * 获取所有班级及其学生统计
     */
    List<ClassStudentDto> listClassesWithStudentCount();
    
    /**
     * 批量为学生分配班级
     */
    void batchAssignStudentsToClass(Integer classId, List<Integer> studentIds);
    
    /**
     * 移除学生的班级分配
     */
    void removeStudentFromClass(Integer studentId);
}
