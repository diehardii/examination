package seucxxy.csd.backend.cet4.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import seucxxy.csd.backend.cet4.entity.CET4Task;
import seucxxy.csd.backend.cet4.mapper.CET4TaskMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CET4PaperGenTaskService {

    public static final String TYPE_PAPER_GEN = "PAPER_GEN";
    public static final String TYPE_INTENSIVE = "INTENSIVE";

    private final CET4TaskMapper taskMapper;
    private final ObjectMapper objectMapper;

    public CET4PaperGenTaskService(CET4TaskMapper taskMapper, ObjectMapper objectMapper) {
        this.taskMapper = taskMapper;
        this.objectMapper = objectMapper;
    }

    public CET4Task createPaperTask(Long userId,
                                    String name,
                                    String description,
                                    String subjectEn,
                                    boolean asyncMode,
                                    String examPaperEnSource) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", defaultName(name));
        payload.put("desc", defaultDesc(description));
        payload.put("subjectEn", subjectEn);
        payload.put("source", examPaperEnSource);

        CET4Task task = baseTask(userId, TYPE_PAPER_GEN, asyncMode, examPaperEnSource, payload);
        taskMapper.insert(task);
        return task;
    }

    public CET4Task createIntensiveTask(Long userId,
                                        List<String> types,
                                        List<Integer> counts,
                                        boolean fromWrongBank,
                                        boolean asyncMode,
                                        String examPaperEnSource) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("types", types);
        payload.put("counts", counts);
        payload.put("fromWrongBank", fromWrongBank);
        int total = calculateTotal(counts);
        payload.put("requestedTotal", total);

        CET4Task task = baseTask(userId, TYPE_INTENSIVE, asyncMode, examPaperEnSource, payload);
        task.setRequestedTotal(total);
        taskMapper.insert(task);
        return task;
    }

    public void markRunning(Long taskId) {
        taskMapper.markRunning(taskId);
    }

    public void updateProgress(Long taskId, int progress, String message) {
        taskMapper.updateProgress(taskId, progress, message);
    }

    public void markPaperSuccess(Long taskId, Long examPaperEnId, Object units, String examPaperEnSource) {
        Map<String, Object> result = new HashMap<>();
        result.put("examPaperEnId", examPaperEnId);
        result.put("units", units);
        result.put("exam_paper_en_source", examPaperEnSource);
        taskMapper.markPaperSuccess(taskId, examPaperEnId, writeJson(result), "生成完成");
    }

    public void markIntensiveSuccess(Long taskId,
                                     Integer generatedCount,
                                     Integer failedCount,
                                     List<String> failedTypes,
                                     List<?> questions,
                                     String examPaperEnSource) {
        Map<String, Object> result = new HashMap<>();
        result.put("questions", questions);
        result.put("generatedCount", generatedCount);
        result.put("failedCount", failedCount);
        result.put("failedTypes", failedTypes);
        result.put("examPaperEnSource", examPaperEnSource);
        taskMapper.markIntensiveSuccess(
                taskId,
                generatedCount,
                failedCount,
                writeJson(failedTypes),
                writeJson(result),
                "生成完成");
    }

    public void markFailed(Long taskId, String message) {
        taskMapper.markFailed(taskId, message);
    }

    public CET4Task findById(Long taskId) {
        return taskMapper.findById(taskId);
    }

    public CET4Task findByIdAndUser(Long taskId, Long userId, String taskType) {
        CET4Task task = taskMapper.findById(taskId);
        if (task == null || !Objects.equals(task.getUserId(), userId)) {
            return null;
        }
        if (taskType != null && !taskType.equalsIgnoreCase(task.getTaskType())) {
            return null;
        }
        return task;
    }

    public List<CET4Task> listRecentTasks(Long userId, int limit, String taskType) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<CET4Task> tasks = taskMapper.findRecentByUser(userId, taskType, limit);
        return CollectionUtils.isEmpty(tasks) ? Collections.emptyList() : tasks;
    }

    public boolean deleteTask(Long taskId, Long userId) {
        if (taskId == null || userId == null) {
            return false;
        }
        return taskMapper.deleteByIdAndUser(taskId, userId) > 0;
    }

    private CET4Task baseTask(Long userId, String taskType, boolean asyncMode, String examPaperEnSource, Map<String, Object> payload) {
        CET4Task task = new CET4Task();
        task.setUserId(userId);
        task.setTaskType(taskType);
        task.setAsyncMode(asyncMode);
        task.setExamPaperEnSource(examPaperEnSource);
        task.setPayloadJson(writeJson(payload));
        task.setStatus("PENDING");
        task.setProgress(0);
        task.setMessage(null);
        return task;
    }

    private int calculateTotal(List<Integer> counts) {
        if (counts == null) {
            return 0;
        }
        int total = 0;
        for (Integer count : counts) {
            if (count != null && count > 0) {
                total += count;
            }
        }
        return total;
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

    private String defaultName(String name) {
        return (name == null || name.isBlank()) ? "Preview-" + System.currentTimeMillis() : name;
    }

    private String defaultDesc(String desc) {
        return (desc == null || desc.isBlank()) ? "待保存的试卷预览" : desc;
    }
}
