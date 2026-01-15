package de.jivz.rag.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.rag.dto.DocumentDto;
import de.jivz.rag.repository.entity.Document;
import de.jivz.rag.repository.entity.Document.DocumentStatus;
import de.jivz.rag.exception.DocumentProcessingException;
import de.jivz.rag.repository.DocumentChunkRepository;
import de.jivz.rag.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map;

/**
 * Сервис загрузки и обработки документов.
 *
 * Единственная ответственность (SRP):
 * Координация pipeline загрузки документа.
 *
 * Pipeline:
 * 1. Сохранение метаданных документа
 * 2. Извлечение текста (делегируется DocumentParserService)
 * 3. Разбиение на чанки (делегируется ChunkingService)
 * 4. Генерация эмбеддингов (делегируется EmbeddingService)
 * 5. Сохранение чанков в pgvector
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentUploadService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentParserService parserService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    /**
     * Загружает и обрабатывает документ.
     *
     * @param file загружаемый файл
     * @return DTO загруженного документа
     * @throws IOException при ошибке чтения файла
     */
    @Transactional
    public DocumentDto uploadDocument(MultipartFile file) throws IOException {
        return uploadDocument(file, null);
    }

    /**
     * Загружает и обрабатывает документ с метаданными.
     *
     * @param file загружаемый файл
     * @param metadataJson метаданные в формате JSON-строки (может быть null)
     * @return DTO загруженного документа
     * @throws IOException при ошибке чтения файла
     */
    @Transactional
    public DocumentDto uploadDocument(MultipartFile file, String metadataJson) throws IOException {
        String fileName = file.getOriginalFilename();
        log.info("Uploading document: {} with metadata: {}", fileName, metadataJson);

        Map<String, Object> metadata = parseMetadata(metadataJson);
        Document document = createDocumentRecord(file, metadata);

        try {
            processDocument(file, document);
            return DocumentDto.fromEntity(document);
        } catch (Exception e) {
            handleProcessingError(document, e);
            throw new DocumentProcessingException("Failed to process document: " + fileName, e);
        }
    }

    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.trim().isEmpty()) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse metadata JSON: {}, using empty map", metadataJson);
            return new HashMap<>();
        }
    }

    private Document createDocumentRecord(MultipartFile file, Map<String, Object> metadata) {
        Document document = Document.builder()
                .fileName(file.getOriginalFilename())
                .fileType(parserService.getFileType(file.getOriginalFilename()))
                .fileSize(file.getSize())
                .metadata(metadata)
                .status(DocumentStatus.PROCESSING)
                .build();

        document = documentRepository.save(document);
        log.debug("Created document record: id={}, metadata={}", document.getId(), metadata);
        return document;
    }

    private void processDocument(MultipartFile file, Document document) throws IOException {
        // Шаг 1: Извлечение текста
        String text = extractText(file, document);

        // Шаг 2: Разбиение на чанки
        List<String> chunks = createChunks(text, document);

        // Шаг 3: Генерация эмбеддингов
        List<float[]> embeddings = generateEmbeddings(chunks, document);

        // Шаг 4: Сохранение чанков
        saveChunks(document, chunks, embeddings);

        // Шаг 5: Финализация
        finalizeDocument(document, chunks.size());
    }

    private String extractText(MultipartFile file, Document document) throws IOException {
        String text = parserService.extractText(file);
        log.debug("Extracted {} characters from document id={}", text.length(), document.getId());

        updateStatus(document, DocumentStatus.CHUNKED);
        return text;
    }

    private List<String> createChunks(String text, Document document) {
        List<String> chunks = chunkingService.chunkText(text);
        log.debug("Created {} chunks for document id={}", chunks.size(), document.getId());

        updateStatus(document, DocumentStatus.EMBEDDING);
        return chunks;
    }

    private List<float[]> generateEmbeddings(List<String> chunks, Document document) {
        List<float[]> embeddings = embeddingService.generateEmbeddings(chunks);
        log.debug("Generated {} embeddings for document id={}", embeddings.size(), document.getId());
        return embeddings;
    }

    private void saveChunks(Document document, List<String> chunks, List<float[]> embeddings) {
        for (int i = 0; i < chunks.size(); i++) {
            ChunkData chunkData = ChunkData.builder()
                    .documentId(document.getId())
                    .documentName(document.getFileName())
                    .chunkIndex(i)
                    .chunkText(chunks.get(i))
                    .embedding(i < embeddings.size() ? embeddings.get(i) : null)
                    .totalChunks(chunks.size())
                    .build();

            saveChunk(chunkData);
        }
    }

    private void saveChunk(ChunkData chunkData) {
        try {
            String embeddingStr = embeddingService.embeddingToString(chunkData.embedding());
            String metadataJson = objectMapper.writeValueAsString(chunkData.toMetadata());

            chunkRepository.saveWithEmbedding(
                    chunkData.documentId(),
                    chunkData.documentName(),
                    chunkData.chunkIndex(),
                    chunkData.chunkText(),
                    embeddingStr,
                    metadataJson
            );
        } catch (JsonProcessingException e) {
            throw new DocumentProcessingException("Failed to serialize chunk metadata", e);
        }
    }

    private void finalizeDocument(Document document, int chunkCount) {
        document.setChunkCount(chunkCount);
        document.setStatus(DocumentStatus.READY);
        documentRepository.save(document);

        log.info("Document processed successfully: {} ({} chunks)",
                document.getFileName(), chunkCount);
    }

    private void updateStatus(Document document, DocumentStatus status) {
        document.setStatus(status);
        documentRepository.save(document);
    }

    private void handleProcessingError(Document document, Exception e) {
        log.error("Error processing document id={}: {}", document.getId(), e.getMessage(), e);
        document.setStatus(DocumentStatus.ERROR);
        documentRepository.save(document);
    }

    /**
     * Внутренний record для передачи данных чанка.
     * Инкапсулирует данные, необходимые для сохранения чанка.
     */
    @lombok.Builder
    private record ChunkData(
            Long documentId,
            String documentName,
            int chunkIndex,
            String chunkText,
            float[] embedding,
            int totalChunks
    ) {
        Map<String, Object> toMetadata() {
            return Map.of(
                    "chunkSize", chunkText.length(),
                    "position", chunkIndex,
                    "totalChunks", totalChunks
            );
        }
    }
}