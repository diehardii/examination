package seucxxy.csd.backend.cet4.entity;

import java.time.LocalDateTime;

public class CET4UserTestRecordEn {
    private Long testEnId;
    private int correctNumber;
    private Double testEnScore;
    private LocalDateTime testEnTime;
    private Long examPaperEnId;
    private Long userId;

    private CET4ExamPaperEn examPaperEn;

    // Getters and Setters
    public Long getTestEnId() {
        return testEnId;
    }

    public void setTestEnId(Long testEnId) {
        this.testEnId = testEnId;
    }

    public int getCorrectNumber() {
        return correctNumber;
    }

    public void setCorrectNumber(int correctNumber) {
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

    public CET4ExamPaperEn getExamPaperEn() {
        return examPaperEn;
    }

    public void setExamPaperEn(CET4ExamPaperEn examPaperEn) {
        this.examPaperEn = examPaperEn;
    }
}
