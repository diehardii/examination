package seucxxy.csd.backend.hs3.service;

import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.*;

/**
 * HS3 高考英语 Neo4j 图数据库服务
 * 
 * 负责将解析后的试卷结构存储到Neo4j图数据库
 * 数据库：examPaperEnStructure
 * 
 * 试卷结构：
 * ExamPaper -> Part -> Section -> Segment
 * 
 * 叶子节点（Segment）存储完整的题目JSON数据
 */
@Service
public class HS3Neo4jService {

    private static final Logger logger = LoggerFactory.getLogger(HS3Neo4jService.class);

    @Value("${neo4j.uri:neo4j://localhost:7687}")
    private String neo4jUri;

    @Value("${neo4j.username:neo4j}")
    private String neo4jUsername;

    @Value("${neo4j.password:12345678}")
    private String neo4jPassword;

    @Value("${neo4j.database:examPaperEnStructure}")
    private String neo4jDatabase;

    private Driver driver;

    @PostConstruct
    public void init() {
        try {
            driver = GraphDatabase.driver(neo4jUri, AuthTokens.basic(neo4jUsername, neo4jPassword));
            driver.verifyConnectivity();
            logger.info("[HS3 Neo4j] 连接成功: uri={}, database={}", neo4jUri, neo4jDatabase);
        } catch (Exception e) {
            logger.error("[HS3 Neo4j] 连接失败: {}", e.getMessage());
            throw new RuntimeException("Neo4j连接失败", e);
        }
    }

    @PreDestroy
    public void close() {
        if (driver != null) {
            driver.close();
            logger.info("[HS3 Neo4j] 连接已关闭");
        }
    }
    
    /**
     * 获取试卷所有Part的信息列表
     * 
     * @param examPaperId 试卷ID
     * @return Part信息列表，包含 partNumber 和 partName
     */
    public List<Map<String, Object>> getPartsList(String examPaperId) {
        try (Session session = driver.session(SessionConfig.forDatabase(neo4jDatabase))) {
            return session.executeRead(tx -> {
                String query = """
                    MATCH (p:ExamPaper {id: $examPaperId})-[:CONTAINS_PART]->(part:Part)
                    RETURN part.part_number AS partNumber, part.part_name AS partName, 
                           part.description AS description
                    ORDER BY toInteger(part.part_number)
                    """;
                Result result = tx.run(query, Map.of("examPaperId", examPaperId));
                List<Map<String, Object>> parts = new ArrayList<>();
                while (result.hasNext()) {
                    Record record = result.next();
                    Map<String, Object> part = new HashMap<>();
                    part.put("partNumber", record.get("partNumber").asString(null));
                    part.put("partName", record.get("partName").asString(null));
                    part.put("description", record.get("description").asString(null));
                    parts.add(part);
                }
                return parts;
            });
        } catch (Exception e) {
            logger.error("[HS3 Neo4j] 获取Part列表失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取试卷所有层级节点的description信息
     * 用于在展示Coze解析结果时附加说明信息
     * 
     * @param examPaperId 试卷ID
     * @return 包含所有Part/Section/Segment description的Map
     *         结构: { "parts": {...}, "sections": {...}, "segments": {...} }
     */
    public Map<String, Object> getAllDescriptions(String examPaperId) {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> partDescs = new HashMap<>();
        Map<String, String> sectionDescs = new HashMap<>();
        Map<String, String> segmentDescs = new HashMap<>();
        
        try (Session session = driver.session(SessionConfig.forDatabase(neo4jDatabase))) {
            session.executeRead(tx -> {
                // 获取所有Part的description，使用 part_number 作为 key
                String partQuery = """
                    MATCH (p:ExamPaper {id: $examPaperId})-[:CONTAINS_PART]->(part:Part)
                    RETURN part.part_number AS partNumber, part.description AS description
                    """;
                Result partResult = tx.run(partQuery, Map.of("examPaperId", examPaperId));
                while (partResult.hasNext()) {
                    Record record = partResult.next();
                    String partNumber = record.get("partNumber").asString(null);
                    String desc = record.get("description").asString(null);
                    if (partNumber != null && desc != null && !desc.isBlank()) {
                        partDescs.put(partNumber, desc);  // key: "1", "2", ...
                    }
                }
                
                // 获取所有Section的description，使用 part_number|section_number 作为 key
                String sectionQuery = """
                    MATCH (p:ExamPaper {id: $examPaperId})-[:CONTAINS_PART]->(part:Part)
                          -[:CONTAINS_SECTION]->(sec:Section)
                    RETURN part.part_number AS partNumber, sec.section_number AS sectionNumber,
                           sec.description AS description
                    """;
                Result sectionResult = tx.run(sectionQuery, Map.of("examPaperId", examPaperId));
                while (sectionResult.hasNext()) {
                    Record record = sectionResult.next();
                    String partNumber = record.get("partNumber").asString("");
                    String sectionNumber = record.get("sectionNumber").asString("");
                    String desc = record.get("description").asString(null);
                    String key = partNumber + "|" + sectionNumber;  // key: "1|1", "1|2", ...
                    if (desc != null && !desc.isBlank()) {
                        sectionDescs.put(key, desc);
                    }
                }
                
                // 获取所有Segment的description，使用 part_number|section_number|segment_number 作为 key
                String segmentQuery = """
                    MATCH (p:ExamPaper {id: $examPaperId})-[:CONTAINS_PART]->(part:Part)
                          -[:CONTAINS_SECTION]->(sec:Section)-[:CONTAINS_SEGMENT]->(seg:Segment)
                    RETURN part.part_number AS partNumber, sec.section_number AS sectionNumber,
                           seg.segment_number AS segmentNumber, seg.description AS description
                    """;
                Result segmentResult = tx.run(segmentQuery, Map.of("examPaperId", examPaperId));
                while (segmentResult.hasNext()) {
                    Record record = segmentResult.next();
                    String partNumber = record.get("partNumber").asString("");
                    String sectionNumber = record.get("sectionNumber").asString("");
                    String segmentNumber = record.get("segmentNumber").asString("");
                    String desc = record.get("description").asString(null);
                    String key = partNumber + "|" + sectionNumber + "|" + segmentNumber;  // key: "1|1|1", ...
                    if (desc != null && !desc.isBlank()) {
                        segmentDescs.put(key, desc);
                    }
                }
                
                return null;
            });
        } catch (Exception e) {
            logger.error("[HS3 Neo4j] 获取description信息失败: {}", e.getMessage());
        }
        
        result.put("parts", partDescs);
        result.put("sections", sectionDescs);
        result.put("segments", segmentDescs);
        return result;
    }
    
}
