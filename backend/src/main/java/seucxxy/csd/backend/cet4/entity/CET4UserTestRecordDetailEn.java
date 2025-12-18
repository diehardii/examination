package seucxxy.csd.backend.cet4.entity;

public class CET4UserTestRecordDetailEn {
    private Long id;
    private String correctAnswer;
    private int questionsEnNumber;
    private String segmentId;
    private String questionsType;
    private String userAnswer;
    private Long testEnId;

    // 新增字段：AI评分结果
    private Integer aiScore;
    private String aiFeedback;
    private String aiReasoning;

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

    public int getQuestionsEnNumber() {
        return questionsEnNumber;
    }

    public void setQuestionsEnNumber(int questionsEnNumber) {
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

    public Integer getAiScore() {
        return aiScore;
    }

    public void setAiScore(Integer aiScore) {
        this.aiScore = aiScore;
    }

    public String getAiFeedback() {
        return aiFeedback;
    }

    public void setAiFeedback(String aiFeedback) {
        this.aiFeedback = aiFeedback;
    }

    public String getAiReasoning() {
        return aiReasoning;
    }

    public void setAiReasoning(String aiReasoning) {
        this.aiReasoning = aiReasoning;
    }
}
