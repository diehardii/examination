package seucxxy.csd.backend.dto;


import lombok.Data;

@Data
public class QuestionWrongDTO {
    private long  questionId;
    private String examPaperName;
    private String content;
    private String correctAnswer;
    private String userAnswer;
    private int number;
}
