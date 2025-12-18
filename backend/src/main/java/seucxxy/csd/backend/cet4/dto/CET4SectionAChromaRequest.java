package seucxxy.csd.backend.cet4.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CET4SectionAChromaRequest {

    @NotBlank(message = "Part III Section A 文本不能为空")
    private String document;

    @NotBlank(message = "subject 不能为空")
    private String subject;

    @NotBlank(message = "examPaperId 不能为空")
    private String examPaperId;

    @NotBlank(message = "examPaperName 不能为空")
    private String examPaperName;

    @NotEmpty(message = "blankNumbers 不能为空")
    private List<String> blankNumbers;

    /**
     * 题型固定为选词填空，但保留字段便于扩展
     */
    private String questionType = "选词填空";

    /**
     * Part III 默认值
     */
    private Integer partId = 3;

    /**
     * Section A 默认值
     */
    private String sectionId = "A";

    /**
     * 题目来源：real(电子文档提取) 或 AI(大模型生成)
     * 默认值为real，AI生成场景需要显式设置为AI
     */
    private String questionSource = "real";

    // 新增：试卷来源标识，将写入 ChromaDB metadata 中
    // real | AIfromreal | AIfromself
    private String examPaperEnSource;

    // 显式生成getter/setter以兼容未启用lombok处理的场景
    public String getDocument() { return document; }
    public void setDocument(String document) { this.document = document; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getExamPaperId() { return examPaperId; }
    public void setExamPaperId(String examPaperId) { this.examPaperId = examPaperId; }
    public String getExamPaperName() { return examPaperName; }
    public void setExamPaperName(String examPaperName) { this.examPaperName = examPaperName; }
    public List<String> getBlankNumbers() { return blankNumbers; }
    public void setBlankNumbers(List<String> blankNumbers) { this.blankNumbers = blankNumbers; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public Integer getPartId() { return partId; }
    public void setPartId(Integer partId) { this.partId = partId; }
    public String getSectionId() { return sectionId; }
    public void setSectionId(String sectionId) { this.sectionId = sectionId; }
    public String getQuestionSource() { return questionSource; }
    public void setQuestionSource(String questionSource) { this.questionSource = questionSource; }
    public String getExamPaperEnSource() { return examPaperEnSource; }
    public void setExamPaperEnSource(String examPaperEnSource) { this.examPaperEnSource = examPaperEnSource; }
}


