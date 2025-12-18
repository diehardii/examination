package seucxxy.csd.backend.cet4.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import seucxxy.csd.backend.cet4.mapper.CET4TopicMapper;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CET4PaperAnalysisSegmentService {

    private static final Logger logger = LoggerFactory.getLogger(CET4PaperAnalysisSegmentService.class);
    private static final List<String> CLOZE_KEYWORDS = Arrays.asList("选词填空", "完形填空", "cloze", "sectiona");
    private static final Pattern UNDERSCORE_NUMBER_PATTERN = Pattern.compile("_+\\s*(\\d{1,4})\\s*_+");
    private static final Pattern SEGMENT_ID_IN_TEXT_PATTERN = Pattern.compile("segment_id\\s*[:=]\\s*\"?([\\w-]+)\"?", Pattern.CASE_INSENSITIVE);

    @Value("${coze.api.url}")
    private String cozeApiUrl;

    @Value("${coze.api.token.examinationAI}")
    private String cozeApiToken;

    @Value("${coze.workflow.paper-structure:7572197348467507263}")
    private String workflowId;

    @Value("${coze.workflow.listening:7573112248694276148}")
    private String listeningWorkflowId;

    @Value("${python.cet4.analysis.url:http://localhost:5000/api/cet4/analyze-exam}")
    private String pythonAnalysisUrl;

    @Value("${python.cet4.listening.url:http://localhost:5000/api/cet4/analyze-listening}")
    private String pythonListeningUrl;

    private final RestTemplate restTemplate;
    private final CET4TopicMapper topicMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> processFullText(String fullText, Executor cozeExecutor) throws IOException {
        if (fullText == null || fullText.isBlank()) {
            throw new IllegalArgumentException("未读取到试卷文本内容，请检查文件是否为扫描版或是否损坏");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("mode", "concurrent");

        String part1 = extractPart1FromFullText(fullText);
        String part2 = extractPart2FromFullTextPart2(fullText);
        String part4 = extractPart4FromFullText(fullText);
        response.put("part1", part1);
        response.put("part2", part2);
        response.put("part4", part4);

        String sectionAText = extractSectionAFromFullText(fullText);
        String sectionBText = safeExtract(() -> extractSectionBFromFullText(fullText));
        String sectionCText = safeExtract(() -> extractSectionCFromFullText(fullText));
        response.put("text", sectionAText);

        // listening split
        String[] part2Split = splitPart2IntoSections(part2);
        String part2A = part2Split[0];
        String part2B = part2Split[1];
        String part2C = part2Split[2];

        CompletableFuture<JsonNode> writingFuture = submitIfNotBlank(part1, () -> callWorkflow(part1, workflowId), cozeExecutor);
        CompletableFuture<JsonNode> translationFuture = submitIfNotBlank(part4, () -> callWorkflow(part4, workflowId), cozeExecutor);
        CompletableFuture<ObjectNode> listeningABFuture = submitIfNotBlank(part2A + part2B, () -> callListeningAB(part2A, part2B), cozeExecutor);
        CompletableFuture<JsonNode> listeningCFuture = submitIfNotBlank(part2C, () -> callListening(part2C), cozeExecutor);
        CompletableFuture<JsonNode> sectionAFuture = submitIfNotBlank(sectionAText, () -> callWorkflow(sectionAText, workflowId), cozeExecutor);
        CompletableFuture<JsonNode> sectionBFuture = submitIfNotBlank(sectionBText, () -> callWorkflow(sectionBText, workflowId), cozeExecutor);
        CompletableFuture<JsonNode> sectionCFuture = submitIfNotBlank(sectionCText, () -> callWorkflow(sectionCText, workflowId), cozeExecutor);

        CompletableFuture.allOf(
                safeFuture(writingFuture),
                safeFuture(translationFuture),
                safeFuture(listeningABFuture),
                safeFuture(listeningCFuture),
                safeFuture(sectionAFuture),
                safeFuture(sectionBFuture),
                safeFuture(sectionCFuture)
        ).join();

        // Writing
        JsonNode structuredWriting = joinOrNull(writingFuture);
        if (structuredWriting != null) {
            response.put("structuredWriting", structuredWriting);
        } else if (!part1.isBlank()) {
            response.put("structuredWriting", buildDefaultWriting(part1));
        }

        // Translation
        JsonNode structuredTranslation = joinOrNull(translationFuture);
        if (structuredTranslation != null) {
            response.put("structuredTranslation", structuredTranslation);
        } else if (!part4.isBlank()) {
            response.put("structuredTranslation", buildDefaultTranslation(part4));
        }

        // Listening AB
        ObjectNode structuredListeningAB = joinOrNull(listeningABFuture);
        if (structuredListeningAB != null) {
            response.put("structuredListeningAB", structuredListeningAB);
        } else {
            response.put("structuredListeningAB", objectMapper.readTree("{\"units\":[]}"));
        }

        // Listening C
        JsonNode structuredListeningC = joinOrNull(listeningCFuture);
        if (structuredListeningC != null) {
            response.put("structuredListeningC", structuredListeningC);
        } else {
            response.put("structuredListeningC", objectMapper.readTree("{\"units\":[]}"));
        }

        // Section A/B/C
        JsonNode structuredA = joinOrNull(sectionAFuture);
        if (structuredA != null) {
            response.put("structured", structuredA);
            JsonNode sectionANode = extractSectionA(structuredA);
            if (sectionANode != null) {
                response.put("sectionA", normalizeSectionA(sectionANode));
            }
        }

        JsonNode structuredB = joinOrNull(sectionBFuture);
        if (structuredB != null) {
            response.put("structuredB", structuredB);
            JsonNode sectionBNode = extractSectionB(structuredB);
            if (sectionBNode != null) {
                response.put("sectionB", normalizeSectionB(sectionBNode));
            }
        }

        JsonNode structuredC = joinOrNull(sectionCFuture);
        if (structuredC != null) {
            response.put("structuredC", structuredC);
            JsonNode sectionC1Node = extractSectionC1(structuredC);
            if (sectionC1Node != null) {
                response.put("sectionC1", normalizeSectionC1(sectionC1Node));
            }
            JsonNode sectionC2Node = extractSectionC2(structuredC);
            if (sectionC2Node != null) {
                response.put("sectionC2", normalizeSectionC2(sectionC2Node));
            }
        }

        return response;
    }

    private <T> CompletableFuture<T> submitIfNotBlank(String text, SupplierWithException<T> supplier, Executor executor) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                logger.warn("并发解析任务失败: {}", e.getMessage());
                return null;
            }
        }, executor);
    }

    private CompletableFuture<?> safeFuture(CompletableFuture<?> f) {
        return f == null ? CompletableFuture.completedFuture(null) : f;
    }

    private <T> T joinOrNull(CompletableFuture<T> f) {
        if (f == null) return null;
        try {
            return f.join();
        } catch (Exception e) {
            return null;
        }
    }

    private ObjectNode callListeningAB(String part2A, String part2B) throws IOException {
        HttpHeaders headers = buildHeaders();
        ArrayNode units = objectMapper.createArrayNode();

        if (part2A != null && !part2A.isBlank()) {
            JsonNode structuredA = callListening(part2A);
            if (structuredA != null && structuredA.has("units")) {
                structuredA.get("units").forEach(units::add);
            }
        }
        if (part2B != null && !part2B.isBlank()) {
            JsonNode structuredB = callListening(part2B);
            if (structuredB != null && structuredB.has("units")) {
                structuredB.get("units").forEach(units::add);
            }
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.set("units", units);
        return result;
    }

    private JsonNode callListening(String text) throws IOException {
        return callWithRetry(text, true, () -> {
            HttpHeaders headers = buildHeaders();
            String requestBody = buildWorkflowRequestPart2(text);
            ResponseEntity<String> resp = restTemplate.exchange(cozeApiUrl, HttpMethod.POST, new HttpEntity<>(requestBody, headers), String.class);
            return resp.getBody();
        });
    }

    private JsonNode callWorkflow(String text, String workflow) throws IOException {
        return callWithRetry(text, false, () -> {
            HttpHeaders headers = buildHeaders();
            String requestBody = buildWorkflowRequest(text, workflow);
            ResponseEntity<String> resp = restTemplate.exchange(cozeApiUrl, HttpMethod.POST, new HttpEntity<>(requestBody, headers), String.class);
            return resp.getBody();
        });
    }

    private JsonNode callWithRetry(String text, boolean listening, SupplierWithException<String> bodySupplier) throws IOException {
        String segmentId = resolveSegmentIdFromText(text);
        String body = null;
        for (int attempt = 1; attempt <= 10; attempt++) {
            try {
                logger.info("coze开始解析 segment_id={} (第{}次)", segmentId, attempt);
                body = bodySupplier.get();
            } catch (Exception e) {
                logger.warn("Coze调用异常，第{}次尝试: {}", attempt, e.getMessage());
            }
            if (body != null && !body.isBlank()) {
                logger.info("coze解析结束 segment_id={}", segmentId);
                return listening ? parseWorkflowResponsePart2(body) : parseWorkflowResponse(body);
            }
        }
        logger.warn("Coze返回为空，已重试10次，触发Python回退(听力: {})", listening);
        JsonNode fallback = callPythonFallback(text, listening);
        if (fallback == null) {
            logger.warn("Python回退也未获得结果");
        }
        return fallback;
    }

    private JsonNode callPythonFallback(String text, boolean listening) {
        String segmentId = resolveSegmentIdFromText(text);
        try {
            logger.info("deepseek开始解析 segment_id={}", segmentId);
            String url = listening ? pythonListeningUrl : pythonAnalysisUrl;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("inputFile", text);
            body.put("topics", getAllTopics());
            body.put("model", "deepseek-reasoner");

            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null || resp.getBody().isBlank()) {
                logger.warn("Python回退HTTP状态异常或空响应，状态: {}", resp.getStatusCode());
                return null;
            }

            String respBody = resp.getBody();
            JsonNode root = objectMapper.readTree(respBody);
            JsonNode dataNode = root.get("data");
            if (dataNode != null && dataNode.isTextual()) {
                String dataStr = dataNode.asText();
                if (!dataStr.isBlank()) {
                    try {
                        return objectMapper.readTree(dataStr);
                    } catch (Exception e) {
                        logger.warn("Python data字段不是有效JSON，直接返回文本节点");
                        return dataNode;
                    }
                }
            }
            if (dataNode != null && dataNode.isObject()) {
                return dataNode;
            }
            return root;
        } catch (Exception e) {
            logger.warn("Python回退调用失败: {}", e.getMessage());
            return null;
        } finally {
            logger.info("deepseek解析结束 segment_id={}", segmentId);
        }
    }

    private String resolveSegmentIdFromText(String text) {
        if (text == null) {
            return "unknown";
        }
        Matcher matcher = SEGMENT_ID_IN_TEXT_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "unknown";
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + cozeApiToken);
        return headers;
    }

    private JsonNode buildDefaultWriting(String part1) {
        try {
            return objectMapper.readTree(objectMapper.writeValueAsString(Map.of(
                    "writing", Map.of(
                            "question_type", "写作",
                            "prompt_requirement", "一道写作题",
                            "passage", part1
                    )
            )));
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    private JsonNode buildDefaultTranslation(String part4) {
        try {
            return objectMapper.readTree(objectMapper.writeValueAsString(Map.of(
                    "translation", Map.of(
                            "question_type", "翻译",
                            "prompt_requirement", "一道翻译题",
                            "passage", part4
                    )
            )));
        } catch (Exception e) {
            return objectMapper.createObjectNode();
        }
    }

    // ============ 请求构造 & 解析 ============

    private List<String> getAllTopics() {
        try {
            List<String> topics = topicMapper.selectAllTopics();
            return topics != null ? topics : Collections.emptyList();
        } catch (Exception e) {
            logger.warn("获取topics列表失败，使用空列表", e);
            return Collections.emptyList();
        }
    }

    private String buildWorkflowRequest(String text, String wfId) throws IOException {
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> params = new HashMap<>();
        params.put("inputFile", text);
        params.put("topics", getAllTopics());
        root.put("workflow_id", wfId);
        root.put("parameters", params);
        return objectMapper.writeValueAsString(root);
    }

    private String buildWorkflowRequestPart2(String text) throws IOException {
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> params = new HashMap<>();
        params.put("inputFile", text);
        params.put("topics", getAllTopics());
        root.put("workflow_id", listeningWorkflowId);
        root.put("parameters", params);
        return objectMapper.writeValueAsString(root);
    }

    private JsonNode parseWorkflowResponse(String body) throws IOException {
        if (body == null || body.isEmpty()) {
            throw new IllegalArgumentException("结构化解析返回内容为空");
        }
        JsonNode rootNode = objectMapper.readTree(body);
        JsonNode dataNode = rootNode.path("data");
        JsonNode parsed = null;
        if (!(dataNode.isMissingNode() || dataNode.isNull())) {
            parsed = unwrapDataNode(dataNode);
        }
        if (parsed == null || parsed.isMissingNode() || parsed.isNull()) {
            parsed = unwrapDataNode(rootNode);
        }
        if (parsed == null || parsed.isMissingNode() || parsed.isNull()) {
            JsonNode outputNode = rootNode.get("output");
            if (outputNode != null && outputNode.isTextual()) {
                String text = normalizeJsonText(outputNode.asText());
                if (text != null && !text.isEmpty()) {
                    try {
                        parsed = objectMapper.readTree(text);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return parsed == null ? rootNode : parsed;
    }

    private JsonNode parseWorkflowResponsePart2(String body) throws IOException {
        if (body == null || body.isEmpty()) {
            throw new IllegalArgumentException("结构化解析返回内容为空");
        }
        JsonNode rootNode = objectMapper.readTree(body);
        JsonNode dataNode = rootNode.path("data");
        JsonNode parsed = null;
        if (!(dataNode.isMissingNode() || dataNode.isNull())) {
            parsed = unwrapDataNode(dataNode);
        }
        if (parsed == null || parsed.isMissingNode() || parsed.isNull()) {
            parsed = unwrapDataNode(rootNode);
        }
        if (parsed == null || parsed.isMissingNode() || parsed.isNull()) {
            JsonNode outputNode = rootNode.get("output");
            if (outputNode != null && outputNode.isTextual()) {
                String text = normalizeJsonText(outputNode.asText());
                if (text != null && !text.isEmpty()) {
                    try {
                        parsed = objectMapper.readTree(text);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return parsed == null ? rootNode : parsed;
    }

    private JsonNode unwrapDataNode(JsonNode node) throws IOException {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            String text = normalizeJsonText(node.asText());
            if (text == null || text.isEmpty()) {
                return null;
            }
            try {
                JsonNode parsed = objectMapper.readTree(text);
                if (parsed.isTextual()) {
                    return unwrapDataNode(parsed);
                }
                if (parsed.isObject() && parsed.has("output")) {
                    return unwrapDataNode(parsed.get("output"));
                }
                return parsed;
            } catch (Exception e) {
                return null;
            }
        }
        if (node.isArray()) {
            for (JsonNode element : node) {
                JsonNode parsed = unwrapDataNode(element);
                if (parsed != null) {
                    return parsed;
                }
            }
            return null;
        }
        if (node.isObject()) {
            if (node.has("units")) {
                return node;
            }
            JsonNode output = node.get("output");
            if (output != null) {
                JsonNode parsedOutput = unwrapDataNode(output);
                if (parsedOutput != null) {
                    return parsedOutput;
                }
            }
            JsonNode content = node.get("content");
            if (content != null) {
                JsonNode parsedContent = unwrapDataNode(content);
                if (parsedContent != null) {
                    return parsedContent;
                }
            }
            if (output != null || content != null) {
                return null;
            }
            return node;
        }
        return node;
    }

    private String normalizeJsonText(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.startsWith("```") ) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
        }
        if (trimmed.endsWith("````")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
        }
        if (trimmed.endsWith("```") ) {
            trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
        }
        int braceIndex = trimmed.indexOf('{');
        int bracketIndex = trimmed.indexOf('[');
        int startIndex = -1;
        if (braceIndex >= 0 && bracketIndex >= 0) {
            startIndex = Math.min(braceIndex, bracketIndex);
        } else if (braceIndex >= 0) {
            startIndex = braceIndex;
        } else if (bracketIndex >= 0) {
            startIndex = bracketIndex;
        }
        if (startIndex > 0) {
            trimmed = trimmed.substring(startIndex);
        }
        int lastBrace = trimmed.lastIndexOf('}');
        int lastBracket = trimmed.lastIndexOf(']');
        int endIndex = Math.max(lastBrace, lastBracket);
        if (endIndex > 0 && endIndex < trimmed.length() - 1) {
            trimmed = trimmed.substring(0, endIndex + 1);
        }
        return trimmed;
    }

    // ============ 文本切片 ============

    private String extractSectionAFromFullText(String fullText) {
        if (fullText == null || fullText.isEmpty()) {
            throw new IllegalArgumentException("未读取到试卷文本内容，请检查文件是否为扫描版或是否损坏");
        }
        String normalized = fullText.replace("\r", "");
        String upper = normalized.toUpperCase();

        int partIndex = upper.indexOf("PART III");
        if (partIndex < 0) {
            partIndex = upper.indexOf("PART IN");
        }
        if (partIndex < 0) {
            return "";
        }

        int sectionAIndex = upper.indexOf("SECTION A", partIndex);
        if (sectionAIndex < 0) {
            return "";
        }

        int sectionBIndex = upper.indexOf("SECTION B", sectionAIndex + 1);
        if (sectionBIndex < 0) {
            sectionBIndex = upper.indexOf("SECTION C", sectionAIndex + 1);
        }
        if (sectionBIndex < 0) {
            int nextPartIndex = upper.indexOf("PART IV", sectionAIndex + 1);
            if (nextPartIndex < 0) {
                nextPartIndex = upper.indexOf("PART ", sectionAIndex + 1);
            }
            sectionBIndex = nextPartIndex > sectionAIndex ? nextPartIndex : normalized.length();
        }
        return normalized.substring(sectionAIndex, sectionBIndex).trim();
    }

    private String extractSectionBFromFullText(String fullText) {
        if (fullText == null || fullText.isEmpty()) {
            throw new IllegalArgumentException("未读取到试卷文本内容，请检查文件是否为扫描版或是否损坏");
        }
        String normalized = fullText.replace("\r", "");
        String upper = normalized.toUpperCase();

        int partIndex = upper.indexOf("PART III");
        if (partIndex < 0) {
            partIndex = upper.indexOf("PART IN");
        }
        if (partIndex < 0) {
            throw new IllegalArgumentException("未找到匹配的内容");
        }

        int sectionBIndex = upper.indexOf("SECTION B", partIndex);
        if (sectionBIndex < 0) {
            return "";
        }
        int sectionCIndex = upper.indexOf("SECTION C", sectionBIndex + 1);
        if (sectionCIndex < 0) {
            int nextPartIndex = upper.indexOf("PART IV", sectionBIndex + 1);
            if (nextPartIndex < 0) {
                nextPartIndex = upper.indexOf("PART ", sectionBIndex + 1);
            }
            sectionCIndex = nextPartIndex > sectionBIndex ? nextPartIndex : normalized.length();
        }
        return normalized.substring(sectionBIndex, sectionCIndex).trim();
    }

    private String extractSectionCFromFullText(String fullText) {
        if (fullText == null || fullText.isEmpty()) {
            throw new IllegalArgumentException("未读取到试卷文本内容，请检查文件是否为扫描版或是否损坏");
        }
        String normalized = fullText.replace("\r", "");
        String upper = normalized.toUpperCase();

        int partIndex = upper.indexOf("PART III");
        if (partIndex < 0) {
            partIndex = upper.indexOf("PART IN");
        }
        if (partIndex < 0) {
            throw new IllegalArgumentException("未找到匹配的内容");
        }
        int sectionCIndex = upper.indexOf("SECTION C", partIndex);
        if (sectionCIndex < 0) {
            return "";
        }
        int nextPartIndex = upper.indexOf("PART IV", sectionCIndex + 1);
        if (nextPartIndex < 0) {
            nextPartIndex = upper.indexOf("PART ", sectionCIndex + 1);
        }
        int endIndex = nextPartIndex > sectionCIndex ? nextPartIndex : normalized.length();
        return normalized.substring(sectionCIndex, endIndex).trim();
    }

    private String extractPart1FromFullText(String fullText) {
        if (fullText == null || fullText.isEmpty()) {
            return "";
        }
        String normalized = fullText.replace("\r", "");
        String upper = normalized.toUpperCase();

        String romanOrNumber = "(?:[IVX]+|[ⅠⅡⅢⅣⅤⅥⅦⅧⅨX]+|[0-9]+|ONE|TWO|THREE|FOUR|FIVE)";
        String writingLoose = looseWordPattern("WRITING");
        Pattern startPat = Pattern.compile(
                "PART\\s*" + romanOrNumber + "\\s*(?:" + writingLoose + "|写作)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        String listeningLoose = looseWordPattern("LISTENING");
        String comprehensionLoose = looseWordPattern("COMPREHENSION");
        Pattern endPat = Pattern.compile(
                "PART\\s*" + romanOrNumber + "\\s*(?:" + listeningLoose + ")(?:\\s+" + comprehensionLoose + ")?",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

        Matcher startM = startPat.matcher(upper);
        if (!startM.find()) {
            Pattern fallbackStart = Pattern.compile("(?:" + writingLoose + "|写作)",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            startM = fallbackStart.matcher(upper);
            if (!startM.find()) {
                return "";
            }
        }
        int startIdx = startM.end();

        Matcher endM = endPat.matcher(upper);
        if (!endM.find(startIdx)) {
            return normalized.substring(startIdx).trim();
        }
        int endIdx = endM.start();

        return normalized.substring(startIdx, Math.max(startIdx, endIdx)).trim();
    }

    private String looseWordPattern(String word) {
        if (word == null || word.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        char[] chars = word.toUpperCase().toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            String group;
            switch (c) {
                case 'I':
                    group = "[I1L]";
                    break;
                case 'L':
                    group = "[L1I]";
                    break;
                case 'O':
                    group = "[O0]";
                    break;
                case 'S':
                    group = "[S5]";
                    break;
                case 'B':
                    group = "[B8]";
                    break;
                case 'Z':
                    group = "[Z2]";
                    break;
                case 'G':
                    group = "[G6]";
                    break;
                default:
                    group = String.valueOf(c);
            }
            sb.append(group);
            if (i != chars.length - 1) {
                sb.append("\\s*");
            }
        }
        return sb.toString();
    }

    private String extractPart2FromFullTextPart2(String fullText) {
        if (fullText == null || fullText.isEmpty()) {
            return "";
        }
        String normalized = fullText.replace("\r", "");
        String upper = normalized.toUpperCase();

        Pattern pStrict = Pattern.compile("PART\\s+II\\b.*?(LISTENING\\s+COMPREHENSION)?", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher mStrict = pStrict.matcher(upper);
        int startIdx = -1;
        if (mStrict.find()) {
            startIdx = mStrict.end();
        } else {
            Pattern pLoose = Pattern.compile("PART\\s*II\\b", Pattern.CASE_INSENSITIVE);
            Matcher mLoose = pLoose.matcher(upper);
            if (mLoose.find()) {
                startIdx = mLoose.end();
            }
        }
        if (startIdx < 0) {
            return "";
        }

        int nextPartIdx = upper.indexOf("PART III", startIdx);
        if (nextPartIdx < 0) nextPartIdx = upper.indexOf("PART IN", startIdx);
        if (nextPartIdx < 0) nextPartIdx = upper.indexOf("PART Ⅲ", startIdx);
        int endIdx = nextPartIdx > startIdx ? nextPartIdx : normalized.length();
        return normalized.substring(startIdx, endIdx).trim();
    }

    private String[] splitPart2IntoSections(String part2Text) {
        if (part2Text == null) return new String[]{"", "", ""};
        String normalized = part2Text.replace("\r", "");
        String upper = normalized.toUpperCase();

        int bIdx = upper.indexOf("SECTION B");
        int cIdx = upper.indexOf("SECTION C");

        String sectionA = "";
        String sectionB = "";
        String sectionC = "";

        if (bIdx >= 0 && cIdx >= 0) {
            sectionA = normalized.substring(0, bIdx).trim();
            sectionB = normalized.substring(bIdx, cIdx).trim();
            sectionC = normalized.substring(cIdx).trim();
        } else if (bIdx >= 0) {
            sectionA = normalized.substring(0, bIdx).trim();
            sectionB = normalized.substring(bIdx).trim();
        } else if (cIdx >= 0) {
            sectionA = normalized.substring(0, cIdx).trim();
            sectionC = normalized.substring(cIdx).trim();
        } else {
            sectionA = normalized.trim();
        }

        return new String[]{sectionA, sectionB, sectionC};
    }

    private String extractPart4FromFullText(String fullText) {
        if (fullText == null || fullText.isEmpty()) {
            return "";
        }
        String normalized = fullText.replace("\r", "");
        String upper = normalized.toUpperCase();

        Pattern startPat = Pattern.compile(
                "PART\\s+\\S+\\s+TRANSLATION", Pattern.CASE_INSENSITIVE);
        Matcher m = startPat.matcher(upper);
        if (!m.find()) {
            return "";
        }
        int startIdx = m.start();
        return normalized.substring(startIdx).trim();
    }

    // ============ Section A 规范化 ============

    private Map<String, Object> normalizeSectionA(JsonNode node) {
        Map<String, Object> normalized = new HashMap<>();

        String segmentId = node.has("segment_id") ? node.get("segment_id").asText("") : "";
        if (!segmentId.isEmpty()) {
            normalized.put("segment_id", segmentId);
        }

        String passage = extractPassageField(node);
        List<String> blankNumbers = determineBlankNumbers(node, passage);
        normalized.put("passage", passage);
        normalized.put("blank_numbers", blankNumbers);
        int blankCount = !blankNumbers.isEmpty() ? blankNumbers.size() : determineBlankCount(node);
        normalized.put("blank_count", blankCount);
        normalized.put("start_number", determineStartNumber(node, blankNumbers));
        List<Map<String, String>> options = new ArrayList<>();
        if (node.has("word_options") && node.get("word_options").isArray()) {
            JsonNode wordOptions = node.get("word_options");
            for (JsonNode optionNode : wordOptions) {
                Map<String, String> option = new HashMap<>();
                option.put("letter", extractOptionLetter(optionNode));
                option.put("word", extractOptionWord(optionNode));
                options.add(option);
            }
        } else {
            options = extractOptions(node);
        }
        normalized.put("options", options);

        List<Map<String, String>> answers = new ArrayList<>();
        if (node.has("answers") && node.get("answers").isArray()) {
            JsonNode answersArray = node.get("answers");
            for (JsonNode answerNode : answersArray) {
                Map<String, String> answer = new HashMap<>();
                answer.put("question_number", answerNode.has("question_number")
                        ? answerNode.get("question_number").asText("") : "");
                answer.put("answer", answerNode.has("answer")
                        ? answerNode.get("answer").asText("") : "");
                answers.add(answer);
            }
        }

        if (!answers.isEmpty()) {
            normalized.put("answers", answers);
        }

        return normalized;
    }

    private String extractPassageField(JsonNode node) {
        JsonNode valueNode = node.get("passage");
        if (valueNode != null && !valueNode.isNull()) {
            return valueNode.asText();
        }
        List<String> candidates = Arrays.asList("passage_content", "text", "article", "content");
        for (String candidate : candidates) {
            valueNode = node.get(candidate);
            if (valueNode != null && !valueNode.isNull()) {
                return valueNode.asText();
            }
        }
        return "[文档中未提及此内容]";
    }

    private int determineBlankCount(JsonNode node) {
        JsonNode explicit = firstNonNull(node.get("blank_count"), node.get("blankCount"), node.get("count"));
        if (explicit != null && explicit.isInt()) {
            return Math.max(explicit.asInt(), 0);
        }
        JsonNode blanks = firstNonNull(node.get("blanks"), node.get("questions"), node.get("items"));
        if (blanks != null && blanks.isArray()) {
            return blanks.size();
        }
        return 0;
    }

    private int determineStartNumber(JsonNode node, List<String> blankNumbers) {
        Integer fromList = extractFirstInteger(blankNumbers);
        if (fromList != null) {
            return fromList;
        }
        JsonNode numbersArray = firstNonNull(node.get("question_numbers"), node.get("questionNumbers"));
        if (numbersArray != null && numbersArray.isArray() && numbersArray.size() > 0) {
            JsonNode first = numbersArray.get(0);
            if (first != null && first.isInt()) {
                return first.asInt();
            }
            if (first != null && first.isTextual()) {
                try {
                    return Integer.parseInt(first.asText().replaceAll("\\D", ""));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        JsonNode blanks = firstNonNull(node.get("blanks"), node.get("questions"), node.get("items"));
        if (blanks != null && blanks.isArray()) {
            for (JsonNode item : blanks) {
                JsonNode numberNode = firstNonNull(
                        item.get("question_number"),
                        item.get("blank_number"),
                        item.get("number")
                );
                if (numberNode != null) {
                    if (numberNode.isInt()) {
                        return numberNode.asInt();
                    }
                    if (numberNode.isTextual()) {
                        try {
                            return Integer.parseInt(numberNode.asText().replaceAll("\\D", ""));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        }
        return 1;
    }

    private List<String> determineBlankNumbers(JsonNode node, String passage) {
        List<Integer> fromPassage = extractNumbersFromPassage(passage);
        if (!fromPassage.isEmpty()) {
            return convertToStringList(fromPassage);
        }
        return Collections.emptyList();
    }

    private Integer parseIntegerFromText(String text) {
        if (text == null) {
            return null;
        }
        String digits = text.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private List<Integer> extractNumbersFromPassage(String passage) {
        if (passage == null || passage.isBlank()) {
            return Collections.emptyList();
        }
        Matcher matcher = UNDERSCORE_NUMBER_PATTERN.matcher(passage);
        Set<Integer> numbers = new TreeSet<>();
        while (matcher.find()) {
            Integer parsed = parseIntegerFromText(matcher.group(1));
            if (parsed != null) {
                numbers.add(parsed);
            }
        }
        return numbers.isEmpty() ? Collections.emptyList() : new ArrayList<>(numbers);
    }

    private Integer extractFirstInteger(List<String> inputs) {
        if (inputs == null) {
            return null;
        }
        for (String value : inputs) {
            Integer parsed = parseIntegerFromText(value);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private List<String> convertToStringList(List<Integer> numbers) {
        if (numbers == null || numbers.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>(numbers.size());
        for (Integer number : numbers) {
            result.add(String.valueOf(number));
        }
        return result;
    }

    private List<Map<String, String>> extractOptions(JsonNode node) {
        JsonNode optionsArray = firstNonNull(
                node.get("word_options"),
                node.get("wordOptions"),
                node.get("options"),
                node.get("choices")
        );
        List<Map<String, String>> options = new ArrayList<>();
        if (optionsArray != null && optionsArray.isArray()) {
            for (JsonNode optionNode : optionsArray) {
                Map<String, String> option = new HashMap<>();
                option.put("letter", extractOptionLetter(optionNode));
                option.put("word", extractOptionWord(optionNode));
                options.add(option);
            }
        }
        return options;
    }

    private String extractOptionLetter(JsonNode node) {
        JsonNode letterNode = firstNonNull(
                node.get("letter"),
                node.get("option_mark"),
                node.get("option"),
                node.get("mark")
        );
        if (letterNode != null && letterNode.isTextual()) {
            return letterNode.asText().trim();
        }
        if (letterNode != null && letterNode.isInt()) {
            return String.valueOf(letterNode.asInt());
        }
        return "";
    }

    private String extractOptionWord(JsonNode node) {
        JsonNode wordNode = firstNonNull(
                node.get("word"),
                node.get("option_content"),
                node.get("text"),
                node.get("content")
        );
        if (wordNode != null && wordNode.isTextual() && !wordNode.asText().isBlank()) {
            return wordNode.asText().trim();
        }
        return "[文档中未提及此内容]";
    }

    private JsonNode extractSectionA(JsonNode root) {
        if (root.has("output") && root.get("output").isTextual()) {
            try {
                String outputStr = root.get("output").asText();
                JsonNode outputNode = objectMapper.readTree(outputStr);
                return extractSectionA(outputNode);
            } catch (Exception ignored) {
            }
        }
        if (root.has("blank_count") && root.has("passage") && root.has("word_options")) {
            return root;
        }
        if (root.has("reading_comprehension")) {
            JsonNode reading = root.get("reading_comprehension");
            if (reading != null && reading.has("section_a")) {
                return reading.get("section_a");
            }
        }
        if (root.has("output") && root.get("output").isObject()) {
            JsonNode output = root.get("output");
            if (output != null && output.has("reading_comprehension")) {
                JsonNode reading = output.get("reading_comprehension");
                if (reading != null && reading.has("section_a")) {
                    return reading.get("section_a");
                }
            }
        }
        JsonNode fromKnownPaths = findSectionAInKnownPaths(root);
        if (fromKnownPaths != null) {
            return fromKnownPaths;
        }
        return findSectionARecursive(root);
    }

    private JsonNode findSectionAInKnownPaths(JsonNode root) {
        JsonNode reading = firstNonNull(
                root.get("reading_comprehension"),
                root.get("readingComprehension"),
                root.get("reading"),
                root.get("readingSection")
        );
        if (reading != null && !reading.isMissingNode() && !reading.isNull()) {
            JsonNode sectionA = firstNonNull(reading.get("section_a"), reading.get("sectionA"));
            if (isValidNode(sectionA)) {
                return sectionA;
            }
            JsonNode sections = reading.get("sections");
            if (sections != null && sections.isArray()) {
                for (JsonNode section : sections) {
                    if (isClozeNode(section)) {
                        return section;
                    }
                }
            }
        }
        JsonNode direct = firstNonNull(root.get("section_a"), root.get("sectionA"));
        if (isValidNode(direct)) {
            return direct;
        }
        return null;
    }

    private JsonNode findSectionARecursive(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            if (isClozeNode(node)) {
                return node;
            }
            for (JsonNode child : node) {
                JsonNode found = findSectionARecursive(child);
                if (found != null) {
                    return found;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode found = findSectionARecursive(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private boolean isClozeNode(JsonNode node) {
        if (node == null || !node.isObject()) {
            return false;
        }
        return isClozeField(node.get("question_type")) ||
                isClozeField(node.get("type")) ||
                isClozeField(node.get("label")) ||
                isClozeField(node.get("name")) ||
                isClozeField(node.get("title"));
    }

    private boolean isClozeField(JsonNode node) {
        return node != null && node.isTextual() && isClozeType(node.asText());
    }

    private boolean isClozeType(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.replaceAll("\\s+", "").toLowerCase();
        if (normalized.isEmpty()) {
            return false;
        }
        return CLOZE_KEYWORDS.stream().anyMatch(keyword -> normalized.contains(keyword));
    }

    private boolean isValidNode(JsonNode node) {
        return node != null && !node.isMissingNode() && !node.isNull();
    }

    private JsonNode firstNonNull(JsonNode... nodes) {
        for (JsonNode node : nodes) {
            if (isValidNode(node)) {
                return node;
            }
        }
        return null;
    }

    // Section B
    private JsonNode extractSectionB(JsonNode root) {
        if (root.has("output") && root.get("output").isTextual()) {
            try {
                String outputStr = root.get("output").asText();
                JsonNode outputNode = objectMapper.readTree(outputStr);
                return extractSectionB(outputNode);
            } catch (Exception ignored) {
            }
        }
        if (root.has("question_type") && root.has("article") && root.has("statements")) {
            return root;
        }
        if (root.has("reading_comprehension")) {
            JsonNode reading = root.get("reading_comprehension");
            if (reading != null && reading.has("section_b")) {
                return reading.get("section_b");
            }
        }
        if (root.has("output") && root.get("output").isObject()) {
            JsonNode output = root.get("output");
            if (output != null && output.has("reading_comprehension")) {
                JsonNode reading = output.get("reading_comprehension");
                if (reading != null && reading.has("section_b")) {
                    return reading.get("section_b");
                }
            }
        }
        JsonNode direct = firstNonNull(root.get("section_b"), root.get("sectionB"));
        if (isValidNode(direct)) {
            return direct;
        }
        return null;
    }

    private Map<String, Object> normalizeSectionB(JsonNode node) {
        Map<String, Object> normalized = new HashMap<>();

        String segmentId = node.has("segment_id") ? node.get("segment_id").asText("") : "";
        if (!segmentId.isEmpty()) {
            normalized.put("segment_id", segmentId);
        }

        String questionType = node.has("question_type") ? node.get("question_type").asText("段落匹配") : "段落匹配";
        normalized.put("question_type", questionType);

        String promptRequirement = node.has("prompt_requirement")
                ? node.get("prompt_requirement").asText()
                : "一篇含13个段落（A-M）的文章，10个陈述句（第36-45题），找出每个陈述句对应的段落来源，段落可重复选择";
        normalized.put("prompt_requirement", promptRequirement);

        List<Map<String, String>> article = new ArrayList<>();
        if (node.has("article") && node.get("article").isArray()) {
            JsonNode articleArray = node.get("article");
            for (JsonNode paragraphNode : articleArray) {
                Map<String, String> paragraph = new HashMap<>();
                paragraph.put("paragraph_mark", paragraphNode.has("paragraph_mark")
                        ? paragraphNode.get("paragraph_mark").asText("") : "");
                paragraph.put("paragraph_content", paragraphNode.has("paragraph_content")
                        ? paragraphNode.get("paragraph_content").asText("") : "");
                article.add(paragraph);
            }
        }
        normalized.put("article", article);

        int statementCount = node.has("statement_count") ? node.get("statement_count").asInt(10) : 10;
        normalized.put("statement_count", statementCount);

        List<Map<String, String>> statements = new ArrayList<>();
        if (node.has("statements") && node.get("statements").isArray()) {
            JsonNode statementsArray = node.get("statements");
            for (JsonNode statementNode : statementsArray) {
                Map<String, String> statement = new HashMap<>();
                statement.put("question_number", statementNode.has("question_number")
                        ? statementNode.get("question_number").asText("") : "");
                statement.put("statement_content", statementNode.has("statement_content")
                        ? statementNode.get("statement_content").asText("") : "");
                statements.add(statement);
            }
        }
        normalized.put("statements", statements);

        List<Map<String, String>> answers = new ArrayList<>();
        if (node.has("answers") && node.get("answers").isArray()) {
            JsonNode answersArray = node.get("answers");
            for (JsonNode answerNode : answersArray) {
                Map<String, String> answer = new HashMap<>();
                answer.put("question_number", answerNode.has("question_number")
                        ? answerNode.get("question_number").asText("") : "");
                answer.put("answer", answerNode.has("answer")
                        ? answerNode.get("answer").asText("") : "");
                answers.add(answer);
            }
        }

        if (!answers.isEmpty()) {
            normalized.put("answers", answers);
        }

        return normalized;
    }

    // Section C
    private JsonNode extractSectionC1(JsonNode root) {
        if (root.has("output") && root.get("output").isTextual()) {
            try {
                String outputStr = root.get("output").asText();
                JsonNode outputNode = objectMapper.readTree(outputStr);
                return extractSectionC1(outputNode);
            } catch (Exception e) {
            }
        }
        JsonNode direct = firstNonNull(root.get("section_c1"), root.get("sectionC1"));
        if (isValidNode(direct)) {
            return direct;
        }
        if (root.has("reading_comprehension")) {
            JsonNode reading = root.get("reading_comprehension");
            if (reading != null && reading.has("section_c1")) {
                return reading.get("section_c1");
            }
            if (reading != null && reading.has("section_c")) {
                JsonNode sectionC = reading.get("section_c");
                if (sectionC != null && sectionC.has("section_c1")) {
                    return sectionC.get("section_c1");
                }
            }
        }
        if (root.has("output") && root.get("output").isObject()) {
            JsonNode output = root.get("output");
            JsonNode outputDirect = firstNonNull(output.get("section_c1"), output.get("sectionC1"));
            if (isValidNode(outputDirect)) {
                return outputDirect;
            }
            if (output != null && output.has("reading_comprehension")) {
                JsonNode reading = output.get("reading_comprehension");
                if (reading != null && reading.has("section_c1")) {
                    return reading.get("section_c1");
                }
                if (reading != null && reading.has("section_c")) {
                    JsonNode sectionC = reading.get("section_c");
                    if (sectionC != null && sectionC.has("section_c1")) {
                        return sectionC.get("section_c1");
                    }
                }
            }
        }
        return null;
    }

    private JsonNode extractSectionC2(JsonNode root) {
        if (root.has("output") && root.get("output").isTextual()) {
            try {
                String outputStr = root.get("output").asText();
                JsonNode outputNode = objectMapper.readTree(outputStr);
                return extractSectionC2(outputNode);
            } catch (Exception e) {
            }
        }
        JsonNode direct = firstNonNull(root.get("section_c2"), root.get("sectionC2"));
        if (isValidNode(direct)) {
            return direct;
        }
        if (root.has("reading_comprehension")) {
            JsonNode reading = root.get("reading_comprehension");
            if (reading != null && reading.has("section_c2")) {
                return reading.get("section_c2");
            }
            if (reading != null && reading.has("section_c")) {
                JsonNode sectionC = reading.get("section_c");
                if (sectionC != null && sectionC.has("section_c2")) {
                    return sectionC.get("section_c2");
                }
            }
        }
        if (root.has("output") && root.get("output").isObject()) {
            JsonNode output = root.get("output");
            JsonNode outputDirect = firstNonNull(output.get("section_c2"), output.get("sectionC2"));
            if (isValidNode(outputDirect)) {
                return outputDirect;
            }
            if (output != null && output.has("reading_comprehension")) {
                JsonNode reading = output.get("reading_comprehension");
                if (reading != null && reading.has("section_c2")) {
                    return reading.get("section_c2");
                }
                if (reading != null && reading.has("section_c")) {
                    JsonNode sectionC = reading.get("section_c");
                    if (sectionC != null && sectionC.has("section_c2")) {
                        return sectionC.get("section_c2");
                    }
                }
            }
        }
        return null;
    }

    private Map<String, Object> normalizeSectionC1(JsonNode node) {
        Map<String, Object> normalized = new HashMap<>();

        String segmentId = node.has("segment_id") ? node.get("segment_id").asText("") : "";
        if (!segmentId.isEmpty()) {
            normalized.put("segment_id", segmentId);
        }

        String questionType = node.has("question_type") ? node.get("question_type").asText("篇章阅读") : "篇章阅读";
        normalized.put("question_type", questionType);

        String promptRequirement = node.has("prompt_requirement")
                ? node.get("prompt_requirement").asText()
                : "1篇独立短文（Passage One），每篇对应5个问题（第46-50题）";
        normalized.put("prompt_requirement", promptRequirement);

        String topic = node.has("topic") ? node.get("topic").asText("") : "";
        normalized.put("topic", topic);

        String passageMark = node.has("passage_mark") ? node.get("passage_mark").asText("Passage One") : "Passage One";
        normalized.put("passage_mark", passageMark);

        String passageContent = node.has("passage_content") ? node.get("passage_content").asText("") : "";
        normalized.put("passage_content", passageContent);

        int questionCount = node.has("question_count") ? node.get("question_count").asInt(5) : 5;
        normalized.put("question_count", questionCount);

        List<Map<String, Object>> questions = new ArrayList<>();
        if (node.has("questions") && node.get("questions").isArray()) {
            JsonNode questionsArray = node.get("questions");
            for (JsonNode questionNode : questionsArray) {
                Map<String, Object> question = new HashMap<>();
                question.put("question_number", questionNode.has("question_number")
                        ? questionNode.get("question_number").asText("") : "");
                question.put("question_content", questionNode.has("question_content")
                        ? questionNode.get("question_content").asText("") : "");

                List<Map<String, String>> options = new ArrayList<>();
                if (questionNode.has("options") && questionNode.get("options").isArray()) {
                    JsonNode optionsArray = questionNode.get("options");
                    for (JsonNode optionNode : optionsArray) {
                        Map<String, String> option = new HashMap<>();
                        option.put("option_mark", optionNode.has("option_mark")
                                ? optionNode.get("option_mark").asText("") : "");
                        option.put("option_content", optionNode.has("option_content")
                                ? optionNode.get("option_content").asText("") : "");
                        options.add(option);
                    }
                }
                question.put("options", options);
                questions.add(question);
            }
        }
        normalized.put("questions", questions);

        List<Map<String, String>> answers = new ArrayList<>();
        if (node.has("answers") && node.get("answers").isArray()) {
            JsonNode answersArray = node.get("answers");
            for (JsonNode answerNode : answersArray) {
                Map<String, String> answer = new HashMap<>();
                answer.put("question_number", answerNode.has("question_number")
                        ? answerNode.get("question_number").asText("") : "");
                answer.put("answer", answerNode.has("answer")
                        ? answerNode.get("answer").asText("") : "");
                answers.add(answer);
            }
        }

        if (!answers.isEmpty()) {
            normalized.put("answers", answers);
        }

        return normalized;
    }

    private Map<String, Object> normalizeSectionC2(JsonNode node) {
        Map<String, Object> normalized = new HashMap<>();

        String segmentId = node.has("segment_id") ? node.get("segment_id").asText("") : "";
        if (!segmentId.isEmpty()) {
            normalized.put("segment_id", segmentId);
        }

        String questionType = node.has("question_type") ? node.get("question_type").asText("篇章阅读") : "篇章阅读";
        normalized.put("question_type", questionType);

        String promptRequirement = node.has("prompt_requirement")
                ? node.get("prompt_requirement").asText()
                : "1篇独立短文（Passage Two），每篇对应5个问题（第51-55题）";
        normalized.put("prompt_requirement", promptRequirement);

        String topic = node.has("topic") ? node.get("topic").asText("") : "";
        normalized.put("topic", topic);

        String passageMark = node.has("passage_mark") ? node.get("passage_mark").asText("Passage Two") : "Passage Two";
        normalized.put("passage_mark", passageMark);

        String passageContent = node.has("passage_content") ? node.get("passage_content").asText("") : "";
        normalized.put("passage_content", passageContent);

        int questionCount = node.has("question_count") ? node.get("question_count").asInt(5) : 5;
        normalized.put("question_count", questionCount);

        List<Map<String, Object>> questions = new ArrayList<>();
        if (node.has("questions") && node.get("questions").isArray()) {
            JsonNode questionsArray = node.get("questions");
            for (JsonNode questionNode : questionsArray) {
                Map<String, Object> question = new HashMap<>();
                question.put("question_number", questionNode.has("question_number")
                        ? questionNode.get("question_number").asText("") : "");
                question.put("question_content", questionNode.has("question_content")
                        ? questionNode.get("question_content").asText("") : "");

                List<Map<String, String>> options = new ArrayList<>();
                if (questionNode.has("options") && questionNode.get("options").isArray()) {
                    JsonNode optionsArray = questionNode.get("options");
                    for (JsonNode optionNode : optionsArray) {
                        Map<String, String> option = new HashMap<>();
                        option.put("option_mark", optionNode.has("option_mark")
                                ? optionNode.get("option_mark").asText("") : "");
                        option.put("option_content", optionNode.has("option_content")
                                ? optionNode.get("option_content").asText("") : "");
                        options.add(option);
                    }
                }
                question.put("options", options);
                questions.add(question);
            }
        }
        normalized.put("questions", questions);

        List<Map<String, String>> answers = new ArrayList<>();
        if (node.has("answers") && node.get("answers").isArray()) {
            JsonNode answersArray = node.get("answers");
            for (JsonNode answerNode : answersArray) {
                Map<String, String> answer = new HashMap<>();
                answer.put("question_number", answerNode.has("question_number")
                        ? answerNode.get("question_number").asText("") : "");
                answer.put("answer", answerNode.has("answer")
                        ? answerNode.get("answer").asText("") : "");
                answers.add(answer);
            }
        }

        if (!answers.isEmpty()) {
            normalized.put("answers", answers);
        }

        return normalized;
    }

    private <T> T safeExtract(SupplierWithException<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return null;
        }
    }

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get() throws Exception;
    }
}
