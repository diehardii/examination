package seucxxy.csd.backend.common.entity;

public class SubjectsEn {
    private Long id;
    private String subjectEnName;

    public SubjectsEn() {}

    public SubjectsEn(String subjectEnName) {
        this.subjectEnName = subjectEnName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubjectEnName() {
        return subjectEnName;
    }

    public void setSubjectEnName(String subjectEnName) {
        this.subjectEnName = subjectEnName;
    }
}