package seucxxy.csd.backend.cet4.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seucxxy.csd.backend.cet4.entity.CET4ExamPaperEn;
import seucxxy.csd.backend.cet4.mapper.CET4EExamPaperEnMapper;
import seucxxy.csd.backend.cet4.service.CET4ChromaEngExamPaperService;
import seucxxy.csd.backend.cet4.service.CET4PaperGenerationProgressListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CET4 试卷生成入口（并发版封装）。
 * 负责参数整理与落库，其余生成逻辑交由并行服务。
 */
@Service
public class CET4PaperGenService {

    private final CET4EExamPaperEnMapper examPaperEnMapper;
    private final CET4ChromaEngExamPaperService chromaService;
    private final CET4PaperGenerationCommonService paperGenerationCommonService;
    private final Random random = new Random();

    @Autowired
    public CET4PaperGenService(CET4EExamPaperEnMapper examPaperEnMapper,
                               CET4ChromaEngExamPaperService chromaService,
                               CET4PaperGenerationCommonService paperGenerationCommonService) {
        this.examPaperEnMapper = examPaperEnMapper;
        this.chromaService = chromaService;
        this.paperGenerationCommonService = paperGenerationCommonService;
    }

    /**
     * 保持旧签名，默认 real 来源。
     */
    public Map<String, Object> generatePaper(String examPaperEnName, String examPaperEnDesc, String subjectEn) throws Exception {
        return generatePaper(examPaperEnName, examPaperEnDesc, subjectEn, "AIfromreal", null);
    }

    public Map<String, Object> generatePaper(String examPaperEnName, String examPaperEnDesc, String subjectEn, String examPaperEnSource) throws Exception {
        return generatePaper(examPaperEnName, examPaperEnDesc, subjectEn, examPaperEnSource, null);
    }

    public Map<String, Object> generatePaper(String examPaperEnName,
                                             String examPaperEnDesc,
                                             String subjectEn,
                                             String examPaperEnSource,
                                             Long userId) throws Exception {
        String source = (examPaperEnSource == null || examPaperEnSource.isBlank()) ? "AIfromreal" : examPaperEnSource;
        return generatePaperConcurrent(
                examPaperEnName,
                examPaperEnDesc,
                subjectEn,
                source,
                userId,
                null
        );
    }

    public Map<String, Object> generatePreview(String subjectEn, String examPaperEnSource) throws Exception {
        return generatePaper(null, null, subjectEn, examPaperEnSource, null);
    }

    /**
     * 仅在用户确认保存时落库并绑定用户。
     */
    public Long createExamPaperRecord(String examPaperEnName, String examPaperEnDesc, String subjectEn,
                                      String examPaperEnSource, Long userId) {
        if (examPaperEnName == null || examPaperEnName.isBlank()) {
            throw new IllegalArgumentException("试卷名称不能为空");
        }
        if (examPaperEnDesc == null || examPaperEnDesc.isBlank()) {
            throw new IllegalArgumentException("试卷描述不能为空");
        }
        if (subjectEn == null || subjectEn.isBlank()) {
            throw new IllegalArgumentException("科目不能为空");
        }
        String source = (examPaperEnSource == null || examPaperEnSource.isBlank()) ? "AIfromreal" : examPaperEnSource;
        CET4ExamPaperEn examPaperEn = new CET4ExamPaperEn(examPaperEnName, examPaperEnDesc, subjectEn);
        examPaperEn.setExamPaperEnSource(source);
        examPaperEnMapper.insert(examPaperEn);
        paperGenerationCommonService.bindExamPaperToUser(userId, examPaperEn.getId());
        return examPaperEn.getId();
    }

    /**
     * 兼容旧接口：返回预览用的空 examPaperEnId。
     */
    public Map<String, Object> generatePaper(String examPaperEnName, String examPaperEnDesc, String subjectEn,
                                             String examPaperEnSource, Long userId, boolean previewOnly) throws Exception {
        Map<String, Object> result = generatePaper(examPaperEnName, examPaperEnDesc, subjectEn, examPaperEnSource, userId);
        if (previewOnly) {
            Map<String, Object> preview = new HashMap<>(result);
            preview.put("examPaperEnId", null);
            return preview;
        }
        return result;
    }

    /**
     * 并行生成试卷（对外暴露，兼容旧并发服务使用）。
     */
    public Map<String, Object> generatePaperConcurrent(String examPaperEnName,
                                                       String examPaperEnDesc,
                                                       String subjectEn,
                                                       String examPaperEnSource,
                                                       Long userId,
                                                       CET4PaperGenerationProgressListener progressListener) throws Exception {
        String source = (examPaperEnSource == null || examPaperEnSource.isBlank()) ? "AIfromreal" : examPaperEnSource;
        return generatePaperInternal(examPaperEnName, examPaperEnDesc, subjectEn, source, userId, progressListener);
    }

