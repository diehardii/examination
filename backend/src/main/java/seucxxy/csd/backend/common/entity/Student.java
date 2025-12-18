package seucxxy.csd.backend.common.entity;

import lombok.Data;

/**
 * Student entity mapped to students table.
 */
@Data
public class Student {
    private Integer studentId;
    private String studentNumber;
    private Integer stageId;
    private Integer gradeId;
    private Integer classId;
    
    // Extended fields for query results
    private String studentName;
    private String username;
    private String email;
    private String phone;
    private String stageName;
    private String gradeName;
    private String classCode;
}
