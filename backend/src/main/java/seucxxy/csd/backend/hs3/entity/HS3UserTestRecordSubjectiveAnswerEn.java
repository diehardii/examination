package seucxxy.csd.backend.hs3.entity;

public class HS3UserTestRecordSubjectiveAnswerEn {
    private Long id;
    private Long testEnId;
    private String segmentName;
    private String userAnswer;
    private String userAnswerGrade; // 评价 + 解析两部分内容

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTestEnId() {
        return testEnId;
    }

    public void setTestEnId(Long testEnId) {
        this.testEnId = testEnId;
    }

    public String getSegmentName() {
        return segmentName;
    }

    public void setSegmentName(String segmentName) {
        this.segmentName = segmentName;
    }

    public String getUserAnswer() {
        return userAnswer;
    }

    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }

    public String getUserAnswerGrade() {
        return userAnswerGrade;
    }

    public void setUserAnswerGrade(String userAnswerGrade) {
        this.userAnswerGrade = userAnswerGrade;
    }
}
