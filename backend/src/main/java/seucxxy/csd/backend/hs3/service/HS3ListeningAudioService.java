package seucxxy.csd.backend.hs3.service;

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
import java.util.*;

/**
 * 高考听力音频生成服务
 * 负责根据高考听力朗读规则生成听力音频
 * 
 * 处理流程：
 * 1. 接收所有听力segments数组
 * 2. 一次性调用Coze生成完整朗读脚本
 * 3. 一次性合成完整音频
 */
@Service
public class HS3ListeningAudioService {

    private static final Logger logger = LoggerFactory.getLogger(HS3ListeningAudioService.class);

    private static final int COZE_SCRIPT_LOG_MAX_CHARS = 20000;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${coze.api.url}")
    private String cozeApiUrl;

    @Value("${coze.api.token.examinationAI}")
    private String cozeApiToken;

    // 高考听力音频生成工作流ID
    private static final String HS3_LISTENING_AUDIO_WORKFLOW_ID = "7583942885047255055";

    // Python服务地址
    private static final String AUDIO_SERVICE_URL = "http://localhost:5000/api/audio";

    /**
     * 根据前端传入的多个听力segment生成完整音频
     * 一次性调用Coze生成完整脚本，一次性合成完整音频
     * @param segments 多个听力segment数据（按顺序排列）
     * @return 音频文件的URL路径
     */
    public String generateFullListeningAudio(List<Map<String, Object>> segments) throws Exception {
        if (segments == null || segments.isEmpty()) {
            throw new Exception("听力segments为空");
        }

        logger.info("[HS3-ListeningAudio] 开始生成听力音频，共 {} 个segment", segments.size());

        // 1. 将所有segments作为JSON数组一次性发送给Coze
        String readingScript = callCozeForReadingScript(segments);
        
        // 2. 解析朗读脚本，提取音频片段
        List<Map<String, Object>> audioSegments = parseReadingScript(readingScript);
        
        // 3. 一次性合成完整音频
        String audioUrl = synthesizeCombinedAudio(audioSegments);
        
        logger.info("[HS3-ListeningAudio] 完整音频生成成功: {}", audioUrl);
        return audioUrl;
    }

    /**
     * 调用Coze API获取标准朗读脚本
     * @param segmentsData 所有听力segment的完整数组（一次性传入）
     */
    private String callCozeForReadingScript(List<Map<String, Object>> segmentsData) throws Exception {
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> params = new HashMap<>();
        
        // 将整个segments数组作为input传给Coze工作流
        params.put("input", objectMapper.writeValueAsString(segmentsData));
        root.put("workflow_id", HS3_LISTENING_AUDIO_WORKFLOW_ID);
        root.put("parameters", params);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + cozeApiToken);

        String jsonBody = objectMapper.writeValueAsString(root);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        logger.info("[HS3-ListeningAudio][Coze] 开始调用 segments_count={} workflow_id={}", 
            segmentsData.size(), HS3_LISTENING_AUDIO_WORKFLOW_ID);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(
                java.util.Objects.requireNonNull(URI.create(cozeApiUrl)),
                java.util.Objects.requireNonNull(HttpMethod.POST),
                entity,
                String.class
            );
        } catch (Exception httpEx) {
            logger.error("[HS3-ListeningAudio][Coze] HTTP调用异常: {}", httpEx.getMessage());
            throw new Exception("Coze API调用失败: " + httpEx.getMessage(), httpEx);
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            logger.error("[HS3-ListeningAudio][Coze] 非2xx响应 status={}", response.getStatusCode());
            throw new Exception("Coze API调用失败");
        }

        logger.info("[HS3-ListeningAudio][Coze] 调用结束 status={}", response.getStatusCode());

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
                    logger.warn("[HS3-ListeningAudio][Coze] 解析data字符串失败: {}", e.getMessage());
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
            logger.error("[HS3-ListeningAudio][Coze] output为空，无法继续");
            throw new Exception("Coze API返回的output为空");
        }
        output = output.replaceAll("```\\n?", "").replaceAll("```$", "").trim();

        logger.info("[HS3-ListeningAudio][Coze] output ready (len={})", output.length());

        logCozeReadingScript("all_segments", HS3_LISTENING_AUDIO_WORKFLOW_ID, output);
        
        return output;
    }

    private void logCozeReadingScript(String segmentId, String workflowId, String script) {
        if (script == null) {
            logger.info("[HS3-ListeningAudio][Script] segment_id={} workflow_id={} script=null", segmentId, workflowId);
            return;
        }

        String normalized = script.replace("\r\n", "\n").replace("\r", "\n");
        String toLog = normalized;
        boolean truncated = false;
        if (normalized.length() > COZE_SCRIPT_LOG_MAX_CHARS) {
            toLog = normalized.substring(0, COZE_SCRIPT_LOG_MAX_CHARS);
            truncated = true;
        }

        logger.info("[HS3-ListeningAudio][Script] BEGIN segment_id={} workflow_id={} len={} truncated={}", segmentId, workflowId, normalized.length(), truncated);
        logger.info("[HS3-ListeningAudio][Script] {}", toLog);
        logger.info("[HS3-ListeningAudio][Script] END segment_id={} workflow_id={}", segmentId, workflowId);
    }

    /**
     * 解析朗读脚本，提取音频片段
     * 识别：[M]、[W]、[停顿X秒]、[切换]、普通文本、引号内的文本
     * @param script 朗读脚本
     */
    private List<Map<String, Object>> parseReadingScript(String script) {
        List<Map<String, Object>> segments = new ArrayList<>();
        
        // 按行分割
        String[] lines = script.split("\\n");
        
        // 用于轮流使用男声女声的标志，初始为男声
        boolean useMaleVoice = true;
        
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

            // 匹配 [切换] - 切换男女声音
            if (line.equals("[切换]")) {
                useMaleVoice = !useMaleVoice;
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

            // 其他文本：使用当前的男/女声状态
            if (!line.startsWith("[")) {
                String voice = useMaleVoice ? "male" : "female";
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

        String audioUrl = "";
        if (responseNode.has("data") && responseNode.get("data").has("audio_url")) {
            audioUrl = responseNode.get("data").get("audio_url").asText("");
        } else if (responseNode.has("audio_url")) {
            audioUrl = responseNode.path("audio_url").asText("");
        }
        
        logger.info("[HS3-ListeningAudio] Python returned audio_url: '{}'", audioUrl);
        
        if (audioUrl.startsWith("/api/audio/")) {
            audioUrl = "http://localhost:5000" + audioUrl;
        }
        
        logger.info("[HS3-ListeningAudio] Final complete audioUrl: '{}'", audioUrl);
        return audioUrl;
    }
}
