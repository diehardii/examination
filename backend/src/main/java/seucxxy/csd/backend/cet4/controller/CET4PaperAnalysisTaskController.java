package seucxxy.csd.backend.cet4.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import seucxxy.csd.backend.cet4.entity.CET4PaperAnalysisTask;
import seucxxy.csd.backend.cet4.service.CET4PaperAnalysisTaskRunner;
import seucxxy.csd.backend.cet4.service.CET4PaperAnalysisTaskService;
import seucxxy.csd.backend.common.util.UserSessionUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cet4/paper-analysis/tasks")
@CrossOrigin(origins = {"http://localhost:5002", "http://localhost:5003"}, allowCredentials = "true")
public class CET4PaperAnalysisTaskController {

    private static final int MAX_LIMIT = 50;

    private final CET4PaperAnalysisTaskService taskService;
    private final CET4PaperAnalysisTaskRunner taskRunner;
    private final ObjectMapper objectMapper;

    public CET4PaperAnalysisTaskController(CET4PaperAnalysisTaskService taskService,
                                           CET4PaperAnalysisTaskRunner taskRunner,
                                           ObjectMapper objectMapper) {
        this.taskService = taskService;
        this.taskRunner = taskRunner;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> submitTask(@RequestParam("file") MultipartFile file,
                                                          @RequestParam(value = "asyncMode", defaultValue = "false") boolean asyncMode,
                                                          HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        CET4PaperAnalysisTask task = null;
        Long userId = null;
        try {
            userId = UserSessionUtil.getCurrentUserId(session);
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("请上传有效的文件");
            }
            byte[] fileBytes = file.getBytes();
            String fileName = file.getOriginalFilename();
            String fileType = file.getContentType();
            String sourceType = resolveSourceType(fileName);
            String examPaperEnSource = "AIfromreal";

            task = taskService.createTask(userId, fileName, sourceType, examPaperEnSource);

            if (asyncMode) {
                taskRunner.executeTaskAsync(task.getId(), fileBytes, fileName, fileType);
                response.put("success", true);
                response.put("taskId", task.getId());
                response.put("async", true);
                response.put("status", task.getStatus());
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            }

            Map<String, Object> result = taskRunner.executeTaskSync(task, fileBytes, fileName, fileType);
            response.putAll(result);
            response.put("taskId", task.getId());
            response.put("async", false);
            response.putIfAbsent("success", true);
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
            if (!asyncMode && task != null && task.getId() != null && userId != null) {
                taskService.deleteTask(task.getId(), userId);
            }
        }
    }

    @GetMapping
    public ResponseEntity<List<TaskSummary>> listTasks(@RequestParam(value = "limit", defaultValue = "20") int limit,
                                                       HttpSession session) {
        try {
            Long userId = UserSessionUtil.getCurrentUserId(session);
            int sanitizedLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
            List<CET4PaperAnalysisTask> tasks = taskService.listRecentTasks(userId, sanitizedLimit);
            List<TaskSummary> summaries = tasks.stream()
                    .map(task -> TaskSummary.fromEntity(task, objectMapper))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<TaskSummary> getTask(@PathVariable("taskId") Long taskId, HttpSession session) {
        try {
            Long userId = UserSessionUtil.getCurrentUserId(session);
            CET4PaperAnalysisTask task = taskService.findByIdAndUser(taskId, userId);
            if (task == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.ok(TaskSummary.fromEntity(task, objectMapper));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping("/{taskId}/result")
    public ResponseEntity<Map<String, Object>> getResult(@PathVariable("taskId") Long taskId, HttpSession session) {
        try {
            Long userId = UserSessionUtil.getCurrentUserId(session);
            CET4PaperAnalysisTask task = taskService.findByIdAndUser(taskId, userId);
            if (task == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            if (!Objects.equals("SUCCEEDED", task.getStatus())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                        "success", false,
                        "message", "任务尚未完成"
                ));
            }
            Map<String, Object> resultNode = parseResult(task.getStructuredJson());
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.putAll(resultNode);
            result.putIfAbsent("fileName", task.getFileName());
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

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Map<String, Object>> deleteTask(@PathVariable("taskId") Long taskId, HttpSession session) {
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

    private Map<String, Object> parseResult(String json) throws Exception {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return objectMapper.readValue(json, new TypeReference<>() {});
    }

    private String resolveSourceType(String fileName) {
        String lowerName = fileName == null ? "" : fileName.toLowerCase();
        if (lowerName.endsWith(".txt") || lowerName.endsWith(".doc") || lowerName.endsWith(".docx")) {
            return "PLAIN";
        }
        return "OCR";
    }

    public static class TaskSummary {
        private Long id;
        private String fileName;
        private String status;
        private Integer progress;
        private String message;
        private String createdAt;
        private String updatedAt;
        private String completedAt;

        public static TaskSummary fromEntity(CET4PaperAnalysisTask task, ObjectMapper objectMapper) {
            TaskSummary summary = new TaskSummary();
            summary.id = task.getId();
            summary.status = task.getStatus();
            summary.progress = task.getProgress();
            summary.message = task.getMessage();
            summary.createdAt = toIso(task.getCreatedAt());
            summary.updatedAt = toIso(task.getUpdatedAt());
            summary.completedAt = toIso(task.getCompletedAt());
            summary.fileName = task.getFileName();
            return summary;
        }

        private static String toIso(Object value) {
            return value == null ? null : value.toString();
        }

        public Long getId() {
            return id;
        }

        public String getFileName() {
            return fileName;
        }

        public String getStatus() {
            return status;
        }

        public Integer getProgress() {
            return progress;
        }

        public String getMessage() {
            return message;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public String getCompletedAt() {
            return completedAt;
        }
    }
}
