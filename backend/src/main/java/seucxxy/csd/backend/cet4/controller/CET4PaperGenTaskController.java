package seucxxy.csd.backend.cet4.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import seucxxy.csd.backend.cet4.entity.CET4Task;
import seucxxy.csd.backend.cet4.service.CET4PaperGenTaskRunner;
import seucxxy.csd.backend.cet4.service.CET4PaperGenTaskService;
import seucxxy.csd.backend.common.util.UserSessionUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5002", "http://localhost:5003"}, allowCredentials = "true")
public class CET4PaperGenTaskController {

    private static final int MAX_LIMIT = 50;

    private final CET4PaperGenTaskService taskService;
    private final CET4PaperGenTaskRunner taskRunner;
    private final ObjectMapper objectMapper;

    public CET4PaperGenTaskController(CET4PaperGenTaskService taskService,
                                      CET4PaperGenTaskRunner taskRunner,
                                      ObjectMapper objectMapper) {
        this.taskService = taskService;
        this.taskRunner = taskRunner;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/cet4/paper-gen/tasks")
    public ResponseEntity<Map<String, Object>> submitTask(@RequestBody PaperGenTaskRequest request,
                                                          HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        CET4Task task = null;
        Long userId = null;
        Boolean asyncMode = null;
        try {
            userId = UserSessionUtil.getCurrentUserId(session);
            validateRequest(request);
            asyncMode = Boolean.TRUE.equals(request.getAsyncMode());
            String source = (request.getExamPaperEnSource() == null || request.getExamPaperEnSource().isBlank())
                    ? "AIfromreal" : request.getExamPaperEnSource();

                task = taskService.createPaperTask(
                    userId,
                    request.getExamPaperEnName(),
                    request.getExamPaperEnDesc(),
                    request.getSubjectEn(),
                    asyncMode,
                    source
            );

            if (Boolean.TRUE.equals(asyncMode)) {
                taskRunner.executeTaskAsync(task.getId());
                response.put("success", true);
                response.put("taskId", task.getId());
                response.put("async", true);
                response.put("status", task.getStatus());
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            }

            Map<String, Object> result = taskRunner.executeTaskSync(task);
            response.put("success", true);
            response.put("taskId", task.getId());
            response.put("async", false);
            response.put("examPaperEnId", result.get("examPaperEnId"));
            response.put("units", result.get("units"));
            response.put("exam_paper_en_source", result.get("exam_paper_en_source"));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            if ("用户未登录".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } finally {
            if (Boolean.FALSE.equals(asyncMode)
                    && task != null
                    && task.getId() != null
                    && userId != null) {
                taskService.deleteTask(task.getId(), userId);
            }
        }
    }

    @GetMapping("/cet4/paper-gen/tasks")
    public ResponseEntity<List<TaskSummary>> listTasks(@RequestParam(value = "limit", defaultValue = "20") int limit,
                                                       HttpSession session) {
        try {
            Long userId = UserSessionUtil.getCurrentUserId(session);
            int sanitizedLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
            List<CET4Task> tasks = taskService.listRecentTasks(userId, sanitizedLimit, CET4PaperGenTaskService.TYPE_PAPER_GEN);
            List<TaskSummary> summaries = tasks.stream().map(task -> TaskSummary.fromEntity(task, objectMapper)).collect(Collectors.toList());
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/cet4/paper-gen/tasks/{taskId}")
    public ResponseEntity<TaskSummary> getTask(@PathVariable("taskId") Long taskId,
                                               HttpSession session) {
        try {
            Long userId = UserSessionUtil.getCurrentUserId(session);
            CET4Task task = taskService.findByIdAndUser(taskId, userId, CET4PaperGenTaskService.TYPE_PAPER_GEN);
            if (task == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok(TaskSummary.fromEntity(task, objectMapper));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/cet4/paper-gen/tasks/{taskId}/result")
    public ResponseEntity<Map<String, Object>> getTaskResult(@PathVariable("taskId") Long taskId,
                                                             HttpSession session) {
        try {
            Long userId = UserSessionUtil.getCurrentUserId(session);
            CET4Task task = taskService.findByIdAndUser(taskId, userId, CET4PaperGenTaskService.TYPE_PAPER_GEN);
            if (task == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            if (!Objects.equals("SUCCEEDED", task.getStatus())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                        "success", false,
                        "message", "任务尚未完成"
                ));
            }
            Map<String, Object> resultNode = parseResult(task.getResultJson());
            Object units = resultNode.get("units");
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("examPaperEnId", task.getExamPaperEnId());
            result.put("units", units);
            result.put("exam_paper_en_source", task.getExamPaperEnSource());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            if ("用户未登录".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/cet4/paper-gen/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> deleteTask(@PathVariable("taskId") Long taskId,
                                                          HttpSession session) {
        try {
            Long userId = UserSessionUtil.getCurrentUserId(session);
            boolean deleted = taskService.deleteTask(taskId, userId);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "success", false,
                        "message", "任务不存在或已被移除"
                ));
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            if ("用户未登录".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", e.getMessage()
                ));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    private void validateRequest(PaperGenTaskRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        if (request.getSubjectEn() == null || request.getSubjectEn().isBlank()) {
            throw new IllegalArgumentException("科目不能为空");
        }
    }

    // ========== Intensive task endpoints (merged from CET4IntensiveTaskController) ==========

    @PostMapping("/intensive/tasks")
    public ResponseEntity<Map<String, Object>> submitIntensiveTask(@RequestBody IntensiveTaskRequest request,
                                                                   HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        CET4Task task = null;
        Long userId = null;
        Boolean asyncMode = null;
        try {
            userId = UserSessionUtil.getCurrentUserId(session);
            validateIntensiveRequest(request);
            asyncMode = Boolean.TRUE.equals(request.getAsyncMode());
            boolean fromWrongBank = Boolean.TRUE.equals(request.getFromWrongBank());
            String source = resolveSource(fromWrongBank, request.getExamPaperEnSource());

            task = taskService.createIntensiveTask(
                    userId,
                    request.getTypes(),
                    request.getCounts(),
                    fromWrongBank,
                    Boolean.TRUE.equals(asyncMode),
                    source
            );

            if (Boolean.TRUE.equals(asyncMode)) {
                taskRunner.executeTaskAsync(task.getId());
                response.put("success", true);
                response.put("taskId", task.getId());
                response.put("async", true);
                response.put("status", task.getStatus());
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            }

            Map<String, Object> result = taskRunner.executeTaskSync(task);
            response.put("success", true);
            response.put("taskId", task.getId());
            response.put("async", false);
            response.putAll(result);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            if ("用户未登录".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } finally {
            if (Boolean.FALSE.equals(asyncMode)
                    && task != null
                    && task.getId() != null
                    && userId != null) {
                taskService.deleteTask(task.getId(), userId);
            }
        }
    }

    @GetMapping("/intensive/tasks")
    public ResponseEntity<List<IntensiveTaskSummary>> listIntensiveTasks(@RequestParam(value = "limit", defaultValue = "20") int limit,
                                                                         HttpSession session) {
        try {
            Long userId = UserSessionUtil.getCurrentUserId(session);
            int sanitizedLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
            List<CET4Task> tasks = taskService.listRecentTasks(userId, sanitizedLimit, CET4PaperGenTaskService.TYPE_INTENSIVE);
            List<IntensiveTaskSummary> summaries = tasks.stream()
                    .map(task -> IntensiveTaskSummary.fromEntity(task, objectMapper))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/intensive/tasks/{taskId}")
    public ResponseEntity<IntensiveTaskSummary> getIntensiveTask(@PathVariable("taskId") Long taskId,
                                                                 HttpSession session) {
        try {
            Long userId = UserSessionUtil.getCurrentUserId(session);
            CET4Task task = taskService.findByIdAndUser(taskId, userId, CET4PaperGenTaskService.TYPE_INTENSIVE);
            if (task == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok(IntensiveTaskSummary.fromEntity(task, objectMapper));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/intensive/tasks/{taskId}/result")
    public ResponseEntity<Map<String, Object>> getIntensiveResult(@PathVariable("taskId") Long taskId,
                                                                  HttpSession session) {
        try {
            Long userId = UserSessionUtil.getCurrentUserId(session);
            CET4Task task = taskService.findByIdAndUser(taskId, userId, CET4PaperGenTaskService.TYPE_INTENSIVE);
            if (task == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            if (!Objects.equals("SUCCEEDED", task.getStatus())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                        "success", false,
                        "message", "任务尚未完成"
                ));
            }
            Map<String, Object> resultNode = readResult(task.getResultJson());
            List<Map<String, Object>> questions = readQuestionList(resultNode.get("questions"));
            List<String> failedTypes = readStringList(resultNode.get("failedTypes"));
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("questions", questions);
            result.put("examPaperEnSource", task.getExamPaperEnSource());
            result.put("totalCount", task.getGeneratedCount());
            result.put("failedCount", task.getFailedCount());
            result.put("failedTypes", failedTypes);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            if ("用户未登录".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/intensive/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> deleteIntensiveTask(@PathVariable("taskId") Long taskId,
                                                                   HttpSession session) {
        try {
            Long userId = UserSessionUtil.getCurrentUserId(session);
            boolean deleted = taskService.deleteTask(taskId, userId);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "success", false,
                        "message", "任务不存在或已被移除"
                ));
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            if ("用户未登录".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", e.getMessage()
                ));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    private void validateIntensiveRequest(IntensiveTaskRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        List<String> types = request.getTypes();
        List<Integer> counts = request.getCounts();
        if (types == null || counts == null || types.size() != counts.size()) {
            throw new IllegalArgumentException("题型与数量不匹配");
        }
        boolean hasPositive = false;
        for (Integer count : counts) {
            if (count != null && count > 0) {
                hasPositive = true;
                break;
            }
        }
        if (!hasPositive) {
            throw new IllegalArgumentException("请至少为一种题型设置练习数量");
        }
    }

    private String resolveSource(boolean fromWrongBank, String examPaperEnSource) {
        if (fromWrongBank) {
            return "AIfromWrongBank";
        }
        return examPaperEnSource == null || examPaperEnSource.isBlank() ? "AIfromself" : examPaperEnSource;
    }

    private Map<String, Object> readResult(String json) throws Exception {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return objectMapper.readValue(json, new TypeReference<>() {});
    }

    private List<Map<String, Object>> readQuestionList(Object source) throws Exception {
        if (source == null) {
            return Collections.emptyList();
        }
        if (source instanceof List<?> list) {
            return list.stream()
                    .map(item -> objectMapper.convertValue(item, new TypeReference<Map<String, Object>>() {}))
                    .collect(Collectors.toList());
        }
        return objectMapper.convertValue(source, new TypeReference<>() {});
    }

    private List<String> readStringList(Object source) throws Exception {
        if (source == null) {
            return Collections.emptyList();
        }
        if (source instanceof List<?> list) {
            return list.stream().map(item -> item == null ? null : item.toString()).collect(Collectors.toList());
        }
        return objectMapper.convertValue(source, new TypeReference<>() {});
    }

    private static Map<String, Object> readPayload(String json, ObjectMapper mapper) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    public static class PaperGenTaskRequest {
        private String examPaperEnName;
        private String examPaperEnDesc;
        private String subjectEn;
        private String examPaperEnSource;
        private Boolean asyncMode;

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

        public String getSubjectEn() {
            return subjectEn;
        }

        public void setSubjectEn(String subjectEn) {
            this.subjectEn = subjectEn;
        }

        public String getExamPaperEnSource() {
            return examPaperEnSource;
        }

        public void setExamPaperEnSource(String examPaperEnSource) {
            this.examPaperEnSource = examPaperEnSource;
        }

        public Boolean getAsyncMode() {
            return asyncMode;
        }

        public void setAsyncMode(Boolean asyncMode) {
            this.asyncMode = asyncMode;
        }
    }

    public static class TaskSummary {
        private Long id;
        private String examPaperEnName;
        private String subjectEn;
        private String status;
        private Integer progress;
        private String message;
        private Boolean asyncMode;
        private String examPaperEnSource;
        private Long examPaperEnId;
        private String createdAt;
        private String updatedAt;
        private String completedAt;

        public static TaskSummary fromEntity(CET4Task task, ObjectMapper mapper) {
            TaskSummary summary = new TaskSummary();
            summary.setId(task.getId());
            summary.setStatus(task.getStatus());
            summary.setProgress(task.getProgress());
            summary.setMessage(task.getMessage());
            summary.setAsyncMode(task.getAsyncMode());
            summary.setExamPaperEnSource(task.getExamPaperEnSource());
            summary.setExamPaperEnId(task.getExamPaperEnId());
            summary.setCreatedAt(task.getCreatedAt() == null ? null : task.getCreatedAt().toString());
            summary.setUpdatedAt(task.getUpdatedAt() == null ? null : task.getUpdatedAt().toString());
            summary.setCompletedAt(task.getCompletedAt() == null ? null : task.getCompletedAt().toString());

            Map<String, Object> payload = readPayload(task.getPayloadJson(), mapper);
            summary.setExamPaperEnName(payload.getOrDefault("name", "自动生成试卷").toString());
            Object subject = payload.get("subjectEn");
            summary.setSubjectEn(subject == null ? null : subject.toString());
            return summary;
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

        public String getSubjectEn() {
            return subjectEn;
        }

        public void setSubjectEn(String subjectEn) {
            this.subjectEn = subjectEn;
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

        public Boolean getAsyncMode() {
            return asyncMode;
        }

        public void setAsyncMode(Boolean asyncMode) {
            this.asyncMode = asyncMode;
        }

        public String getExamPaperEnSource() {
            return examPaperEnSource;
        }

        public void setExamPaperEnSource(String examPaperEnSource) {
            this.examPaperEnSource = examPaperEnSource;
        }

        public Long getExamPaperEnId() {
            return examPaperEnId;
        }

        public void setExamPaperEnId(Long examPaperEnId) {
            this.examPaperEnId = examPaperEnId;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }

        public String getCompletedAt() {
            return completedAt;
        }

        public void setCompletedAt(String completedAt) {
            this.completedAt = completedAt;
        }

    }

    public static class IntensiveTaskRequest {
        private List<String> types;
        private List<Integer> counts;
        private Boolean fromWrongBank;
        private String examPaperEnSource;
        private Boolean asyncMode;

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

        public String getExamPaperEnSource() {
            return examPaperEnSource;
        }

        @JsonProperty("exam_paper_en_source")
        public void setExamPaperEnSource(String examPaperEnSource) {
            this.examPaperEnSource = examPaperEnSource;
        }

        public Boolean getAsyncMode() {
            return asyncMode;
        }

        public void setAsyncMode(Boolean asyncMode) {
            this.asyncMode = asyncMode;
        }
    }

    public static class IntensiveTaskSummary {
        private Long id;
        private String status;
        private Integer progress;
        private Boolean asyncMode;
        private Boolean fromWrongBank;
        private Integer requestedTotal;
        private Integer generatedCount;
        private Integer failedCount;
        private List<String> types;
        private List<Integer> counts;
        private String examPaperEnSource;
        private String createdAt;
        private String updatedAt;
        private String completedAt;

        public static IntensiveTaskSummary fromEntity(CET4Task task, ObjectMapper objectMapper) {
            IntensiveTaskSummary summary = new IntensiveTaskSummary();
            summary.setId(task.getId());
            summary.setStatus(task.getStatus());
            summary.setProgress(task.getProgress());
            summary.setAsyncMode(task.getAsyncMode());
            summary.setGeneratedCount(task.getGeneratedCount());
            summary.setFailedCount(task.getFailedCount());
            summary.setExamPaperEnSource(task.getExamPaperEnSource());
            summary.setCreatedAt(task.getCreatedAt() == null ? null : task.getCreatedAt().toString());
            summary.setUpdatedAt(task.getUpdatedAt() == null ? null : task.getUpdatedAt().toString());
            summary.setCompletedAt(task.getCompletedAt() == null ? null : task.getCompletedAt().toString());

            Map<String, Object> payload = readPayload(task.getPayloadJson(), objectMapper);
            Boolean fromWrong = payload.containsKey("fromWrongBank")
                    ? objectMapper.convertValue(payload.get("fromWrongBank"), Boolean.class)
                    : Boolean.FALSE;
            summary.setFromWrongBank(Boolean.TRUE.equals(fromWrong));
            Object typesObj = payload.get("types");
            Object countsObj = payload.get("counts");
            summary.setTypes(typesObj == null ? null : objectMapper.convertValue(typesObj, new TypeReference<List<String>>() {}));
            summary.setCounts(countsObj == null ? null : objectMapper.convertValue(countsObj, new TypeReference<List<Integer>>() {}));
            int requestedTotal = 0;
            if (summary.getCounts() != null) {
                for (Integer c : summary.getCounts()) {
                    if (c != null) requestedTotal += c;
                }
            }
            summary.setRequestedTotal(requestedTotal);
            return summary;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
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

        public Boolean getAsyncMode() {
            return asyncMode;
        }

        public void setAsyncMode(Boolean asyncMode) {
            this.asyncMode = asyncMode;
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

        public String getExamPaperEnSource() {
            return examPaperEnSource;
        }

        public void setExamPaperEnSource(String examPaperEnSource) {
            this.examPaperEnSource = examPaperEnSource;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }

        public String getCompletedAt() {
            return completedAt;
        }

        public void setCompletedAt(String completedAt) {
            this.completedAt = completedAt;
        }
    }

    private Map<String, Object> parseResult(String json) throws Exception {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        JsonNode node = objectMapper.readTree(json);
        return objectMapper.convertValue(node, new TypeReference<>() {});
    }
}
