package seucxxy.csd.backend.hs3.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seucxxy.csd.backend.common.entity.User;
import seucxxy.csd.backend.hs3.entity.*;
import seucxxy.csd.backend.hs3.mapper.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * HS3高考英语考试提交服务
 */
@Service
public class HS3PaperTestService {

    private static final Logger logger = LoggerFactory.getLogger(HS3PaperTestService.class);

    private final HS3ChromaEngExamPaperService chromaEngExamPaperService;
    private final HS3UserTestRecordEnMapper userTestRecordEnMapper;
    private final HS3UserTestRecordDetailEnMapper userTestRecordDetailEnMapper;
    private final HS3UserTestRecordSegmentEnMapper userTestRecordSegmentEnMapper;
    private final HS3UserTestRecordSubjectiveAnswerEnMapper userTestRecordSubjectiveAnswerEnMapper;
    private final HS3ExamPaperEnMapper examPaperEnMapper;
    private final HS3CozeAIGradingService cozeAIGradingService;
    private final ObjectMapper objectMapper;

    @Autowired
    public HS3PaperTestService(
            HS3ChromaEngExamPaperService chromaEngExamPaperService,
            HS3UserTestRecordEnMapper userTestRecordEnMapper,
            HS3UserTestRecordDetailEnMapper userTestRecordDetailEnMapper,
            HS3UserTestRecordSegmentEnMapper userTestRecordSegmentEnMapper,
            HS3UserTestRecordSubjectiveAnswerEnMapper userTestRecordSubjectiveAnswerEnMapper,
            HS3ExamPaperEnMapper examPaperEnMapper,
            HS3CozeAIGradingService cozeAIGradingService,
            ObjectMapper objectMapper) {
        this.chromaEngExamPaperService = chromaEngExamPaperService;
        this.userTestRecordEnMapper = userTestRecordEnMapper;
        this.userTestRecordDetailEnMapper = userTestRecordDetailEnMapper;
        this.userTestRecordSegmentEnMapper = userTestRecordSegmentEnMapper;
        this.userTestRecordSubjectiveAnswerEnMapper = userTestRecordSubjectiveAnswerEnMapper;
        this.examPaperEnMapper = examPaperEnMapper;
        this.cozeAIGradingService = cozeAIGradingService;
        this.objectMapper = objectMapper;
    }

    /**
     * 提交答案（高考英语试卷）
     */
    public Map<String, Object> submitAnswerEn(Map<String, Object> request, User user) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long examPaperEnId = Long.parseLong(request.get("examPaperEnId").toString());
            @SuppressWarnings("unchecked")
            Map<String, Object> answers = (Map<String, Object>) request.get("answers");
            if (answers == null) {
                answers = new HashMap<>();
            }

            // 获取试卷信息
            HS3ExamPaperEn examPaperEn = examPaperEnMapper.getExamPaperEnById(examPaperEnId);
            if (examPaperEn == null) {
                result.put("success", false);
                result.put("message", "试卷不存在");
                return result;
            }

            logger.info("[HS3-Submit] 用户{}开始提交试卷{}", user.getId(), examPaperEn.getExamPaperEnName());

            // 从ChromaDB获取试卷所有segment
            List<Map<String, Object>> segments = chromaEngExamPaperService.fetchExamPaperSegments(
                    String.valueOf(examPaperEnId), examPaperEn.getExamPaperEnName());

            // 创建考试记录
            HS3UserTestRecordEn testRecord = new HS3UserTestRecordEn();
            testRecord.setExamPaperEnId(examPaperEnId);
            testRecord.setUserId(user.getId());
            testRecord.setTestEnTime(LocalDateTime.now());
            testRecord.setCorrectNumber(0);
            testRecord.setTestEnScore(0.0);
            userTestRecordEnMapper.insertUserTestRecordEn(testRecord);
            Long testEnId = testRecord.getTestEnId();

