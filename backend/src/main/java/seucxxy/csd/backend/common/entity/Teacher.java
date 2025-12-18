package seucxxy.csd.backend.common.entity;

import lombok.Data;

/**
 * Teaching assignment record linking teacher, stage, and class.
 */
@Data
public class Teacher {
    private Integer assignmentId;
    private Long teacherId;
    private Integer stageId;
    private Integer classId;
    private String stageName;
    private String classCode;
}
