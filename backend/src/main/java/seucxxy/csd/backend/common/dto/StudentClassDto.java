package seucxxy.csd.backend.common.dto;

import lombok.Data;

/**
 * DTO for student class information.
 */
@Data
public class StudentClassDto {
    private Integer studentId;
    private String studentName;
    private String studentNumber;
    private String username;
    private String email;
    private String phone;
    
    // Class information
    private Integer classId;
    private String classCode;
    private Integer gradeId;
    private String gradeName;
    private Integer stageId;
    private String stageName;
}
