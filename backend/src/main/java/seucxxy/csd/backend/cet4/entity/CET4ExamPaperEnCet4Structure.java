package seucxxy.csd.backend.cet4.entity;

public class CET4ExamPaperEnCet4Structure {
    private String partId;
    private String sectionId;
    private String segmentId;
    private Integer numberOfQuestions;
    private Double scorePerQuestion;
    private Double segmentTotalScore;

    public String getPartId() { return partId; }
    public void setPartId(String partId) { this.partId = partId; }

    public String getSectionId() { return sectionId; }
    public void setSectionId(String sectionId) { this.sectionId = sectionId; }

    public String getSegmentId() { return segmentId; }
    public void setSegmentId(String segmentId) { this.segmentId = segmentId; }

    public Integer getNumberOfQuestions() { return numberOfQuestions; }
    public void setNumberOfQuestions(Integer numberOfQuestions) { this.numberOfQuestions = numberOfQuestions; }

    public Double getScorePerQuestion() { return scorePerQuestion; }
    public void setScorePerQuestion(Double scorePerQuestion) { this.scorePerQuestion = scorePerQuestion; }

    public Double getSegmentTotalScore() { return segmentTotalScore; }
    public void setSegmentTotalScore(Double segmentTotalScore) { this.segmentTotalScore = segmentTotalScore; }
}
