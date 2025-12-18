package seucxxy.csd.backend.common.dto;

import lombok.Data;
import java.util.List;

/**
 * Lightweight teacher summary for listing.
 */
@Data
public class TeacherProfileDto {
    private Long id;
    private String username;
    private String realName;
    private String phone;
    private List<StageInfo> stages; // 教师的所有学段
    
    @Data
    public static class StageInfo {
        private Integer stageId;
        private String stageName;
        private Integer classCount; // 该学段下的班级数量
    }
}
