package seucxxy.csd.backend.dto;

import lombok.Data;

import java.util.Map;

// DTO类
@Data
public class QuestionWithAnswerDTO {
    private int number;
    private String content;
    private String userAnswer;
    private String correctAnswer;
    private String type;
    private String questionType;
    private Map<String, String> options;
}