package seucxxy.csd.backend.cet4.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import seucxxy.csd.backend.cet4.entity.CET4PaperAnalysisTask;
import seucxxy.csd.backend.cet4.mapper.CET4PaperAnalysisTaskMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CET4PaperAnalysisTaskService {

    private final CET4PaperAnalysisTaskMapper taskMapper;
    private final ObjectMapper objectMapper;

    public CET4PaperAnalysisTaskService(CET4PaperAnalysisTaskMapper taskMapper, ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.objectMapper = objectMapper;
    }

    public CET4PaperAnalysisTask createTask(Long userId, String fileName, String sourceType, String examPaperEnSource) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("fileName", fileName);
        payload.put("sourceType", sourceType);
        CET4PaperAnalysisTask task = baseTask(userId, fileName, sourceType, examPaperEnSource, payload);
        taskMapper.insert(task);
        return task;
    }

    public void markRunning(Long taskId) {
        taskMapper.markRunning(taskId);
    }

    public void updateProgress(Long taskId, int progress, String message) {
        taskMapper.updateProgress(taskId, progress, message);
    }

    public void markSuccess(Long taskId, Map<String, Object> result) {
        String structuredJson = writeJson(result);
        taskMapper.markSuccess(taskId, structuredJson, "解析完成");
    }

    public void markFailed(Long taskId, String message) {
        taskMapper.markFailed(taskId, message);
    }

    public CET4PaperAnalysisTask findById(Long taskId) {
        return taskMapper.findById(taskId);
    }

    public CET4PaperAnalysisTask findByIdAndUser(Long taskId, Long userId) {
        CET4PaperAnalysisTask task = taskMapper.findById(taskId);
        if (task == null || task.getUserId() == null || !task.getUserId().equals(userId)) {
            return null;
        }
        return task;
    }

    public List<CET4PaperAnalysisTask> listRecentTasks(Long userId, int limit) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<CET4PaperAnalysisTask> tasks = taskMapper.findRecentByUser(userId, limit);
        return CollectionUtils.isEmpty(tasks) ? Collections.emptyList() : tasks;
    }

    public boolean deleteTask(Long taskId, Long userId) {
        if (taskId == null || userId == null) {
            return false;
        }
        return taskMapper.deleteByIdAndUser(taskId, userId) > 0;
    }

    private CET4PaperAnalysisTask baseTask(Long userId, String fileName, String sourceType, String examPaperEnSource, Map<String, Object> payload) {
        CET4PaperAnalysisTask task = new CET4PaperAnalysisTask();
        task.setUserId(userId);
        task.setStatus("PENDING");
        task.setProgress(0);
        task.setMessage(null);
        task.setFileName(fileName);
        task.setSourceType(sourceType);
        task.setExamPaperEnSource(examPaperEnSource);
        task.setRawText(null);
        task.setSegmentsJson(null);
        task.setStructuredJson(writeJson(payload));
        return task;
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化任务参数失败", e);
        }
    }
}
