package seucxxy.csd.backend.cet4.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Python DeepSeek服务调用类
 */
@Service
public class CET4PythonDeepSeekService {

    private static final String PYTHON_API_URL = "http://localhost:5000/api/cet4/generate-exam";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public CET4PythonDeepSeekService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 调用Python DeepSeek服务生成试卷
     * 
     * @param examTopic 考试主题
     * @param inputExamPaperSamp 输入的试卷样例
     * @param examPaperEnSource 试卷来源
     * @param segmentIdSelf 段落ID
     * @return 生成结果
     * @throws Exception 如果调用失败
     */
    public Map<String, Object> callPythonDeepSeek(String examTopic, String inputExamPaperSamp, 
                                                    String examPaperEnSource, String segmentIdSelf) throws Exception {
        // 打印开始信息
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        int contentLength = inputExamPaperSamp != null ? inputExamPaperSamp.length() : 0;
        

        try {
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("inputExamPaperSamp", inputExamPaperSamp);
            requestBody.put("examTopic", examTopic);
            requestBody.put("model", "deepseek-reasoner");
            
            if (examPaperEnSource != null && !examPaperEnSource.trim().isEmpty()) {
                requestBody.put("examPaperEnSource", examPaperEnSource);
            }
            if (segmentIdSelf != null && !segmentIdSelf.trim().isEmpty()) {
                requestBody.put("segmentIdSelf", segmentIdSelf);
            }

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 创建请求实体
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 发送POST请求
            ResponseEntity<String> response = restTemplate.postForEntity(PYTHON_API_URL, entity, String.class);

            // 解析响应
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode responseNode = objectMapper.readTree(response.getBody());
                
                // 检查success字段
                boolean success = responseNode.path("success").asBoolean(false);
                if (!success) {
                    String message = responseNode.path("message").asText("Python服务返回失败");
                    throw new RuntimeException("Python DeepSeek调用失败: " + message);
                }

                // 获取生成的数据
                String generatedData = responseNode.path("data").asText("");
                if (generatedData.isEmpty()) {
                    throw new RuntimeException("Python DeepSeek返回数据为空");
                }

                // 构建返回结果
                Map<String, Object> result = new HashMap<>();
                result.put("output", generatedData);
                result.put("source", "python_deepseek");
                
                // 打印成功信息
                String successTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                

                return result;
            } else {
                String errorMsg = "Python服务返回HTTP状态码: " + response.getStatusCode();
                throw new RuntimeException(errorMsg);
            }

        } catch (Exception e) {
            String errorTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            throw new RuntimeException("Python DeepSeek调用异常: " + e.getMessage(), e);
        }
    }
}
