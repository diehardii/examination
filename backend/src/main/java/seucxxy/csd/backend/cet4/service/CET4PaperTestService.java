package seucxxy.csd.backend.cet4.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import seucxxy.csd.backend.cet4.dto.CET4SectionAChromaRecord;
import seucxxy.csd.backend.cet4.entity.CET4ExamPaperEn;
import seucxxy.csd.backend.cet4.entity.CET4ExamPaperEnCet4Structure;
import seucxxy.csd.backend.cet4.entity.CET4UserTestRecordDetailEn;
import seucxxy.csd.backend.cet4.entity.CET4UserTestRecordEn;
import seucxxy.csd.backend.cet4.entity.CET4UserTestRecordSegmentEn;
import seucxxy.csd.backend.cet4.mapper.CET4ExamPaperEnCet4StructureMapper;
import seucxxy.csd.backend.cet4.mapper.CET4UserTestRecordDetailEnMapper;
import seucxxy.csd.backend.cet4.mapper.CET4UserTestRecordEnMapper;
import seucxxy.csd.backend.cet4.mapper.CET4UserTestRecordSegmentEnMapper;
import seucxxy.csd.backend.common.entity.User;
import seucxxy.csd.backend.common.mapper.ExamPaperEnStructureMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class CET4PaperTestService {

    private final CET4ExamPaperEnService examPaperEnService;
    private final CET4ChromaEngExamPaperService chromaEngExamPaperService;
    private final CET4UserTestRecordEnMapper userTestRecordEnMapper;
    private final CET4UserTestRecordDetailEnMapper userTestRecordDetailEnMapper;
    private final ObjectMapper objectMapper;
    private final ExamPaperEnStructureMapper examPaperEnStructureMapper;
    private final CET4CozeAIGradingService cozeAIGradingService;
    private final CET4ExamPaperEnCet4StructureMapper examPaperEnCet4StructureMapper;
    private final CET4UserTestRecordChromaService userTestRecordChromaService;
    private final CET4UserTestRecordSegmentEnMapper userTestRecordSegmentEnMapper;
    private final CET4ExamPaperQuestionExtractor questionExtractor;
    private final CET4AnswerMapper answerMapper;
    private final CET4SubjectiveGradingService subjectiveGradingService;

    @Autowired
    public CET4PaperTestService(
            CET4ExamPaperEnService examPaperEnService,
            CET4ChromaEngExamPaperService chromaEngExamPaperService,
            CET4UserTestRecordEnMapper userTestRecordEnMapper,
            CET4UserTestRecordDetailEnMapper userTestRecordDetailEnMapper,
            ObjectMapper objectMapper,
            ExamPaperEnStructureMapper examPaperEnStructureMapper,
            CET4CozeAIGradingService cozeAIGradingService,
            CET4ExamPaperEnCet4StructureMapper examPaperEnCet4StructureMapper,
            CET4UserTestRecordChromaService userTestRecordChromaService,
            CET4UserTestRecordSegmentEnMapper userTestRecordSegmentEnMapper,
            CET4ExamPaperQuestionExtractor questionExtractor,
            CET4AnswerMapper answerMapper,
            CET4SubjectiveGradingService subjectiveGradingService) {
        this.examPaperEnService = examPaperEnService;
        this.chromaEngExamPaperService = chromaEngExamPaperService;
        this.userTestRecordEnMapper = userTestRecordEnMapper;
        this.userTestRecordDetailEnMapper = userTestRecordDetailEnMapper;
        this.objectMapper = objectMapper;
        this.examPaperEnStructureMapper = examPaperEnStructureMapper;
        this.cozeAIGradingService = cozeAIGradingService;
        this.examPaperEnCet4StructureMapper = examPaperEnCet4StructureMapper;
        this.userTestRecordChromaService = userTestRecordChromaService;
        this.userTestRecordSegmentEnMapper = userTestRecordSegmentEnMapper;
        this.questionExtractor = questionExtractor;
        this.answerMapper = answerMapper;
        this.subjectiveGradingService = subjectiveGradingService;
    }

    /**
     * 试卷列表（英文 CET4）
     */
    public ResponseEntity<?> getExamPaperEnList() {
        try {
            List<CET4ExamPaperEn> papers = examPaperEnService.getAllExamPapers();
            List<CET4ExamPaperEn> cet4Papers = papers.stream()
                    .filter(p -> "CET4".equals(p.getExamPaperEnSubject()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(cet4Papers);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "获取试卷列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    /**
     * 提交答案（英文试卷）
     */
    public Map<String, Object> submitAnswerEn(Map<String, Object> request, User user) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long examPaperEnId = Long.parseLong(request.get("examPaperEnId").toString());
                Map<String, Object> answers = (request.get("answers") instanceof Map)
                    ? answerMapper.castToStringObjectMap(request.get("answers"))
                    : Collections.emptyMap();

            CET4ExamPaperEn examPaperEn = examPaperEnService.getAllExamPapers().stream()
                    .filter(p -> p.getId().equals(examPaperEnId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("试卷不存在"));

            List<CET4SectionAChromaRecord> records = chromaEngExamPaperService.fetchExamPaperUnits(
                    String.valueOf(examPaperEnId), examPaperEn.getExamPaperEnName());

            List<Map<String, Object>> allQuestions = new ArrayList<>();
            for (CET4SectionAChromaRecord record : records) {
                String document = record.document();
                Map<String, Object> metadata = record.metadata();
                String questionType = (String) metadata.get("question_type");
                try {
                    JsonNode docNode = objectMapper.readTree(document);
                    List<Map<String, Object>> questions = questionExtractor.parseQuestionsFromDocument(docNode, metadata, questionType, document);
                    for (Map<String, Object> question : questions) {
                        question.put("segment_id", metadata.get("segment_id"));
                        question.put("document", document);
                        question.put("metadata", metadata);
                    }
                    allQuestions.addAll(questions);
                } catch (Exception parseEx) {
                }
            }

            Integer subjectEnId = mapSubjectNameToId(examPaperEn.getExamPaperEnSubject());
            if (subjectEnId == null) {
                try {
                    List<String> types = examPaperEnStructureMapper.listQuestionTypes();
                    if (types != null && !types.isEmpty()) {
                        subjectEnId = examPaperEnStructureMapper.findSubjectEnIdByQuestionType(types.get(0));
                    }
                } catch (Exception ignore) {
                }
            }
            if (subjectEnId == null) subjectEnId = 17;

            int correctNumber = 0;
            Map<String, Integer> correctCountByType = new HashMap<>();
            Map<String, Integer> totalCountByType = new HashMap<>();

            ExecutorService gradingExecutor = Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors()));
            List<CompletableFuture<Void>> gradingTasks = new ArrayList<>();

            CET4UserTestRecordEn testRecord = new CET4UserTestRecordEn();
            testRecord.setExamPaperEnId(examPaperEnId);
            testRecord.setUserId(user.getId());
            testRecord.setTestEnTime(LocalDateTime.now());
            testRecord.setCorrectNumber(0);
            testRecord.setTestEnScore(0.0);
            userTestRecordEnMapper.insertUserTestRecordEn(testRecord);
            Long testEnId = testRecord.getTestEnId();

            boolean shouldCalculateScore = !"AIfromself".equals(examPaperEn.getExamPaperEnSource())
                    && !"AIfromWrongBank".equals(examPaperEn.getExamPaperEnSource());

            final Integer finalSubjectEnId = subjectEnId;
            final Long finalExamPaperEnId = examPaperEnId;
            final String finalExamPaperName = examPaperEn.getExamPaperEnName();
            final Long finalTestEnId = testEnId;
            final User finalUser = user;

            for (Map<String, Object> question : allQuestions) {
                int questionNumber = (Integer) question.get("number");
                String questionType = (String) question.get("type");
                String correctAnswer = (String) question.get("correctAnswer");
                String segmentId = (String) question.get("segment_id");

                CET4UserTestRecordDetailEn detail = new CET4UserTestRecordDetailEn();
                detail.setQuestionsEnNumber(questionNumber);
                detail.setQuestionsType(questionType);
                detail.setCorrectAnswer(correctAnswer);
                detail.setSegmentId(segmentId);
                detail.setTestEnId(testEnId);

                String userAnswer = "";

                if (questionType.equals("Writing")) {
                    Map<String, Object> writingAnswers = answerMapper.getChildAnswerMap(answers, "writing");
                    userAnswer = writingAnswers.containsKey(segmentId)
                            ? String.valueOf(writingAnswers.get(segmentId))
                            : String.valueOf(writingAnswers.getOrDefault("answer", ""));

                    gradingTasks.add(subjectiveGradingService.gradeWritingAsync(
                            userAnswer,
                            segmentId,
                            questionNumber,
                            (String) question.get("document"),
                            shouldCalculateScore,
                            testEnId,
                            subjectEnId,
                            examPaperEnId,
                            examPaperEn.getExamPaperEnName(),
                            user,
                            gradingExecutor));
                    continue;
                }

                if (questionType.equals("Translation")) {
                    Map<String, Object> translationAnswers = answerMapper.getChildAnswerMap(answers, "translation");
                    userAnswer = translationAnswers.containsKey(segmentId)
                            ? String.valueOf(translationAnswers.get(segmentId))
                            : String.valueOf(translationAnswers.getOrDefault("answer", ""));

                    gradingTasks.add(subjectiveGradingService.gradeTranslationAsync(
                            userAnswer,
                            segmentId,
                            questionNumber,
                            (String) question.get("document"),
                            shouldCalculateScore,
                            testEnId,
                            subjectEnId,
                            examPaperEnId,
                            examPaperEn.getExamPaperEnName(),
                            user,
                            gradingExecutor));
                    continue;
                }

                if (questionType.equals("NewsReport") || questionType.equals("Conversation") || questionType.equals("ListeningPassage")) {
                    Map<String, Object> listeningAnswers = answerMapper.getChildAnswerMap(answers, "listening");
                    String answerKey = segmentId + "-" + questionNumber;
                    userAnswer = String.valueOf(listeningAnswers.getOrDefault(answerKey, ""));
                    if (userAnswer == null || userAnswer.trim().isEmpty() || "null".equals(userAnswer)) {
                        userAnswer = String.valueOf(listeningAnswers.getOrDefault(String.valueOf(questionNumber), ""));
                    }
                    totalCountByType.put(questionType, totalCountByType.getOrDefault(questionType, 0) + 1);
                    if (userAnswer != null && !userAnswer.trim().isEmpty() && !"null".equals(userAnswer) && userAnswer.equals(correctAnswer)) {
                        correctNumber++;
                        correctCountByType.put(questionType, correctCountByType.getOrDefault(questionType, 0) + 1);
                    }
                } else if (questionType.equals("BlankedCloze")) {
                    Map<String, Object> sectionAAnswers = answerMapper.getChildAnswerMap(answers, "sectionA");
                    String answerKey = segmentId + "-" + questionNumber;
                    userAnswer = String.valueOf(sectionAAnswers.getOrDefault(answerKey, ""));
                    if (userAnswer == null || userAnswer.trim().isEmpty() || "null".equals(userAnswer)) {
                        userAnswer = String.valueOf(sectionAAnswers.getOrDefault(String.valueOf(questionNumber), ""));
                    }
                    totalCountByType.put(questionType, totalCountByType.getOrDefault(questionType, 0) + 1);
                    if (userAnswer != null && !userAnswer.trim().isEmpty() && userAnswer.equals(correctAnswer)) {
                        correctNumber++;
                        correctCountByType.put(questionType, correctCountByType.getOrDefault(questionType, 0) + 1);
                    }
                } else if (questionType.equals("Matching")) {
                    Map<String, Object> sectionBAnswers = answerMapper.getChildAnswerMap(answers, "sectionB");
                    String answerKey = segmentId + "-" + questionNumber;
                    userAnswer = String.valueOf(sectionBAnswers.getOrDefault(answerKey, ""));
                    if (userAnswer == null || userAnswer.trim().isEmpty() || "null".equals(userAnswer)) {
                        userAnswer = String.valueOf(sectionBAnswers.getOrDefault(String.valueOf(questionNumber), ""));
                    }
                    totalCountByType.put(questionType, totalCountByType.getOrDefault(questionType, 0) + 1);
                    if (userAnswer != null && !userAnswer.trim().isEmpty() && userAnswer.equals(correctAnswer)) {
                        correctNumber++;
                        correctCountByType.put(questionType, correctCountByType.getOrDefault(questionType, 0) + 1);
                    }
                } else if (questionType.equals("ReadingPassage")) {
                    Map<String, Object> sectionCAnswers = answerMapper.getChildAnswerMap(answers, "sectionC");
                    String answerKey = segmentId + "-" + questionNumber;
                    userAnswer = String.valueOf(sectionCAnswers.getOrDefault(answerKey, ""));
                    if (userAnswer == null || userAnswer.trim().isEmpty() || "null".equals(userAnswer)) {
                        userAnswer = String.valueOf(sectionCAnswers.getOrDefault(String.valueOf(questionNumber), ""));
                    }
                    totalCountByType.put(questionType, totalCountByType.getOrDefault(questionType, 0) + 1);
                    if (userAnswer != null && !userAnswer.trim().isEmpty() && userAnswer.equals(correctAnswer)) {
                        correctNumber++;
                        correctCountByType.put(questionType, correctCountByType.getOrDefault(questionType, 0) + 1);
                    }
                }

                detail.setUserAnswer(userAnswer != null ? userAnswer : "");
                userTestRecordDetailEnMapper.insertUserTestRecordDetailEn(detail);
            }

            Map<String, Integer> segmentCorrectCount = new HashMap<>();
            Map<String, Integer> segmentTotalCount = new HashMap<>();
            Map<String, String> segmentQuestionType = new HashMap<>();

            for (Map<String, Object> question : allQuestions) {
                String segmentId = (String) question.get("segment_id");
                String questionType = (String) question.get("type");
                if ("Writing".equals(questionType) || "Translation".equals(questionType)) continue;
                int questionNumber = (Integer) question.get("number");
                String correctAnswer = (String) question.get("correctAnswer");
                segmentQuestionType.put(segmentId, questionType);
                segmentTotalCount.put(segmentId, segmentTotalCount.getOrDefault(segmentId, 0) + 1);
                String userAnswer = "";
                if ("NewsReport".equals(questionType) || "Conversation".equals(questionType) || "ListeningPassage".equals(questionType)) {
                    Map<String, Object> listeningAnswers = answerMapper.getChildAnswerMap(answers, "listening");
                    String answerKey = segmentId + "-" + questionNumber;
                    userAnswer = String.valueOf(listeningAnswers.getOrDefault(answerKey, ""));
                    if (userAnswer == null || userAnswer.trim().isEmpty() || "null".equals(userAnswer)) {
                        userAnswer = String.valueOf(listeningAnswers.getOrDefault(String.valueOf(questionNumber), ""));
                    }
                } else if ("BlankedCloze".equals(questionType)) {
                    Map<String, Object> sectionAAnswers = answerMapper.getChildAnswerMap(answers, "sectionA");
                    String answerKey = segmentId + "-" + questionNumber;
                    userAnswer = String.valueOf(sectionAAnswers.getOrDefault(answerKey, ""));
                    if (userAnswer == null || userAnswer.trim().isEmpty() || "null".equals(userAnswer)) {
                        userAnswer = String.valueOf(sectionAAnswers.getOrDefault(String.valueOf(questionNumber), ""));
                    }
                } else if ("Matching".equals(questionType)) {
                    Map<String, Object> sectionBAnswers = answerMapper.getChildAnswerMap(answers, "sectionB");
                    String answerKey = segmentId + "-" + questionNumber;
                    userAnswer = String.valueOf(sectionBAnswers.getOrDefault(answerKey, ""));
                    if (userAnswer == null || userAnswer.trim().isEmpty() || "null".equals(userAnswer)) {
                        userAnswer = String.valueOf(sectionBAnswers.getOrDefault(String.valueOf(questionNumber), ""));
                    }
                } else if ("ReadingPassage".equals(questionType)) {
                    Map<String, Object> sectionCAnswers = answerMapper.getChildAnswerMap(answers, "sectionC");
                    String answerKey = segmentId + "-" + questionNumber;
                    userAnswer = String.valueOf(sectionCAnswers.getOrDefault(answerKey, ""));
                    if (userAnswer == null || userAnswer.trim().isEmpty() || "null".equals(userAnswer)) {
                        userAnswer = String.valueOf(sectionCAnswers.getOrDefault(String.valueOf(questionNumber), ""));
                    }
                }
                if (userAnswer != null && !userAnswer.trim().isEmpty() && userAnswer.equals(correctAnswer)) {
                    segmentCorrectCount.put(segmentId, segmentCorrectCount.getOrDefault(segmentId, 0) + 1);
                }
            }

            try {
                CompletableFuture.allOf(gradingTasks.toArray(new CompletableFuture<?>[0])).join();
            } finally {
                gradingExecutor.shutdown();
            }

            for (String segmentId : segmentTotalCount.keySet()) {
                String questionType = segmentQuestionType.get(segmentId);
                int correct = segmentCorrectCount.getOrDefault(segmentId, 0);
                int segmentQuestionCount = segmentTotalCount.get(segmentId);
                double segmentScore = 0.0;
                if (shouldCalculateScore) {
                    CET4ExamPaperEnCet4Structure structure = examPaperEnCet4StructureMapper.findBySegmentId(segmentId);
                    double perQuestionScore = (structure != null && structure.getScorePerQuestion() != null)
                            ? structure.getScorePerQuestion() : 0.0;
                    segmentScore = correct * perQuestionScore;
                }
                double correctPercent = segmentQuestionCount > 0 ? (correct / (double) segmentQuestionCount) * 100.0 : 0.0;
                CET4UserTestRecordSegmentEn rec = new CET4UserTestRecordSegmentEn();
                rec.setSegmentId(segmentId);
                rec.setQuestionType(questionType);
                rec.setScore(BigDecimal.valueOf(segmentScore).setScale(2, RoundingMode.HALF_UP).doubleValue());
                rec.setCorrectAnswersNumber(correct);
                rec.setNumberOfQuestions(segmentQuestionCount);
                rec.setCorrectAnswersPercent(BigDecimal.valueOf(correctPercent).setScale(2, RoundingMode.HALF_UP).doubleValue());
                rec.setTestEnId(testEnId);
                userTestRecordSegmentEnMapper.insert(rec);
            }

            double finalScore = 0.0;
            List<CET4UserTestRecordSegmentEn> segments = userTestRecordSegmentEnMapper.findByTestEnId(testEnId);
            for (CET4UserTestRecordSegmentEn seg : segments) {
                finalScore += seg.getScore() != null ? seg.getScore() : 0.0;
            }
            double finalScore2 = BigDecimal.valueOf(finalScore).setScale(2, RoundingMode.HALF_UP).doubleValue();
            testRecord.setCorrectNumber(correctNumber);
            testRecord.setTestEnScore(finalScore2);
            userTestRecordEnMapper.updateUserTestRecordEn(testRecord);

            result.put("success", true);
            result.put("message", "提交成功");
            result.put("testEnId", testEnId);
            result.put("finalScore", finalScore2);
            result.put("segmentCount", segments.size());
            List<Map<String, Object>> segmentScoreList = new ArrayList<>();
            for (CET4UserTestRecordSegmentEn seg : segments) {
                Map<String, Object> segMap = new HashMap<>();
                segMap.put("segmentId", seg.getSegmentId());
                segMap.put("questionType", seg.getQuestionType());
                segMap.put("score", seg.getScore());
                segmentScoreList.add(segMap);
            }
            result.put("segmentScores", segmentScoreList);
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.trim().isEmpty()) {
                errorMsg = "提交答案失败，后端发生未知异常";
            }
            result.put("success", false);
            result.put("message", errorMsg);
        }
        return result;
    }

    /**
     * 获取测试结果详情（英文试卷）
     */
    public Map<String, Object> getTestRecordEnDetails(Long testEnId) {
        Map<String, Object> result = new HashMap<>();
        try {
            CET4UserTestRecordEn testRecord = userTestRecordEnMapper.getUserTestRecordEnById(testEnId);

            if (testRecord == null) {
                result.put("success", false);
                result.put("message", "测试记录不存在");
                return result;
            }

            List<CET4UserTestRecordDetailEn> details = userTestRecordDetailEnMapper.getUserTestRecordDetailsEnByTestEnId(testEnId);

            Long examPaperEnId = testRecord.getExamPaperEnId();
            if (examPaperEnId == null) {
                throw new IllegalArgumentException("试卷ID为空");
            }
            CET4ExamPaperEn examPaperEn = examPaperEnService.getAllExamPapers().stream()
                    .filter(p -> p.getId().equals(examPaperEnId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("试卷不存在"));

            List<CET4SectionAChromaRecord> records = chromaEngExamPaperService.fetchExamPaperUnits(
                    String.valueOf(examPaperEnId), examPaperEn.getExamPaperEnName());

            Map<String, String> segmentDocuments = new HashMap<>();
            Map<String, Map<String, Object>> segmentMetadata = new HashMap<>();
            for (CET4SectionAChromaRecord record : records) {
                String segmentId = (String) record.metadata().get("segment_id");
                segmentDocuments.put(segmentId, record.document());
                segmentMetadata.put(segmentId, record.metadata());
            }

            List<Map<String, Object>> questionDetails = new ArrayList<>();

            for (CET4UserTestRecordDetailEn detail : details) {
                Map<String, Object> questionDetail = new HashMap<>();

                questionDetail.put("questionNumber", detail.getQuestionsEnNumber());
                questionDetail.put("questionType", detail.getQuestionsType());
                questionDetail.put("segmentId", detail.getSegmentId());
                questionDetail.put("correctAnswer", detail.getCorrectAnswer());
                questionDetail.put("userAnswer", detail.getUserAnswer());

                boolean isCorrect = false;
                if (detail.getCorrectAnswer() != null && detail.getUserAnswer() != null && !detail.getUserAnswer().trim().isEmpty()) {
                    isCorrect = detail.getCorrectAnswer().equals(detail.getUserAnswer());
                }
                questionDetail.put("isCorrect", isCorrect);

                String segmentId = detail.getSegmentId();
                if (segmentDocuments.containsKey(segmentId)) {
                    String document = segmentDocuments.get(segmentId);
                    questionDetail.put("questionDocument", document);
                    questionDetail.put("questionMetadata", segmentMetadata.get(segmentId));
                }

                String cacheKey = subjectiveGradingService.buildCacheKey(testEnId, detail.getSegmentId(), detail.getQuestionsEnNumber());
                Map<String, Object> cachedAiScore = subjectiveGradingService.getCachedAiScore(cacheKey);
                if (cachedAiScore != null) {
                    questionDetail.put("aiScore", cachedAiScore.get("score"));
                    questionDetail.put("aiFeedback", cachedAiScore.get("feedback"));
                    questionDetail.put("aiReasoning", cachedAiScore.get("reasoning"));
                }

                questionDetails.add(questionDetail);
            }

            Map<String, Map<String, Object>> chromaData = userTestRecordChromaService.fetchUserAnswersAndGrades(testEnId, examPaperEnId);

            for (CET4SectionAChromaRecord record : records) {
                Map<String, Object> metadata = record.metadata();
                String questionType = (String) metadata.get("question_type");
                String segmentId = (String) metadata.get("segment_id");

                if ("Writing".equals(questionType) || "Translation".equals(questionType)) {
                    Map<String, Object> questionDetail = new HashMap<>();

                    questionDetail.put("questionNumber", 1);
                    questionDetail.put("questionType", questionType);
                    questionDetail.put("segmentId", segmentId);
                    questionDetail.put("correctAnswer", "");
                    questionDetail.put("questionDocument", record.document());
                    questionDetail.put("questionMetadata", metadata);

                    Map<String, Object> chromaAnswerData = chromaData.get(segmentId);
                    if (chromaAnswerData != null) {
                        questionDetail.put("userAnswer", chromaAnswerData.get("userAnswer"));
                        questionDetail.put("aiScore", chromaAnswerData.get("score"));
                        questionDetail.put("aiFeedback", chromaAnswerData.get("feedback"));
                        questionDetail.put("aiReasoning", chromaAnswerData.get("reasoning"));
                    } else {
                        String cacheKey = subjectiveGradingService.buildCacheKey(testEnId, segmentId, 1);
                        Map<String, Object> cachedAiScore = subjectiveGradingService.getCachedAiScore(cacheKey);
                        if (cachedAiScore != null) {
                            questionDetail.put("userAnswer", cachedAiScore.get("userAnswer"));
                            questionDetail.put("aiScore", cachedAiScore.get("score"));
                            questionDetail.put("aiFeedback", cachedAiScore.get("feedback"));
                            questionDetail.put("aiReasoning", cachedAiScore.get("reasoning"));
                        } else {
                            questionDetail.put("userAnswer", "");
                        }
                    }

                    questionDetail.put("isCorrect", false);
                    questionDetails.add(questionDetail);
                }
            }

            result.put("success", true);
            result.put("testRecord", testRecord);
            result.put("questionDetails", questionDetails);
            result.put("examPaperEn", examPaperEn);

            if (testRecord.getTestEnScore() != null) {
                Double originalScore = testRecord.getTestEnScore();
                Double roundedScore = BigDecimal.valueOf(originalScore)
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();
                testRecord.setTestEnScore(roundedScore);
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "获取测试结果失败：" + e.getMessage());
        }
        return result;
    }

    private Integer mapSubjectNameToId(String subject) {
        if (subject == null) return null;
        String s = subject.trim();
        if ("CET4".equalsIgnoreCase(s)) return 17;
        return null;
    }
}
