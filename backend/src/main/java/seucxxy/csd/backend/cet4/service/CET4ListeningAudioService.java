package seucxxy.csd.backend.cet4.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 听力音频生成服务
 * 负责根据CET4听力朗读规则生成听力音频
 */
@Service
public class CET4ListeningAudioService {

    private static final Logger logger = LoggerFactory.getLogger(CET4ListeningAudioService.class);

    private static final int COZE_SCRIPT_LOG_MAX_CHARS = 20000;
    
    // 音频日志文件路径
    private static final Path AUDIO_LOG_FILE = Paths.get("logs", "audio.log");
    private static final DateTimeFormatter LOG_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${coze.api.url}")
    private String cozeApiUrl;

    @Value("${coze.api.token.examinationAI}")
    private String cozeApiToken;

    @Value("${coze.workflow.listening-audio}")
    private String listeningAudioWorkflowId;

    // 更新为新的Python服务地址
    private static final String AUDIO_SERVICE_URL = "http://localhost:5000/api/audio";

    /**
     * 根据前端传入的多个听力segment生成完整音频（逐段调用Coze，最后合并）
     * 新增 exam_paper_en_source 以在 Coze 工作流中进行判断
     * @param segments 多个听力segment数据
     * @param examPaperEnSource 英文试卷来源标识字符串
     * @return 合并后音频文件的URL路径
     */
    public String generateFullListeningAudio(List<Map<String, Object>> segments, String examPaperEnSource) throws Exception {
        AtomicInteger newsReportCounter = new AtomicInteger(0);
        AtomicInteger passageCounter = new AtomicInteger(0);

        int parallelism = Math.max(2, Math.min(segments.size(), 4));
        ExecutorService pool = Executors.newFixedThreadPool(parallelism);

        try {
            List<CompletableFuture<SegmentResult>> futures = new ArrayList<>();
            for (int i = 0; i < segments.size(); i++) {
                int idx = i;
                Map<String, Object> segment = segments.get(i);
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        return processSegment(segment, idx, examPaperEnSource, newsReportCounter, passageCounter);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }, pool));
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();

            List<String> tempAudioFiles = futures.stream()
                    .map(CompletableFuture::join)
                    .sorted(Comparator.comparingInt(SegmentResult::index))
                    .map(SegmentResult::filePath)
                    .collect(Collectors.toList());

