package seucxxy.csd.backend.hs3.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seucxxy.csd.backend.hs3.entity.HS3ExamPaperEn;
import seucxxy.csd.backend.hs3.mapper.HS3ExamPaperEnMapper;
import seucxxy.csd.backend.hs3.mapper.HS3TopicMapper;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HS3 高考英语试卷生成服务
 * 负责参数整理与生成逻辑调度
 */
@Service
public class HS3PaperGenService {

    private static final Logger logger = LoggerFactory.getLogger(HS3PaperGenService.class);

    private final HS3ExamPaperEnMapper examPaperEnMapper;
    private final HS3ChromaEngExamPaperService chromaService;
    private final HS3TopicMapper topicMapper;
    private final HS3PaperGenerationCommonService paperGenerationCommonService;
    private final Random random = new Random();

    @Autowired
    public HS3PaperGenService(HS3ExamPaperEnMapper examPaperEnMapper,
                              HS3ChromaEngExamPaperService chromaService,
                              HS3TopicMapper topicMapper,
                              HS3PaperGenerationCommonService paperGenerationCommonService) {
        this.examPaperEnMapper = examPaperEnMapper;
        this.chromaService = chromaService;
        this.topicMapper = topicMapper;
        this.paperGenerationCommonService = paperGenerationCommonService;
    }

