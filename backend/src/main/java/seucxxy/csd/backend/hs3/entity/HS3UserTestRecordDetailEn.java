package seucxxy.csd.backend.hs3.entity;

public class HS3UserTestRecordDetailEn {
    private Long id;
    private String correctAnswer;
    private Integer questionsEnNumber;
    private String segmentId;
    private String questionsType;
    private String userAnswer;
    private Long testEnId;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public Integer getQuestionsEnNumber() {
        return questionsEnNumber;
    }

    public void setQuestionsEnNumber(Integer questionsEnNumber) {
        this.questionsEnNumber = questionsEnNumber;
    }

    public String getSegmentId() {
        return segmentId;
    }

    public void setSegmentId(String segmentId) {
        this.segmentId = segmentId;
    }

    public String getQuestionsType() {
        return questionsType;
    }

    public void setQuestionsType(String questionsType) {
        this.questionsType = questionsType;
    }

    public String getUserAnswer() {
        return userAnswer;
    }

    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }

    public Long getTestEnId() {
        return testEnId;
    }

    public void setTestEnId(Long testEnId) {
        this.testEnId = testEnId;
    }
}
