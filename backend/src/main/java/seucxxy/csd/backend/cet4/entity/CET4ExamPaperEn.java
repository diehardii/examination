package seucxxy.csd.backend.cet4.entity;

public class CET4ExamPaperEn {
    private Long id;
    private String examPaperEnName;
    private String examPaperEnDesc;
    private String examPaperEnSubject;
    private String examPaperEnSource; // real, AIfromreal, AIfromwrong

    public CET4ExamPaperEn() {
    }

    public CET4ExamPaperEn(String examPaperEnName, String examPaperEnDesc, String examPaperEnSubject) {
        this.examPaperEnName = examPaperEnName;
        this.examPaperEnDesc = examPaperEnDesc;
        this.examPaperEnSubject = examPaperEnSubject;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExamPaperEnName() {
        return examPaperEnName;
    }

    public void setExamPaperEnName(String examPaperEnName) {
        this.examPaperEnName = examPaperEnName;
    }

    public String getExamPaperEnDesc() {
        return examPaperEnDesc;
    }

    public void setExamPaperEnDesc(String examPaperEnDesc) {
        this.examPaperEnDesc = examPaperEnDesc;
    }

    public String getExamPaperEnSubject() {
        return examPaperEnSubject;
    }

    public void setExamPaperEnSubject(String examPaperEnSubject) {
        this.examPaperEnSubject = examPaperEnSubject;
    }

    public String getExamPaperEnSource() {
        return examPaperEnSource;
    }

    public void setExamPaperEnSource(String examPaperEnSource) {
        this.examPaperEnSource = examPaperEnSource;
    }
}

