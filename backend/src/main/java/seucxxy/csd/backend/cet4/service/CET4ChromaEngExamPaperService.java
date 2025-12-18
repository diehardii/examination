package seucxxy.csd.backend.cet4.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.constraints.NotNull;
import seucxxy.csd.backend.cet4.dto.CET4SectionAChromaRecord;
import seucxxy.csd.backend.cet4.dto.CET4SectionAChromaRequest;
import seucxxy.csd.backend.cet4.dto.CET4SimpleChromaRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Optional;

@Service("cet4ChromaEngExamPaperService")
public class CET4ChromaEngExamPaperService {

    private static final Logger logger = LoggerFactory.getLogger(CET4ChromaEngExamPaperService.class);
    private static final int EMBEDDING_DIMENSION = 384;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<String> collectionIdCache = new AtomicReference<>();

    @Value("${chromadb.base-url:http://localhost:8000/api/v2}")
    private String chromaBaseUrl;

    @Value("${chromadb.tenant:default_tenant}")
    private String tenant;

    @Value("${chromadb.database:default_database}")
    private String database;

    @Value("${chromadb.collection.eng-exam-papers:eng_exam_papers_cet4}")
    private String collectionName;

    public CET4ChromaEngExamPaperService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void storeSectionA(@NotNull CET4SectionAChromaRequest request) throws JsonProcessingException {
        // 强制默认：Section A 选词填空
        if (request.getSectionId() == null || request.getSectionId().isBlank()) {
            request.setSectionId("A");
        }
        if (request.getQuestionType() == null || request.getQuestionType().isBlank()) {
            request.setQuestionType("选词填空");
        }
        storeSectionGeneric(request);
    }

    public void storeSectionB(@NotNull CET4SectionAChromaRequest request) throws JsonProcessingException {
        // 强制默认：Section B 段落匹配
        request.setSectionId("B");
        if (request.getQuestionType() == null || request.getQuestionType().isBlank()) {
            request.setQuestionType("段落匹配");
        }
        storeSectionGeneric(request);
    }

    public void storeSectionC(@NotNull CET4SectionAChromaRequest request) throws JsonProcessingException {
        // Section C现在分为C1和C2，需要分别存储
        request.setQuestionType("篇章阅读");
        storeSectionCGeneric(request);
    }

    /**
     * Section C专用存储方法，处理section_c1和section_c2，分别存储为两个独立单元
     */
    private void storeSectionCGeneric(@NotNull CET4SectionAChromaRequest request) throws JsonProcessingException {
        String document = normalizeString(request.getDocument());
        if (document.isEmpty()) {
            throw new IllegalArgumentException("存储文档内容不能为空");
        }

        List<Integer> blankNumbers = normalizeBlankNumbers(request.getBlankNumbers());
        if (blankNumbers.isEmpty()) {
            throw new IllegalArgumentException("未提供可用的题号");
        }

        String subject = normalizeString(request.getSubject());
        if (subject.isEmpty()) {
            throw new IllegalArgumentException("subject 不能为空");
        }

        String examPaperId = normalizeString(request.getExamPaperId());
        if (examPaperId.isEmpty()) {
            throw new IllegalArgumentException("试卷编号不能为空");
        }

        String examPaperName = normalizeString(request.getExamPaperName());
        if (examPaperName.isEmpty()) {
            throw new IllegalArgumentException("试卷名称不能为空");
        }

        int partId = request.getPartId() == null ? 3 : request.getPartId();
        // Section C 的 question_type 为 ReadingPassage
        String questionType = normalizeQuestionTypeEnum("ReadingPassage");
        String createdTime = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());
        
        // 判断是否为纯粹的强化训练场景（只有AIfromWrongBank和AIfromself需要统一passage_mark为"Passage"）
        String examPaperEnSource = request.getExamPaperEnSource();
        boolean isIntensiveTrain = examPaperEnSource != null && 
            (examPaperEnSource.equals("AIfromWrongBank") || examPaperEnSource.equals("AIfromself"));

