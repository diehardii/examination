package seucxxy.csd.backend.entity;



import java.util.List;

public class ExamPaper {
    private Long id;
    private String examPaperName;
    private String examPaperContent;
    private String examPaperDifficulty;
    private int examPaperQuestionNumber;
    private String examPaperSubject;
    private List<Question> questions;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExamPaperName() {
        return examPaperName;
    }

    public void setExamPaperName(String examPaperName) {
        this.examPaperName = examPaperName;
    }

    public String getExamPaperContent() {
        return examPaperContent;
    }

    public void setExamPaperContent(String examPaperContent) {
        this.examPaperContent = examPaperContent;
    }

    public String getExamPaperDifficulty() {
        return examPaperDifficulty;
    }

    public void setExamPaperDifficulty(String examPaperDifficulty) {
        this.examPaperDifficulty = examPaperDifficulty;
    }

    public int getExamPaperQuestionNumber() {
        return examPaperQuestionNumber;
    }

    public void setExamPaperQuestionNumber(int examPaperQuestionNumber) {
        this.examPaperQuestionNumber = examPaperQuestionNumber;
    }

    public String getExamPaperSubject() {
        return examPaperSubject;
    }

    public void setExamPaperSubject(String examPaperSubject) {
        this.examPaperSubject = examPaperSubject;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }
}