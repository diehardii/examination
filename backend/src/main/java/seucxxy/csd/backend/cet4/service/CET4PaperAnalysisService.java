package seucxxy.csd.backend.cet4.service;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CET4PaperAnalysisService {

    @Value("${pdf.render.dpi:300}")
    private int pdfRenderDpi;

    @Value("${analysis.concurrent.ocr-pool:4}")
    private int ocrPoolSize;

    @Value("${analysis.concurrent.coze-pool:8}")
    private int cozePoolSize;

    private final CET4PaperAnalysisImageTextService imageTextService;
    private final CET4PaperAnalysisSegmentService segmentService;

    public Map<String, Object> extractFromPdf(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException("该 PDF 文件已加密，无法解析");
            }
            PDFRenderer renderer = new PDFRenderer(document);
            ExecutorService ocrExecutor = Executors.newFixedThreadPool(ocrPoolSize);
            List<CompletableFuture<PageText>> futures = new ArrayList<>();
            List<PageImage> renderedPages = new ArrayList<>(document.getNumberOfPages());
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                try {
                    BufferedImage pageImage = renderer.renderImageWithDPI(i, pdfRenderDpi, ImageType.RGB);
                    renderedPages.add(new PageImage(i, pageImage));
                } catch (IOException e) {
                    throw new IllegalArgumentException("第 " + (i + 1) + " 页渲染失败", e);
                }
            }

            for (PageImage page : renderedPages) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        String text = imageTextService.ocrImage(page.image);
                        return new PageText(page.index, text);
                    } catch (Exception e) {
                        throw new RuntimeException("PDF 第 " + (page.index + 1) + " 页 OCR 失败", e);
                    }
                }, ocrExecutor));
            }
            List<PageText> pages = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
            shutdownQuietly(ocrExecutor);
            String fullText = pages.stream()
                    .sorted((a, b) -> Integer.compare(a.index, b.index))
                    .map(p -> p.text)
                    .filter(t -> t != null && !t.isBlank())
                    .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()))
                    .trim();
            if (fullText.isBlank()) {
                throw new IllegalArgumentException("OCR 未识别到文本内容，请确认 PDF 是否清晰可读");
            }
            return processWithSegments(fullText);
        }
    }

    public Map<String, Object> extractFromPlain(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String text = new String(file.getBytes());
        return processWithSegments(text);
    }

    public Map<String, Object> extractFromImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            throw new IllegalArgumentException("无法读取图片，请检查文件格式");
        }
        String fullText = imageTextService.ocrImage(image);
        if (fullText == null || fullText.isBlank()) {
            throw new IllegalArgumentException("OCR 未识别到文本内容，请确认图片是否清晰可读");
        }
        return processWithSegments(fullText);
    }

    private Map<String, Object> processWithSegments(String fullText) throws IOException {
        ExecutorService cozeExecutor = Executors.newFixedThreadPool(cozePoolSize);
        try {
            return segmentService.processFullText(fullText, cozeExecutor);
        } finally {
            shutdownQuietly(cozeExecutor);
        }
    }

    private void shutdownQuietly(ExecutorService executor) {
        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private record PageText(int index, String text) { }

    private record PageImage(int index, BufferedImage image) { }
}
