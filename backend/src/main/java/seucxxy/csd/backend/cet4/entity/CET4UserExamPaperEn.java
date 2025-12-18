package seucxxy.csd.backend.cet4.entity;

/**
 * 用户与英文试卷的关联实体
 */
public class CET4UserExamPaperEn {
    private Long id;
    private Long examPaperEnId;
    private Long userId;

    public CET4UserExamPaperEn() {
    }

    public CET4UserExamPaperEn(Long examPaperEnId, Long userId) {
        this.examPaperEnId = examPaperEnId;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
