package seucxxy.csd.backend.hs3.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import seucxxy.csd.backend.hs3.dto.HS3PaperDisplayResponse;
import seucxxy.csd.backend.hs3.entity.HS3ExamPaperEn;
import seucxxy.csd.backend.hs3.mapper.HS3TopicMapper;
import seucxxy.csd.backend.hs3.mapper.HS3ExamPaperEnMapper;
import seucxxy.csd.backend.hs3.service.HS3CozeWorkflowService;
import seucxxy.csd.backend.hs3.service.HS3Neo4jService;
import seucxxy.csd.backend.hs3.service.HS3PaperAnalysisService;
import seucxxy.csd.backend.hs3.service.HS3ChromaEngExamPaperService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HS3 高考英语试卷解析控制器
 * 
 * 提供 Word 文件上传、Coze智能体解析、Neo4j存储等功能
 */
@RestController
@RequestMapping("/api/hs3/paper-analysis")
@CrossOrigin(origins = {"http://localhost:5002", "http://localhost:5003"}, allowCredentials = "true")
public class HS3PaperAnalysisController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HS3PaperAnalysisController.class);

    private final HS3PaperAnalysisService paperAnalysisService;
    private final HS3CozeWorkflowService cozeWorkflowService;
    private final HS3Neo4jService neo4jService;
    private final HS3TopicMapper topicMapper;
    private final HS3ChromaEngExamPaperService chromaService;
    private final HS3ExamPaperEnMapper examPaperEnMapper;

    public HS3PaperAnalysisController(HS3PaperAnalysisService paperAnalysisService,
                                       HS3CozeWorkflowService cozeWorkflowService,
                                       HS3Neo4jService neo4jService,
                                       HS3TopicMapper topicMapper,
                                       HS3ChromaEngExamPaperService chromaService,
                                       HS3ExamPaperEnMapper examPaperEnMapper) {
        this.paperAnalysisService = paperAnalysisService;
        this.cozeWorkflowService = cozeWorkflowService;
        this.neo4jService = neo4jService;
        this.topicMapper = topicMapper;
        this.chromaService = chromaService;
        this.examPaperEnMapper = examPaperEnMapper;
    }
    
    /**
     * 获取试卷结构及题目数据（用于前端展示）
     * 
     * @param examPaperId 试卷ID
     * @return 带层级信息的题目列表
     */
    @GetMapping("/paper/{examPaperId}")
    public ResponseEntity<HS3PaperDisplayResponse> getPaperDisplay(@PathVariable String examPaperId) {
        try {
            List<Map<String, Object>> chromaRecords = chromaService.fetchExamPaperSegments(examPaperId, null);
            Map<String, Object> descriptions = neo4jService.getAllDescriptions("HS3");
            List<Map<String, Object>> segments = paperAnalysisService.buildSegmentsFromChroma(chromaRecords, descriptions);
            
            if (segments.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(HS3PaperDisplayResponse.error("ChromaDB 未找到试卷题目: " + examPaperId));
            }
            
            // 从 metadata 提取试卷名称/描述
            @SuppressWarnings("unchecked")
            Map<String, Object> meta0 = chromaRecords.get(0).get("metadata") instanceof Map
                ? (Map<String, Object>) chromaRecords.get(0).get("metadata") : new HashMap<>();
            String examPaperName = String.valueOf(meta0.getOrDefault("exam_paper_en_name", "高考英语试卷"));
            String examPaperDesc = String.valueOf(meta0.getOrDefault("exam_paper_en_desc", ""));

            HS3PaperDisplayResponse response = HS3PaperDisplayResponse.success(
                    examPaperId,
                    examPaperName,
                    examPaperDesc,
                    150,
                    segments
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[HS3] 获取试卷展示数据失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(HS3PaperDisplayResponse.error("获取失败：" + e.getMessage()));
        }
    }
    
    /**
     * 上传Word文件，调用Coze智能体解析（完整流程 - 按Part分段解析）
     * 
     * 流程：
     * 1. 上传文件 → 后端解析为字符串
     * 2. 查询Neo4j → 从根节点(id='HS3')获取所有Part节点的part_number
     * 3. 循环调用Coze → 每个Part调用一次，传入partNumber
     * 4. 保存结果 → 将解析结果存入对应Part下的叶子节点
     * 5. 返回前端 → 从根节点遍历展示所有题目
     * 
     * 注意：Neo4j中存储的是高考英语试卷的模板结构，根节点id固定为'HS3'
     * 
     * @param file 上传的 Word 文件
     * @param topics JSON格式的知识点数组（可选）
     * @return 解析结果
     */
    @PostMapping("/parse-and-analyze")
    public ResponseEntity<Map<String, Object>> parseAndAnalyze(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "topics", required = false) String topicsJson) {
        
        // 高考英语试卷模板的根节点ID固定为 'HS3'
        final String examPaperId = "HS3";
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 1. 验证文件
            if (file == null || file.isEmpty()) {
                response.put("success", false);
                response.put("message", "请上传有效的文件");
                return ResponseEntity.badRequest().body(response);
            }
            
            String fileName = file.getOriginalFilename();
            if (fileName == null) {
                response.put("success", false);
                response.put("message", "无法获取文件名");
                return ResponseEntity.badRequest().body(response);
            }
            
            String lowerName = fileName.toLowerCase();
            if (!lowerName.endsWith(".doc") && !lowerName.endsWith(".docx")) {
                response.put("success", false);
                response.put("message", "仅支持 Word 文件（.doc 或 .docx）");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 2. 提取Word文件的文本内容并保存到文件
            Map<String, Object> parseResult = paperAnalysisService.parseWordFile(file);
            String inputFile = (String) parseResult.get("text");
            String outputFilePath = (String) parseResult.get("outputPath");
            
            if (inputFile == null || inputFile.isBlank()) {
                response.put("success", false);
                response.put("message", "未能从文件中提取到文本内容");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 3. 解析topics参数
            List<String> topics = null;
            if (topicsJson != null && !topicsJson.isBlank()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    topics = mapper.readValue(topicsJson, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
                } catch (Exception e) {
                    logger.warn("[HS3] 解析topics参数失败: {}", e.getMessage());
                }
            }
            
            // 如果没有传入topics，从数据库获取
            if (topics == null || topics.isEmpty()) {
                topics = topicMapper.selectAllTopics();
            }
            
            // 4. 从Neo4j获取试卷的所有Part信息
            List<Map<String, Object>> partsList = neo4jService.getPartsList(examPaperId);
            
            if (partsList == null || partsList.isEmpty()) {
                logger.info("[HS3] 试卷 {} 没有Part结构，使用默认单次调用", examPaperId);
                // 如果没有Part结构，使用默认的单次调用（不传partName）
                List<Map<String, Object>> segments = cozeWorkflowService.parsePaperWithCoze(inputFile, topics);
                
                if (segments == null || segments.isEmpty()) {
                    response.put("success", false);
                    response.put("message", "Coze解析未返回有效的题目数据");
                    return ResponseEntity.badRequest().body(response);
                }
                
                response.put("success", true);
                response.put("message", "解析成功（单次调用）");
                response.put("examPaperId", examPaperId);
                response.put("outputFilePath", outputFilePath);
                response.put("totalSegments", segments.size());
                response.put("segments", segments);
                response.put("textLength", inputFile.length());
                return ResponseEntity.ok(response);
            }
            
            // 5. 遍历所有Part，逐个调用Coze工作流解析
            logger.info("[HS3] 试卷模板 {} 共有 {} 个Part，开始遍历解析...", examPaperId, partsList.size());
            
            List<Map<String, Object>> allSegments = new ArrayList<>();
            List<Map<String, Object>> partResults = new ArrayList<>();
            
            // 遍历所有Part，使用 partName 作为参数
            for (int i = 0; i < partsList.size(); i++) {
                String partName = (String) partsList.get(i).get("partName");
                String partNumber = (String) partsList.get(i).get("partNumber");
                
                try {
                    logger.debug("[HS3] 正在解析 Part {}/{}: {}...", (i + 1), partsList.size(), partName);
                    
                    List<Map<String, Object>> segments = cozeWorkflowService.parsePaperWithCoze(inputFile, topics, partName);
                    
                    if (segments != null && !segments.isEmpty()) {
                        allSegments.addAll(segments);
                        
                        Map<String, Object> partResult = new HashMap<>();
                        partResult.put("partNumber", partNumber);
                        partResult.put("partName", partName);
                        partResult.put("segmentCount", segments.size());
                        partResult.put("success", true);
                        partResults.add(partResult);
                        
                        logger.debug("[HS3] Part [{}] 解析完成: {} 个片段", partName, segments.size());
                    } else {
                        logger.warn("[HS3] Part [{}] Coze返回空结果", partName);
                        Map<String, Object> partResult = new HashMap<>();
                        partResult.put("partNumber", partNumber);
                        partResult.put("partName", partName);
                        partResult.put("segmentCount", 0);
                        partResult.put("success", false);
                        partResult.put("message", "Coze返回空结果");
                        partResults.add(partResult);
                    }
                } catch (Exception e) {
                    logger.error("[HS3] Part [{}] 解析失败: {}", partName, e.getMessage());
                    Map<String, Object> partResult = new HashMap<>();
                    partResult.put("partNumber", partNumber);
                    partResult.put("partName", partName);
                    partResult.put("segmentCount", 0);
                    partResult.put("success", false);
                    partResult.put("message", e.getMessage());
                    partResults.add(partResult);
                }
            }
            
            // 6. 获取Neo4j中所有层级节点的description信息
            Map<String, Object> descriptions = neo4jService.getAllDescriptions(examPaperId);
            
            // 打印解析结果统计
            System.out.println("========== [HS3] 试卷解析完成 ==========");
            System.out.println("[HS3] 共解析 " + partsList.size() + " 个Part，总计 " + allSegments.size() + " 个Segment片段");
            for (Map<String, Object> partResult : partResults) {
                System.out.println("[HS3]   - " + partResult.get("partName") + ": " + partResult.get("segmentCount") + " 个片段");
            }
            System.out.println("========================================");
            
            // 7. 返回结果（仅用于页面展示，不存储）
            response.put("success", true);
            response.put("message", "解析完成，共解析 " + partsList.size() + " 个Part");
            response.put("examPaperId", examPaperId);
            response.put("outputFilePath", outputFilePath);
            response.put("totalParts", partsList.size());
            response.put("totalSegments", allSegments.size());
            response.put("partResults", partResults);
            response.put("segments", allSegments);
            response.put("descriptions", descriptions);  // 添加description信息
            response.put("textLength", inputFile.length());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("[HS3] 试卷解析失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "解析失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取试卷列表
     */
    @GetMapping("/exam-papers")
    public ResponseEntity<List<Map<String, Object>>> getExamPaperList() {
        try {
            List<seucxxy.csd.backend.hs3.entity.HS3ExamPaperEn> list = examPaperEnMapper.selectAll();
            List<Map<String, Object>> papers = new ArrayList<>();
            for (seucxxy.csd.backend.hs3.entity.HS3ExamPaperEn p : list) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("examPaperEnName", p.getExamPaperEnName());
                map.put("examPaperEnDesc", p.getExamPaperEnDesc());
                map.put("examPaperEnSubject", p.getExamPaperEnSubject());
                map.put("examPaperEnSource", p.getExamPaperEnSource());
                papers.add(map);
            }
            return ResponseEntity.ok(papers);
        } catch (Exception e) {
            System.err.println("获取试卷列表失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }
    
    /**
     * 存储整份试卷到关系型数据库和ChromaDB
     * 
     * @param request 包含试卷信息和大题列表的请求体
     * @return 存储结果
     */
    @PostMapping("/store-to-chroma")
    public ResponseEntity<Map<String, Object>> storeToChroma(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证参数
            String examPaperName = (String) request.get("examPaperName");
            String subject = (String) request.get("subject");
            String examPaperSource = (String) request.get("examPaperSource");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> segments = (List<Map<String, Object>>) request.get("segments");
            
            if (examPaperName == null || examPaperName.isBlank()) {
                response.put("success", false);
                response.put("message", "试卷名称不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (subject == null || subject.isBlank()) {
                response.put("success", false);
                response.put("message", "科目不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (examPaperSource == null || examPaperSource.isBlank()) {
                examPaperSource = "real"; // 默认为真题
            }
            
            if (segments == null || segments.isEmpty()) {
                response.put("success", false);
                response.put("message", "大题列表不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            logger.info("[HS3] 开始存储试卷: examPaperName={}, 大题数={}", examPaperName, segments.size());
            
            // 1. 先存储到关系型数据库exam_paper_en表
            HS3ExamPaperEn examPaper = new HS3ExamPaperEn();
            examPaper.setExamPaperEnName(examPaperName);
            // 解析流程未要求填写描述，保持为 null 不写入空串
            examPaper.setExamPaperEnDesc(null);
            examPaper.setExamPaperEnSubject(subject);
            examPaper.setExamPaperEnSource(examPaperSource);
            
            int insertResult = examPaperEnMapper.insert(examPaper);
            if (insertResult <= 0) {
                response.put("success", false);
                response.put("message", "试卷存储到数据库失败");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            
            Long examPaperId = examPaper.getId();
            logger.info("[HS3] 试卷存储到数据库成功，获得ID: {}", examPaperId);
            
            // 2. 再存储到ChromaDB
            chromaService.storeExamPaperSegments(
                    examPaperId, 
                    examPaperName, 
                    subject, 
                    examPaperSource, 
                    segments
            );
            
            response.put("success", true);
            response.put("message", String.format("成功存储 %d 个大题到ChromaDB", segments.size()));
            response.put("segmentsCount", segments.size());
            response.put("examPaperId", examPaperId);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("[HS3] ChromaDB存储参数错误: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (DataIntegrityViolationException e) {
            // 试卷名称唯一约束冲突等
            logger.error("[HS3] 试卷名称或约束冲突: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "试卷名称已存在或数据约束冲突，请更换名称后重试");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            logger.error("[HS3] ChromaDB存储失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "存储失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
