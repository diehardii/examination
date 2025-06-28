package seucxxy.csd.backend.dto;


import lombok.Data;

@Data
public class AnalyzeRequest {
    private Long userTestRecordId;
    private int questionNumber;
    private Long userId;
}