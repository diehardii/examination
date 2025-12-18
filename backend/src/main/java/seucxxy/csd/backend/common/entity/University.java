package seucxxy.csd.backend.common.entity;



import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class University {
    private Integer universityId;  // 对应auto_increment主键
    private String universityName;

    // 一对多关联院系（可选，根据业务需求）
    private List<Department> departments;
}