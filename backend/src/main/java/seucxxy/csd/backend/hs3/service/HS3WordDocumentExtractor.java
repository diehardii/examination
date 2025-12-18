package seucxxy.csd.backend.hs3.service;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableIterator;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.xml.bind.JAXBElement;
import java.io.*;
import java.util.List;

/**
 * Word 文档文本提取服务
 * 
 * 提供 .doc/.docx 文件的文本提取功能
 * 支持：
 * - 使用 docx4j 解析 .docx 文件（保持段落和表格的原始顺序）
 * - 使用 Apache POI 解析 .doc 文件
 * - 将带下划线的数字转换为 _数字_ 格式
 * - 表格格式化输出（带边框和对齐）
 */
@Service
public class HS3WordDocumentExtractor {

    /**
     * 从 Word 文件中提取文本
     * 
     * @param file 上传的 Word 文件
     * @return 提取的文本内容
     * @throws IOException 如果文件读取失败
     */
    public String extractText(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("无法获取文件名");
        }
        
        String lowerName = fileName.toLowerCase();
        
        if (lowerName.endsWith(".docx")) {
            return extractFromDocx(file);
        } else if (lowerName.endsWith(".doc")) {
            return extractFromDoc(file);
        } else {
            throw new IllegalArgumentException("不支持的文件格式，仅支持 .doc 和 .docx");
        }
    }
    
    // ==================== .docx 文件解析（docx4j） ====================
    
    /**
     * 从 .docx 文件中提取文本，使用docx4j保持段落和表格的原始顺序
     * 表格线用下划线代替，文字保持不变
     */
    private String extractFromDocx(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream()) {
            // 使用 docx4j 加载文档
            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(is);
            MainDocumentPart mainDocumentPart = wordMLPackage.getMainDocumentPart();
            
            StringBuilder sb = new StringBuilder();
            
            // 获取文档body中的所有内容（段落和表格按原始顺序）
            List<Object> content = mainDocumentPart.getContent();
            System.out.println("[docx4j] 文档内容元素数量: " + content.size());
            
            int tableCount = 0;
            int paragraphCount = 0;
            
            for (Object obj : content) {
                // 处理JAXBElement包装
                if (obj instanceof JAXBElement) {
                    obj = ((JAXBElement<?>) obj).getValue();
                }
                
                System.out.println("[docx4j] 元素类型: " + obj.getClass().getSimpleName());
                
                if (obj instanceof P) {
                    // 段落
                    paragraphCount++;
                    String paragraphText = extractTextFromDocx4jParagraph((P) obj);
                    if (!paragraphText.isEmpty()) {
                        sb.append(paragraphText).append("\n");
                    }
                } else if (obj instanceof Tbl) {
                    // 表格 - 转换为下划线格式
                    tableCount++;
                    System.out.println("========== 表格线 ==========");
                    System.out.println("[docx4j] 发现第 " + tableCount + " 个表格");
                    String tableText = convertDocx4jTableToUnderline((Tbl) obj);
                    sb.append(tableText);
                }
            }
            
            System.out.println("[docx4j] 解析完成: " + paragraphCount + " 个段落, " + tableCount + " 个表格");
            
            return sb.toString();
            
        } catch (Exception e) {
            System.err.println("[docx4j] 解析失败，回退到POI方式: " + e.getMessage());
            e.printStackTrace();
            // 回退到 POI 方式
            return extractFromDocxWithPoi(file);
        }
    }
    
    /**
     * 从docx4j段落中提取文本
     * 检测带下划线的文本（如填空题号），用下划线包围
     */
    private String extractTextFromDocx4jParagraph(P paragraph) {
        StringBuilder sb = new StringBuilder();
        
        List<Object> content = paragraph.getContent();
        for (Object obj : content) {
            if (obj instanceof JAXBElement) {
                obj = ((JAXBElement<?>) obj).getValue();
            }
            
            if (obj instanceof R) {
                // Run（文本运行）
                R run = (R) obj;
                
                // 检查是否有下划线格式
                boolean isUnderlined = false;
                RPr rPr = run.getRPr();
                if (rPr != null && rPr.getU() != null) {
                    // 有下划线属性
                    U underline = rPr.getU();
                    if (underline.getVal() != null && underline.getVal() != UnderlineEnumeration.NONE) {
                        isUnderlined = true;
                    }
                }
                
                for (Object runContent : run.getContent()) {
                    if (runContent instanceof JAXBElement) {
                        runContent = ((JAXBElement<?>) runContent).getValue();
                    }
                    
                    if (runContent instanceof Text) {
                        Text text = (Text) runContent;
                        String textValue = text.getValue();
                        
                        if (isUnderlined && textValue != null && !textValue.trim().isEmpty()) {
                            // 带下划线的文本：用下划线包围
                            String trimmed = textValue.trim();
                            if (trimmed.matches("\\d+")) {
                                // 纯数字，左右各补1个下划线（最短）
                                sb.append("_").append(trimmed).append("_");
                            } else {
                                // 其他带下划线文本，左右补3个下划线
                                sb.append("___").append(trimmed).append("___");
                            }
                        } else {
                            sb.append(textValue);
                        }
                    }
                }
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 将docx4j表格转换为带下划线的文本格式
     * 策略：
     * 1. 多列表格：每列固定宽度，空格补齐对齐
     * 2. 单列写作表格：每一行都输出，有文字行左右补下划线，无文字行输出一整行下划线
     */
    private String convertDocx4jTableToUnderline(Tbl table) {
        StringBuilder sb = new StringBuilder();
        
        List<Object> tableContent = table.getContent();
        System.out.println("[表格解析] 表格内容元素数: " + tableContent.size());
        
        // 先判断是单列还是多列表格
        int maxCols = 1;
        for (Object rowObj : tableContent) {
            if (rowObj instanceof JAXBElement) {
                rowObj = ((JAXBElement<?>) rowObj).getValue();
            }
            if (rowObj instanceof Tr) {
                int cols = 0;
                for (Object cellObj : ((Tr) rowObj).getContent()) {
                    if (cellObj instanceof JAXBElement) {
                        cellObj = ((JAXBElement<?>) cellObj).getValue();
                    }
                    if (cellObj instanceof Tc) cols++;
                }
                maxCols = Math.max(maxCols, cols);
            }
        }
        
        System.out.println("[表格解析] 最大列数: " + maxCols);
        
        if (maxCols == 1) {
            // 单列写作表格：提取单元格内的每个段落作为单独一行
            sb.append(formatWritingTableByParagraphs(table));
        } else {
            // 多列数据表格
            java.util.List<java.util.List<String>> allRows = new java.util.ArrayList<>();
            for (Object rowObj : tableContent) {
                if (rowObj instanceof JAXBElement) {
                    rowObj = ((JAXBElement<?>) rowObj).getValue();
                }
                if (rowObj instanceof Tr) {
                    java.util.List<String> rowCells = new java.util.ArrayList<>();
                    for (Object cellObj : ((Tr) rowObj).getContent()) {
                        if (cellObj instanceof JAXBElement) {
                            cellObj = ((JAXBElement<?>) cellObj).getValue();
                        }
                        if (cellObj instanceof Tc) {
                            String cellText = extractTextFromDocx4jCell((Tc) cellObj);
                            rowCells.add(cellText.trim());
                        }
                    }
                    if (!rowCells.isEmpty()) {
                        allRows.add(rowCells);
                    }
                }
            }
            sb.append(formatDataTable(allRows, maxCols));
        }
        
        return sb.toString();
    }
    
    /**
     * 格式化写作表格 - 按单元格内的段落逐行处理
     * 每个段落都作为一行输出
     */
    private String formatWritingTableByParagraphs(Tbl table) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");

        List<Object> tableContent = table.getContent();
        java.util.List<String> lines = new java.util.ArrayList<>();
        int totalParagraphs = 0;
        int maxContentWidth = 0;

        // 第1遍：收集每个段落内容，并计算最长可见宽度
        for (Object rowObj : tableContent) {
            if (rowObj instanceof JAXBElement) {
                rowObj = ((JAXBElement<?>) rowObj).getValue();
            }
            if (!(rowObj instanceof Tr)) {
                continue;
            }

            Tr row = (Tr) rowObj;
            for (Object cellObj : row.getContent()) {
                if (cellObj instanceof JAXBElement) {
                    cellObj = ((JAXBElement<?>) cellObj).getValue();
                }
                if (!(cellObj instanceof Tc)) {
                    continue;
                }

                Tc cell = (Tc) cellObj;
                for (Object paraObj : cell.getContent()) {
                    if (paraObj instanceof JAXBElement) {
                        paraObj = ((JAXBElement<?>) paraObj).getValue();
                    }
                    if (paraObj instanceof P) {
                        totalParagraphs++;
                        String paraText = extractTextFromDocx4jParagraph((P) paraObj);
                        String content = normalizeInlineText(paraText);
                        lines.add(content);
                        maxContentWidth = Math.max(maxContentWidth, getDisplayWidth(content));
                    }
                }
            }
        }

        // 行宽至少为70，并保证左右各3个下划线时仍能容纳最长文字
        int lineWidth = Math.max(70, maxContentWidth + 6);
        System.out.println("[写作表格] 共处理 " + totalParagraphs + " 个段落, 行宽=" + lineWidth);

        // 第2遍：按统一行宽输出，所有下划线行与最长行对齐
        for (String content : lines) {
            if (content.isEmpty()) {
                sb.append(generateUnderline(lineWidth)).append("\n");
            } else {
                sb.append(formatWritingLine(content, lineWidth)).append("\n");
            }
        }

        sb.append("\n");
        return sb.toString();
    }
    
    /**
     * 格式化数据表格（多列）
     * 每列固定宽度，用空格补齐对齐
     */
    private String formatDataTable(java.util.List<java.util.List<String>> allRows, int maxCols) {
        StringBuilder sb = new StringBuilder();

        // 先规范化所有单元格内容，并保证每行列数一致
        for (java.util.List<String> row : allRows) {
            for (int i = 0; i < row.size(); i++) {
                row.set(i, normalizeCellText(row.get(i)));
            }
            while (row.size() < maxCols) {
                row.add("");
            }
        }
        
        // 计算每列的最大显示宽度
        int[] colWidths = new int[maxCols];
        for (java.util.List<String> row : allRows) {
            for (int i = 0; i < row.size(); i++) {
                String cellText = row.get(i);
                int displayWidth = getDisplayWidth(cellText);
                colWidths[i] = Math.max(colWidths[i], displayWidth);
            }
        }
        
        // 确保最小列宽
        for (int i = 0; i < maxCols; i++) {
            colWidths[i] = Math.max(colWidths[i], 10);
        }
        
        // 先生成所有行文本，每列都补齐宽度
        java.util.List<String> renderedLines = new java.util.ArrayList<>();
        for (java.util.List<String> row : allRows) {
            StringBuilder rowText = new StringBuilder();
            for (int i = 0; i < maxCols; i++) {
                String cellContent = row.get(i);
                if (i > 0) {
                    rowText.append(" | ");
                }
                // 所有列都补齐宽度，确保右边框对齐
                rowText.append(padRightByDisplayWidth(cellContent, colWidths[i]));
            }
            renderedLines.add(rowText.toString());
        }

        // 计算总宽度: "| " + 内容 + " |"
        // 内容宽度 = sum(colWidths) + (maxCols-1)*3 (分隔符 " | ")
        int contentWidth = 0;
        for (int w : colWidths) {
            contentWidth += w;
        }
        contentWidth += (maxCols - 1) * 3; // 分隔符 " | "
        int totalWidth = 2 + contentWidth + 2; // "| " 和 " |"

        // 输出表格，每行有边框和下划线
        String borderLine = generateUnderline(totalWidth);
        sb.append("\n").append(borderLine).append("\n");
        for (String line : renderedLines) {
            sb.append("| ").append(line).append(" |\n");
            sb.append(borderLine).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 从docx4j单元格中提取文本
     */
    private String extractTextFromDocx4jCell(Tc cell) {
        StringBuilder sb = new StringBuilder();
        
        for (Object obj : cell.getContent()) {
            if (obj instanceof JAXBElement) {
                obj = ((JAXBElement<?>) obj).getValue();
            }
            
            if (obj instanceof P) {
                String paraText = extractTextFromDocx4jParagraph((P) obj);
                if (!paraText.isEmpty()) {
                    sb.append(paraText).append(" ");
                }
            }
        }
        
        return sb.toString().trim();
    }
    
    /**
     * 使用POI方式从.docx文件提取文本（作为回退方案）
     */
    private String extractFromDocxWithPoi(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream();
             XWPFDocument document = new XWPFDocument(is)) {
            
            StringBuilder sb = new StringBuilder();
            
            // 处理段落
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String paragraphText = extractTextFromParagraph(paragraph);
                if (!paragraphText.isEmpty()) {
                    sb.append(paragraphText);
                    sb.append("\n");
                }
            }
            
            // 处理表格
            for (XWPFTable table : document.getTables()) {
                sb.append("\n");
                for (XWPFTableRow row : table.getRows()) {
                    String rowContent = extractTableRowContentForMd(row);
                    sb.append(rowContent).append("\n");
                }
                sb.append("\n");
            }
            
            return sb.toString();
        }
    }
    
    // ==================== .doc 文件解析（Apache POI） ====================
    
    /**
     * 从 .doc 文件中提取文本，处理带下划线的数字
     * 同时处理表格内容，还原下划线
     */
    private String extractFromDoc(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream();
             HWPFDocument document = new HWPFDocument(is)) {
            
            StringBuilder sb = new StringBuilder();
            Range range = document.getRange();
            
            // 处理段落
            for (int i = 0; i < range.numParagraphs(); i++) {
                Paragraph paragraph = range.getParagraph(i);
                String paragraphText = extractTextFromDocParagraph(paragraph);
                
                if (!paragraphText.isEmpty()) {
                    sb.append(paragraphText);
                    // 段落结束添加换行（如果不是已经换行结尾）
                    if (!paragraphText.endsWith("\n") && !paragraphText.endsWith("\r")) {
                        sb.append("\n");
                    }
                }
            }
            
            // 处理表格
            TableIterator tableIterator = new TableIterator(range);
            int tableWidth = 80; // 默认表格宽度
            
            while (tableIterator.hasNext()) {
                Table table = tableIterator.next();
                sb.append("\n[表格开始]\n");
                
                for (int rowIdx = 0; rowIdx < table.numRows(); rowIdx++) {
                    TableRow row = table.getRow(rowIdx);
                    String rowContent = extractDocTableRowContent(row, tableWidth);
                    sb.append(rowContent).append("\n");
                }
                sb.append("[表格结束]\n");
            }
            
            return sb.toString();
        }
    }
    
    /**
     * 从 .doc 段落中提取文本，处理下划线（用于普通段落）
     * 对于空白下划线，输出下划线字符
     */
    private String extractTextFromDocParagraph(Paragraph paragraph) {
        StringBuilder paragraphText = new StringBuilder();
        boolean hasBlankUnderline = false;
        boolean hasRealText = false;
        
        for (int j = 0; j < paragraph.numCharacterRuns(); j++) {
            CharacterRun run = paragraph.getCharacterRun(j);
            String text = run.text();
            
            // 检查是否有下划线
            boolean isUnderlined = run.getUnderlineCode() != 0;
            
            if (isUnderlined) {
                if (text == null || text.trim().isEmpty()) {
                    // 空白下划线
                    hasBlankUnderline = true;
                } else {
                    // 处理带下划线的文本，将其中的数字转换为 _数字_ 格式
                    paragraphText.append(processUnderlinedText(text));
                    hasRealText = true;
                }
            } else {
                if (text != null && !text.isEmpty()) {
                    paragraphText.append(text);
                    if (!text.trim().isEmpty()) {
                        hasRealText = true;
                    }
                }
            }
        }
        
        // 如果这个段落只有空白下划线，没有真正的文字，输出下划线
        if (hasBlankUnderline && !hasRealText) {
            return generateUnderline(80);
        }
        
        return paragraphText.toString();
    }
    
    /**
     * 从.doc表格段落提取文本（不处理空白下划线）
     */
    private String extractTextFromDocParagraphForTable(Paragraph paragraph) {
        StringBuilder paragraphText = new StringBuilder();
        
        for (int j = 0; j < paragraph.numCharacterRuns(); j++) {
            CharacterRun run = paragraph.getCharacterRun(j);
            String text = run.text();
            if (text == null) continue;
            
            boolean isUnderlined = run.getUnderlineCode() != 0;
            
            if (isUnderlined) {
                // 如果是空白下划线，跳过
                if (text.trim().isEmpty()) {
                    continue;
                }
                // 处理带下划线的文本
                paragraphText.append(processUnderlinedText(text));
            } else {
                paragraphText.append(text);
            }
        }
        
        return paragraphText.toString();
    }
    
    /**
     * 提取.doc表格行内容，处理下划线和空行
     */
    private String extractDocTableRowContent(TableRow row, int tableWidth) {
        StringBuilder rowText = new StringBuilder();
        boolean hasRealText = false;
        
        for (int colIdx = 0; colIdx < row.numCells(); colIdx++) {
            TableCell cell = row.getCell(colIdx);
            
            for (int paraIdx = 0; paraIdx < cell.numParagraphs(); paraIdx++) {
                Paragraph cellPara = cell.getParagraph(paraIdx);
                String cellText = extractTextFromDocParagraphForTable(cellPara);
                
                if (!cellText.trim().isEmpty()) {
                    // 检查是否只是下划线字符
                    String trimmed = cellText.trim();
                    if (isOnlyUnderscores(trimmed)) {
                        rowText.append(trimmed).append(" ");
                    } else {
                        rowText.append(trimmed).append(" ");
                        hasRealText = true;
                    }
                }
            }
        }
        
        String result = rowText.toString().trim();
        
        // 如果这行没有真正的文字
        if (!hasRealText) {
            // 如果这行是空的或只有下划线字符，生成标准宽度的下划线
            if (result.isEmpty() || isOnlyUnderscores(result)) {
                return generateUnderline(tableWidth);
            }
        }
        
        return result;
    }
    
    // ==================== POI .docx 辅助方法 ====================
    
    /**
     * 从 XWPFParagraph 中提取文本，处理下划线
     */
    private String extractTextFromParagraph(XWPFParagraph paragraph) {
        StringBuilder paragraphText = new StringBuilder();
        boolean hasBlankUnderline = false;
        boolean hasRealText = false;
        
        for (XWPFRun run : paragraph.getRuns()) {
            String text = run.getText(0);
            
            // 检查是否有下划线
            UnderlinePatterns underline = run.getUnderline();
            boolean isUnderlined = underline != null && underline != UnderlinePatterns.NONE;
            
            if (isUnderlined) {
                if (text == null || text.trim().isEmpty()) {
                    // 空白下划线
                    hasBlankUnderline = true;
                } else {
                    // 处理带下划线的文本，将其中的数字转换为 _数字_ 格式
                    paragraphText.append(processUnderlinedText(text));
                    hasRealText = true;
                }
            } else {
                if (text != null && !text.isEmpty()) {
                    paragraphText.append(text);
                    if (!text.trim().isEmpty()) {
                        hasRealText = true;
                    }
                }
            }
        }
        
        // 如果这个段落只有空白下划线，没有真正的文字，输出下划线
        if (hasBlankUnderline && !hasRealText) {
            return generateUnderline(80);
        }
        
        return paragraphText.toString();
    }
    
    /**
     * 从表格段落提取文本（不处理空白下划线）
     */
    private String extractTextFromParagraphForTable(XWPFParagraph paragraph) {
        StringBuilder paragraphText = new StringBuilder();
        
        for (XWPFRun run : paragraph.getRuns()) {
            String text = run.getText(0);
            if (text == null) continue;
            
            // 检查是否有下划线
            UnderlinePatterns underline = run.getUnderline();
            boolean isUnderlined = underline != null && underline != UnderlinePatterns.NONE;
            
            if (isUnderlined) {
                // 如果是空白下划线，跳过（由调用者处理）
                if (text.trim().isEmpty()) {
                    continue;
                }
                // 处理带下划线的文本，将其中的数字转换为 _数字_ 格式
                paragraphText.append(processUnderlinedText(text));
            } else {
                paragraphText.append(text);
            }
        }
        
        return paragraphText.toString();
    }
    
    /**
     * 为Markdown格式提取表格行内容
     */
    private String extractTableRowContentForMd(XWPFTableRow row) {
        StringBuilder rowText = new StringBuilder();
        boolean hasRealText = false;
        
        for (XWPFTableCell cell : row.getTableCells()) {
            for (XWPFParagraph cellPara : cell.getParagraphs()) {
                String cellText = extractTextFromParagraphForTable(cellPara);
                
                if (!cellText.trim().isEmpty()) {
                    String trimmed = cellText.trim();
                    if (!isOnlyUnderscores(trimmed)) {
                        hasRealText = true;
                    }
                    rowText.append(trimmed).append(" ");
                }
            }
        }
        
        String result = rowText.toString().trim();
        
        // 如果这行没有真正的文字，生成下划线
        if (!hasRealText && (result.isEmpty() || isOnlyUnderscores(result))) {
            return generateUnderline(60);
        }
        
        return result;
    }
    
    // ==================== 通用工具方法 ====================
    
    /**
     * 处理带下划线的文本
     * 将数字（包括连续数字）转换为 _数字_ 格式
     */
    private String processUnderlinedText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        StringBuilder result = new StringBuilder();
        StringBuilder currentNumber = new StringBuilder();
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (Character.isDigit(c)) {
                // 收集连续的数字
                currentNumber.append(c);
            } else {
                // 如果之前有收集到数字，先输出
                if (currentNumber.length() > 0) {
                    result.append("_").append(currentNumber).append("_");
                    currentNumber.setLength(0);
                }
                // 输出当前非数字字符（带下划线的非数字保持原样）
                result.append(c);
            }
        }
        
        // 处理末尾的数字
        if (currentNumber.length() > 0) {
            result.append("_").append(currentNumber).append("_");
        }
        
        return result.toString();
    }
    
    /**
     * 生成指定宽度的下划线
     */
    private String generateUnderline(int width) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < width; i++) {
            sb.append("_");
        }
        return sb.toString();
    }
    
    /**
     * 检查字符串是否只包含下划线字符
     */
    private boolean isOnlyUnderscores(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (char c : text.toCharArray()) {
            if (c != '_' && c != ' ') {
                return false;
            }
        }
        return text.contains("_");
    }
    
    /**
     * 计算字符串的显示宽度（中文字符算2个宽度，英文算1个）
     */
    private int getDisplayWidth(String text) {
        if (text == null) return 0;
        int width = 0;
        for (char c : text.toCharArray()) {
            // 中文字符、全角字符等按2个宽度计算
            if (c >= '\u4e00' && c <= '\u9fff' ||   // CJK统一汉字
                c >= '\u3000' && c <= '\u303f' ||   // CJK标点符号
                c >= '\uff00' && c <= '\uffef') {   // 全角ASCII、全角标点
                width += 2;
            } else {
                width += 1;
            }
        }
        return width;
    }
    
    /**
     * 右侧补空格到指定显示宽度（考虑中文字符宽度）
     */
    private String padRightByDisplayWidth(String text, int targetWidth) {
        if (text == null) {
            text = "";
        }
        int currentWidth = getDisplayWidth(text);
        if (currentWidth >= targetWidth) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text);
        int spacesNeeded = targetWidth - currentWidth;
        for (int i = 0; i < spacesNeeded; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * 规范化表格单元格文本：移除换行/制表/不间断空格等，折叠多空格
     */
    private String normalizeCellText(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.replace('\u00A0', ' ')
                              .replace('\t', ' ')
                              .replace('\f', ' ')
                              .replace('\r', ' ')
                              .replace('\n', ' ');
        return cleaned.replaceAll(" {2,}", " ").trim();
    }

    /**
     * 段落内容规范化（用于写作表格逐行输出）
     */
    private String normalizeInlineText(String text) {
        return normalizeCellText(text);
    }

    /**
     * 将写作段落格式化为固定行宽，并在两侧补下划线
     */
    private String formatWritingLine(String content, int lineWidth) {
        int contentWidth = getDisplayWidth(content);
        int leftPad = 3;
        int rightPad = Math.max(3, lineWidth - contentWidth - leftPad);
        return generateUnderline(leftPad) + content + generateUnderline(rightPad);
    }
}
