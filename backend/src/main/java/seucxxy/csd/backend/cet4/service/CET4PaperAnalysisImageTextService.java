package seucxxy.csd.backend.cet4.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

@Service
public class CET4PaperAnalysisImageTextService {

    private final String tesseractDataPath;
    private final String tesseractLanguage;

    public CET4PaperAnalysisImageTextService(
            @Value("${tesseract.data-path:}") String tesseractDataPath,
            @Value("${tesseract.language:eng+chi_sim}") String tesseractLanguage
    ) {
        String envPrefix = System.getenv("TESSDATA_PREFIX");
        String fallback = "E:/xmj/tessdata";
        this.tesseractDataPath = (tesseractDataPath != null && !tesseractDataPath.isBlank())
                ? tesseractDataPath
                : (envPrefix != null && !envPrefix.isBlank() ? envPrefix : fallback);
        this.tesseractLanguage = (tesseractLanguage == null || tesseractLanguage.isBlank())
                ? "eng+chi_sim"
                : tesseractLanguage;
    }

    public String ocrImage(BufferedImage image) {
        if (image == null) {
            return "";
        }
        try {
            return createTesseract().doOCR(image).trim();
        } catch (Exception e) {
            throw new IllegalArgumentException("OCR 识别失败，请检查图片是否清晰", e);
        }
    }

    public String ocrPage(Supplier<BufferedImage> pageSupplier) {
        try {
            BufferedImage image = pageSupplier.get();
            return ocrImage(image);
        } catch (Exception e) {
            throw new IllegalArgumentException("OCR 识别失败，请检查文件是否清晰", e);
        }
    }

    @SuppressWarnings("deprecation")
    private ITesseract createTesseract() {
        Tesseract tesseract = new Tesseract();
        Path dataPath = Paths.get(tesseractDataPath);
        if (!Files.exists(dataPath)) {
            throw new IllegalArgumentException("Tesseract 数据目录不存在: " + tesseractDataPath);
        }
        tesseract.setDatapath(dataPath.toAbsolutePath().toString());
        tesseract.setLanguage(tesseractLanguage);
        try {
            tesseract.setTessVariable("preserve_interword_spaces", "1");
        } catch (Exception ignored) {
            // 某些版本不支持，可忽略
        }
        return tesseract;
    }
}
