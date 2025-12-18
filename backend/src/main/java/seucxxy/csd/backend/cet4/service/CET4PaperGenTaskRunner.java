package seucxxy.csd.backend.cet4.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import seucxxy.csd.backend.cet4.entity.CET4Task;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class CET4PaperGenTaskRunner {

    private static final Logger log = LoggerFactory.getLogger(CET4PaperGenTaskRunner.class);

    private final CET4PaperGenTaskService taskService;
    private final CET4PaperGenService paperGenService;
    private final CET4IntensiveTrainService intensiveTrainService;
    private final ObjectMapper objectMapper;

    public CET4PaperGenTaskRunner(CET4PaperGenTaskService taskService,
                                  CET4PaperGenService paperGenService,
                                  CET4IntensiveTrainService intensiveTrainService,
                                  ObjectMapper objectMapper) {
        this.taskService = taskService;
        this.paperGenService = paperGenService;
        this.intensiveTrainService = intensiveTrainService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> executeTaskSync(CET4Task task) throws Exception {
        return executeTask(task);
    }

    @Async("paperGenTaskExecutor")
    public void executeTaskAsync(Long taskId) {
        CET4Task task = taskService.findById(taskId);
        if (task == null) {
            return;
        }
        try {
            executeTask(task);
        } catch (Exception ex) {
            log.error("异步任务执行失败, taskId={}", taskId, ex);
        }
    }

    private Map<String, Object> executeTask(CET4Task task) throws Exception {
        taskService.markRunning(task.getId());
        CET4PaperGenerationProgressListener listener = (completed, total) -> {
            if (total <= 0) {
                return;
            }
            int calculated = (int) Math.round((completed * 100.0) / total);
            int percent = Math.min(99, Math.max(5, calculated));
            taskService.updateProgress(task.getId(), percent, null);
        };
        try {
            if (CET4PaperGenTaskService.TYPE_PAPER_GEN.equalsIgnoreCase(task.getTaskType())) {
                return handlePaperGen(task, listener);
            }
            if (CET4PaperGenTaskService.TYPE_INTENSIVE.equalsIgnoreCase(task.getTaskType())) {
                return handleIntensive(task, listener);
            }
            throw new IllegalArgumentException("未知的任务类型: " + task.getTaskType());
        } catch (Exception ex) {
            taskService.markFailed(task.getId(), ex.getMessage());
            throw ex;
        }
    }

    private Map<String, Object> handlePaperGen(CET4Task task, CET4PaperGenerationProgressListener listener) throws Exception {
        PaperPayload payload = objectMapper.readValue(task.getPayloadJson(), PaperPayload.class);
        Map<String, Object> result = paperGenService.generatePaperConcurrent(
                payload.getName(),
                payload.getDesc(),
                payload.getSubjectEn(),
                task.getExamPaperEnSource(),
                task.getUserId(),
                listener
        );
        Object units = result.get("units");
        Long examPaperEnId = extractLong(result.get("examPaperEnId"));
        taskService.markPaperSuccess(task.getId(), examPaperEnId, units, task.getExamPaperEnSource());
        return result;
    }

    private Map<String, Object> handleIntensive(CET4Task task, CET4PaperGenerationProgressListener listener) throws Exception {
        IntensivePayload payload = objectMapper.readValue(task.getPayloadJson(), IntensivePayload.class);
        List<String> types = payload.getTypes() == null ? Collections.emptyList() : payload.getTypes();
        List<Integer> counts = payload.getCounts() == null ? Collections.emptyList() : payload.getCounts();
        boolean fromWrongBank = Boolean.TRUE.equals(payload.getFromWrongBank());

        Map<String, Object> result = intensiveTrainService.generateIntensivePaperContentConcurrent(
            task.getUserId(),
            fromWrongBank,
            types,
            counts,
            task.getExamPaperEnSource(),
            listener
        );

        List<?> questions = extractList(result.get("questions"));
        Integer generatedCount = extractInteger(result.get("totalCount"));
        Integer failedCount = extractInteger(result.get("failedCount"));
        List<String> failedTypes = extractStringList(result.get("failedTypes"));

        taskService.markIntensiveSuccess(task.getId(), generatedCount, failedCount, failedTypes, questions, task.getExamPaperEnSource());
        return result;
    }

    private List<?> extractList(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        if (value == null) {
            return Collections.emptyList();
        }
        return objectMapper.convertValue(value, List.class);
    }

    private Integer extractInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private List<String> extractStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(item -> item == null ? null : item.toString()).collect(java.util.stream.Collectors.toList());
        }
        if (value == null) {
            return Collections.emptyList();
        }
        return objectMapper.convertValue(value, new TypeReference<List<String>>() {});
    }

    private Long extractLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 试卷生成任务参数
     */
    public static class PaperPayload {
        private String name;
        private String desc;
        private String subjectEn;
        private String source;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public String getSubjectEn() {
            return subjectEn;
        }

        public void setSubjectEn(String subjectEn) {
            this.subjectEn = subjectEn;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }

    /**
     * 强化训练任务参数
     */
    public static class IntensivePayload {
        private List<String> types;
        private List<Integer> counts;
        private Boolean fromWrongBank;
        private Integer requestedTotal;

        public List<String> getTypes() {
            return types;
        }

        public void setTypes(List<String> types) {
            this.types = types;
        }

        public List<Integer> getCounts() {
            return counts;
        }

        public void setCounts(List<Integer> counts) {
            this.counts = counts;
        }

        public Boolean getFromWrongBank() {
            return fromWrongBank;
        }

        public void setFromWrongBank(Boolean fromWrongBank) {
            this.fromWrongBank = fromWrongBank;
        }

        public Integer getRequestedTotal() {
            return requestedTotal;
        }

        public void setRequestedTotal(Integer requestedTotal) {
            this.requestedTotal = requestedTotal;
        }
    }
}
