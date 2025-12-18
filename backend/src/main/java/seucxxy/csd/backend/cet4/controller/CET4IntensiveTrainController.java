package seucxxy.csd.backend.cet4.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import seucxxy.csd.backend.common.entity.User;
import seucxxy.csd.backend.cet4.service.CET4IntensiveTrainService;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/intensive")
public class CET4IntensiveTrainController {
    private final CET4IntensiveTrainService intensiveTrainService;

    public CET4IntensiveTrainController(CET4IntensiveTrainService intensiveTrainService) {
        this.intensiveTrainService = intensiveTrainService;
    }

    @GetMapping("/question-types")
    public List<String> listQuestionTypes() {
        return intensiveTrainService.getQuestionTypes();
    }

    @PostMapping("/generate")
    public Map<String, Object> generate(HttpSession session,
                                        @RequestBody GenerateRequest request) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        // 生成题目内容但不保存
        return intensiveTrainService.generateIntensivePaperContentConcurrent(
            user.getId().longValue(),
            request.isFromWrongBank(),
            request.getTypes(),
            request.getCounts(),
            request.getExamPaperEnSource()
        );
    }

    @PostMapping("/generate/concurrent")
    public Map<String, Object> generateConcurrent(HttpSession session,
                                                  @RequestBody GenerateRequest request) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }

        return intensiveTrainService.generateIntensivePaperContentConcurrent(
                user.getId().longValue(),
                request.isFromWrongBank(),
                request.getTypes(),
                request.getCounts(),
                request.getExamPaperEnSource()
        );
    }
    
    @PostMapping("/save")
    public Map<String, Object> save(HttpSession session,
                                    @RequestBody SaveRequest request) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            throw new RuntimeException("用户未登录");
        }
        // 保存试卷
        Long paperId = intensiveTrainService.saveIntensivePaper(
                user.getId().longValue(),
                request.getPaperName(),
                request.getQuestionsData(),
                request.getExamPaperEnSource()
        );
        return Map.of("paperId", paperId);
    }

    public static class GenerateRequest {
        private List<String> types;
        private List<Integer> counts; // 与 types 对应
        private boolean fromWrongBank; // 是否从错题库选择
        private String examPaperEnSource; // 前端传入的来源（AIfromself/AIfromreal）

        public List<String> getTypes() { return types; }
        public void setTypes(List<String> types) { this.types = types; }
        public List<Integer> getCounts() { return counts; }
        public void setCounts(List<Integer> counts) { this.counts = counts; }
        public boolean isFromWrongBank() { return fromWrongBank; }
        public void setFromWrongBank(boolean fromWrongBank) { this.fromWrongBank = fromWrongBank; }
        public String getExamPaperEnSource() { return examPaperEnSource; }
        @JsonProperty("exam_paper_en_source")
        public void setExamPaperEnSource(String examPaperEnSource) { this.examPaperEnSource = examPaperEnSource; }
    }
    
    public static class SaveRequest {
        private String paperName;
        private List<Map<String, Object>> questionsData;
        private String examPaperEnSource;

        public String getPaperName() { return paperName; }
        public void setPaperName(String paperName) { this.paperName = paperName; }
        public List<Map<String, Object>> getQuestionsData() { return questionsData; }
        public void setQuestionsData(List<Map<String, Object>> questionsData) { this.questionsData = questionsData; }
        public String getExamPaperEnSource() { return examPaperEnSource; }
        @JsonProperty("exam_paper_en_source")
        public void setExamPaperEnSource(String examPaperEnSource) { this.examPaperEnSource = examPaperEnSource; }
    }
}
