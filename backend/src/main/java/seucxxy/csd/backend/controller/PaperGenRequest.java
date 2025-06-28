package seucxxy.csd.backend.controller;

// PaperGenRequest.java
import lombok.Data;

@Data
public class PaperGenRequest {
    private String name;
    private String content;
    private String difficulty;
    private String subject;
    private int  singleChoiceNumber;
    private int  judgeNumber;

    // Getters and Setters
}

