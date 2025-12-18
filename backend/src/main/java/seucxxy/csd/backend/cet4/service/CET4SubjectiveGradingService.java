package seucxxy.csd.backend.cet4.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import seucxxy.csd.backend.cet4.entity.CET4ExamPaperEnCet4Structure;
import seucxxy.csd.backend.cet4.entity.CET4UserTestRecordSegmentEn;
import seucxxy.csd.backend.cet4.mapper.CET4ExamPaperEnCet4StructureMapper;
import seucxxy.csd.backend.cet4.mapper.CET4UserTestRecordSegmentEnMapper;
import seucxxy.csd.backend.common.entity.User;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@Service
public class CET4SubjectiveGradingService {

    // AI评分结果缓存: key = "testEnId_segmentId_questionNumber", value = AI评分结果Map
    private static final ConcurrentHashMap<String, Map<String, Object>> aiScoreCache = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    private final CET4CozeAIGradingService cozeAIGradingService;
    private final CET4ExamPaperEnCet4StructureMapper examPaperEnCet4StructureMapper;
    private final CET4UserTestRecordChromaService userTestRecordChromaService;
    private final CET4UserTestRecordSegmentEnMapper userTestRecordSegmentEnMapper;

    public CET4SubjectiveGradingService(ObjectMapper objectMapper,
                                    CET4CozeAIGradingService cozeAIGradingService,
                                    CET4ExamPaperEnCet4StructureMapper examPaperEnCet4StructureMapper,
                                    CET4UserTestRecordChromaService userTestRecordChromaService,
                                    CET4UserTestRecordSegmentEnMapper userTestRecordSegmentEnMapper) {
        this.objectMapper = objectMapper;
        this.cozeAIGradingService = cozeAIGradingService;
        this.examPaperEnCet4StructureMapper = examPaperEnCet4StructureMapper;
        this.userTestRecordChromaService = userTestRecordChromaService;
        this.userTestRecordSegmentEnMapper = userTestRecordSegmentEnMapper;
    }

    public CompletableFuture<Void> gradeWritingAsync(String userAnswer,
                                                     String segmentId,
                                                     int questionNumber,
                                                     String document,
                                                     boolean shouldCalculateScore,
                                                     Long testEnId,
                                                     Integer subjectEnId,
                                                     Long examPaperEnId,
                                                     String examPaperEnName,
                                                     User user,
                                                     ExecutorService executor) {
        return CompletableFuture.runAsync(() -> {
            double writingSegScore = 0.0;
            try {
                if (userAnswer != null && !userAnswer.trim().isEmpty()) {
                    JsonNode docNode = objectMapper.readTree(document);
                    JsonNode writingNode = docNode.path("writing");
                    Map<String, Object> questionInfo = Map.of("writing", objectMapper.convertValue(writingNode, Map.class));
                    Map<String, Object> gradeResult = cozeAIGradingService.gradeSubjectiveQuestion(questionInfo, userAnswer);
                    Double score = gradeResult.get("score") != null ? Double.valueOf(gradeResult.get("score").toString()) : 0.0;
                    if (shouldCalculateScore) {
                        CET4ExamPaperEnCet4Structure structure = examPaperEnCet4StructureMapper.findBySegmentId(segmentId);
                        double segmentTotalScore = (structure != null && structure.getSegmentTotalScore() != null)
                                ? structure.getSegmentTotalScore() : 106.5;
                        writingSegScore = (score / 100.0) * segmentTotalScore;
                    }
                    String cacheKey = buildCacheKey(testEnId, segmentId, questionNumber);
                    gradeResult.put("userAnswer", userAnswer);
                    aiScoreCache.put(cacheKey, gradeResult);
                    try {
                        userTestRecordChromaService.storeUserAnswer(userAnswer, subjectEnId, testEnId,
                                user.getId(), user.getRealName() != null ? user.getRealName() : user.getUsername(),
                                "Writing", examPaperEnId, examPaperEnName, 1, segmentId, score);
                    } catch (Exception ignore) {
                    }
                    try {
                        userTestRecordChromaService.storeAIGradeResult(gradeResult, subjectEnId, testEnId,
                                user.getId(), user.getRealName() != null ? user.getRealName() : user.getUsername(),
                                "Writing", examPaperEnId, examPaperEnName, 1, segmentId, score);
                    } catch (Exception ignore) {
                    }
                }
            } catch (Exception ignore) {
            } finally {
                CET4UserTestRecordSegmentEn writingRec = new CET4UserTestRecordSegmentEn();
                writingRec.setSegmentId(segmentId);
                writingRec.setQuestionType("Writing");
                writingRec.setScore(BigDecimal.valueOf(writingSegScore).setScale(2, RoundingMode.HALF_UP).doubleValue());
                int writingCorrectNum = writingSegScore > 0 ? 1 : 0;
                writingRec.setCorrectAnswersNumber(writingCorrectNum);
                writingRec.setNumberOfQuestions(1);
                double writingPercent = writingCorrectNum * 100.0;
                writingRec.setCorrectAnswersPercent(BigDecimal.valueOf(writingPercent).setScale(2, RoundingMode.HALF_UP).doubleValue());
                writingRec.setTestEnId(testEnId);
                userTestRecordSegmentEnMapper.insert(writingRec);
            }
        }, executor);
    }

