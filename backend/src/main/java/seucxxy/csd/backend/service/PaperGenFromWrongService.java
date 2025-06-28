package seucxxy.csd.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import seucxxy.csd.backend.entity.*;
import seucxxy.csd.backend.mapper.*;

import java.util.*;

@Service
public class PaperGenFromWrongService {

    private final QuestionMapper questionMapper;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ExamPaperMapper examPaperMapper;
    private final UserDetailMapper userDetailMapper;
    private final UserExamPaperService userExamPaperService;

    public PaperGenFromWrongService(
            QuestionMapper questionMapper,
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper,
            ExamPaperMapper examPaperMapper, UserDetailMapper userDetailMapper, UserExamPaperService userExamPaperService)
     {

        this.questionMapper = questionMapper;
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
        this.examPaperMapper = examPaperMapper;
         this.userDetailMapper = userDetailMapper;
         this.userExamPaperService = userExamPaperService;
     }


    public ExamPaper PaperGenFromWrongApi(long examPaperId,String name, String content, String difficulty, String subject,int singleChoiceNumber,int judgeNumber) {
        String examResultsJson = getExamResultsJson(examPaperId);
        try {
            return callCozePaperGenFromWrongApi(examResultsJson, name, content, difficulty, subject, singleChoiceNumber, judgeNumber);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String getExamResultsJson(long examPaperId) {
        List<Map<String, Object>> examResults = new ArrayList<>();


            // 获取单选题
            List<SingleChoiceQuestion> singleChoiceQuestions =
                    questionMapper.findSCByExamPaperId(examPaperId);

            for (SingleChoiceQuestion question : singleChoiceQuestions) {
                Map<String, Object> questionData = new HashMap<>();
                questionData.put("question", question.getContent());

                List<Map<String, String>> options = new ArrayList<>();
                options.add(Map.of("label", "A", "text", question.getOptionA()));
                options.add(Map.of("label", "B", "text", question.getOptionB()));
                options.add(Map.of("label", "C", "text", question.getOptionC()));
                options.add(Map.of("label", "D", "text", question.getOptionD()));

                questionData.put("options", options);
                questionData.put("correctAnswer", question.getCorrectAnswer());

                examResults.add(questionData);
            }

            // 获取判断题
            List<JudgeQuestion> judgeQuestions =
                    questionMapper.findJQByExamPaperId(examPaperId);

            for (JudgeQuestion question : judgeQuestions) {
                Map<String, Object> questionData = new HashMap<>();
                questionData.put("question", question.getContent());
                questionData.put("correctAnswer", question.getCorrectAnswer());
                  examResults.add(questionData);
            }
        try {
            return objectMapper.writeValueAsString(examResults);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize exam results", e);
        }
    }

    private ExamPaper callCozePaperGenFromWrongApi(String examResultsJson,String name, String content, String difficulty, String subject,int singleChoiceNumber,int judgeNumber) throws JsonProcessingException {
        // 清理JSON字符串
        String cleanJson = examResultsJson
                .replaceAll("\\s+", "")
                .replaceAll("\"", "")
                .replaceAll("[{}[\\\\]]", "");

        System.out.println(cleanJson);
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("workflow_id", "7519341985825407012");

        Map<String, String> parameters = new HashMap<>();
        parameters.put("examResult", cleanJson);
        parameters.put("singleChoiceNumber", Integer.toString(singleChoiceNumber));
        parameters.put("judgeNumber", Integer.toString(judgeNumber));
        requestBody.put("parameters", parameters);


        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer pat_OTmQLzBzAYHoTtGkWskQAeKgC9uQ1X5PvbawypMLZmfyX0PmiK7kgaThbKtAJpDN");

        System.out.println(requestBody.toString());
        // 发送请求
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                "https://api.coze.cn/v1/workflow/run",
                HttpMethod.POST,
                entity,
                String.class
        );


        // 解析响应
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(response.getBody());
        String dataValue = rootNode.path("data").asText();
        JsonNode dataNode = mapper.readTree(dataValue);

        // 解析问题
        String questionsJson = dataNode.get("questions").asText();
        List<Question> questions = null;
        try {
            questions = parseQuestions(questionsJson);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

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
}