package de.jivz.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Сервис для парсинга документов различных форматов.
 *
 * Поддерживаемые форматы:
 * - PDF (Apache PDFBox)
 * - EPUB (Apache Tika)
 * - TXT, MD (прямое чтение)
 * - DOCX, DOC (Apache Tika)
 * - Код (.java, .py, .js, etc.)
 */
@Service
@Slf4j
public class DocumentParserService {

    private final Tika tika;

    public DocumentParserService() {
        this.tika = new Tika();
    }

    /**
     * Извлекает текст из загруженного файла.
     */
    public String extractText(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        String contentType = file.getContentType();

        log.info("📄 Parsing file: {} (type: {})", fileName, contentType);

        if (fileName == null) {
            throw new IllegalArgumentException("File name is null");
        }

        String extension = getFileExtension(fileName).toLowerCase();

        return switch (extension) {
            case "pdf" -> extractFromPdf(file.getInputStream());
            case "epub" -> extractWithTika(file.getInputStream()); // Используем Tika для EPUB
            case "txt", "md", "markdown" -> extractFromText(file.getInputStream());
            case "java", "py", "js", "ts", "cpp", "c", "h", "go", "rs", "kt", "scala"
                    -> extractFromText(file.getInputStream());
            case "docx", "doc", "odt", "rtf" -> extractWithTika(file.getInputStream());
            case "html", "htm", "xml" -> extractWithTika(file.getInputStream());
            default -> extractWithTika(file.getInputStream()); // Fallback to Tika
        };
    }

    /**
     * Извлечение текста из PDF через PDFBox.
     */
    private String extractFromPdf(InputStream inputStream) throws IOException {
        byte[] pdfBytes = inputStream.readAllBytes();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);

            log.info("✅ Extracted {} characters from PDF ({} pages)",
                    text.length(), document.getNumberOfPages());
            return text;
        }
    }

    /**
     * Прямое чтение текстовых файлов.
     */
    private String extractFromText(InputStream inputStream) throws IOException {
        String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        log.info("✅ Read {} characters from text file", text.length());
        return text;
    }

    /**
     * Извлечение через Apache Tika (универсальный парсер).
     */
    private String extractWithTika(InputStream inputStream) throws IOException {
        try {
            String text = tika.parseToString(inputStream);
            log.info("✅ Extracted {} characters via Tika", text.length());
            return text;
        } catch (TikaException e) {
            log.error("❌ Tika parsing error: {}", e.getMessage());
            throw new IOException("Failed to parse document with Tika", e);
        }
    }

    /**
     * Определяет тип файла.
     */
    public String getFileType(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        return switch (extension) {
            case "pdf" -> "PDF";
            case "epub" -> "EPUB";
            case "txt" -> "TEXT";
            case "md", "markdown" -> "MARKDOWN";
            case "docx" -> "DOCX";
            case "doc" -> "DOC";
            case "java", "py", "js", "ts", "cpp", "c", "go", "rs", "kt" -> "CODE";
            case "html", "htm" -> "HTML";
            case "xml" -> "XML";
            default -> "UNKNOWN";
        };
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }
}
