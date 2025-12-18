package seucxxy.csd.backend.cet4.dto;

import java.time.LocalDateTime;

/**
 * 错题记录DTO
 * 用于展示用户的错题列表
 */
public class CET4WrongQuestionRecordDTO {
    private Long testEnId;
    private String examPaperEnName;
    private String segmentId;
    private String questionType;
    private Integer correctAnswersNumber;  // 答对的题目数
    private Integer numberOfQuestions;     // 总题目数
    private Double correctAnswersPercent;  // 正确率
    private LocalDateTime testEnTime;      // 考试时间
    private Double score;                  // 该segment的得分

    public CET4WrongQuestionRecordDTO() {
    }

    public Long getTestEnId() {
        return testEnId;
    }

    public void setTestEnId(Long testEnId) {
        this.testEnId = testEnId;
    }

    public String getExamPaperEnName() {
        return examPaperEnName;
    }

    public void setExamPaperEnName(String examPaperEnName) {
        this.examPaperEnName = examPaperEnName;
    }

    public String getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(String segmentId) {
        this.segmentId = segmentId;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public Integer getCorrectAnswersNumber() {
        return correctAnswersNumber;
    }

    public void setCorrectAnswersNumber(Integer correctAnswersNumber) {
        this.correctAnswersNumber = correctAnswersNumber;
    }

    public Integer getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(Integer numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }

    public Double getCorrectAnswersPercent() {
        return correctAnswersPercent;
    }

    public void setCorrectAnswersPercent(Double correctAnswersPercent) {
        this.correctAnswersPercent = correctAnswersPercent;
    }

    public LocalDateTime getTestEnTime() {
        return testEnTime;
    }

    public void setTestEnTime(LocalDateTime testEnTime) {
        this.testEnTime = testEnTime;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}
