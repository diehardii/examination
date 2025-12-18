package seucxxy.csd.backend.common.dto;

import lombok.Data;
import java.util.List;

/**
 * DTO for class student information.
 */
@Data
public class ClassStudentDto {
    private Integer classId;
    private String classCode;
    private String gradeName;
    private String stageName;
    private Integer studentCount;
    private List<StudentInfo> students;
    
    @Data
    public static class StudentInfo {
        private Long studentId;
        private String studentName;
        private String username;
        private String email;
    }
}
