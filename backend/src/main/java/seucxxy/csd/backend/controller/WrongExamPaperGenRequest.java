package seucxxy.csd.backend.controller;



import lombok.Data;

import java.util.List;


@Data
public class WrongExamPaperGenRequest {
    private String paperName;
    private int subjectId;
    private Long userId;
    private List<Long> questionIds;
    // getters and setters
}
