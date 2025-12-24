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
 * - FB2 (FictionBook 2.0 XML format)
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
            case "fb2" -> extractFromFb2(file.getInputStream()); // FB2 format
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
     * Извлечение текста из FB2 (FictionBook 2.0) файлов.
     * FB2 это XML формат, который содержит текст внутри различных элементов.
     * Мы используем SAX парсер для эффективного извлечения текста.
     */
    private String extractFromFb2(InputStream inputStream) throws IOException {
        byte[] fb2Bytes = inputStream.readAllBytes();

        try {
            // Используем SAX парсер для безопасную обработку XML
            javax.xml.parsers.SAXParserFactory factory = javax.xml.parsers.SAXParserFactory.newInstance();

            // Отключаем внешние DTD и сущности для безопасность
            try {
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            } catch (Exception e) {
                log.warn("Could not set all SAX parser features: {}", e.getMessage());
            }

            FB2TextExtractor extractor = new FB2TextExtractor();
            factory.newSAXParser().parse(
                    new java.io.ByteArrayInputStream(fb2Bytes),
                    extractor
            );

            String text = extractor.getText();
            log.info("✅ Extracted {} characters from FB2 file", text.length());
            return text;
        } catch (Exception e) {
            log.warn("⚠️ FB2 SAX parsing failed: {}, trying Tika fallback", e.getMessage());

            // Fallback: Nutze Tika als universellen Parser
            try {
                String text = tika.parseToString(new java.io.ByteArrayInputStream(fb2Bytes));
                log.info("✅ Extracted {} characters from FB2 via Tika fallback", text.length());
                return text;
            } catch (TikaException tikaEx) {
                log.error("❌ Both FB2 and Tika parsing failed");
                throw new IOException("Failed to parse FB2 file: " + e.getMessage(), tikaEx);
            }
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
            case "fb2" -> "FB2";
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
