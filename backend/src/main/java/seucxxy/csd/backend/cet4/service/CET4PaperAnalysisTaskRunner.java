package seucxxy.csd.backend.cet4.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import seucxxy.csd.backend.cet4.entity.CET4PaperAnalysisTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class CET4PaperAnalysisTaskRunner {

    private static final Logger log = LoggerFactory.getLogger(CET4PaperAnalysisTaskRunner.class);

    private final CET4PaperAnalysisTaskService taskService;
    private final CET4PaperAnalysisService concurrenceService;

    public CET4PaperAnalysisTaskRunner(CET4PaperAnalysisTaskService taskService,
                                       CET4PaperAnalysisService concurrenceService) {
        this.taskService = taskService;
        this.concurrenceService = concurrenceService;
    }

    public Map<String, Object> executeTaskSync(CET4PaperAnalysisTask task,
                                               byte[] fileBytes,
                                               String fileName,
                                               String contentType) throws Exception {
        return execute(task, fileBytes, fileName, contentType);
    }

    @Async("paperAnalysisTaskExecutor")
    public void executeTaskAsync(Long taskId,
                                 byte[] fileBytes,
                                 String fileName,
                                 String contentType) {
        CET4PaperAnalysisTask task = taskService.findById(taskId);
        if (task == null) {
            return;
        }
        try {
            execute(task, fileBytes, fileName, contentType);
        } catch (Exception ex) {
            log.error("异步解析任务执行失败, taskId={}", taskId, ex);
        }
    }

    private Map<String, Object> execute(CET4PaperAnalysisTask task,
                                        byte[] fileBytes,
                                        String fileName,
                                        String contentType) throws Exception {
        taskService.markRunning(task.getId());
        taskService.updateProgress(task.getId(), 10, "正在解析文件...");
        try {
            Map<String, Object> result = processFile(fileBytes, fileName, contentType);
            if (result == null) {
                throw new IllegalStateException("未获取到解析结果");
            }
            Map<String, Object> wrapped = new HashMap<>(result);
            wrapped.putIfAbsent("success", true);
            wrapped.putIfAbsent("fileName", fileName);
            String mode = toText(result.get("mode"));
            if (mode == null || mode.isBlank()) {
                mode = "concurrent";
            }
            taskService.markSuccess(task.getId(), wrapped);
            return wrapped;
        } catch (Exception ex) {
            taskService.markFailed(task.getId(), ex.getMessage());
            throw ex;
        }
    }

    private Map<String, Object> processFile(byte[] fileBytes, String fileName, String contentType) throws Exception {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("文件内容为空，无法解析");
        }
        String lowerName = fileName == null ? "" : fileName.toLowerCase();
        boolean isPdf = lowerName.endsWith(".pdf");
        boolean isTxt = lowerName.endsWith(".txt");
        boolean isDoc = lowerName.endsWith(".doc") || lowerName.endsWith(".docx");
        boolean isImage = lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg");

        MultipartFile multipartFile = new InMemoryMultipartFile(
            "file",
            fileName,
            safeContentType(contentType, isPdf, isImage),
            fileBytes
        );

        if (isPdf) {
            return concurrenceService.extractFromPdf(multipartFile);
        }
        if (isImage) {
            return concurrenceService.extractFromImage(multipartFile);
        }
        if (isTxt || isDoc) {
            return concurrenceService.extractFromPlain(multipartFile);
        }
        // 默认按纯文本处理
        return concurrenceService.extractFromPlain(multipartFile);
    }

    private String safeContentType(String contentType, boolean pdf, boolean image) {
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }
        if (pdf) {
            return "application/pdf";
        }
        if (image) {
            return "image/png";
        }
        return "text/plain";
    }

    private static class InMemoryMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        InMemoryMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content == null ? new byte[0] : content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content.clone();
        }

        @Override
        public java.io.InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws java.io.IOException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }

    private String toText(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String str) {
            return str;
        }
        return Objects.toString(value, null);
    }
}
