package seucxxy.csd.backend.cet4.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import seucxxy.csd.backend.cet4.dto.CET4SectionAChromaRecord;
import seucxxy.csd.backend.cet4.dto.CET4SectionAChromaRequest;
import seucxxy.csd.backend.cet4.dto.CET4SimpleChromaRequest;
import seucxxy.csd.backend.cet4.service.CET4ChromaEngExamPaperService;
import seucxxy.csd.backend.cet4.service.CET4PaperAnalysisService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/api/cet4/paper-analysis-concurrent", "/api/cet4/paper-analysis"})
@CrossOrigin(origins = {"http://localhost:5002", "http://localhost:5003"}, allowCredentials = "true")
@RequiredArgsConstructor
public class CET4PaperAnalysisController {

    private final CET4PaperAnalysisService concurrenceService;
    private final CET4ChromaEngExamPaperService cet4ChromaService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> parsePdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "structure", defaultValue = "false") boolean structure) {
        Map<String, Object> body = new HashMap<>();
        if (file == null || file.isEmpty()) {
            body.put("success", false);
            body.put("message", "请上传有效的文件");
            return ResponseEntity.badRequest().body(body);
        }
        try {
            body.put("success", true);
            body.put("fileName", file.getOriginalFilename());
            Map<String, Object> result = concurrenceService.extractFromPdf(file);
            body.putAll(result);
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        } catch (IOException e) {
            body.put("success", false);
            body.put("message", "解析 PDF 文件失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    @PostMapping("/plain")
    public ResponseEntity<Map<String, Object>> parsePlain(@RequestParam("file") MultipartFile file) {
        Map<String, Object> body = new HashMap<>();
        if (file == null || file.isEmpty()) {
            body.put("success", false);
            body.put("message", "请上传有效的文件");
            return ResponseEntity.badRequest().body(body);
        }
        try {
            body.put("success", true);
            body.put("fileName", file.getOriginalFilename());
            Map<String, Object> result = concurrenceService.extractFromPlain(file);
            body.putAll(result);
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        } catch (IOException e) {
            body.put("success", false);
            body.put("message", "解析文本失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    @PostMapping("/image")
    public ResponseEntity<Map<String, Object>> parseImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> body = new HashMap<>();
        if (file == null || file.isEmpty()) {
            body.put("success", false);
            body.put("message", "请上传有效的文件");
            return ResponseEntity.badRequest().body(body);
        }
        try {
            body.put("success", true);
            body.put("fileName", file.getOriginalFilename());
            Map<String, Object> result = concurrenceService.extractFromImage(file);
            body.putAll(result);
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        } catch (IOException e) {
            body.put("success", false);
            body.put("message", "解析图片失败，请稍后重试");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    @PostMapping("/section-a/chroma")
    public ResponseEntity<Map<String, Object>> storeSectionAInChroma(
            @Valid @RequestBody CET4SectionAChromaRequest request) {

        Map<String, Object> body = new HashMap<>();
        try {
            cet4ChromaService.storeSectionA(request);

            body.put("success", true);
            body.put("message", "Section A 已成功写入 ChromaDB");
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        } catch (IOException e) {
            e.printStackTrace();
            body.put("success", false);
            body.put("message", "写入 ChromaDB 失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        } catch (Exception e) {
            e.printStackTrace();
            body.put("success", false);
            body.put("message", "写入 ChromaDB 失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    @PostMapping("/section-b/chroma")
    public ResponseEntity<Map<String, Object>> storeSectionBInChroma(
            @Valid @RequestBody CET4SectionAChromaRequest request) {

        Map<String, Object> body = new HashMap<>();
        try {
            request.setSectionId("B");
            request.setQuestionType("段落匹配");
            cet4ChromaService.storeSectionB(request);

            body.put("success", true);
            body.put("message", "Section B 已成功写入 ChromaDB");
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        } catch (Exception e) {
            body.put("success", false);
            body.put("message", "写入 ChromaDB 失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    @PostMapping("/section-c/chroma")
    public ResponseEntity<Map<String, Object>> storeSectionCInChroma(
            @Valid @RequestBody CET4SectionAChromaRequest request) {

        Map<String, Object> body = new HashMap<>();
        try {
            request.setSectionId("C");
            request.setQuestionType("篇章阅读");
            cet4ChromaService.storeSectionC(request);

            body.put("success", true);
            body.put("message", "Section C 已成功写入 ChromaDB");
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        } catch (Exception e) {
            body.put("success", false);
            body.put("message", "写入 ChromaDB 失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    @PostMapping("/writing/chroma")
    public ResponseEntity<Map<String, Object>> storeWritingInChroma(
            @Valid @RequestBody CET4SimpleChromaRequest request) {

        Map<String, Object> body = new HashMap<>();
        try {
            request.setPartId(1);
            request.setQuestionType("写作");
            cet4ChromaService.storePartSimple(request);

            body.put("success", true);
            body.put("message", "Part I（写作）已成功写入 ChromaDB");
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        } catch (Exception e) {
            body.put("success", false);
            body.put("message", "写入 ChromaDB 失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    @PostMapping("/translation/chroma")
    public ResponseEntity<Map<String, Object>> storeTranslationInChroma(
            @Valid @RequestBody CET4SimpleChromaRequest request) {

        Map<String, Object> body = new HashMap<>();
        try {
            request.setPartId(4);
            request.setQuestionType("翻译");
            cet4ChromaService.storePartSimple(request);

            body.put("success", true);
            body.put("message", "Part IV（翻译）已成功写入 ChromaDB");
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        } catch (Exception e) {
            body.put("success", false);
            body.put("message", "写入 ChromaDB 失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    @PostMapping("/part2/ab/chroma")
    public ResponseEntity<Map<String, Object>> storePart2ABInChroma(
            @Valid @RequestBody CET4SectionAChromaRequest request) {

        Map<String, Object> body = new HashMap<>();
        try {
            request.setSectionId("AB");
            request.setPartId(2);
            request.setQuestionType("听力");
            cet4ChromaService.storeListeningPart2AB(request);

            body.put("success", true);
            body.put("message", "Part II Section AB（听力）已成功写入 ChromaDB");
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        } catch (Exception e) {
            body.put("success", false);
            body.put("message", "写入 ChromaDB 失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    @PostMapping("/part2/c/chroma")
    public ResponseEntity<Map<String, Object>> storePart2CInChroma(
            @Valid @RequestBody CET4SectionAChromaRequest request) {

        Map<String, Object> body = new HashMap<>();
        try {
            request.setSectionId("C");
            request.setPartId(2);
            request.setQuestionType("听力");
            cet4ChromaService.storeListeningPart2C(request);

            body.put("success", true);
            body.put("message", "Part II Section C（听力）已成功写入 ChromaDB");
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            body.put("success", false);
            body.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        } catch (Exception e) {
            body.put("success", false);
            body.put("message", "写入 ChromaDB 失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }

    @GetMapping("/exam-paper/units")
    public ResponseEntity<Map<String, Object>> queryExamPaperUnits(
            @RequestParam(value = "examPaperId", required = false) String examPaperId,
            @RequestParam(value = "examPaperName", required = false) String examPaperName) {

        Map<String, Object> body = new HashMap<>();
        try {
            java.util.List<CET4SectionAChromaRecord> records = cet4ChromaService.fetchExamPaperUnits(examPaperId, examPaperName);

            if (records.isEmpty()) {
                body.put("success", false);
                body.put("message", "未在 ChromaDB 中找到对应的试卷数据");
                return ResponseEntity.badRequest().body(body);
            }

            java.util.List<Map<String, Object>> units = new java.util.ArrayList<>();

            for (CET4SectionAChromaRecord record : records) {
                Map<String, Object> unit = new HashMap<>();
                Map<String, Object> metadata = record.metadata();

                unit.put("metadata", metadata);
                unit.put("document", record.document());

                units.add(unit);
            }

            body.put("success", true);
            body.put("units", units);

            return ResponseEntity.ok(body);
        } catch (Exception e) {
            System.err.println("[ERROR] 查询单元时发生异常: " + e.getMessage());
            e.printStackTrace();
            body.put("success", false);
            body.put("message", "查询失败，请稍后重试。");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }
}