    public CompletableFuture<Void> gradeTranslationAsync(String userAnswer,
                                                         String segmentId,
                                                         int questionNumber,
                                                         String document,
                                                         boolean shouldCalculateScore,
                                                         Long testEnId,
                                                         Integer subjectEnId,
                                                         Long examPaperEnId,
                                                         String examPaperEnName,
                                                         User user,
                                                         ExecutorService executor) {
        return CompletableFuture.runAsync(() -> {
            double translationSegScore = 0.0;
            try {
                if (userAnswer != null && !userAnswer.trim().isEmpty()) {
                    JsonNode translationDocNode = objectMapper.readTree(document);
                    JsonNode translationNodeObj = translationDocNode.path("translation");
                    Map<String, Object> translationQuestionInfo = Map.of("translation", objectMapper.convertValue(translationNodeObj, Map.class));
                    Map<String, Object> translationGradeResult = cozeAIGradingService.gradeSubjectiveQuestion(translationQuestionInfo, userAnswer);
                    Double translationScore = translationGradeResult.get("score") != null ? Double.valueOf(translationGradeResult.get("score").toString()) : 0.0;
                    if (shouldCalculateScore) {
                        CET4ExamPaperEnCet4Structure structure = examPaperEnCet4StructureMapper.findBySegmentId(segmentId);
                        double segmentTotalScore = (structure != null && structure.getSegmentTotalScore() != null)
                                ? structure.getSegmentTotalScore() : 106.5;
                        translationSegScore = (translationScore / 100.0) * segmentTotalScore;
                    }
                    String cacheKey = buildCacheKey(testEnId, segmentId, questionNumber);
                    translationGradeResult.put("userAnswer", userAnswer);
                    aiScoreCache.put(cacheKey, translationGradeResult);
                    try {
                        userTestRecordChromaService.storeUserAnswer(userAnswer, subjectEnId, testEnId,
                                user.getId(), user.getRealName() != null ? user.getRealName() : user.getUsername(),
                                "Translation", examPaperEnId, examPaperEnName, 4, segmentId, translationScore);
                    } catch (Exception ignore) {
                    }
                    try {
                        userTestRecordChromaService.storeAIGradeResult(translationGradeResult, subjectEnId, testEnId,
                                user.getId(), user.getRealName() != null ? user.getRealName() : user.getUsername(),
                                "Translation", examPaperEnId, examPaperEnName, 4, segmentId, translationScore);
                    } catch (Exception ignore) {
                    }
                }
            } catch (Exception ignore) {
            } finally {
                CET4UserTestRecordSegmentEn translationRec = new CET4UserTestRecordSegmentEn();
                translationRec.setSegmentId(segmentId);
                translationRec.setQuestionType("Translation");
                translationRec.setScore(BigDecimal.valueOf(translationSegScore).setScale(2, RoundingMode.HALF_UP).doubleValue());
                int translationCorrectNum = translationSegScore > 0 ? 1 : 0;
                translationRec.setCorrectAnswersNumber(translationCorrectNum);
                translationRec.setNumberOfQuestions(1);
                double translationPercent = translationCorrectNum * 100.0;
                translationRec.setCorrectAnswersPercent(BigDecimal.valueOf(translationPercent).setScale(2, RoundingMode.HALF_UP).doubleValue());
                translationRec.setTestEnId(testEnId);
                userTestRecordSegmentEnMapper.insert(translationRec);
            }
        }, executor);
    }

    public Map<String, Object> getCachedAiScore(String cacheKey) {
        return aiScoreCache.get(cacheKey);
    }

    public String buildCacheKey(Long testEnId, String segmentId, int questionNumber) {
        return testEnId + "_" + segmentId + "_" + questionNumber;
    }
}