            return mergeAudioFiles(tempAudioFiles);
        } catch (Exception e) {
            throw e;
        } finally {
            pool.shutdown();
        }
    }

    private SegmentResult processSegment(Map<String, Object> segment,
                                         int index,
                                         String examPaperEnSource,
                                         AtomicInteger newsReportCounter,
                                         AtomicInteger passageCounter) throws Exception {
        String unitType = (String) segment.getOrDefault("unit_type", "");
        boolean isNewsReport = unitType.toLowerCase().contains("news");
        boolean isConversation = unitType.toLowerCase().contains("conversation");
        boolean isPassage = unitType.toLowerCase().contains("passage");

        String contentVoice = null;
        if (isNewsReport) {
            int count = newsReportCounter.getAndIncrement();
            contentVoice = (count % 3 == 0) ? "male" : ((count % 3 == 1) ? "female" : "male");
        } else if (isPassage) {
            int count = passageCounter.getAndIncrement();
            contentVoice = (count % 3 == 0) ? "male" : ((count % 3 == 1) ? "female" : "male");
        }
        // Conversation不设置默认声音，由M:/W:标记控制

        String readingScript = callCozeForReadingScript(segment, examPaperEnSource);
        List<Map<String, Object>> audioSegments = parseReadingScript(readingScript, contentVoice, isConversation);
        String segmentAudioUrl = synthesizeCombinedAudio(audioSegments);

        String filePath = segmentAudioUrl;
        if (filePath.contains("/file/")) {
            filePath = filePath.substring(filePath.lastIndexOf("/file/") + 6);
        } else if (filePath.contains("/audio/")) {
            filePath = filePath.substring(filePath.lastIndexOf("/audio/") + 7);
        }

        logger.info("[ListeningAudio] segment {} audio file: {}", index, filePath);
        return new SegmentResult(index, filePath);
    }

    private static class SegmentResult {
        private final int index;
        private final String filePath;

        SegmentResult(int index, String filePath) {
            this.index = index;
            this.filePath = filePath;
        }

        public int index() {
            return index;
        }

        public String filePath() {
            return filePath;
        }
    }

    /**
     * 调用Python服务合并多个音频文件
     */
    private String mergeAudioFiles(List<String> audioFilePaths) throws Exception {
        // AUDIO_SERVICE_URL 已包含 /api/audio
        String url = AUDIO_SERVICE_URL + "/merge-audios";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("audio_files", audioFilePaths);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        
        ResponseEntity<String> response = restTemplate.exchange(URI.create(url), HttpMethod.POST, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new Exception("音频合并服务调用失败");
        }

        JsonNode responseNode = objectMapper.readTree(response.getBody());
        if (!responseNode.path("success").asBoolean(false)) {
            String error = responseNode.path("error").asText("未知错误");
            throw new Exception("音频合并失败: " + error);
        }

        // Python服务返回格式：{"success": true, "data": {"audio_url": "..."}}
        // 先尝试从data字段获取audio_url
        String audioUrl = "";
        if (responseNode.has("data") && responseNode.get("data").has("audio_url")) {
            audioUrl = responseNode.get("data").get("audio_url").asText("");
        } else if (responseNode.has("audio_url")) {
            // 兼容旧格式：直接在根级别
            audioUrl = responseNode.path("audio_url").asText("");
        }
        
        logger.info("[ListeningAudio][Merge] Python returned merged audio_url: '{}'", audioUrl);
        
        // Python服务返回的路径格式：/api/audio/file/xxx.wav
        // 转换为完整URL：http://localhost:5000/api/audio/file/xxx.wav
        if (audioUrl.startsWith("/api/audio/")) {
            audioUrl = "http://localhost:5000" + audioUrl;
        }
        
        logger.info("[ListeningAudio][Merge] Final merged audioUrl: '{}'", audioUrl);
        return audioUrl;
    }

    /**
     * 调用Coze API获取标准朗读脚本（带 exam_paper_en_source）
     */
    private String callCozeForReadingScript(Map<String, Object> segmentData, String examPaperEnSource) throws Exception {
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> params = new HashMap<>();
        String inputJson = objectMapper.writeValueAsString(segmentData);
        params.put("input", inputJson);
        if (examPaperEnSource != null) {
            params.put("exam_paper_en_source", examPaperEnSource);
        }
        root.put("workflow_id", listeningAudioWorkflowId);
        root.put("parameters", params);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + cozeApiToken);

        String jsonBody = objectMapper.writeValueAsString(root);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        // 新增调用前日志（简洁）
        String segmentId = Optional.ofNullable(segmentData.get("segment_id")).map(Object::toString).orElse("");
        String unitType = "";
        try {
            unitType = Optional.ofNullable(segmentData.get("unit_type")).map(Object::toString).orElse("");
            logger.info("[ListeningAudio][Coze] 开始调用 segment_id={} workflow_id={} source={} unit_type={}", segmentId, listeningAudioWorkflowId, examPaperEnSource, unitType);
        } catch (Exception logEx) {
            logger.warn("[ListeningAudio][Coze] 生成输入日志失败: {}", logEx.getMessage());
        }

        // 记录详细输入到 audio.log
        writeAudioLog("========== COZE REQUEST ==========");
        writeAudioLog("segment_id: " + segmentId);
        writeAudioLog("unit_type: " + unitType);
        writeAudioLog("workflow_id: " + listeningAudioWorkflowId);
        writeAudioLog("exam_paper_en_source: " + examPaperEnSource);
        writeAudioLog("----- INPUT JSON (segment data) -----");
        writeAudioLog(formatJsonPretty(inputJson));
        writeAudioLog("----- FULL REQUEST BODY -----");
        writeAudioLog(formatJsonPretty(jsonBody));
        writeAudioLog("==================================");

        long startTime = System.currentTimeMillis();
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(
                java.util.Objects.requireNonNull(URI.create(cozeApiUrl)),
                java.util.Objects.requireNonNull(HttpMethod.POST),
                entity,
                String.class
            );
        } catch (Exception httpEx) {
            logger.error("[ListeningAudio][Coze] HTTP调用异常: {}", httpEx.getMessage());
            writeAudioLog("========== COZE ERROR ==========");
            writeAudioLog("segment_id: " + segmentId);
            writeAudioLog("error: " + httpEx.getMessage());
            writeAudioLog("================================");
            throw new Exception("Coze API调用失败: " + httpEx.getMessage(), httpEx);
        }
        long elapsed = System.currentTimeMillis() - startTime;

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            logger.error("[ListeningAudio][Coze] 非2xx响应 status={}", response.getStatusCode());
            writeAudioLog("========== COZE ERROR ==========");
            writeAudioLog("segment_id: " + segmentId);
            writeAudioLog("status: " + response.getStatusCode());
            writeAudioLog("================================");
            throw new Exception("Coze API调用失败");
        }

        logger.info("[ListeningAudio][Coze] 调用结束 segment_id={} status={} elapsed={}ms", segmentId, response.getStatusCode(), elapsed);

        // 记录原始响应到 audio.log
        writeAudioLog("========== COZE RESPONSE ==========");
        writeAudioLog("segment_id: " + segmentId);
        writeAudioLog("status: " + response.getStatusCode());
        writeAudioLog("elapsed: " + elapsed + "ms");
        writeAudioLog("----- RAW RESPONSE BODY -----");
        writeAudioLog(formatJsonPretty(response.getBody()));

        JsonNode rootNode = objectMapper.readTree(response.getBody());
        JsonNode dataNode = rootNode.path("data");
        String output = "";
        if (!(dataNode.isMissingNode() || dataNode.isNull())) {
            if (dataNode.isTextual()) {
                try {
                    JsonNode parsedData = objectMapper.readTree(dataNode.asText());
                    JsonNode outputNode = parsedData.get("output");
                    if (outputNode != null) {
                        if (outputNode.isTextual()) {
                            output = outputNode.asText();
                        } else if (outputNode.isObject()) {
                            output = objectMapper.writeValueAsString(outputNode);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("[ListeningAudio][Coze] 解析data字符串失败: {}", e.getMessage());
                }
            } else if (dataNode.isObject()) {
                JsonNode outputNode = dataNode.get("output");
                if (outputNode != null) {
                    if (outputNode.isTextual()) {
                        output = outputNode.asText();
                    } else if (outputNode.isObject()) {
                        output = objectMapper.writeValueAsString(outputNode);
                    }
                }
            }
        }
        if (output.isEmpty()) {
            JsonNode outputNode = rootNode.get("output");
            if (outputNode != null && outputNode.isTextual()) {
                output = outputNode.asText();
            }
        }
        if (output.isEmpty()) {
            logger.error("[ListeningAudio][Coze] output为空，无法继续");
            writeAudioLog("----- PARSED OUTPUT -----");
            writeAudioLog("ERROR: output为空");
            writeAudioLog("===================================");
            throw new Exception("Coze API返回的output为空");
        }
        output = output.replaceAll("```\\n?", "").replaceAll("```$", "").trim();

        // 记录解析后的 output 到 audio.log
        writeAudioLog("----- PARSED OUTPUT (reading script) -----");
        writeAudioLog(output);
        writeAudioLog("===================================");

        // 精简：仅打印简短完成日志
        logger.info("[ListeningAudio][Coze] output ready (len={})", output.length());

        // 打印脚本（用于定位 [停顿X秒] 是否异常）
        logCozeReadingScript(segmentId, listeningAudioWorkflowId, output);
        return output;
    }

    /**
     * 写入音频日志到 logs/audio.log
     */
    private void writeAudioLog(String message) {
        try {
            // 确保目录存在
            Files.createDirectories(AUDIO_LOG_FILE.getParent());
            
            String timestamp = LocalDateTime.now().format(LOG_TIME_FMT);
            String logLine = "[" + timestamp + "] " + message + "\n";
            
            Files.writeString(AUDIO_LOG_FILE, logLine, 
                StandardOpenOption.CREATE, 
                StandardOpenOption.APPEND);
        } catch (Exception e) {
            logger.warn("[ListeningAudio] 写入audio.log失败: {}", e.getMessage());
        }
    }

    /**
     * 格式化 JSON 字符串（美化输出）
     */
    private String formatJsonPretty(String json) {
        if (json == null || json.isEmpty()) {
            return "<empty>";
        }
        try {
            Object obj = objectMapper.readValue(json, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            // 如果不是有效的 JSON，直接返回原字符串
            return json;
        }
    }

    private void logCozeReadingScript(String segmentId, String workflowId, String script) {
        if (script == null) {
            logger.info("[ListeningAudio][Script] segment_id={} workflow_id={} script=null", segmentId, workflowId);
            return;
        }

        String normalized = script.replace("\r\n", "\n").replace("\r", "\n");
        String toLog = normalized;
        boolean truncated = false;
        if (normalized.length() > COZE_SCRIPT_LOG_MAX_CHARS) {
            toLog = normalized.substring(0, COZE_SCRIPT_LOG_MAX_CHARS);
            truncated = true;
        }

        logger.info("[ListeningAudio][Script] BEGIN segment_id={} workflow_id={} len={} truncated={}", segmentId, workflowId, normalized.length(), truncated);
        // 保持多行原样输出，方便肉眼定位停顿行
        logger.info("[ListeningAudio][Script] {}", toLog);
        logger.info("[ListeningAudio][Script] END segment_id={} workflow_id={}", segmentId, workflowId);
    }

    /**
     * 解析朗读脚本，提取音频片段
     * 识别：[M]、[W]、[停顿X秒]、普通文本、引号内的文本
     * @param script 朗读脚本
     * @param contentVoice 正文默认声音（male/female），仅对News Report和Passage有效
     * @param isConversation 是否为对话类型
     */
    private List<Map<String, Object>> parseReadingScript(String script, String contentVoice, boolean isConversation) {
        List<Map<String, Object>> segments = new ArrayList<>();
        
        // 按行分割
        String[] lines = script.split("\\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            // 匹配 [停顿X秒]
            if (line.matches("\\[停顿(\\d+)秒\\]")) {
                int seconds = Integer.parseInt(line.replaceAll("\\[停顿(\\d+)秒\\]", "$1"));
                segments.add(createPauseSegment(seconds * 1000));
                continue;
            }

            // 匹配 M: 或 W: 或 [M]: 或 [W]: (对话格式)
            if (line.matches("^\\[?M\\]?:\\s*.+") || line.matches("^\\[?W\\]?:\\s*.+")) {
                String voice = line.matches("^\\[?M\\]?:\\s*.+") ? "male" : "female";
                // 提取冒号后的文本
                String text = line.replaceFirst("^\\[?[MW]\\]?:\\s*", "").trim();
                segments.add(createTextSegment(text, voice));
                continue;
            }

            // 匹配引号内的文本（题目和引导语，用女声）
            if (line.startsWith("\"") && line.endsWith("\"")) {
                String text = line.substring(1, line.length() - 1);
                segments.add(createTextSegment(text, "female"));
                continue;
            }

            // 其他文本：
            // - 对于Conversation：用女声（默认）
            // - 对于News Report/Passage：使用传入的contentVoice
            if (!line.startsWith("[")) {
                String voice = isConversation ? "female" : (contentVoice != null ? contentVoice : "female");
                segments.add(createTextSegment(line, voice));
            }
        }

        return segments;
    }



    /**
     * 创建文本片段
     */
    private Map<String, Object> createTextSegment(String text, String voice) {
        Map<String, Object> segment = new HashMap<>();
        segment.put("text", text);
        segment.put("voice", voice);
        return segment;
    }

    /**
     * 创建停顿片段
     */
    private Map<String, Object> createPauseSegment(int durationMs) {
        Map<String, Object> segment = new HashMap<>();
        segment.put("text", "pause");
        segment.put("duration", durationMs);
        return segment;
    }

    /**
     * 调用Python服务合成音频
     */
    private String synthesizeCombinedAudio(List<Map<String, Object>> segments) throws Exception {
        // AUDIO_SERVICE_URL 已包含 /api/audio
        String url = AUDIO_SERVICE_URL + "/synthesize-combined";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("segments", segments);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(URI.create(url), HttpMethod.POST, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new Exception("音频合成服务调用失败");
        }

        JsonNode responseNode = objectMapper.readTree(response.getBody());
        if (!responseNode.path("success").asBoolean(false)) {
            String error = responseNode.path("error").asText("未知错误");
            throw new Exception("音频合成失败: " + error);
        }

        // Python服务返回格式：{"success": true, "data": {"audio_url": "..."}}
        // 先尝试从data字段获取audio_url
        String audioUrl = "";
        if (responseNode.has("data") && responseNode.get("data").has("audio_url")) {
            audioUrl = responseNode.get("data").get("audio_url").asText("");
        } else if (responseNode.has("audio_url")) {
            // 兼容旧格式：直接在根级别
            audioUrl = responseNode.path("audio_url").asText("");
        }
        
        logger.info("[ListeningAudio] Python returned audio_url: '{}'", audioUrl);
        
        // Python服务返回的路径格式：/api/audio/file/xxx.wav
        // 转换为完整URL：http://localhost:5000/api/audio/file/xxx.wav
        if (audioUrl.startsWith("/api/audio/")) {
            audioUrl = "http://localhost:5000" + audioUrl;
        }
        
        logger.info("[ListeningAudio] Final complete audioUrl: '{}'", audioUrl);
        return audioUrl;
    }
}
