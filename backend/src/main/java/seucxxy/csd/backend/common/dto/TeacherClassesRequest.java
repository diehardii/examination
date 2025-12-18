package seucxxy.csd.backend.common.dto;

import java.util.List;
import lombok.Data;

@Data
public class TeacherClassesRequest {
    private List<Integer> classIds;
}
