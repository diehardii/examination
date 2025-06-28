package seucxxy.csd.backend.dto;

import lombok.Data;
import seucxxy.csd.backend.entity.Question;
import seucxxy.csd.backend.entity.UserTestRecordDetail;

import java.util.Map;

@Data
public class AnalysisResultDTO {
    private Question question;
    private UserTestRecordDetail detail;
    private String analysisData;
    private Map<String, String> options;
}
