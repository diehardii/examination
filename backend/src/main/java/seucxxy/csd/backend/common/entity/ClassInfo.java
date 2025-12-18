package seucxxy.csd.backend.common.entity;

import lombok.Data;

/**
 * Represents a class (班级) record.
 */
@Data
public class ClassInfo {
    private Integer classId;
    private Integer stageId;
    private Integer gradeId;
    private Integer departmentId;
    private String classCode;

    // Extra display fields from joins
    private String stageName;
    private String gradeName;
    private String departmentName;
}
