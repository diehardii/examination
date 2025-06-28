package seucxxy.csd.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import seucxxy.csd.backend.dto.UserExamPaperDTO;
import seucxxy.csd.backend.entity.*;
import seucxxy.csd.backend.mapper.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ExamPaperService {
    private final ExamPaperMapper examPaperMapper;
    private final QuestionMapper questionMapper;
    private final UserExamPaperMapper userExamPaperMapper;
    private final UserTestRecordMapper userTestRecordMapper;
    private final UserTestRecordDetailMapper userTestRecordDetailMapper;
    private final RestTemplate restTemplate;

    @Value("${coze.api.url}")
    private String cozeApiUrl;

    @Value("${coze.api.token.paper.gen}")
    private String cozeApiToken;

    @Autowired
    public ExamPaperService(ExamPaperMapper examPaperMapper,
                            QuestionMapper questionMapper,
                            UserExamPaperMapper userExamPaperMapper,
                            UserTestRecordMapper userTestRecordMapper,
                            UserTestRecordDetailMapper userTestRecordDetailMapper,
                            RestTemplate restTemplate) {
        this.examPaperMapper = examPaperMapper;
        this.questionMapper = questionMapper;
        this.userExamPaperMapper = userExamPaperMapper;
        this.userTestRecordMapper = userTestRecordMapper;
        this.userTestRecordDetailMapper = userTestRecordDetailMapper;
         this.restTemplate = restTemplate;
    }



    public ExamPaper getExamPaperByIdWithQuestions(Long id) {
        return examPaperMapper.findExamPaperByIdWithQuestions(id);
    }

    public List<ExamPaper> getAvailableExamPapers(String username) {
        return examPaperMapper.findAvailableExamPapers(username);
    }

    @Transactional
    public ExamPaper generateExamPaper(String name, String content, String difficulty, String subject,int singleChoiceNumber,int judgeNumber) throws Exception {
        // 检查试卷名称是否已存在
        ExamPaper existingPaper = examPaperMapper.findExamPaperByName(name);
        if (existingPaper != null) {
            throw new Exception("试卷名称已存在");
        }

        // 构建请求到Coze API
        String requestBody = String.format("{\"workflow_id\": \"7517184496379838499\", \"parameters\": {\"content\": \"%s\", \"difficulty\": \"%s\", \"singleChoiceNumber\": %d, \"judgeNumber\": %d}}",
                content, difficulty,singleChoiceNumber,judgeNumber);
        System.out.println(requestBody);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + cozeApiToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(cozeApiUrl, HttpMethod.POST, requestEntity, String.class);

        // 解析响应
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(response.getBody());
        String dataValue = rootNode.path("data").asText();
        JsonNode dataNode = mapper.readTree(dataValue);

        // 解析问题
        String questionsJson = dataNode.get("questions").asText();
        List<Question> questions = parseQuestions(questionsJson);

        // 创建并保存试卷
        ExamPaper examPaper = new ExamPaper();
        examPaper.setExamPaperName(name);
        examPaper.setExamPaperContent(content);
        examPaper.setExamPaperDifficulty(difficulty);
        examPaper.setExamPaperQuestionNumber(questions.size());
        examPaper.setExamPaperSubject(subject);

        examPaperMapper.insertExamPaper(examPaper);

        // 保存问题
        batchInsertQuestions(questions, examPaper.getId());

        // 设置关联关系
        examPaper.setQuestions(questions);
        return examPaper;
    }

    public void batchInsertQuestions(List<Question> questions, Long examPaperId) {
        if (questions == null || questions.isEmpty()) {
            System.out.println("题目列表为空，无需插入");
            return;
        }

        int successCount = 0;

        for (Question question : questions) {
            try {
                // 设置试卷ID
                question.setExamPaperId(examPaperId);

                // 插入主表
                int result = questionMapper.insertQuestion(question);

                if (result > 0) {
                    successCount++;
                    // 根据不同类型打印不同信息
                    if (question.getQuestionType().equals("SINGLE_CHOICE")) {
                        SingleChoiceQuestion scq = (SingleChoiceQuestion) question;
                        System.out.printf("成功插入单选题: ID=%d, 内容=%s, 选项A=%s, 正确答案=%s%n",
                                question.getId(), question.getContent(),
                                scq.getOptionA(), question.getCorrectAnswer());
                    } else if (question.getQuestionType().equals("JUDGE")) {
                        System.out.printf("成功插入判断题: ID=%d, 内容=%s, 正确答案=%s%n",
                                question.getId(), question.getContent(),
                                question.getCorrectAnswer());
                    }
                } else {
                    System.out.println("插入失败: " + question.getContent());
                }
            } catch (Exception e) {
                System.out.println("插入题目异常: " + question.getContent() + ", 错误: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.printf("插入完成! 成功插入%d条，共%d条%n", successCount, questions.size());
    }

    private List<Question> parseQuestions(String questionsJson) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode questionsNode = mapper.readTree(questionsJson);
        List<Question> questions = new ArrayList<>();

        for (JsonNode questionNode : questionsNode) {
            String type = questionNode.path("type").asText();
            String content = questionNode.path("question").asText();
            int number = questionNode.path("number").asInt();

            if ("单选题".equals(type)) {
                SingleChoiceQuestion scq = new SingleChoiceQuestion();
                scq.setType(type);
                scq.setQuestionType("SINGLE_CHOICE");
                scq.setContent(content);
                scq.setNumber(number);
                scq.setOptionA(questionNode.path("optionA").asText());
                scq.setOptionB(questionNode.path("optionB").asText());
                scq.setOptionC(questionNode.path("optionC").asText());
                scq.setOptionD(questionNode.path("optionD").asText());
                scq.setCorrectAnswer(questionNode.path("correctAnswer").asText());

                questions.add(scq);
            } else if ("是非题".equals(type)) {
                JudgeQuestion jq = new JudgeQuestion();
                jq.setType(type);
                jq.setQuestionType("JUDGE");
                jq.setContent(content);
                jq.setNumber(number);
                jq.setCorrectAnswer(questionNode.path("correctAnswer").asText());
                questions.add(jq);
            }
        }
        System.out.println(questions.toString());
        return questions;
    }


    public List<ExamPaper> getAllExamPapers() {
        return examPaperMapper.findAll();
    }

    @Transactional
    public void deleteExamPaperCascade(Long examPaperId) {
        // 1. Delete user_test_record_detail records first
        userTestRecordDetailMapper.deleteDetailsByExamPaperId(examPaperId);

        // 2. Delete user_test_record records
        userTestRecordMapper.deleteByExamPaperId(examPaperId);

        // 3. Delete user_exam_paper records
        userExamPaperMapper.deleteByExamPaperId(examPaperId);

        // 4. Delete questions
        questionMapper.deleteByExamPaperId(examPaperId);

        // 5. Finally delete the exam paper
        examPaperMapper.deleteById(examPaperId);
    }
}