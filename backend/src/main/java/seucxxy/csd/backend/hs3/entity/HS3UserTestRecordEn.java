package seucxxy.csd.backend.hs3.entity;

import java.time.LocalDateTime;

public class HS3UserTestRecordEn {
    private Long testEnId;
    private Integer correctNumber;
    private Double testEnScore;
    private LocalDateTime testEnTime;
    private Long examPaperEnId;
    private Long userId;

    private HS3ExamPaperEn examPaperEn;

    // Getters and Setters
    public Long getTestEnId() {
        return testEnId;
    }

    public void setTestEnId(Long testEnId) {
        this.testEnId = testEnId;
    }

    public Integer getCorrectNumber() {
        return correctNumber;
    }

    public void setCorrectNumber(Integer correctNumber) {
        this.correctNumber = correctNumber;
    }

    public Double getTestEnScore() {
        return testEnScore;
    }

    public void setTestEnScore(Double testEnScore) {
        this.testEnScore = testEnScore;
    }

    public LocalDateTime getTestEnTime() {
        return testEnTime;
    }

    public void setTestEnTime(LocalDateTime testEnTime) {
        this.testEnTime = testEnTime;
    }

    public Long getExamPaperEnId() {
        return examPaperEnId;
    }

    public void setExamPaperEnId(Long examPaperEnId) {
        this.examPaperEnId = examPaperEnId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public HS3ExamPaperEn getExamPaperEn() {
        return examPaperEn;
    }

    public void setExamPaperEn(HS3ExamPaperEn examPaperEn) {
        this.examPaperEn = examPaperEn;
    }
}
