package seucxxy.csd.backend.hs3.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * HS3 高考英语试卷解析服务
 * 
 * 提供 Word 文件（.doc/.docx）的文本提取功能
 * 以及从ChromaDB构建展示数据等业务逻辑
 */
@Service
public class HS3PaperAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(HS3PaperAnalysisService.class);

    // 输出目录，默认为 e:\xmj\
    @Value("${hs3.paper.output.dir:e:/xmj/}")
    private String outputDir;
    
    @Autowired
    private HS3WordDocumentExtractor wordDocumentExtractor;
    
    @Autowired
    private HS3Neo4jService neo4jService;
    
    @Autowired
    private HS3ChromaEngExamPaperService chromaService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析 Word 文件，提取文本内容并输出到指定目录
     * 
     * @param file 上传的 Word 文件
     * @return 包含提取文本和输出路径的 Map
     * @throws IOException 如果文件读取或写入失败
     */
    public Map<String, Object> parseWordFile(MultipartFile file) throws IOException {
        Map<String, Object> result = new HashMap<>();
        
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("无法获取文件名");
        }
        
        String lowerName = fileName.toLowerCase();
        if (!lowerName.endsWith(".doc") && !lowerName.endsWith(".docx")) {
            throw new IllegalArgumentException("不支持的文件格式，仅支持 .doc 和 .docx");
        }
        
        // 使用 WordDocumentExtractor 提取文本
        String extractedText = wordDocumentExtractor.extractText(file);
        
        if (extractedText == null || extractedText.trim().isEmpty()) {
            throw new IllegalArgumentException("未能从文件中提取到文本内容");
        }
        
        // 生成输出文件名
        String outputFileName = generateOutputFileName(fileName);
        Path outputPath = Paths.get(outputDir, outputFileName);
        
        // 确保输出目录存在
        Files.createDirectories(outputPath.getParent());
        
        // 写入文件
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write(extractedText);
        }
        
        logger.info("[HS3] 试卷解析完成，输出文件：{}", outputPath.toAbsolutePath());
        
        result.put("text", extractedText);
        result.put("outputPath", outputPath.toAbsolutePath().toString());
        result.put("fileName", fileName);
        result.put("textLength", extractedText.length());
        
        return result;
    }
    
    /**
     * 生成输出文件名
     * 格式：原文件名_yyyyMMdd_HHmmss.txt
     */
    private String generateOutputFileName(String originalFileName) {
        // 移除扩展名
        String baseName = originalFileName;
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = originalFileName.substring(0, dotIndex);
        }
        
        // 添加时间戳
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        return baseName + "_" + timestamp + ".txt";
    }
    
    /**
     * 从ChromaDB记录构建segments列表
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> buildSegmentsFromChroma(List<Map<String, Object>> chromaRecords,
                                                             Map<String, Object> descriptions) {
        Map<String, String> partDescs = (Map<String, String>) descriptions.getOrDefault("parts", Collections.emptyMap());
        Map<String, String> sectionDescs = (Map<String, String>) descriptions.getOrDefault("sections", Collections.emptyMap());
        Map<String, String> segmentDescs = (Map<String, String>) descriptions.getOrDefault("segments", Collections.emptyMap());

        List<Map<String, Object>> segments = new ArrayList<>();

        for (Map<String, Object> record : chromaRecords) {
            String document = String.valueOf(record.getOrDefault("document", ""));
            Map<String, Object> metadata = safeCastMap(record.get("metadata"));
            Map<String, Object> segData = parseDocument(document);

            String partNumber = firstNonEmpty(
                    metadata.get("part_number"), metadata.get("partNumber"), segData.get("part_number"));
            String sectionNumber = firstNonEmpty(
                    metadata.get("section_number"), metadata.get("sectionNumber"), segData.get("section_number"));
            String segmentNumber = firstNonEmpty(
                    metadata.get("segment_number"), metadata.get("segmentNumber"), segData.get("segment_number"));

            Map<String, Object> segment = new HashMap<>();
            segment.put("partNumber", partNumber);
            segment.put("partName", firstNonEmpty(metadata.get("part_name"), metadata.get("partName"), segData.get("part_name")));
            segment.put("partDescription", partDescs.getOrDefault(stringOrEmpty(partNumber), null));

            segment.put("sectionNumber", sectionNumber);
            segment.put("sectionName", firstNonEmpty(metadata.get("section_name"), metadata.get("sectionName"), segData.get("section_name")));
            segment.put("sectionDescription", sectionDescs.getOrDefault(stringOrEmpty(partNumber) + "|" + stringOrEmpty(sectionNumber), null));

            segment.put("segmentNumber", segmentNumber);
            segment.put("segmentName", firstNonEmpty(metadata.get("segment_name"), metadata.get("segmentName"), segData.get("segment_name")));
            segment.put("topic", firstNonEmpty(segData.get("topic"), metadata.get("topic")));
            segment.put("questionData", document);
            segment.put("content", firstNonEmpty(segData.get("content"), segData.get("passage"), segData.get("article")));
            String segKey = stringOrEmpty(partNumber) + "|" + stringOrEmpty(sectionNumber) + "|" + stringOrEmpty(segmentNumber);
            segment.put("segmentDescription", segmentDescs.getOrDefault(segKey, null));

            segments.add(segment);
        }

        // 按 part -> section -> segment 排序
        segments.sort(Comparator.comparingInt((Map<String, Object> s) -> parseIntSafe(s.get("partNumber")))
                .thenComparingInt(s -> parseIntSafe(s.get("sectionNumber")))
                .thenComparingInt(s -> parseIntSafe(s.get("segmentNumber"))));

        // 标记首段
        String lastPart = null;
        String lastSection = null;
        for (Map<String, Object> seg : segments) {
            String p = stringOrEmpty(seg.get("partNumber"));
            String s = stringOrEmpty(seg.get("sectionNumber"));
            boolean firstPart = !Objects.equals(p, lastPart);
            boolean firstSection = firstPart || !Objects.equals(s, lastSection);
            seg.put("isFirstInPart", firstPart);
            seg.put("isFirstInSection", firstSection);
            lastPart = p;
            lastSection = s;
        }

        return segments;
    }
    
    // ==================== 辅助方法 ====================
    
    private Map<String, Object> parseDocument(String document) {
        if (document == null || document.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(document, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeCastMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return Collections.emptyMap();
    }

    private String firstNonEmpty(Object... values) {
        for (Object v : values) {
            if (v == null) continue;
            String s = v.toString();
            if (!s.isBlank()) return s;
        }
        return "";
    }

    private String stringOrEmpty(Object value) {
        return value == null ? "" : value.toString();
    }

    private int parseIntSafe(Object value) {
        try {
            return Integer.parseInt(stringOrEmpty(value));
        } catch (Exception e) {
            return 0;
        }
    }
}