    /**
     * 生成高考英语试卷
     * 
     * @param subjectEn 科目（高考）
     * @param source 来源（AIfromreal等）
     * @param userId 用户ID
     * @return 生成结果，包含segments列表
     */
    public Map<String, Object> generatePaper(String subjectEn, String source, Long userId) throws Exception {
        String effectiveSource = (source == null || source.isBlank()) ? "AIfromreal" : source;
        logger.info("[HS3 PaperGen] 开始生成试卷, subject={}, source={}, userId={}", subjectEn, effectiveSource, userId);

        // 1. 获取所有topic并打乱
        List<String> allTopics = getAllTopicsShuffled();

        // 2. 从数据库获取高考真题试卷ID列表
        List<Long> realPaperIds = examPaperEnMapper.findIdsBySubjectAndSource(subjectEn, "real");
        if (realPaperIds == null || realPaperIds.isEmpty()) {
            throw new RuntimeException("数据库中没有科目为 " + subjectEn + " 且来源为 real 的真题试卷");
        }

        // 3. 随机选择一套真题作为模板
        Long templatePaperId = realPaperIds.get(random.nextInt(realPaperIds.size()));
        logger.info("[HS3 PaperGen] 选中模板试卷ID={}", templatePaperId);

        // 4. 从ChromaDB获取模板试卷的所有segments
        List<Map<String, Object>> templateSegments = chromaService.fetchExamPaperSegments(
                String.valueOf(templatePaperId), null);
        if (templateSegments == null || templateSegments.isEmpty()) {
            throw new RuntimeException("ChromaDB中没有找到试卷ID为 " + templatePaperId + " 的片段");
        }

        logger.info("[HS3 PaperGen] 从ChromaDB获取到 {} 个模板片段", templateSegments.size());

        // 5. 为每个segment分配topic
        List<String> assignedTopics = new ArrayList<>();
        for (int i = 0; i < templateSegments.size(); i++) {
            assignedTopics.add(allTopics.get(i % allTopics.size()));
        }

        // 6. 并行调用Coze生成新题目
        int poolSize = Math.max(1, Math.min(templateSegments.size(), Runtime.getRuntime().availableProcessors()));
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();
        AtomicInteger unitIndex = new AtomicInteger(0);

        try {
            for (Map<String, Object> segment : templateSegments) {
                final int currentIndex = unitIndex.getAndIncrement();
                final String topic = assignedTopics.get(currentIndex);
                final Map<String, Object> segmentSnapshot = new HashMap<>(segment);

                CompletableFuture<Map<String, Object>> future = CompletableFuture.supplyAsync(
                        () -> generateSingleSegment(segmentSnapshot, topic, effectiveSource, currentIndex),
                        executor
                );
                futures.add(future);
            }

            // 等待所有任务完成
            CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]));
            try {
                all.join();
            } catch (CompletionException ex) {
                throw unwrap(ex);
            }

            // 收集结果
            List<Map<String, Object>> generatedSegments = new ArrayList<>();
            for (CompletableFuture<Map<String, Object>> future : futures) {
                generatedSegments.add(future.join());
            }

            // 按索引排序
            generatedSegments.sort((a, b) -> 
                Integer.compare((Integer) a.get("segment_index"), (Integer) b.get("segment_index")));

            logger.info("[HS3 PaperGen] 成功生成 {} 个片段", generatedSegments.size());

            Map<String, Object> result = new HashMap<>();
            result.put("examPaperEnId", null); // 预览模式不创建记录
            result.put("segments", generatedSegments);
            result.put("exam_paper_en_source", effectiveSource);
            return result;

        } finally {
            executor.shutdown();
        }
    }

    /**
     * 生成单个segment
     */
    private Map<String, Object> generateSingleSegment(Map<String, Object> segment, 
                                                       String topic, 
                                                       String examPaperEnSource,
                                                       int currentIndex) {
        try {
            // 从segment中提取必要信息
            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = (Map<String, Object>) segment.get("metadata");
            String document = (String) segment.get("document");

            if (metadata == null) {
                metadata = new HashMap<>();
            }

            String partName = getStringValue(metadata, "part_name", "");
            Integer partNumber = getIntValue(metadata, "part_number", 1);
            String sectionName = getStringValue(metadata, "section_name", "");
            Integer sectionNumber = getIntValue(metadata, "section_number", 1);
            Integer segmentNumber = getIntValue(metadata, "segment_number", 1);
            String segmentName = getStringValue(metadata, "segment_name", "");
            Integer questionNumberStart = getIntValue(metadata, "question_number_start", 1);

            // 调用Coze工作流生成新题目
            Map<String, Object> cozeResult = paperGenerationCommonService.callCozeWorkflow(
                    topic,
                    document,
                    partName,
                    String.valueOf(partNumber),
                    sectionName,
                    String.valueOf(sectionNumber),
                    String.valueOf(segmentNumber),
                    String.valueOf(questionNumberStart)
            );

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("segment_index", currentIndex);
            result.put("segment_name", segmentName);
            result.put("part_name", partName);
            result.put("part_number", partNumber);
            result.put("section_name", sectionName);
            result.put("section_number", sectionNumber);
            result.put("segment_number", segmentNumber);
            result.put("topic", topic);
            result.put("original_document", document);
            result.put("output", cozeResult.get("output"));
            result.put("answers", cozeResult.get("answers"));
            result.put("exam_paper_en_source", examPaperEnSource);

            return result;
        } catch (Exception e) {
            throw new CompletionException("片段生成失败：" + e.getMessage(), e);
        }
    }

    /**
     * 存储segment到ChromaDB
     */
    public void storeSegmentToChroma(String examPaperId, 
                                      String examPaperName,
                                      String subject,
                                      Map<String, Object> segment,
                                      String examPaperEnSource) throws Exception {
        List<Map<String, Object>> segments = new ArrayList<>();
        segments.add(segment);
        
        chromaService.storeExamPaperSegments(
                Long.parseLong(examPaperId),
                examPaperName,
                subject,
                examPaperEnSource,
                segments
        );
    }

    /**
     * 获取所有topic并打乱顺序
     */
    private List<String> getAllTopicsShuffled() {
        List<String> allTopics = topicMapper.selectAllTopics();
        if (allTopics == null || allTopics.isEmpty()) {
            throw new RuntimeException("topics表中没有数据");
        }
        Collections.shuffle(allTopics);
        return allTopics;
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString().trim();
    }

    private Integer getIntValue(Map<String, Object> map, String key, Integer defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private RuntimeException unwrap(CompletionException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new RuntimeException(cause == null ? "并发生成失败" : cause.getMessage(), cause);
    }
}
