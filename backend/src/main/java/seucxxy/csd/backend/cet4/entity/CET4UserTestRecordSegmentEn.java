package seucxxy.csd.backend.cet4.entity;

public class CET4UserTestRecordSegmentEn {
    private Long id;
    private String segmentId;
    private String questionType;
    private Double score; // 该单元（segment）下小题总得分
    private Integer correctAnswersNumber; // 该单元（segment）答对的题目数
    private Integer numberOfQuestions; // 该单元（segment）的总题目数
    private Double correctAnswersPercent; // 答对题目数占该题型总题数的百分比
    private Long testEnId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSegmentId() { return segmentId; }
    public void setSegmentId(String segmentId) { this.segmentId = segmentId; }

    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public Integer getCorrectAnswersNumber() { return correctAnswersNumber; }
    public void setCorrectAnswersNumber(Integer correctAnswersNumber) { this.correctAnswersNumber = correctAnswersNumber; }

    public Integer getNumberOfQuestions() { return numberOfQuestions; }
    public void setNumberOfQuestions(Integer numberOfQuestions) { this.numberOfQuestions = numberOfQuestions; }

    public Double getCorrectAnswersPercent() { return correctAnswersPercent; }
    public void setCorrectAnswersPercent(Double correctAnswersPercent) { this.correctAnswersPercent = correctAnswersPercent; }

    public Long getTestEnId() { return testEnId; }
    public void setTestEnId(Long testEnId) { this.testEnId = testEnId; }
}
