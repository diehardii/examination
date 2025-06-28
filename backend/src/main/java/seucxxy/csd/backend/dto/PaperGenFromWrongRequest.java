package seucxxy.csd.backend.dto;

import lombok.Data;

@Data
public class PaperGenFromWrongRequest {
    private Long examPaperId;
    private String name;
    private String content;
    private String difficulty; // 可设置默认值"自定义"
    private String subject;
    private int singleChoiceNumber;
    private int judgeNumber;

    // getters/setters
}