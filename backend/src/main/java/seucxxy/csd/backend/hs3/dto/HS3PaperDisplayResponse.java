package seucxxy.csd.backend.hs3.dto;

import java.util.List;
import java.util.Map;

/**
 * HS3 试卷展示响应DTO
 * 
 * 用于前端展示试卷结构，包含层级信息
 */
public class HS3PaperDisplayResponse {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 试卷ID
     */
    private String examPaperId;
    
    /**
     * 试卷名称
     */
    private String examPaperName;
    
    /**
     * 试卷描述
     */
    private String examPaperDesc;
    
    /**
     * 总分
     */
    private int totalScore;
    
    /**
     * 带层级信息的Segment列表
     * 每个Segment包含：
     * - isFirstInPart: 是否是该Part的第一个segment
     * - partName/partDescription: Part的信息（仅当isFirstInPart=true时有值）
     * - isFirstInSection: 是否是该Section的第一个segment
     * - sectionName/sectionDescription: Section的信息（仅当isFirstInSection=true时有值）
     * - segmentNumber/segmentName/topic/questionData: Segment的信息
     */
    private List<Map<String, Object>> segments;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getExamPaperId() {
        return examPaperId;
    }

    public void setExamPaperId(String examPaperId) {
        this.examPaperId = examPaperId;
    }

    public String getExamPaperName() {
        return examPaperName;
    }

    public void setExamPaperName(String examPaperName) {
        this.examPaperName = examPaperName;
    }

    public String getExamPaperDesc() {
        return examPaperDesc;
    }

    public void setExamPaperDesc(String examPaperDesc) {
        this.examPaperDesc = examPaperDesc;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public List<Map<String, Object>> getSegments() {
        return segments;
    }

    public void setSegments(List<Map<String, Object>> segments) {
        this.segments = segments;
    }

    // 静态工厂方法
    public static HS3PaperDisplayResponse success(String examPaperId, String examPaperName, 
            String examPaperDesc, int totalScore, List<Map<String, Object>> segments) {
        HS3PaperDisplayResponse response = new HS3PaperDisplayResponse();
        response.setSuccess(true);
        response.setMessage("获取成功");
        response.setExamPaperId(examPaperId);
        response.setExamPaperName(examPaperName);
        response.setExamPaperDesc(examPaperDesc);
        response.setTotalScore(totalScore);
        response.setSegments(segments);
        return response;
    }

    public static HS3PaperDisplayResponse error(String message) {
        HS3PaperDisplayResponse response = new HS3PaperDisplayResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
