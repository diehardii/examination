package seucxxy.csd.backend.common.entity;


import lombok.Data;

@Data
public class UserTeacher {
    

    private Long studentId;

    private Long teacherId;
    
    // Constructors
    public UserTeacher() {}
    
    public UserTeacher(Long studentId, Long teacherId) {
        this.studentId = studentId;
        this.teacherId = teacherId;
    }
} 