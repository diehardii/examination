package seucxxy.csd.backend.cet4.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seucxxy.csd.backend.cet4.dto.CET4SectionAChromaRequest;
import seucxxy.csd.backend.cet4.dto.CET4SimpleChromaRequest;
import seucxxy.csd.backend.cet4.entity.CET4ExamPaperEn;
import seucxxy.csd.backend.cet4.entity.CET4UserTestRecordDetailEn;
import seucxxy.csd.backend.cet4.mapper.CET4EExamPaperEnMapper;
import seucxxy.csd.backend.cet4.mapper.CET4UserTestRecordDetailEnMapper;
import seucxxy.csd.backend.cet4.mapper.CET4UserTestRecordEnMapper;
import seucxxy.csd.backend.common.mapper.ExamPaperEnStructureMapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CET4IntensiveTrainService {
    private final ExamPaperEnStructureMapper structureMapper;
    private final CET4EExamPaperEnMapper examPaperEnMapper;
    private final CET4ChromaEngExamPaperService chromaService;
    private final CET4PaperGenService paperGenService;
    private final CET4PaperGenerationCommonService paperGenerationCommonService;
    private final CET4UserTestRecordDetailEnMapper userTestRecordDetailEnMapper;
    private final CET4UserTestRecordEnMapper userTestRecordEnMapper;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();
    private static final int MAX_ATTEMPTS_PER_QUESTION = 3;

    public CET4IntensiveTrainService(ExamPaperEnStructureMapper structureMapper,
                                 CET4EExamPaperEnMapper examPaperEnMapper,
                                 CET4ChromaEngExamPaperService chromaService,
                                 CET4PaperGenService paperGenService,
                                 CET4PaperGenerationCommonService paperGenerationCommonService,
                                 CET4UserTestRecordDetailEnMapper userTestRecordDetailEnMapper,
                                 CET4UserTestRecordEnMapper userTestRecordEnMapper) {
        this.structureMapper = structureMapper;
        this.examPaperEnMapper = examPaperEnMapper;
        this.chromaService = chromaService;
        this.paperGenService = paperGenService;
        this.paperGenerationCommonService = paperGenerationCommonService;
        this.userTestRecordDetailEnMapper = userTestRecordDetailEnMapper;
        this.userTestRecordEnMapper = userTestRecordEnMapper;
        this.objectMapper = new ObjectMapper();
    }

    public List<String> getQuestionTypes() {
        return structureMapper.listQuestionTypes();
    }

    /**
     * 从错题库或ChromaDB获取样本文档
     */
    private String getSampleDocument(Long userId, String questionType, boolean fromWrongBank) {
        String qtUpper = questionType == null ? "" : questionType.toUpperCase();
        boolean isWritingOrTranslation = "WRITING".equals(qtUpper) || qtUpper.contains("写作")
                                       || "TRANSLATION".equals(qtUpper) || qtUpper.contains("翻译");

        if (!fromWrongBank || isWritingOrTranslation) {
            return getRandomRealQuestionFromChromaByType(questionType);
        }

        try {
            CET4UserTestRecordDetailEn wrongQuestion = userTestRecordDetailEnMapper.getRandomWrongQuestion(userId, questionType);
            if (wrongQuestion != null) {
                Long testEnId = wrongQuestion.getTestEnId();
                var userTestRecord = userTestRecordEnMapper.getUserTestRecordEnById(testEnId);
                if (userTestRecord != null) {
                    Long examPaperEnId = userTestRecord.getExamPaperEnId();
                    String segmentId = wrongQuestion.getSegmentId();
                    String document = paperGenerationCommonService.getDocumentFromChroma(String.valueOf(examPaperEnId), segmentId, questionType);
                    if (document != null && !document.isEmpty()) {
                        return document;
                    }
                }
            }
        } catch (Exception e) {
            // Fallback to random question if wrong-bank lookup fails
        }
        return getRandomRealQuestionFromChromaByType(questionType);
    }

    /**
     * 从ChromaDB随机选择一道real来源且题型匹配的题目
     */
    private String getRandomRealQuestionFromChromaByType(String questionType) {
        try {
            List<Long> realIds = examPaperEnMapper.findIdsBySubjectAndSource("CET4", "real");
            if (realIds == null || realIds.isEmpty()) {
                realIds = examPaperEnMapper.findIdsBySource("real");
            }
            if (realIds == null || realIds.isEmpty()) {
                throw new RuntimeException("没有可用的real来源试卷");
            }

            int maxAttempts = Math.min(10, realIds.size());
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                Long pickedId = realIds.get(random.nextInt(realIds.size()));
                List<Map<String, Object>> candidates = paperGenerationCommonService.queryExistingUnits(String.valueOf(pickedId), questionType);
                if (!candidates.isEmpty()) {
                    Map<String, Object> unit = candidates.get(random.nextInt(candidates.size()));
                    String document = String.valueOf(unit.getOrDefault("document", ""));
                    return document;
                }
            }
            throw new RuntimeException("在ChromaDB中找不到exam_paper_en_source=real且question_type=" + questionType + "的题目");
        } catch (Exception e) {
            throw new RuntimeException("无法从ChromaDB获取样本题目: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> generateIntensivePaperContent(Long userId,
                                                             boolean fromWrongBank,
                                                             List<String> types,
                                                             List<Integer> counts,
                                                             String examPaperEnSource) {
        return generateIntensivePaperContentConcurrent(userId, fromWrongBank, types, counts, examPaperEnSource, null);
    }

    public Map<String, Object> generateIntensivePaperContentConcurrent(Long userId,
                                                                       boolean fromWrongBank,
                                                                       List<String> types,
                                                                       List<Integer> counts,
                                                                       String examPaperEnSource) {
        return generateIntensivePaperContentConcurrent(userId, fromWrongBank, types, counts, examPaperEnSource, null);
    }

    public Map<String, Object> generateIntensivePaperContentConcurrent(Long userId,
                                                                       boolean fromWrongBank,
                                                                       List<String> types,
                                                                       List<Integer> counts,
                                                                       String examPaperEnSource,
                                                                       CET4PaperGenerationProgressListener progressListener) {
        if (types == null || counts == null || types.size() != counts.size()) {
            throw new IllegalArgumentException("题型与数量不匹配");
        }

        int requestedTotal = calculateRequestedTotal(counts);
        if (requestedTotal <= 0) {
            throw new IllegalArgumentException("至少为一种题型设置大于0的数量");
        }

        String finalExamPaperEnSource = resolveExamPaperEnSource(fromWrongBank, examPaperEnSource);
        List<String> taskTypes = new ArrayList<>();
        for (int i = 0; i < types.size(); i++) {
            String questionType = String.valueOf(types.get(i));
            int count = counts.get(i) == null ? 0 : counts.get(i);
            for (int j = 0; j < count; j++) {
                taskTypes.add(questionType);
            }
        }

        AtomicInteger segmentCounter = new AtomicInteger(1);
        List<Map<String, Object>> generatedQuestions = Collections.synchronizedList(new ArrayList<>());
        List<String> failedTypes = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger completedTasks = new AtomicInteger(0);
        final int totalTasks = taskTypes.size();

        int poolSize = Math.max(1, Math.min(taskTypes.size(), Runtime.getRuntime().availableProcessors() * 2));
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (String questionType : taskTypes) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    int segmentIndex = segmentCounter.getAndIncrement();
                    Map<String, Object> question = generateQuestionTaskWithRetry(
                            userId,
                            questionType,
                            fromWrongBank,
                            finalExamPaperEnSource,
                            segmentIndex,
                            MAX_ATTEMPTS_PER_QUESTION
                    );

                    if (question != null) {
                        generatedQuestions.add(question);
                    } else {
                        failedTypes.add(questionType);
                    }
                }, executor).whenComplete((ignored, throwable) -> notifyProgress(progressListener, completedTasks.incrementAndGet(), totalTasks));
                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
        } finally {
            executor.shutdown();
        }

        if (generatedQuestions.isEmpty()) {
            throw new RuntimeException("未能生成任何题目, 请稍后重试");
        }

        List<Map<String, Object>> orderedQuestions = new ArrayList<>(generatedQuestions);
        orderedQuestions.sort(Comparator.comparingInt(q -> {
            Object indexValue = q.get("segmentIndex");
            if (indexValue instanceof Number number) {
                return number.intValue();
            }
            return 0;
        }));

        return buildGenerationResult(finalExamPaperEnSource, orderedQuestions, requestedTotal, new ArrayList<>(failedTypes));
    }

    private void notifyProgress(CET4PaperGenerationProgressListener listener, int completed, int total) {
        if (listener == null || total <= 0) {
            return;
        }
        try {
            listener.onProgress(completed, total);
        } catch (Exception ignored) {
        }
    }

    private Map<String, Object> generateQuestionTaskWithRetry(Long userId,
                                                              String questionType,
                                                              boolean fromWrongBank,
                                                              String finalExamPaperEnSource,
                                                              int segmentIndex,
                                                              int maxAttempts) {
        String safeQuestionType = questionType == null ? "" : questionType;
        String segmentIdSelf = segmentIndex + safeQuestionType;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Map<String, Object> question = buildSingleQuestion(
                        userId,
                        safeQuestionType,
                        fromWrongBank,
                        finalExamPaperEnSource,
                        segmentIndex,
                        segmentIdSelf
                );

                // Generation succeeded; result returned to caller
                return question;
            } catch (Exception e) {
            }
        }

        return null;
    }

    private Map<String, Object> buildSingleQuestion(Long userId,
                                                    String questionType,
                                                    boolean fromWrongBank,
                                                    String finalExamPaperEnSource,
                                                    int segmentIndex,
                                                    String segmentIdSelf) throws Exception {
        String srcDocument = getSampleDocument(userId, questionType, fromWrongBank);
        if (srcDocument == null || srcDocument.isEmpty()) {
            throw new RuntimeException("无法获取题型 " + questionType + " 的样本文档");
        }

        String randomTopic = paperGenerationCommonService.getRandomTopic();
        Map<String, Object> generated = paperGenService.generateSingleQuestion(
            randomTopic,
            srcDocument,
            segmentIdSelf,
            questionType,
            finalExamPaperEnSource,
            segmentIndex
        );

        JsonNode parsedOutput = normalizeOutput(generated.get("output"));
        if (parsedOutput == null) {
            throw new RuntimeException("Coze返回的output为空");
        }

        String generatedJson = objectMapper.writeValueAsString(parsedOutput);
        generatedJson = paperGenerationCommonService.cleanEscapedQuotes(generatedJson);

        Map<String, Object> questionData = new HashMap<>();
        questionData.put("questionType", questionType);
        questionData.put("segmentIdSelf", segmentIdSelf);
        questionData.put("documentJson", generatedJson);
        questionData.put("examPaperEnSource", finalExamPaperEnSource);
        questionData.put("segmentIndex", segmentIndex);
        return questionData;
    }

    private JsonNode normalizeOutput(Object output) throws Exception {
        if (output == null) {
            return null;
        }

        JsonNode parsedOutput;
        if (output instanceof JsonNode jsonNode) {
            parsedOutput = jsonNode;
        } else if (output instanceof String outputStr) {
            parsedOutput = objectMapper.readTree(outputStr);
        } else if (output instanceof Map<?, ?> mapOutput) {
            parsedOutput = objectMapper.valueToTree(mapOutput);
        } else {
            parsedOutput = objectMapper.valueToTree(output);
        }

        if (parsedOutput == null) {
            return null;
        }

        if (parsedOutput.isObject() || parsedOutput.isArray()) {
            if (parsedOutput.size() == 0) {
                return null;
            }
        }

        return parsedOutput;
    }

    private String resolveExamPaperEnSource(boolean fromWrongBank, String examPaperEnSource) {
        if (fromWrongBank) {
            return "AIfromWrongBank";
        }
        return examPaperEnSource == null || examPaperEnSource.isBlank() ? "AIfromself" : examPaperEnSource;
    }

    private int calculateRequestedTotal(List<Integer> counts) {
        if (counts == null) {
            return 0;
        }

        int total = 0;
        for (Integer count : counts) {
            if (count != null && count > 0) {
                total += count;
            }
        }
        return total;
    }

    private Map<String, Object> buildGenerationResult(String finalExamPaperEnSource,
                                                      List<Map<String, Object>> generatedQuestions,
                                                      int requestedTotal,
                                                      List<String> failedTypes) {
        if (generatedQuestions != null) {
            generatedQuestions.forEach(question -> {
                if (question != null) {
                    question.remove("segmentIndex");
                }
            });
        }

        Map<String, Object> result = new HashMap<>();
        result.put("questions", generatedQuestions);
        result.put("examPaperEnSource", finalExamPaperEnSource);
        result.put("totalCount", generatedQuestions.size());
        result.put("failedCount", Math.max(0, requestedTotal - generatedQuestions.size()));
        result.put("failedTypes", failedTypes == null ? Collections.emptyList() : failedTypes);
        return result;
    }
    
    /**
     * 保存强化训练试卷到数据库和ChromaDB
     */
    @Transactional
    public Long saveIntensivePaper(Long userId, String paperName, List<Map<String, Object>> questionsData, String examPaperEnSource) {
        if (questionsData == null || questionsData.isEmpty()) {
            throw new IllegalArgumentException("没有可保存的题目数据");
        }

        String finalPaperName = paperName == null || paperName.isBlank() ? 
            "强化训练-" + System.currentTimeMillis() : paperName;

        CET4ExamPaperEn exam = new CET4ExamPaperEn(finalPaperName, "强化训练自定义", "CET4");
        exam.setExamPaperEnSource(examPaperEnSource);
        examPaperEnMapper.insert(exam);
        Long examPaperEnId = exam.getId();
        String examPaperEnName = exam.getExamPaperEnName();
        paperGenerationCommonService.bindExamPaperToUser(userId, examPaperEnId);

        for (Map<String, Object> questionData : questionsData) {
            String qtMeta = (String) questionData.get("questionType");
            String segmentIdSelf = (String) questionData.get("segmentIdSelf");
            String generatedJson = (String) questionData.get("documentJson");
            String source = (String) questionData.get("examPaperEnSource");
            storeGeneratedUnit(examPaperEnId, examPaperEnName, qtMeta, segmentIdSelf, generatedJson, source, true);
        }

        // Paper saved successfully; caller can log details if needed

        return examPaperEnId;
    }

    private void storeGeneratedUnit(Long examPaperEnId, String examPaperEnName, String questionTypeMeta, String segmentIdSelf, String documentJson, String examPaperEnSource, boolean isIntensiveTrain) {
        String qt = questionTypeMeta == null ? "" : questionTypeMeta.trim();
        String qtUpper = qt.toUpperCase();

        if (isIntensiveTrain) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(documentJson);

                if (rootNode instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
                    com.fasterxml.jackson.databind.node.ObjectNode objNode = 
                        (com.fasterxml.jackson.databind.node.ObjectNode) rootNode;

                    if ("WRITING".equals(qtUpper) || qt.contains("写作")) {
                        JsonNode writingNode = objNode.path("writing");
                        if (writingNode.isMissingNode()) {
                            writingNode = objNode.path("Writing");
                        }
                        if (writingNode instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
                            ((com.fasterxml.jackson.databind.node.ObjectNode) writingNode)
                                .put("segment_id", segmentIdSelf);
                        }
                    }
                    else if ("TRANSLATION".equals(qtUpper) || qt.contains("翻译")) {
                        JsonNode translationNode = objNode.path("translation");
                        if (translationNode.isMissingNode()) {
                            translationNode = objNode.path("Translation");
                        }
                        if (translationNode instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
                            ((com.fasterxml.jackson.databind.node.ObjectNode) translationNode)
                                .put("segment_id", segmentIdSelf);
                        }
                    }
                    else if ("BLANKEDCLOZE".equals(qtUpper) || qt.contains("选词填空")) {
                        injectSegmentIdToSection(objNode, "a", segmentIdSelf);
                    }
                    else if ("MATCHING".equals(qtUpper) || qt.contains("段落匹配") || qt.contains("匹配")) {
                        injectSegmentIdToSection(objNode, "b", segmentIdSelf);
                    }
                    else if ("READINGPASSAGE".equals(qtUpper) || qt.contains("篇章阅读") || qt.contains("阅读")) {
                        injectSegmentIdToSection(objNode, "c", segmentIdSelf);
                    }
                    else if ("NEWSREPORT".equals(qtUpper) || "CONVERSATION".equals(qtUpper) || 
                             "LISTENINGPASSAGE".equals(qtUpper) || qt.contains("听力")) {
                        objNode.put("segment_id", segmentIdSelf);
                    }
                    else {
                        objNode.put("segment_id", segmentIdSelf);
                    }

                    documentJson = mapper.writeValueAsString(objNode);
                }
            } catch (Exception e) {
                // Use original document if injection fails
            }
        }

        try {
            if ("WRITING".equals(qtUpper) || qt.contains("写作")) {
                CET4SimpleChromaRequest req = new CET4SimpleChromaRequest();
                req.setDocument(documentJson);
                req.setSubject("CET4");
                req.setExamPaperId(String.valueOf(examPaperEnId));
                req.setExamPaperName(examPaperEnName);
                req.setQuestionType("Writing");
                req.setPartId(1);
                req.setQuestionSource("AI");
                req.setExamPaperEnSource(examPaperEnSource);
                chromaService.storePartSimple(req);
                return;
            }
            if ("TRANSLATION".equals(qtUpper) || qt.contains("翻译")) {
                CET4SimpleChromaRequest req = new CET4SimpleChromaRequest();
                req.setDocument(documentJson);
                req.setSubject("CET4");
                req.setExamPaperId(String.valueOf(examPaperEnId));
                req.setExamPaperName(examPaperEnName);
                req.setQuestionType("Translation");
                req.setPartId(4);
                req.setQuestionSource("AI");
                req.setExamPaperEnSource(examPaperEnSource);
                chromaService.storePartSimple(req);
                return;
            }
            if ("BLANKEDCLOZE".equals(qtUpper) || qt.contains("选词填空")) {
                CET4SectionAChromaRequest req = new CET4SectionAChromaRequest();
                req.setDocument(documentJson);
                req.setSubject("CET4");
                req.setExamPaperId(String.valueOf(examPaperEnId));
                req.setExamPaperName(examPaperEnName);
                req.setBlankNumbers(java.util.List.of("1"));
                req.setPartId(3);
                req.setSectionId("A");
                req.setQuestionSource("AI");
                req.setExamPaperEnSource(examPaperEnSource);
                chromaService.storeSectionA(req);
                return;
            }
            if ("MATCHING".equals(qtUpper) || qt.contains("段落匹配") || qt.contains("匹配")) {
                CET4SectionAChromaRequest req = new CET4SectionAChromaRequest();
                req.setDocument(documentJson);
                req.setSubject("CET4");
                req.setExamPaperId(String.valueOf(examPaperEnId));
                req.setExamPaperName(examPaperEnName);
                req.setBlankNumbers(java.util.List.of("1"));
                req.setPartId(3);
                req.setSectionId("B");
                req.setQuestionSource("AI");
                req.setExamPaperEnSource(examPaperEnSource);
                chromaService.storeSectionB(req);
                return;
            }
            if ("READINGPASSAGE".equals(qtUpper) || qt.contains("篇章阅读") || qt.contains("阅读")) {
                CET4SectionAChromaRequest req = new CET4SectionAChromaRequest();
                req.setDocument(documentJson);
                req.setSubject("CET4");
                req.setExamPaperId(String.valueOf(examPaperEnId));
                req.setExamPaperName(examPaperEnName);
                req.setBlankNumbers(java.util.List.of("1"));
                req.setPartId(3);
                req.setSectionId("C1");
                req.setQuestionSource("AI");
                req.setExamPaperEnSource(examPaperEnSource);
                chromaService.storeSectionC(req);
                return;
            }
            if ("NEWSREPORT".equals(qtUpper) || "CONVERSATION".equals(qtUpper) || "LISTENINGPASSAGE".equals(qtUpper) || qt.contains("听力")) {
                CET4SectionAChromaRequest req = new CET4SectionAChromaRequest();
                req.setDocument(documentJson);
                req.setSubject("CET4");
                req.setExamPaperId(String.valueOf(examPaperEnId));
                req.setExamPaperName(examPaperEnName);
                req.setBlankNumbers(java.util.List.of("1"));
                req.setQuestionSource("AI");
                req.setExamPaperEnSource(examPaperEnSource);
                if (segmentIdSelf != null && segmentIdSelf.toUpperCase().contains("LISTENINGPASSAGE")) {
                    chromaService.storeListeningPart2C(req);
                } else {
                    chromaService.storeListeningPart2AB(req);
                }
                return;
            }
            CET4SectionAChromaRequest req = new CET4SectionAChromaRequest();
            req.setDocument(documentJson);
            req.setSubject("CET4");
            req.setExamPaperId(String.valueOf(examPaperEnId));
            req.setExamPaperName(examPaperEnName);
            req.setBlankNumbers(java.util.List.of("1"));
            req.setPartId(3);
            req.setSectionId("A");
            req.setQuestionSource("AI");
            req.setExamPaperEnSource(examPaperEnSource);
            chromaService.storeSectionA(req);
        } catch (Exception e) {
            throw new RuntimeException("写入Chroma失败: " + e.getMessage(), e);
        }
    }

    private void injectSegmentIdToSection(com.fasterxml.jackson.databind.node.ObjectNode rootNode, 
                                          String sectionLetter, String segmentIdSelf) {
        String sectionKey = "section_" + sectionLetter.toLowerCase();

        JsonNode readingNode = rootNode.path("reading_comprehension");
        if (!readingNode.isMissingNode() && readingNode instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
            JsonNode sectionNode = readingNode.path(sectionKey);
            if (!sectionNode.isMissingNode() && sectionNode instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) sectionNode).put("segment_id", segmentIdSelf);
                return;
            }
        }

        JsonNode sectionNode = rootNode.path(sectionKey);
        if (!sectionNode.isMissingNode() && sectionNode instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) sectionNode).put("segment_id", segmentIdSelf);
            return;
        }

        rootNode.put("segment_id", segmentIdSelf);
    }
}