            logger.info("[HS3-Submit] 创建考试记录, testEnId={}", testEnId);

            // 用于统计
            int totalCorrectNumber = 0;
            double totalScore = 0.0;
            Map<String, SegmentStats> segmentStatsMap = new HashMap<>();

            // 处理每个segment
            for (Map<String, Object> segmentData : segments) {
                String document = (String) segmentData.get("document");
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) segmentData.get("metadata");
                // ChromaDB中只有segment_name字段，用它作为数据库的segment_id
                String segmentName = (String) metadata.get("segment_name");
                String partName = (String) metadata.get("part_name");
                String sectionName = (String) metadata.get("section_name");

                JsonNode docNode;
                try {
                    docNode = objectMapper.readTree(document);
                } catch (Exception e) {
                    logger.error("[HS3-Submit] 解析document失败: {}", e.getMessage());
                    continue;
                }

                // 从part_name和section_name确定题型
                String questionType = determineQuestionType(partName, sectionName);
                
                logger.info("[HS3-Submit] 处理segment: {}, part: {}, section: {}, type: {}", 
                    segmentName, partName, sectionName, questionType);

                // 处理非客观题（续写、应用文写作）
                if ("读后续写".equals(questionType) || "应用文写作".equals(questionType)) {
                    String userAnswer = extractSubjectiveAnswer(answers, segmentName, questionType);
                    
                    int userScore = 0;
                    String gradeBase = "";
                    String userAnswerAna = "";
                    
                    // 只有当写作内容不为空时才调用Coze AI评分
                    if (userAnswer != null && !userAnswer.trim().isEmpty()) {
                        Map<String, Object> gradeResult = cozeAIGradingService.gradeSubjectiveQuestion(document, userAnswer);
                        userScore = (Integer) gradeResult.get("user_score");
                        gradeBase = (String) gradeResult.get("grade_base");
                        userAnswerAna = (String) gradeResult.get("user_answer_ana");
                    } else {
                        logger.info("[HS3-Submit] 主观题用户答案为空，跳过AI评分: segment={}", segmentName);
                        gradeBase = "未作答";
                        userAnswerAna = "用户未提交写作内容，无法进行评分。";
                    }

                    // 保存到主观题答案表
                    HS3UserTestRecordSubjectiveAnswerEn subjectiveAnswer = new HS3UserTestRecordSubjectiveAnswerEn();
                    subjectiveAnswer.setTestEnId(testEnId);
                    subjectiveAnswer.setSegmentName(segmentName);
                    subjectiveAnswer.setUserAnswer(userAnswer);
                    subjectiveAnswer.setUserAnswerGrade(gradeBase + "\n\n" + userAnswerAna);
                    userTestRecordSubjectiveAnswerEnMapper.insert(subjectiveAnswer);

                    // 保存到segment统计表 - 数据库字段segment_id用segmentName赋值
                    HS3UserTestRecordSegmentEn segmentRecord = new HS3UserTestRecordSegmentEn();
                    segmentRecord.setSegmentId(segmentName);
                    segmentRecord.setQuestionType(questionType);
                    segmentRecord.setScore((double) userScore);
                    segmentRecord.setCorrectAnswersNumber(userScore > 0 ? 1 : 0);
                    segmentRecord.setNumberOfQuestions(1);
                    segmentRecord.setCorrectAnswersPercent(userScore > 0 ? 100.0 : 0.0);
                    segmentRecord.setTestEnId(testEnId);
                    userTestRecordSegmentEnMapper.insert(segmentRecord);

                    totalScore += userScore;
                    if (userScore > 0) {
                        totalCorrectNumber++;
                    }

                    logger.info("[HS3-Submit] 主观题评分完成: segment={}, score={}", segmentName, userScore);
                    continue;
                }