    private Map<String, Object> generatePaperInternal(String examPaperEnName,
                                                      String examPaperEnDesc,
                                                      String subjectEn,
                                                      String examPaperEnSource,
                                                      Long userId,
                                                      CET4PaperGenerationProgressListener progressListener) throws Exception {
        // 生成阶段不再插入试卷记录，等用户确认保存后再落库
        Long examPaperEnId = null;

        List<String> allTopics = paperGenerationCommonService.getAllTopicsShuffled();

        Set<String> existingPaperIds = chromaService.findRealPaperIdsBySubject(subjectEn);
        if (existingPaperIds.isEmpty()) {
            throw new RuntimeException("ChromaDB中没有其他科目为 " + subjectEn + " 且 exam_paper_en_source='real' 的真题试卷可供参考");
        }

        List<String> paperIdList = new ArrayList<>(existingPaperIds);
        String templatePaperId = paperIdList.get(random.nextInt(paperIdList.size()));
        List<Map<String, Object>> templateUnits = chromaService.fetchPaperUnitsWithMeta(templatePaperId);
        if (templateUnits.isEmpty()) {
            throw new RuntimeException("ChromaDB中没有找到试卷ID为 " + templatePaperId + " 的片段");
        }

        List<String> assignedTopics = new ArrayList<>();
        for (int i = 0; i < templateUnits.size(); i++) {
            assignedTopics.add(allTopics.get(i % allTopics.size()));
        }

        int poolSize = Math.max(1, Math.min(templateUnits.size(), Runtime.getRuntime().availableProcessors()));
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();
        AtomicInteger unitIndex = new AtomicInteger(0);
        AtomicInteger completedUnits = new AtomicInteger(0);
        final int totalUnits = templateUnits.size();

        

        final String sourceForGeneration = examPaperEnSource;

        try {
            for (Map<String, Object> unit : templateUnits) {
                final int currentIndex = unitIndex.getAndIncrement();
                final String topic = assignedTopics.get(currentIndex);
                final Map<String, Object> unitSnapshot = new HashMap<>(unit);
                CompletableFuture<Map<String, Object>> future = CompletableFuture
                        .supplyAsync(() -> generateSingleUnit(unitSnapshot, topic, sourceForGeneration, currentIndex), executor)
                        .whenComplete((ignored, throwable) -> notifyProgress(progressListener, completedUnits.incrementAndGet(), totalUnits));
                futures.add(future);
            }

            CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]));
            try {
                all.join();
            } catch (CompletionException ex) {
                throw unwrap(ex);
            }

            List<Map<String, Object>> generatedUnits = new ArrayList<>();
            for (CompletableFuture<Map<String, Object>> future : futures) {
                generatedUnits.add(future.join());
            }
            generatedUnits.sort((a, b) -> Integer.compare((Integer) a.get("unit_index"), (Integer) b.get("unit_index")));

            

            Map<String, Object> result = new HashMap<>();
            result.put("examPaperEnId", examPaperEnId);
            result.put("units", generatedUnits);
            result.put("exam_paper_en_source", sourceForGeneration);
            return result;
        } finally {
            executor.shutdown();
        }
    }

    private void notifyProgress(CET4PaperGenerationProgressListener listener, int completed, int total) {
        if (listener == null || total <= 0) {
            return;
        }
        try {
            listener.onProgress(completed, total);
        } catch (Exception ignored) {
            // 避免进度回调影响主流程
        }
    }

    private Map<String, Object> generateSingleUnit(Map<String, Object> unit,
                                                   String topic,
                                                   String examPaperEnSource,
                                                   int currentIndex) {
        try {
            String document = (String) unit.get("document");
            Object partIdObj = unit.get("part_id");
            String segmentId = (String) unit.get("segment_id");
            String questionType = (String) unit.get("question_type");
            int partId = partIdObj instanceof Integer ? (Integer) partIdObj : Integer.parseInt(String.valueOf(partIdObj));

            Map<String, Object> unitResult = generateSingleQuestion(
                    topic,
                    document,
                    segmentId,
                    questionType,
                    examPaperEnSource,
                    currentIndex
            );
            unitResult.put("part_id", partId);
            return unitResult;
        } catch (Exception e) {
            throw new CompletionException("片段生成失败：" + e.getMessage(), e);
        }
    }

    /**
     * 单个题目的生成（Coze/DeepSeek 调用集中在这里）。
     */
    public Map<String, Object> generateSingleQuestion(String topic,
                                                      String document,
                                                      String segmentId,
                                                      String questionType,
                                                      String examPaperEnSource,
                                                      int unitIndex) {
        try {
            Map<String, Object> cozeResult = paperGenerationCommonService.callCozeWorkflow(
                    topic,
                    document,
                    examPaperEnSource,
                    segmentId
            );

            Map<String, Object> unitResult = new HashMap<>();
            unitResult.put("unit_index", unitIndex);
            unitResult.put("segment_id", segmentId);
            unitResult.put("question_type", questionType);
            unitResult.put("topic", topic);
            unitResult.put("original_document", document);
            unitResult.put("output", cozeResult.get("output"));
            unitResult.put("answers", cozeResult.get("answers"));
            unitResult.put("exam_paper_en_source", examPaperEnSource);
            return unitResult;
        } catch (Exception e) {
            throw new CompletionException("单题生成失败：" + e.getMessage(), e);
        }
    }

    /**
     * 对外暴露的单题 Coze 生成（供错题再训练等场景直接调用）。
     */
    public Map<String, Object> generateSingleQuestionDirect(String examTopic,
                                                            String inputExamPaperSamp,
                                                            String examPaperEnSource,
                                                            String segmentIdSelf) throws Exception {
        Map<String, Object> cozeResult = paperGenerationCommonService.callCozeWorkflow(
                examTopic,
                inputExamPaperSamp,
                examPaperEnSource,
                segmentIdSelf
        );

        Map<String, Object> result = new HashMap<>();
        result.put("output", cozeResult.get("output"));
        result.put("answers", cozeResult.get("answers"));
        result.put("rawResponse", cozeResult.get("rawResponse"));
        result.put("examTopic", examTopic);
        result.put("segment_id", segmentIdSelf);
        result.put("exam_paper_en_source", examPaperEnSource);
        return result;
    }

    private RuntimeException unwrap(CompletionException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new RuntimeException(cause == null ? "并发生成失败" : cause.getMessage(), cause);
    }
}
