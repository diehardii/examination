package seucxxy.csd.backend.common.entity;


import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Department {
    private Integer departmentId;  // 对应auto_increment主键
    private Integer universityId;  // 外键，关联university表
    private String departmentName;

    // 多对一关联大学（可选，根据业务需求）
    private University university;
}