                // 处理客观题
                List<QuestionInfo> questions = extractQuestionsFromDocument(docNode, metadata, questionType);
                if (questions.isEmpty()) {
                    logger.warn("[HS3-Submit] segment {} 没有找到题目", segmentName);
                    continue;
                }

                SegmentStats stats = segmentStatsMap.computeIfAbsent(segmentName, k -> new SegmentStats());
                stats.questionType = questionType;
                stats.segmentId = segmentName;

                // 处理每道客观题
                for (QuestionInfo question : questions) {
                    int questionNumber = question.questionNumber;
                    String correctAnswer = question.correctAnswer;
                    double questionScore = question.score;

                    // 获取用户答案
                    String userAnswer = extractObjectiveAnswer(answers, segmentName, questionNumber, questionType);

                    // 判断正误
                    boolean isCorrect = userAnswer != null && userAnswer.equals(correctAnswer);
                    if (isCorrect) {
                        stats.correctCount++;
                        totalCorrectNumber++;
                        stats.score += questionScore;
                        totalScore += questionScore;
                    }
                    stats.totalCount++;

                    // 保存到detail表 - 数据库字段segment_id用segmentName赋值
                    HS3UserTestRecordDetailEn detail = new HS3UserTestRecordDetailEn();
                    detail.setQuestionsEnNumber(questionNumber);
                    detail.setQuestionsType(questionType);
                    detail.setCorrectAnswer(correctAnswer);
                    detail.setSegmentId(segmentName);
                    detail.setUserAnswer(userAnswer != null ? userAnswer : "");
                    detail.setTestEnId(testEnId);
                    userTestRecordDetailEnMapper.insertUserTestRecordDetailEn(detail);
                }
            }

            // 保存segment统计
            for (SegmentStats stats : segmentStatsMap.values()) {
                HS3UserTestRecordSegmentEn segmentRecord = new HS3UserTestRecordSegmentEn();
                segmentRecord.setSegmentId(stats.segmentId);
                segmentRecord.setQuestionType(stats.questionType);
                segmentRecord.setScore(BigDecimal.valueOf(stats.score).setScale(2, RoundingMode.HALF_UP).doubleValue());
                segmentRecord.setCorrectAnswersNumber(stats.correctCount);
                segmentRecord.setNumberOfQuestions(stats.totalCount);
                double percent = stats.totalCount > 0 ? (stats.correctCount * 100.0 / stats.totalCount) : 0.0;
                segmentRecord.setCorrectAnswersPercent(BigDecimal.valueOf(percent).setScale(2, RoundingMode.HALF_UP).doubleValue());
                segmentRecord.setTestEnId(testEnId);
                userTestRecordSegmentEnMapper.insert(segmentRecord);
            }

            // 更新总记录
            testRecord.setCorrectNumber(totalCorrectNumber);
            testRecord.setTestEnScore(BigDecimal.valueOf(totalScore).setScale(2, RoundingMode.HALF_UP).doubleValue());
            userTestRecordEnMapper.updateUserTestRecordEn(testRecord);

            logger.info("[HS3-Submit] 提交完成: testEnId={}, correctNumber={}, totalScore={}",
                    testEnId, totalCorrectNumber, totalScore);

            result.put("success", true);
            result.put("message", "提交成功");
            result.put("testEnId", testEnId);
            result.put("correctNumber", totalCorrectNumber);
            result.put("totalScore", BigDecimal.valueOf(totalScore).setScale(2, RoundingMode.HALF_UP).doubleValue());

        } catch (Exception e) {
            logger.error("[HS3-Submit] 提交失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "提交失败: " + e.getMessage());
        }

        return result;
    }

    // 辅助类：segment统计
    private static class SegmentStats {
        String segmentId;
        String questionType;
        int correctCount = 0;
        int totalCount = 0;
        double score = 0.0;
    }

    // 辅助类：题目信息
    private static class QuestionInfo {
        int questionNumber;
        String correctAnswer;
        double score;
    }

    /**
     * 从document中提取题目列表
     * 支持多种数据格式：
     * - questions.items[] (Coze新版格式)
     * - questions[] (直接数组)
     * - question_and_options[] (旧格式)
     */
    private List<QuestionInfo> extractQuestionsFromDocument(JsonNode docNode, Map<String, Object> metadata, String questionType) {
        List<QuestionInfo> questions = new ArrayList<>();

        try {
            // 获取每道题的默认分值
            double defaultScore = getDefaultScoreByType(questionType);
            
            // 尝试多种可能的题目数组路径
            JsonNode questionsNode = null;
            
            // 1. 优先检查 questions.items（Coze新版格式）
            JsonNode questionsWrapper = docNode.path("questions");
            if (questionsWrapper.isObject() && questionsWrapper.has("items")) {
                questionsNode = questionsWrapper.path("items");
                logger.debug("[HS3-Submit] 使用 questions.items 格式");
            }
            // 2. questions 直接是数组
            else if (questionsWrapper.isArray() && questionsWrapper.size() > 0) {
                questionsNode = questionsWrapper;
                logger.debug("[HS3-Submit] 使用 questions[] 格式");
            }
            // 3. question_and_options 格式
            else {
                JsonNode qaoNode = docNode.path("question_and_options");
                if (qaoNode.isArray() && qaoNode.size() > 0) {
                    questionsNode = qaoNode;
                    logger.debug("[HS3-Submit] 使用 question_and_options[] 格式");
                }
            }

            if (questionsNode != null && questionsNode.isArray()) {
                for (JsonNode qNode : questionsNode) {
                    QuestionInfo qi = new QuestionInfo();
                    
                    // 题号 - 尝试多个字段名
                    qi.questionNumber = qNode.path("question_number").asInt(0);
                    if (qi.questionNumber == 0) {
                        qi.questionNumber = qNode.path("questionNumber").asInt(0);
                    }
                    
                    // 正确答案 - 尝试多个字段名 (Coze格式用 answer)
                    qi.correctAnswer = qNode.path("answer").asText("");
                    if (qi.correctAnswer.isEmpty()) {
                        qi.correctAnswer = qNode.path("correct_answer").asText("");
                    }
                    if (qi.correctAnswer.isEmpty()) {
                        qi.correctAnswer = qNode.path("correctAnswer").asText("");
                    }
                    
                    // 分值
                    qi.score = qNode.path("question_score").asDouble(0);
                    if (qi.score <= 0) {
                        qi.score = qNode.path("score").asDouble(defaultScore);
                    }
                    if (qi.score <= 0) {
                        qi.score = defaultScore;
                    }
                    
                    // 只有题号和答案都有效才添加
                    if (qi.questionNumber > 0 && !qi.correctAnswer.isEmpty()) {
                        questions.add(qi);
                        logger.debug("[HS3-Submit] 提取题目: number={}, answer={}, score={}", 
                            qi.questionNumber, qi.correctAnswer, qi.score);
                    }
                }
            }

            logger.info("[HS3-Submit] 从document提取到 {} 道题目, questionType={}", questions.size(), questionType);

        } catch (Exception e) {
            logger.error("[HS3-Submit] 解析题目失败: {}", e.getMessage(), e);
        }

        return questions;
    }

    /**
     * 根据题型获取默认分值
     */
    private double getDefaultScoreByType(String questionType) {
        if (questionType == null) return 1.0;
        switch (questionType) {
            case "听力": return 1.5;
            case "阅读理解": return 2.5;
            case "七选五": return 2.5;
            case "完形填空": return 1.0;
            case "语法填空": return 1.5;
            default: return 1.0;
        }
    }

    /**
     * 提取主观题答案
     */
    private String extractSubjectiveAnswer(Map<String, Object> answers, String segmentId, String questionType) {
        try {
            if ("读后续写".equals(questionType)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> writingAnswers = (Map<String, Object>) answers.get("writing");
                if (writingAnswers != null && writingAnswers.containsKey(segmentId)) {
                    return String.valueOf(writingAnswers.get(segmentId));
                }
            } else if ("应用文写作".equals(questionType)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> applicationAnswers = (Map<String, Object>) answers.get("application");
                if (applicationAnswers != null && applicationAnswers.containsKey(segmentId)) {
                    return String.valueOf(applicationAnswers.get(segmentId));
                }
            }
        } catch (Exception e) {
            logger.error("[HS3-Submit] 提取主观题答案失败: {}", e.getMessage());
        }
        return "";
    }

    /**
     * 提取客观题答案
     */
    private String extractObjectiveAnswer(Map<String, Object> answers, String segmentId, int questionNumber, String questionType) {
        try {
            String answerKey = segmentId + "-" + questionNumber;
            
            if ("听力".equals(questionType)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> listeningAnswers = (Map<String, Object>) answers.get("listening");
                if (listeningAnswers != null) {
                    if (listeningAnswers.containsKey(answerKey)) {
                        return String.valueOf(listeningAnswers.get(answerKey));
                    }
                    if (listeningAnswers.containsKey(String.valueOf(questionNumber))) {
                        return String.valueOf(listeningAnswers.get(String.valueOf(questionNumber)));
                    }
                }
            } else if ("阅读理解".equals(questionType) || "七选五".equals(questionType)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> readingAnswers = (Map<String, Object>) answers.get("reading");
                if (readingAnswers != null) {
                    if (readingAnswers.containsKey(answerKey)) {
                        return String.valueOf(readingAnswers.get(answerKey));
                    }
                    if (readingAnswers.containsKey(String.valueOf(questionNumber))) {
                        return String.valueOf(readingAnswers.get(String.valueOf(questionNumber)));
                    }
                }
            } else if ("完形填空".equals(questionType)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> clozeAnswers = (Map<String, Object>) answers.get("cloze");
                if (clozeAnswers != null) {
                    if (clozeAnswers.containsKey(answerKey)) {
                        return String.valueOf(clozeAnswers.get(answerKey));
                    }
                    if (clozeAnswers.containsKey(String.valueOf(questionNumber))) {
                        return String.valueOf(clozeAnswers.get(String.valueOf(questionNumber)));
                    }
                }
            } else if ("语法填空".equals(questionType)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> grammarAnswers = (Map<String, Object>) answers.get("grammar");
                if (grammarAnswers != null) {
                    if (grammarAnswers.containsKey(answerKey)) {
                        return String.valueOf(grammarAnswers.get(answerKey));
                    }
                    if (grammarAnswers.containsKey(String.valueOf(questionNumber))) {
                        return String.valueOf(grammarAnswers.get(String.valueOf(questionNumber)));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("[HS3-Submit] 提取客观题答案失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 根据part_name和section_name严格匹配确定题型
     */
    private String determineQuestionType(String partName, String sectionName) {
        if (partName == null || sectionName == null) {
            logger.warn("[HS3-Submit] part_name或section_name为空");
            return "未知";
        }

        // 根据 part_name 识别标准化题型
        String normalizedPart = partName.toLowerCase();
        String normalizedSection = sectionName.toLowerCase();
        
        // 听力
        if (normalizedPart.contains("听力")) {
            return "听力";
        }
        
        // 阅读理解
        if (normalizedPart.contains("阅读")) {
            if (normalizedSection.contains("七选五")) {
                return "七选五";
            }
            return "阅读理解";
        }
        
        // 语言知识运用
        if (normalizedPart.contains("语言知识") || normalizedPart.contains("语言运用")) {
            if (normalizedSection.contains("完形") || normalizedSection.contains("cloze")) {
                return "完形填空";
            }
            if (normalizedSection.contains("语法") || normalizedSection.contains("grammar")) {
                return "语法填空";
            }
        }
        
        // 写作
        if (normalizedPart.contains("写作") || normalizedPart.contains("writing")) {
            if (normalizedSection.contains("续写") || normalizedSection.contains("读后")) {
                return "读后续写";
            }
            return "应用文写作";
        }
        
        logger.warn("[HS3-Submit] 未能识别题型: partName={}, sectionName={}", partName, sectionName);
        return "未知";
    }

    /**
     * 获取测试结果详情（包含原题内容）
     */
    public Map<String, Object> getTestRecordEnDetails(Long testEnId) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 获取考试记录
            HS3UserTestRecordEn testRecord = userTestRecordEnMapper.getUserTestRecordEnById(testEnId);
            if (testRecord == null) {
                result.put("success", false);
                result.put("message", "测试记录不存在");
                return result;
            }

            // 获取答题详情
            List<HS3UserTestRecordDetailEn> details = userTestRecordDetailEnMapper.getUserTestRecordDetailsEnByTestEnId(testEnId);

            // 获取试卷信息
            Long examPaperEnId = testRecord.getExamPaperEnId();
            if (examPaperEnId == null) {
                throw new IllegalArgumentException("试卷ID为空");
            }
            HS3ExamPaperEn examPaperEn = examPaperEnMapper.getExamPaperEnById(examPaperEnId);
            if (examPaperEn == null) {
                throw new IllegalArgumentException("试卷不存在");
            }

            // 从ChromaDB获取试卷segment文档
            List<Map<String, Object>> segments = chromaEngExamPaperService.fetchExamPaperSegments(
                    String.valueOf(examPaperEnId), examPaperEn.getExamPaperEnName());
            
            logger.info("[HS3-Test] 从ChromaDB获取到 {} 个segment", segments.size());

            // 构建 segment_name -> document 映射 (ChromaDB中只有segment_name字段)
            Map<String, String> segmentDocuments = new HashMap<>();
            Map<String, Map<String, Object>> segmentMetadata = new HashMap<>();
            for (Map<String, Object> segment : segments) {
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) segment.get("metadata");
                if (metadata != null) {
                    String segmentName = String.valueOf(metadata.get("segment_name"));
                    String document = (String) segment.get("document");
                    segmentDocuments.put(segmentName, document);
                    segmentMetadata.put(segmentName, metadata);
                    logger.debug("[HS3-Test] 加载segment: name={}, docLen={}", segmentName, document != null ? document.length() : 0);
                }
            }
            logger.info("[HS3-Test] ChromaDB segment映射: {}", segmentDocuments.keySet());

            // 构建答题详情列表（包含原题）
            List<Map<String, Object>> questionDetails = new ArrayList<>();
            
            // 记录数据库中所有的segment_id用于调试
            Set<String> dbSegmentIds = new HashSet<>();
            for (HS3UserTestRecordDetailEn detail : details) {
                dbSegmentIds.add(detail.getSegmentId());
            }
            logger.info("[HS3-Test] 数据库中的segment_id: {}", dbSegmentIds);
            
            for (HS3UserTestRecordDetailEn detail : details) {
                Map<String, Object> questionDetail = new HashMap<>();
                
                questionDetail.put("questionNumber", detail.getQuestionsEnNumber());
                questionDetail.put("questionType", detail.getQuestionsType());
                questionDetail.put("segmentId", detail.getSegmentId());
                questionDetail.put("correctAnswer", detail.getCorrectAnswer());
                questionDetail.put("userAnswer", detail.getUserAnswer());
                
                // 判断是否正确
                boolean isCorrect = false;
                if (detail.getCorrectAnswer() != null && detail.getUserAnswer() != null 
                        && !detail.getUserAnswer().trim().isEmpty()) {
                    isCorrect = detail.getCorrectAnswer().equalsIgnoreCase(detail.getUserAnswer().trim());
                }
                questionDetail.put("isCorrect", isCorrect);
                
                // 添加原题文档 - 数据库segment_id字段存的是segment_name值
                // 兼容旧数据：旧数据的segment_id可能是 "examPaperId_segmentName" 格式
                String dbSegmentId = detail.getSegmentId();
                String segmentName = dbSegmentId;
                
                // 如果直接匹配不到，尝试去掉可能的examPaperId_前缀
                if (!segmentDocuments.containsKey(segmentName) && segmentName != null && segmentName.contains("_")) {
                    // 旧格式: "examPaperId_segmentName"，提取下划线后面的部分
                    int underscoreIdx = segmentName.indexOf("_");
                    if (underscoreIdx > 0 && underscoreIdx < segmentName.length() - 1) {
                        segmentName = segmentName.substring(underscoreIdx + 1);
                    }
                }
                
                if (segmentDocuments.containsKey(segmentName)) {
                    questionDetail.put("questionDocument", segmentDocuments.get(segmentName));
                    questionDetail.put("questionMetadata", segmentMetadata.get(segmentName));
                } else {
                    logger.warn("[HS3-Test] 未找到segment文档: dbSegmentId={}, segmentName={}", dbSegmentId, segmentName);
                }
                
                questionDetails.add(questionDetail);
            }

            // 处理主观题（应用文写作、读后续写）
            List<HS3UserTestRecordSubjectiveAnswerEn> subjectiveAnswers = 
                    userTestRecordSubjectiveAnswerEnMapper.findByTestEnId(testEnId);
            
            for (Map<String, Object> segment : segments) {
                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) segment.get("metadata");
                if (metadata == null) continue;
                
                String partName = String.valueOf(metadata.getOrDefault("part_name", ""));
                String sectionName = String.valueOf(metadata.getOrDefault("section_name", ""));
                String segmentName = String.valueOf(metadata.get("segment_name"));
                
                // 判断是否是主观题
                if (partName.contains("写作") || sectionName.contains("写作") || sectionName.contains("续写")) {
                    Map<String, Object> questionDetail = new HashMap<>();
                    
                    String questionType = sectionName.contains("续写") ? "读后续写" : "应用文写作";
                    questionDetail.put("questionNumber", 1);
                    questionDetail.put("questionType", questionType);
                    questionDetail.put("segmentId", segmentName);
                    questionDetail.put("correctAnswer", "");
                    questionDetail.put("questionDocument", segment.get("document"));
                    questionDetail.put("questionMetadata", metadata);
                    questionDetail.put("isCorrect", false);
                    
                    // 查找用户答案和AI评分 - 使用segment_name匹配
                    for (HS3UserTestRecordSubjectiveAnswerEn subjective : subjectiveAnswers) {
                        // 主观题答案表存储的是segment_name
                        if (segmentName.equals(subjective.getSegmentName())) {
                            questionDetail.put("userAnswer", subjective.getUserAnswer());
                            questionDetail.put("aiGrade", subjective.getUserAnswerGrade());
                            break;
                        }
                    }
                    
                    questionDetails.add(questionDetail);
                }
            }

            // 获取segment统计
            List<HS3UserTestRecordSegmentEn> segmentStats = userTestRecordSegmentEnMapper.findByTestEnId(testEnId);

            // 确保分数格式
            if (testRecord.getTestEnScore() != null) {
                Double originalScore = testRecord.getTestEnScore();
                Double roundedScore = BigDecimal.valueOf(originalScore)
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();
                testRecord.setTestEnScore(roundedScore);
            }

            result.put("success", true);
            result.put("testRecord", testRecord);
            result.put("questionDetails", questionDetails);
            result.put("segments", segmentStats);
            result.put("examPaperEn", examPaperEn);

        } catch (Exception e) {
            logger.error("[HS3-Test] 获取测试结果详情失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "获取测试结果失败：" + e.getMessage());
        }
        return result;
    }
}