        // 解析document中的section_c1和section_c2
        try {
            JsonNode root = objectMapper.readTree(document);
            
            // 强化训练场景：统一修改passage_mark为"Passage"
            if (isIntensiveTrain && root instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
                com.fasterxml.jackson.databind.node.ObjectNode rootObj = (com.fasterxml.jackson.databind.node.ObjectNode) root;
                
                // 检查并修改根节点的passage_mark
                if (root.has("passage_mark")) {
                    String originalPassageMark = root.get("passage_mark").asText("");
                    rootObj.put("passage_mark", "Passage");
                    logger.info("[强化训练-{}] passage_mark规范化: \"{}\" -> \"Passage\"", 
                        examPaperEnSource, originalPassageMark);
                }
                
                // 检查并修改嵌套结构中的passage_mark（section_c1/section_c2）
                JsonNode readingComp = root.path("reading_comprehension");
                if (readingComp instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
                    com.fasterxml.jackson.databind.node.ObjectNode rcObj = 
                        (com.fasterxml.jackson.databind.node.ObjectNode) readingComp;
                    
                    // 处理section_c1
                    JsonNode c1 = rcObj.path("section_c1");
                    if (c1 instanceof com.fasterxml.jackson.databind.node.ObjectNode && c1.has("passage_mark")) {
                        String originalMark1 = c1.get("passage_mark").asText("");
                        ((com.fasterxml.jackson.databind.node.ObjectNode) c1).put("passage_mark", "Passage");
                        logger.info("[强化训练-{}] section_c1 passage_mark规范化: \"{}\" -> \"Passage\"", 
                            examPaperEnSource, originalMark1);
                    }
                    
                    // 处理section_c2
                    JsonNode c2 = rcObj.path("section_c2");
                    if (c2 instanceof com.fasterxml.jackson.databind.node.ObjectNode && c2.has("passage_mark")) {
                        String originalMark2 = c2.get("passage_mark").asText("");
                        ((com.fasterxml.jackson.databind.node.ObjectNode) c2).put("passage_mark", "Passage");
                        logger.info("[强化训练-{}] section_c2 passage_mark规范化: \"{}\" -> \"Passage\"", 
                            examPaperEnSource, originalMark2);
                    }
                }
                
                // 处理根节点直接包含section_c1/section_c2的情况
                if (rootObj.has("section_c1")) {
                    JsonNode c1 = rootObj.path("section_c1");
                    if (c1 instanceof com.fasterxml.jackson.databind.node.ObjectNode && c1.has("passage_mark")) {
                        String originalMark1 = c1.get("passage_mark").asText("");
                        ((com.fasterxml.jackson.databind.node.ObjectNode) c1).put("passage_mark", "Passage");
                        logger.info("[强化训练-{}] section_c1 passage_mark规范化: \"{}\" -> \"Passage\"", 
                            examPaperEnSource, originalMark1);
                    }
                }
                if (rootObj.has("section_c2")) {
                    JsonNode c2 = rootObj.path("section_c2");
                    if (c2 instanceof com.fasterxml.jackson.databind.node.ObjectNode && c2.has("passage_mark")) {
                        String originalMark2 = c2.get("passage_mark").asText("");
                        ((com.fasterxml.jackson.databind.node.ObjectNode) c2).put("passage_mark", "Passage");
                        logger.info("[强化训练-{}] section_c2 passage_mark规范化: \"{}\" -> \"Passage\"", 
                            examPaperEnSource, originalMark2);
                    }
                }
                
                // 更新document字符串
                document = objectMapper.writeValueAsString(root);
            }
            
            List<String> ids = new ArrayList<>();
            List<String> documents = new ArrayList<>();
            List<Map<String, Object>> metadatas = new ArrayList<>();
            List<List<Float>> embeddings = new ArrayList<>();

            // 首先检查是否是平面结构（passage_content + questions）
            if (root.has("passage_content") && root.has("questions")) {
                // 从request中获取sectionId，或使用默认值
                String sectionId = request.getSectionId();
                if (sectionId == null || sectionId.isEmpty()) {
                    sectionId = "C1"; // 默认为C1
                }
                
                // 提取topic（如果有）
                String topic = root.has("topic") ? root.get("topic").asText("") : "";
                if (topic == null || topic.trim().isEmpty()) {
                    topic = "unknown topic";
                }
                
                // 提取segment_id
                String segmentId = root.has("segment_id") ? root.get("segment_id").asText("") : "";
                if (segmentId.isEmpty()) {
                    segmentId = partId + "section" + sectionId; // 例如 "3sectionC1"
                }
                
                // 生成question_id: exam_paper_id + segment_id
                String questionId = examPaperId + segmentId;
                
                // 序列化整个root JSON
                String docJson = objectMapper.writeValueAsString(root);
                
                // 构建metadata
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("topic", topic);
                metadata.put("subject", subject);
                metadata.put("question_type", questionType);
                metadata.put("exam_paper_en_id", parseIntegerSafely(examPaperId, "exam_paper_id"));
                metadata.put("exam_paper_en_name", examPaperName);
                metadata.put("part_id", partId);
                metadata.put("segment_id", segmentId);
                metadata.put("question_id", questionId);
                metadata.put("created_time", createdTime);
                // 新增：试卷来源
                metadata.put("exam_paper_en_source", normalizeExamPaperSource(request.getExamPaperEnSource()));
                
                ids.add(questionId);
                documents.add(docJson);
                metadatas.add(metadata);
                embeddings.add(generateDeterministicEmbedding(questionId));
            } else {
                // 嵌套结构：处理Section C1
                JsonNode sectionC1 = root.path("reading_comprehension").path("section_c1");
                if (sectionC1.isMissingNode()) {
                    sectionC1 = root.path("section_c1");
                }
            
            if (!sectionC1.isMissingNode() && sectionC1.isObject()) {
                String topic1 = sectionC1.has("topic") ? sectionC1.get("topic").asText("") : "";
                if (topic1 == null || topic1.trim().isEmpty()) {
                    topic1 = "unknown topic";
                }
                
                // 提取segment_id
                String segmentId1 = sectionC1.has("segment_id") ? sectionC1.get("segment_id").asText("") : "";
                if (segmentId1.isEmpty()) {
                    segmentId1 = partId + "C1";
                }
                
                // 生成question_id: exam_paper_id + segment_id
                String questionId1 = examPaperId + segmentId1;
                
                // 序列化 section_c1 JSON
                String c1Json = objectMapper.writeValueAsString(sectionC1);
                
                // 构建新的metadata格式
                Map<String, Object> metadata1 = new LinkedHashMap<>();
                metadata1.put("topic", topic1);
                metadata1.put("subject", subject);
                metadata1.put("question_type", questionType);
                metadata1.put("exam_paper_en_id", parseIntegerSafely(examPaperId, "exam_paper_id"));
                metadata1.put("exam_paper_en_name", examPaperName);
                metadata1.put("part_id", partId);
                metadata1.put("segment_id", segmentId1);
                metadata1.put("question_id", questionId1);
                metadata1.put("created_time", createdTime);
                // 新增：试卷来源
                metadata1.put("exam_paper_en_source", normalizeExamPaperSource(request.getExamPaperEnSource()));
                
                ids.add(questionId1);
                documents.add(c1Json);  // 存储C1的JSON输出
                metadatas.add(metadata1);
                embeddings.add(generateDeterministicEmbedding(questionId1));
            }

            // 处理Section C2
            JsonNode sectionC2 = root.path("reading_comprehension").path("section_c2");
            if (sectionC2.isMissingNode()) {
                sectionC2 = root.path("section_c2");
            }
            
            if (!sectionC2.isMissingNode() && sectionC2.isObject()) {
                String topic2 = sectionC2.has("topic") ? sectionC2.get("topic").asText("") : "";
                if (topic2 == null || topic2.trim().isEmpty()) {
                    topic2 = "unknown topic";
                }
                
                // 提取segment_id
                String segmentId2 = sectionC2.has("segment_id") ? sectionC2.get("segment_id").asText("") : "";
                if (segmentId2.isEmpty()) {
                    segmentId2 = partId + "C2";
                }
                
                // 生成question_id: exam_paper_id + segment_id
                String questionId2 = examPaperId + segmentId2;
                
                // 构建新的metadata格式
                Map<String, Object> metadata2 = new LinkedHashMap<>();
                metadata2.put("topic", topic2);
                metadata2.put("subject", subject);
                metadata2.put("question_type", questionType);
                metadata2.put("exam_paper_en_id", parseIntegerSafely(examPaperId, "exam_paper_id"));
                metadata2.put("exam_paper_en_name", examPaperName);
                metadata2.put("part_id", partId);
                metadata2.put("segment_id", segmentId2);
                metadata2.put("question_id", questionId2);
                metadata2.put("created_time", createdTime);
                // 新增：试卷来源
                metadata2.put("exam_paper_en_source", normalizeExamPaperSource(request.getExamPaperEnSource()));
                
                ids.add(questionId2);
                documents.add(objectMapper.writeValueAsString(sectionC2));  // 存储C2的JSON输出
                metadatas.add(metadata2);
                embeddings.add(generateDeterministicEmbedding(questionId2));
            }
            } // 结束嵌套结构的else块

            if (ids.isEmpty()) {
                throw new IllegalArgumentException("未找到有效的Section C1或C2数据");
            }

            // 批量存储到ChromaDB
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("ids", ids);
            requestBody.put("documents", documents);
            requestBody.put("metadatas", metadatas);
            requestBody.put("embeddings", embeddings);

            String collectionId = ensureCollectionId();
            String url = collectionAddUrl(collectionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                java.util.Objects.requireNonNull(java.net.URI.create(url)),
                java.util.Objects.requireNonNull(org.springframework.http.HttpMethod.POST),
                entity,
                String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Section C 数据已写入 ChromaDB，共 {} 个单元（C1和C2）", ids.size());
            } else {
                logger.warn("写入 Section C 数据到 ChromaDB 失败，status={}, body={}",
                        response.getStatusCode(), response.getBody());
                throw new IllegalStateException("写入 ChromaDB 失败，状态码：" + response.getStatusCode());
            }
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("解析document JSON失败: " + e.getMessage(), e);
        }
    }

    // 将中文/混合的 question_type 统一映射为英文规范值（CET4 枚举）
    private String normalizeQuestionTypeEnum(String qt) {
        if (qt == null) return "";
        String s = qt.trim();
        switch (s) {
            case "Writing":
            case "NewsReport":
            case "Conversation":
            case "ListeningPassage":
            case "BlankedCloze":
            case "Matching":
            case "ReadingPassage":
            case "Translation":
                return s;
        }
        if ("写作".equals(s)) return "Writing";
        if ("新闻报道".equals(s) || "新闻".equals(s)) return "NewsReport";
        if ("对话".equals(s)) return "Conversation";
        if ("选词填空".equals(s)) return "BlankedCloze";
        if ("段落匹配".equals(s)) return "Matching";
        if ("篇章阅读".equals(s) || "阅读理解".equals(s)) return "ReadingPassage";
        if ("翻译".equals(s)) return "Translation";
        // 兜底：根据关键词猜测
        String lower = s.toLowerCase();
        if (lower.contains("writing")) return "Writing";
        if (lower.contains("news")) return "NewsReport";
        if (lower.contains("conversation")) return "Conversation";
        if (lower.contains("listening") && lower.contains("passage")) return "ListeningPassage";
        if (lower.contains("blank") || lower.contains("cloze")) return "BlankedCloze";
        if (lower.contains("match")) return "Matching";
        if (lower.contains("reading") || lower.contains("passage")) return "ReadingPassage";
        if (lower.contains("translation")) return "Translation";
        return s;
    }

    // ==================== Part II Listening 存储（AB / C） ====================
    public void storeListeningPart2AB(@NotNull CET4SectionAChromaRequest request) throws JsonProcessingException {
        request.setSectionId("AB");
        request.setPartId(2);
        request.setQuestionType("听力");
        storeListeningPart2Generic(request);
    }

    public void storeListeningPart2C(@NotNull CET4SectionAChromaRequest request) throws JsonProcessingException {
        request.setSectionId("C");
        request.setPartId(2);
        request.setQuestionType("听力");
        storeListeningPart2Generic(request);
    }

    /**
     * Part II Listening专用存储方法，处理units数组，为每个unit单独存储
     */
    private void storeListeningPart2Generic(@NotNull CET4SectionAChromaRequest request) throws JsonProcessingException {
        String document = normalizeString(request.getDocument());
        if (document.isEmpty()) {
            throw new IllegalArgumentException("存储文档内容不能为空");
        }

        List<Integer> blankNumbers = normalizeBlankNumbers(request.getBlankNumbers());
        if (blankNumbers.isEmpty()) {
            throw new IllegalArgumentException("未提供可用的题号");
        }

        String subject = normalizeString(request.getSubject());
        if (subject.isEmpty()) {
            throw new IllegalArgumentException("subject 不能为空");
        }

        String examPaperId = normalizeString(request.getExamPaperId());
        if (examPaperId.isEmpty()) {
            throw new IllegalArgumentException("试卷编号不能为空");
        }

        String examPaperName = normalizeString(request.getExamPaperName());
        if (examPaperName.isEmpty()) {
            throw new IllegalArgumentException("试卷名称不能为空");
        }

        int partId = request.getPartId() == null ? 2 : request.getPartId();
        String sectionId = normalizeSectionId(request.getSectionId());
        String createdTime = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());
        
        // 判断是否为纯粹的强化训练场景（只有AIfromWrongBank和AIfromself需要移除unit_type中的数字）
        // real: 真题解析 - 保留原始unit_type
        // AIfromreal: 依据真题生成 - 保留原始unit_type（遵循真题的结构规则）
        // AIfromWrongBank: 从错题库生成的强化训练 - 移除unit_type中的数字
        // AIfromself: 自定义强化训练 - 移除unit_type中的数字
        String examPaperEnSource = request.getExamPaperEnSource();
        boolean isIntensiveTrain = examPaperEnSource != null && 
            (examPaperEnSource.equals("AIfromWrongBank") || examPaperEnSource.equals("AIfromself"));

        // 解析document中的units数组
        try {
            JsonNode root = objectMapper.readTree(document);
            JsonNode units = root.path("units");
            
            if (!units.isArray() || units.size() == 0) {
                // 兼容扁平结构：存在 answers + question_and_options（可选 listening_content / unit_type / segment_id）
                boolean looksLikeSingleUnit = (root.has("answers") && root.has("question_and_options"))
                        || root.has("listening_content") || root.has("unit_type");
                if (looksLikeSingleUnit) {
                    com.fasterxml.jackson.databind.node.ArrayNode single = objectMapper.createArrayNode();
                    single.add(root);
                    units = single;
                    logger.info("[Listening存储] 兼容扁平结构：已将document包装为单个unit进行入库");
                } else {
                    logger.warn("未找到有效的units数组，document结构: {}", root.toPrettyString());
                    throw new IllegalArgumentException("未找到有效的units数组");
                }
            }

            logger.info("找到 {} 个units", units.size());

            List<String> ids = new ArrayList<>();
            List<String> documents = new ArrayList<>();
            List<Map<String, Object>> metadatas = new ArrayList<>();
            List<List<Float>> embeddings = new ArrayList<>();

            for (int i = 0; i < units.size(); i++) {
                JsonNode unit = units.get(i);
                if (unit == null || !unit.isObject()) {
                    logger.warn("跳过无效的unit[{}]", i);
                    continue;
                }

                // 提取unit信息
                String originalUnitType = unit.has("unit_type") ? unit.get("unit_type").asText("") : (root.has("unit_type") ? root.get("unit_type").asText("") : ("Unit " + (i + 1)));
                
                // 规范化unit_type：仅在纯粹的强化训练场景下移除末尾的数字（如 "Conversation 2" -> "Conversation"）
                // real/AIfromreal时保留原始的unit_type（遵循真题的结构规则）
                String unitType = originalUnitType;
                if (isIntensiveTrain) {
                    // 强化训练场景：移除unit_type末尾的数字
                    unitType = originalUnitType.replaceAll("\\s+\\d+$", "").trim();
                    if (!originalUnitType.equals(unitType)) {
                        logger.info("[强化训练-{}] unit_type规范化: \"{}\" -> \"{}\"", examPaperEnSource, originalUnitType, unitType);
                        
                        // 修改unit对象中的unit_type字段，确保存储到ChromaDB的document也是规范化后的值
                        if (unit instanceof com.fasterxml.jackson.databind.node.ObjectNode) {
                            ((com.fasterxml.jackson.databind.node.ObjectNode) unit).put("unit_type", unitType);
                        }
                    }
                } else {
                    // 真题或依据真题生成：保留原始unit_type
                    logger.info("[保留原始-{}] unit_type: \"{}\"", examPaperEnSource != null ? examPaperEnSource : "unknown", unitType);
                }
                
                String topic = unit.has("topic") ? unit.get("topic").asText("") : (root.has("topic") ? root.get("topic").asText("") : "");
                // 如果topic为空字符串，设置为"unknown topic"以确保ChromaDB能存储
                if (topic == null || topic.trim().isEmpty()) {
                    topic = "unknown topic";
                }
                
                // 提取segment_id（若unit缺失，尝试root）
                String segmentId = unit.has("segment_id") ? unit.get("segment_id").asText("") : (root.has("segment_id") ? root.get("segment_id").asText("") : "");
                if (segmentId.isEmpty()) {
                    segmentId = partId + sectionId + "_" + (i + 1);
                }
                
                // 根据segment_id或unit_type确定question_type
                String questionType;
                if (segmentId.contains("NewsReport") || segmentId.toLowerCase().contains("news")) {
                    questionType = "NewsReport";
                } else if (segmentId.contains("conversation") || unitType.toLowerCase().contains("conversation")) {
                    questionType = "Conversation";
                } else if (segmentId.contains("passage") || unitType.toLowerCase().contains("passage")) {
                    questionType = "ListeningPassage";
                } else {
                    String unitTypeLower = unitType.toLowerCase();
                    if (unitTypeLower.contains("news")) {
                        questionType = "NewsReport";
                    } else if (unitTypeLower.contains("conversation")) {
                        questionType = "Conversation";
                    } else if (unitTypeLower.contains("passage")) {
                        questionType = "ListeningPassage";
                    } else {
                        questionType = "ListeningPassage"; // 兜底到听力篇章
                    }
                }
                // 统一英文枚举
                questionType = normalizeQuestionTypeEnum(questionType);

                // 计算该unit的题目数量
                JsonNode questions = unit.path("question_and_options");
                int questionCount = questions.isArray() ? questions.size() : 0;
                
                // 生成question_id: exam_paper_id + segment_id
                String questionId = examPaperId + segmentId;
                
                logger.info("Unit[{}] - unit_type: {}, topic: {}, segment_id: {}, question_id: {}", 
                    i, unitType, topic, segmentId, questionId);

                // 构建新的metadata格式
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("topic", topic);
                metadata.put("subject", subject);
                metadata.put("question_type", questionType);
                metadata.put("exam_paper_en_id", parseIntegerSafely(examPaperId, "exam_paper_id"));
                metadata.put("exam_paper_en_name", examPaperName);
                metadata.put("part_id", partId);
                metadata.put("segment_id", segmentId);
                metadata.put("question_id", questionId);
                metadata.put("created_time", createdTime);
                // 新增：试卷来源
                metadata.put("exam_paper_en_source", normalizeExamPaperSource(request.getExamPaperEnSource()));

                ids.add(questionId);
                documents.add(objectMapper.writeValueAsString(unit));  // 存储每个unit的JSON输出
                metadatas.add(metadata);
                embeddings.add(generateDeterministicEmbedding(questionId));
            }
            
            if (ids.isEmpty()) {
                throw new IllegalArgumentException("未找到有效的unit数据");
            }

            // 批量存储到ChromaDB
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("ids", ids);
            requestBody.put("documents", documents);
            requestBody.put("metadatas", metadatas);
            requestBody.put("embeddings", embeddings);

            logger.info("========== 准备发送Part II {}到ChromaDB ==========", sectionId);
            logger.info("ids数量: {}", ids.size());
            for (int i = 0; i < metadatas.size(); i++) {
                Map<String, Object> meta = metadatas.get(i);
                logger.info("Metadata[{}] - question_id: {}, segment_id: {}, topic: {}", 
                    i, meta.get("question_id"), meta.get("segment_id"), meta.get("topic"));
            }
            logger.info("================================================");

            String collectionId = ensureCollectionId();
            String url = collectionAddUrl(collectionId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                java.util.Objects.requireNonNull(java.net.URI.create(url)),
                java.util.Objects.requireNonNull(org.springframework.http.HttpMethod.POST),
                entity,
                String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Part II {} 数据已写入 ChromaDB，共 {} 个units", sectionId, ids.size());
            } else {
                logger.warn("写入 Part II {} 数据到 ChromaDB 失败，status={}, body={}",
                        sectionId, response.getStatusCode(), response.getBody());
                throw new IllegalStateException("写入 ChromaDB 失败，状态码：" + response.getStatusCode());
            }
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("解析document JSON失败: " + e.getMessage(), e);
        }
    }

    public void storePartSimple(@NotNull CET4SimpleChromaRequest request) throws JsonProcessingException {
        String document = normalizeString(request.getDocument());
        if (document.isEmpty()) {
            throw new IllegalArgumentException("存储文档内容不能为空");
        }
        
        String subject = normalizeString(request.getSubject());
        if (subject.isEmpty()) {
            throw new IllegalArgumentException("subject 不能为空");
        }
        String examPaperId = normalizeString(request.getExamPaperId());
        if (examPaperId.isEmpty()) {
            throw new IllegalArgumentException("试卷编号不能为空");
        }
        String examPaperName = normalizeString(request.getExamPaperName());
        if (examPaperName.isEmpty()) {
            throw new IllegalArgumentException("试卷名称不能为空");
        }
        int partId = request.getPartId() == null ? 0 : request.getPartId();
        if (partId <= 0) {
            throw new IllegalArgumentException("partId 非法");
        }
        // 根据partId设置question_type
        String questionType;
        if (partId == 1) {
            questionType = "Writing";
        } else if (partId == 4) {
            questionType = "Translation";
        } else {
            throw new IllegalArgumentException("storePartSimple仅支持Part I(写作)和Part IV(翻译)");
        }
        
        String createdTime = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());

        // 从Coze输出的JSON中提取对应的部分（writing或translation）和metadata
        String topic = "unknown topic";
        String segmentId = "";
        String cleanedDocument = ""; // 只包含对应part的干净数据
        
        try {
            JsonNode root = objectMapper.readTree(document);
            
            if (partId == 1) {
                // Part I: 写作 - 只提取writing部分（不区分大小写）
                JsonNode writingNode = root.path("writing");
                // 如果小写不存在，尝试大写开头
                if (writingNode.isMissingNode()) {
                    writingNode = root.path("Writing");
                }
                if (writingNode.isMissingNode()) {
                    logger.warn("[Part I Writing] document中未找到writing/Writing节点，跳过存储");
                    return; // 静默忽略，不抛异常
                }
                
                // 提取topic和segment_id
                if (writingNode.has("topic")) {
                    String t = writingNode.get("topic").asText("");
                    if (!t.isEmpty()) {
                        topic = t;
                    }
                }
                if (writingNode.has("segment_id")) {
                    segmentId = writingNode.get("segment_id").asText("");
                }
                
                // 只保存writing部分的JSON
                ObjectNode cleanRoot = objectMapper.createObjectNode();
                cleanRoot.set("writing", writingNode);
                cleanedDocument = objectMapper.writeValueAsString(cleanRoot);
                
            } else if (partId == 4) {
                // Part IV: 翻译 - 只提取translation部分（不区分大小写）
                JsonNode translationNode = root.path("translation");
                // 如果小写不存在，尝试大写开头
                if (translationNode.isMissingNode()) {
                    translationNode = root.path("Translation");
                }
                if (translationNode.isMissingNode()) {
                    logger.warn("[Part IV Translation] document中未找到translation/Translation节点，跳过存储");
                    return; // 静默忽略，不抛异常
                }
                
                // 提取topic和segment_id
                if (translationNode.has("topic")) {
                    String t = translationNode.get("topic").asText("");
                    if (!t.isEmpty()) {
                        topic = t;
                    }
                }
                if (translationNode.has("segment_id")) {
                    segmentId = translationNode.get("segment_id").asText("");
                }
                
                // 只保存translation部分的JSON
                ObjectNode cleanRoot = objectMapper.createObjectNode();
                cleanRoot.set("translation", translationNode);
                cleanedDocument = objectMapper.writeValueAsString(cleanRoot);
            } else {
                throw new IllegalArgumentException("storePartSimple仅支持Part I(写作)和Part IV(翻译)");
            }
        } catch (JsonProcessingException e) {
            logger.error("解析document JSON失败: {}", e.getMessage());
            throw new IllegalArgumentException("解析document JSON失败: " + e.getMessage(), e);
        }
        
        if (segmentId.isEmpty()) {
            segmentId = String.valueOf(partId);
        }

        // 生成question_id: exam_paper_id + segment_id
        String questionId = examPaperId + segmentId;

        // 构建新的metadata格式
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("topic", topic);
        metadata.put("subject", subject);
        metadata.put("question_type", questionType);
        metadata.put("exam_paper_en_id", parseIntegerSafely(examPaperId, "exam_paper_id"));
        metadata.put("exam_paper_en_name", examPaperName);
        metadata.put("part_id", partId);
        metadata.put("segment_id", segmentId);
        metadata.put("question_id", questionId);
        metadata.put("created_time", createdTime);
        // 新增：试卷来源
        metadata.put("exam_paper_en_source", normalizeExamPaperSource(request.getExamPaperEnSource()));
        
        // Removed printing question_source
        // System.out.println("[ChromaDB存储] Part " + partId + " - question_source = " + request.getQuestionSource() + ", question_id = " + questionId);
        // System.out.println("[SQL语句] INSERT INTO chroma_collection (id, document, metadata) VALUES ('" + questionId + "', ..., '" + objectMapper.writeValueAsString(metadata) + "')");
        
        // 构建请求体：documents字段使用清理后的数据
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("ids", Collections.singletonList(questionId));
        requestBody.put("documents", Collections.singletonList(cleanedDocument));
        requestBody.put("metadatas", Collections.singletonList(metadata));
        requestBody.put("embeddings", Collections.singletonList(generateDeterministicEmbedding(questionId)));

        String collectionId = ensureCollectionId();
        String url = collectionAddUrl(collectionId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            java.util.Objects.requireNonNull(java.net.URI.create(url)),
            java.util.Objects.requireNonNull(org.springframework.http.HttpMethod.POST),
            entity,
            String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            // 成功，不输出
        } else {
            logger.warn("写入 Part {} 数据到 ChromaDB 失败，status={}, body={}", partId, response.getStatusCode(), response.getBody());
            throw new IllegalStateException("写入 ChromaDB 失败，状态码：" + response.getStatusCode());
        }
    }

    private void storeSectionGeneric(@NotNull CET4SectionAChromaRequest request) throws JsonProcessingException {
        String document = normalizeString(request.getDocument());
        if (document.isEmpty()) {
            throw new IllegalArgumentException("存储文档内容不能为空");
        }

        List<Integer> blankNumbers = normalizeBlankNumbers(request.getBlankNumbers());
        if (blankNumbers.isEmpty()) {
            throw new IllegalArgumentException("未提供可用的题号");
        }

        String subject = normalizeString(request.getSubject());
        if (subject.isEmpty()) {
            throw new IllegalArgumentException("subject 不能为空");
        }

        String examPaperId = normalizeString(request.getExamPaperId());
        if (examPaperId.isEmpty()) {
            throw new IllegalArgumentException("试卷编号不能为空");
        }

        String examPaperName = normalizeString(request.getExamPaperName());
        if (examPaperName.isEmpty()) {
            throw new IllegalArgumentException("试卷名称不能为空");
        }

        // Removed unused variables
        // int questionNumberBegin = blankNumbers.get(0);
        // int questionNumberEnd = blankNumbers.get(blankNumbers.size() - 1);
        int partId = request.getPartId() == null ? 3 : request.getPartId();
        String sectionId = normalizeSectionId(request.getSectionId());
        // 根据sectionId设置question_type
        String questionType;
        if ("A".equalsIgnoreCase(sectionId)) {
            questionType = "BlankedCloze";
        } else if ("B".equalsIgnoreCase(sectionId)) {
            questionType = "Matching";
        } else {
            questionType = "ReadingPassage";
        }
        questionType = normalizeQuestionTypeEnum(questionType);
        String createdTime = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());

        // 从Coze输出的JSON中提取对应的Section部分
        String topic = "unknown topic";
        String segmentId = "";
        String cleanedDocument = ""; // 只包含对应section的干净数据
        
        try {
            JsonNode root = objectMapper.readTree(document);
            
            // 尝试从 reading_comprehension.section_X 或 section_X 提取
            JsonNode sectionNode = null;
            JsonNode reading = root.path("reading_comprehension");
            if (!reading.isMissingNode()) {
                sectionNode = reading.path("section_" + sectionId.toLowerCase());
            }
            
            // 如果没有找到，尝试直接从根节点提取
            if (sectionNode == null || sectionNode.isMissingNode()) {
                sectionNode = root.path("section_" + sectionId.toLowerCase());
            }
            
            if (sectionNode == null || sectionNode.isMissingNode()) {
                throw new IllegalArgumentException("document中未找到section_" + sectionId.toLowerCase() + "节点");
            }
            
            // 提取topic和segment_id
            if (sectionNode.has("topic")) {
                String t = sectionNode.get("topic").asText("");
                if (!t.isEmpty()) {
                    topic = t;
                }
            }
            if (sectionNode.has("segment_id")) {
                segmentId = sectionNode.get("segment_id").asText("");
            }
            
            // 只保存对应section的JSON
            ObjectNode cleanRoot = objectMapper.createObjectNode();
            ObjectNode readingComprehension = objectMapper.createObjectNode();
            readingComprehension.set("section_" + sectionId.toLowerCase(), sectionNode);
            cleanRoot.set("reading_comprehension", readingComprehension);
            cleanedDocument = objectMapper.writeValueAsString(cleanRoot);
            
        } catch (JsonProcessingException e) {
            logger.error("解析document JSON失败: {}", e.getMessage());
            throw new IllegalArgumentException("解析document JSON失败: " + e.getMessage(), e);
        }

        // 生成question_id: exam_paper_id + segment_id
        String questionId;
        if (!segmentId.isEmpty()) {
            questionId = examPaperId + segmentId;
        } else {
            // 如果没有segment_id，使用原来的格式
            segmentId = partId + sectionId;
            questionId = examPaperId + segmentId;
        }

        // 构建新的metadata格式
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("topic", topic);
        metadata.put("subject", subject);
        metadata.put("question_type", questionType);
        metadata.put("exam_paper_en_id", parseIntegerSafely(examPaperId, "exam_paper_id"));
        metadata.put("exam_paper_en_name", examPaperName);
        metadata.put("part_id", partId);
        metadata.put("segment_id", segmentId);
        metadata.put("question_id", questionId);
        metadata.put("created_time", createdTime);
        // 新增：试卷来源
        metadata.put("exam_paper_en_source", normalizeExamPaperSource(request.getExamPaperEnSource()));
        
        // Removed printing question_source
        // System.out.println("[ChromaDB存储] Section " + sectionId + " - question_source = " + request.getQuestionSource() + ", question_id = " + questionId);
        // System.out.println("[SQL语句] INSERT INTO chroma_collection (id, document, metadata) VALUES ('" + questionId + "', ..., '" + objectMapper.writeValueAsString(metadata) + "')");

        // 构建请求体：documents字段使用清理后的数据
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("ids", Collections.singletonList(questionId));
        requestBody.put("documents", Collections.singletonList(cleanedDocument));
        requestBody.put("metadatas", Collections.singletonList(metadata));
        requestBody.put("embeddings", Collections.singletonList(generateDeterministicEmbedding(questionId)));

        String collectionId = ensureCollectionId();
        String url = collectionAddUrl(collectionId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            java.util.Objects.requireNonNull(java.net.URI.create(url)),
            java.util.Objects.requireNonNull(org.springframework.http.HttpMethod.POST),
            entity,
            String.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            // 成功，不输出
        } else {
            logger.warn("写入 Section {} 数据到 ChromaDB 失败，status={}, body={}",
                    sectionId, response.getStatusCode(), response.getBody());
            throw new IllegalStateException("写入 ChromaDB 失败，状态码：" + response.getStatusCode());
        }
    }

    public Optional<CET4SectionAChromaRecord> fetchSectionARecord(String examPaperId, String questionType) throws JsonProcessingException {
        String normalizedId = normalizeString(examPaperId);
        if (normalizedId.isEmpty()) {
            throw new IllegalArgumentException("examPaperId 不能为空");
        }
        String normalizedType = normalizeQuestionType(questionType);

        String collectionId = ensureCollectionId();
        String url = collectionGetUrl(collectionId);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("limit", 1000);
        requestBody.put("include", List.of("documents", "metadatas"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(URI.create(url), HttpMethod.POST, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            logger.warn("从 ChromaDB 查询 Section A 失败，status={}", response.getStatusCode());
            return Optional.empty();
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode documents = root.path("documents");
        JsonNode metadatas = root.path("metadatas");
        if (!documents.isArray() || !metadatas.isArray()) {
            return Optional.empty();
        }
        
        int size = Math.min(documents.size(), metadatas.size());
        
        for (int i = 0; i < size; i++) {
            JsonNode metaNode = metadatas.get(i);
            if (metaNode == null || !metaNode.isObject()) {
                continue;
            }
            String metaExamId = getTextValue(metaNode, "exam_paper_id");
            String metaType = getTextValue(metaNode, "question_type");
            
            if (!normalizedId.equals(metaExamId) || !normalizedType.equals(metaType)) {
                continue;
            }
            
            String document = documents.get(i).asText("");
            Map<String, Object> metadata = jsonNodeToMap(metaNode);
            return Optional.of(new CET4SectionAChromaRecord(document, metadata));
        }
        
        return Optional.empty();
    }

    /**
     * 根据试卷编号或试卷名称查询该试卷的所有单元
     * @param examPaperId 试卷编号（可选）
     * @param examPaperName 试卷名称（可选）
     * @return 该试卷的所有单元列表
     */
    public List<CET4SectionAChromaRecord> fetchExamPaperUnits(String examPaperId, String examPaperName) throws JsonProcessingException {
        String normalizedId = normalizeString(examPaperId);
        String normalizedName = normalizeString(examPaperName);
        
        if (normalizedId.isEmpty() && normalizedName.isEmpty()) {
            throw new IllegalArgumentException("试卷编号和试卷名称至少提供一个");
        }

        String collectionId = ensureCollectionId();
        String url = collectionGetUrl(collectionId);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("limit", 1000);
        requestBody.put("include", List.of("documents", "metadatas"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(URI.create(url), HttpMethod.POST, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            logger.warn("从 ChromaDB 查询试卷单元失败，status={}", response.getStatusCode());
            return Collections.emptyList();
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode documents = root.path("documents");
        JsonNode metadatas = root.path("metadatas");
        JsonNode ids = root.path("ids");
        
        if (!documents.isArray() || !metadatas.isArray()) {
            return Collections.emptyList();
        }
        
        // ids 可能不存在，但 ChromaDB 默认会返回
        boolean hasIds = ids.isArray();
        int size = Math.min(documents.size(), metadatas.size());
        if (hasIds) {
            size = Math.min(size, ids.size());
        }
        
        List<CET4SectionAChromaRecord> records = new ArrayList<>();
        
        for (int i = 0; i < size; i++) {
            JsonNode metaNode = metadatas.get(i);
            if (metaNode == null || !metaNode.isObject()) {
                continue;
            }
            
            // 获取exam_paper_en_id，支持数字和字符串类型（新格式）
            String metaExamId = "";
            JsonNode examIdNode = metaNode.get("exam_paper_en_id");
            if (examIdNode != null) {
                if (examIdNode.isNumber()) {
                    metaExamId = String.valueOf(examIdNode.asInt());
                } else {
                    metaExamId = examIdNode.asText("");
                }
            }
            
            String metaExamName = getTextValue(metaNode, "exam_paper_en_name");
            
            // 匹配试卷编号或试卷名称
            boolean matches = false;
            if (!normalizedId.isEmpty() && normalizedId.equals(metaExamId)) {
                matches = true;
            }
            if (!normalizedName.isEmpty() && normalizedName.equals(metaExamName)) {
                matches = true;
            }
            
            if (matches) {
                String document = documents.get(i).asText("");
                Map<String, Object> metadata = jsonNodeToMap(metaNode);
                // 如果 ids 存在，添加到 metadata 中
                if (hasIds && i < ids.size()) {
                    String id = ids.get(i).asText("");
                    metadata.put("id", id);
                }
                
                records.add(new CET4SectionAChromaRecord(document, metadata));
            }
        }
        
        // 按 partId 和 sectionId 排序
        records.sort((a, b) -> {
            Map<String, Object> metaA = a.metadata();
            Map<String, Object> metaB = b.metadata();
            
            int partIdA = getIntValue(metaA, "part_id", 0);
            int partIdB = getIntValue(metaB, "part_id", 0);
            if (partIdA != partIdB) {
                return Integer.compare(partIdA, partIdB);
            }
            
            String sectionIdA = String.valueOf(metaA.getOrDefault("section_id", ""));
            String sectionIdB = String.valueOf(metaB.getOrDefault("section_id", ""));
            return sectionIdA.compareTo(sectionIdB);
        });
        
        return records;
    }
    
    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Map<String, Object> jsonNodeToMap(JsonNode objectNode) {
        Map<String, Object> map = new LinkedHashMap<>();
        Iterator<String> fieldNames = objectNode.fieldNames();
        while (fieldNames.hasNext()) {
            String field = fieldNames.next();
            JsonNode value = objectNode.get(field);
            if (value == null || value.isNull()) {
                map.put(field, null);
            } else if (value.isNumber()) {
                // 保持数字类型，但同时也提供字符串形式以便匹配
                map.put(field, value.asInt());
                map.put(field + "_str", value.asText()); // 添加字符串版本
            } else if (value.isValueNode()) {
                map.put(field, value.asText());
            } else {
                map.put(field, value.toString());
            }
        }
        return map;
    }

    private String getTextValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null ? value.asText("") : "";
    }

    public String ensureCollectionId() throws JsonProcessingException {
        String cached = collectionIdCache.get();
        if (cached != null) {
            return cached;
        }
        synchronized (collectionIdCache) {
            cached = collectionIdCache.get();
            if (cached != null) {
                return cached;
            }
            String collectionId = fetchCollectionId();
            if (collectionId == null) {
                throw new IllegalStateException(
                        String.format("ChromaDB 集合不存在: %s (tenant=%s, database=%s)",
                                collectionName, tenant, database));
            }
            collectionIdCache.set(collectionId);
            return collectionId;
        }
    }

    private String fetchCollectionId() throws JsonProcessingException {
        String url = collectionsBaseUrl();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(URI.create(url), HttpMethod.GET, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            logger.warn("获取 ChromaDB 集合列表失败，status={}", response.getStatusCode());
            return null;
        }

        List<Map<String, Object>> collections =
                objectMapper.readValue(response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {});
        if (collections == null) {
            return null;
        }

        for (Map<String, Object> collection : collections) {
            if (collectionName.equals(collection.get("name"))) {
                return String.valueOf(collection.get("id"));
            }
        }
        return null;
    }

    private List<Float> generateDeterministicEmbedding(String seed) {
        java.util.Random random = new java.util.Random(seed == null ? 0 : seed.hashCode());
        List<Float> embedding = new ArrayList<>(EMBEDDING_DIMENSION);
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            embedding.add(random.nextFloat());
        }
        return embedding;
    }

    private String collectionsBaseUrl() {
        return String.format("%s/tenants/%s/databases/%s/collections",
                trimTrailingSlash(Objects.requireNonNull(chromaBaseUrl, "chromadb.base-url 未配置")),
                Objects.requireNonNull(tenant, "chromadb.tenant 未配置"),
                Objects.requireNonNull(database, "chromadb.database 未配置"));
    }

    private String collectionAddUrl(String collectionId) {
        return collectionsBaseUrl() + "/" + collectionId + "/add";
    }

    public String collectionGetUrl(String collectionId) {
        return collectionsBaseUrl() + "/" + collectionId + "/get";
    }

    /**
     * 查询指定科目且来源为 real 的试卷ID集合。
     */
    public Set<String> findRealPaperIdsBySubject(String subject) throws Exception {
        String collectionId = ensureCollectionId();
        String url = collectionGetUrl(collectionId);

        Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("limit", 10000);
        requestBody.put("include", java.util.List.of("metadatas"));

        Map<String, Object> condition1 = new java.util.HashMap<>();
        condition1.put("subject", subject);
        Map<String, Object> condition2 = new java.util.HashMap<>();
        condition2.put("exam_paper_en_source", "real");
        Map<String, Object> whereClause = new java.util.HashMap<>();
        whereClause.put("$and", java.util.List.of(condition1, condition2));
        requestBody.put("where", whereClause);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("从ChromaDB查询失败");
        }

        JsonNode metadatas = objectMapper.readTree(response.getBody()).path("metadatas");
        Set<String> paperIds = new java.util.HashSet<>();
        if (metadatas.isArray()) {
            for (JsonNode metaNode : metadatas) {
                if (metaNode != null && metaNode.has("exam_paper_en_id")) {
                    String id = metaNode.get("exam_paper_en_id").asText("");
                    if (!id.isEmpty()) {
                        paperIds.add(id);
                    }
                }
            }
        }
        return paperIds;
    }

    /**
     * 按试卷ID取出文档+元数据列表，用于生成模板。
     */
    public java.util.List<java.util.Map<String, Object>> fetchPaperUnitsWithMeta(String examPaperId) throws Exception {
        String collectionId = ensureCollectionId();
        String url = collectionGetUrl(collectionId);

        Map<String, Object> requestBody = new java.util.HashMap<>();
        requestBody.put("limit", 10000);
        requestBody.put("include", java.util.List.of("documents", "metadatas"));

        Map<String, Object> whereClause = new java.util.HashMap<>();
        whereClause.put("exam_paper_en_id", Integer.parseInt(examPaperId));
        requestBody.put("where", whereClause);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("从ChromaDB查询失败");
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode documents = root.path("documents");
        JsonNode metadatas = root.path("metadatas");

        java.util.List<java.util.Map<String, Object>> results = new java.util.ArrayList<>();
        if (documents.isArray() && metadatas.isArray()) {
            int size = Math.min(documents.size(), metadatas.size());
            for (int i = 0; i < size; i++) {
                JsonNode metaNode = metadatas.get(i);
                java.util.Map<String, Object> unit = new java.util.HashMap<>();
                unit.put("document", documents.get(i).asText(""));
                if (metaNode != null && metaNode.isObject()) {
                    if (metaNode.has("exam_paper_en_id")) {
                        unit.put("exam_paper_id", metaNode.get("exam_paper_en_id").asText(""));
                    }
                    if (metaNode.has("part_id")) {
                        unit.put("part_id", metaNode.get("part_id").asInt(0));
                    }
                    if (metaNode.has("segment_id")) {
                        unit.put("segment_id", metaNode.get("segment_id").asText(""));
                    }
                    if (metaNode.has("question_type")) {
                        unit.put("question_type", metaNode.get("question_type").asText(""));
                    }
                }
                results.add(unit);
            }
        }
        return results;
    }

    private List<Integer> normalizeBlankNumbers(List<String> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Integer> ordered = new TreeSet<>();
        for (String input : inputs) {
            Integer parsed = parseInteger(input);
            if (parsed != null) {
                ordered.add(parsed);
            }
        }
        return ordered.isEmpty() ? Collections.emptyList() : new ArrayList<>(ordered);
    }

    private Integer parseInteger(String input) {
        if (input == null) {
            return null;
        }
        String digits = input.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            logger.warn("无法解析题号: {}", input);
            return null;
        }
    }

    private Integer parseIntegerSafely(String input, String fieldName) {
        Integer value = parseInteger(input);
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " 必须为数字");
        }
        return value;
    }

    private String normalizeString(String input) {
        return input == null ? "" : input.trim();
    }

    private String normalizeSectionId(String sectionId) {
        String normalized = normalizeString(sectionId);
        return normalized.isEmpty() ? "A" : normalized.toUpperCase();
    }

    private String normalizeQuestionType(String questionType) {
        String normalized = normalizeString(questionType);
        return normalized.isEmpty() ? "选词填空" : normalized;
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return null;
        }
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String normalizeExamPaperSource(String source) {
        String s = normalizeString(source);
        if (s.isEmpty()) return "real"; // 解析生成默认 real
        // 仅允许三种取值
        if ("real".equalsIgnoreCase(s)) return "real";
        if ("AIfromreal".equalsIgnoreCase(s)) return "AIfromreal";
        if ("AIfromself".equalsIgnoreCase(s)) return "AIfromself";
        return s; // 传入其他值则原样保留
    }
}


