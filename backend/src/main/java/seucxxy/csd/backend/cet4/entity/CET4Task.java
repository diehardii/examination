package seucxxy.csd.backend.cet4.entity;

import java.time.LocalDateTime;

/**
 * Unified CET4 async task (paper generation or intensive training).
 */
public class CET4Task {

    private Long id;
    private Long userId;
    private String taskType; // PAPER_GEN or INTENSIVE
    private Boolean asyncMode;
    private String status;
    private Integer progress;
    private String message;
    private String examPaperEnSource;
    private String payloadJson;
    private String resultJson;
    private Long examPaperEnId;
    private Integer requestedTotal;
    private Integer generatedCount;
    private Integer failedCount;
    private String failedTypesJson;
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

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public Boolean getAsyncMode() {
        return asyncMode;
    }

    public void setAsyncMode(Boolean asyncMode) {
        this.asyncMode = asyncMode;
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

    public String getExamPaperEnSource() {
        return examPaperEnSource;
    }

    public void setExamPaperEnSource(String examPaperEnSource) {
        this.examPaperEnSource = examPaperEnSource;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public String getResultJson() {
        return resultJson;
    }

    public void setResultJson(String resultJson) {
        this.resultJson = resultJson;
    }

    public Long getExamPaperEnId() {
        return examPaperEnId;
    }

    public void setExamPaperEnId(Long examPaperEnId) {
        this.examPaperEnId = examPaperEnId;
    }

    public Integer getRequestedTotal() {
        return requestedTotal;
    }

    public void setRequestedTotal(Integer requestedTotal) {
        this.requestedTotal = requestedTotal;
    }

    public Integer getGeneratedCount() {
        return generatedCount;
    }

    public void setGeneratedCount(Integer generatedCount) {
        this.generatedCount = generatedCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public String getFailedTypesJson() {
        return failedTypesJson;
    }

    public void setFailedTypesJson(String failedTypesJson) {
        this.failedTypesJson = failedTypesJson;
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
