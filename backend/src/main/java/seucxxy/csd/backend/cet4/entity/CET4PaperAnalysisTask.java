package seucxxy.csd.backend.cet4.entity;

import java.time.LocalDateTime;

/**
 * Async task record for CET4 paper parsing.
 */
public class CET4PaperAnalysisTask {

    private Long id;
    private Long userId;
    private String status;
    private Integer progress;
    private String message;
    private String fileName;
    private String sourceType;
    private String examPaperEnSource;
    private String rawText;
    private String segmentsJson;
    private String structuredJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getExamPaperEnSource() {
        return examPaperEnSource;
    }

    public void setExamPaperEnSource(String examPaperEnSource) {
        this.examPaperEnSource = examPaperEnSource;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getSegmentsJson() {
        return segmentsJson;
    }

    public void setSegmentsJson(String segmentsJson) {
        this.segmentsJson = segmentsJson;
    }

    public String getStructuredJson() {
        return structuredJson;
    }

    public void setStructuredJson(String structuredJson) {
        this.structuredJson = structuredJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
