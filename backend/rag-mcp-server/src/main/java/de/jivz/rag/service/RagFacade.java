package de.jivz.rag.service;

import de.jivz.rag.dto.DocumentDto;
import de.jivz.rag.dto.FinalSearchResultDto;
import de.jivz.rag.dto.MergedSearchResultDto;
import de.jivz.rag.dto.SearchResultDto;
import de.jivz.rag.repository.entity.DocumentChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Фасад RAG системы.
 *
 * Паттерн Facade:
 * Предоставляет упрощённый интерфейс к подсистеме RAG.
 * Скрывает сложность внутренней структуры от клиентов.
 *
 * Преимущества:
 * - Единая точка входа для контроллеров
 * - Обратная совместимость при рефакторинге
 * - Простой API для типичных сценариев
 *
 * Клиенты могут использовать фасад или обращаться
 * к специализированным сервисам напрямую для сложных сценариев.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RagFacade {

    private final DocumentUploadService uploadService;
    private final DocumentManagementService documentService;
    private final SemanticSearchService semanticSearchService;
    private final HybridSearchService hybridSearchService;
    private final HybridSearchPipelineService pipelineService;
    private final KeywordSearchService keywordSearchService;

    // ==================== Документы ====================

    /**
     * Загрузить документ.
     */
    public DocumentDto uploadDocument(MultipartFile file) throws IOException {
        return uploadService.uploadDocument(file);
    }

    /**
     * Загрузить документ с метаданными.
     * @param file загружаемый файл
     * @param metadata метаданные в формате JSON-строки (может быть null)
     */
    public DocumentDto uploadDocument(MultipartFile file, String metadata) throws IOException {
        return uploadService.uploadDocument(file, metadata);
    }

    /**
     * Получить все документы.
     */
    public List<DocumentDto> getAllDocuments() {
        return documentService.getAllDocuments();
    }

    /**
     * Получить документ по ID.
     */
    public Optional<DocumentDto> getDocument(Long id) {
        return documentService.getDocument(id);
    }

    /**
     * Обновить метаданные документа.
     */
    public DocumentDto updateDocumentMetadata(Long id, Map<String, Object> metadata) {
        return documentService.updateDocumentMetadata(id, metadata);
    }

    /**
     * Удалить документ.
     */
    public boolean deleteDocument(Long id) {
        return documentService.deleteDocument(id);
    }

    /**
     * Удалить документ по имени.
     */
    public boolean deleteDocumentByName(String fileName) {
        return documentService.deleteDocumentByFileName(fileName);
    }

    /**
     * Получить чанки документа.
     */
    public List<DocumentChunk> getDocumentChunks(Long documentId) {
        return documentService.getDocumentChunks(documentId);
    }

    // ==================== Поиск ====================

    /**
     * Семантический поиск.
     */
    public List<SearchResultDto> search(String query, int topK, double threshold, Long documentId) {
        return semanticSearchService.search(query, topK, threshold, documentId);
    }

    /**
     * Семантический поиск по всем документам.
     */
    public List<SearchResultDto> search(String query, int topK, double threshold) {
        return semanticSearchService.search(query, topK, threshold);
    }

    /**
     * Ключевой поиск.
     */
    public List<SearchResultDto> keywordSearch(String query, int topK) {
        return keywordSearchService.keywordSearch(query, topK);
    }

    /**
     * Ключевой поиск в документе.
     */
    public List<SearchResultDto> keywordSearchInDocument(String query, Long documentId, int topK) {
        return keywordSearchService.keywordSearchInDocument(query, documentId, topK);
    }

    /**
     * Расширенный ключевой поиск.
     */
    public List<SearchResultDto> advancedKeywordSearch(String query, int topK) {
        return keywordSearchService.advancedKeywordSearch(query, topK);
    }

    /**
     * Ключевой поиск с расширенным ранжированием (ts_rank_cd).
     */
    public List<SearchResultDto> advancedRankedKeywordSearch(String query, int topK) {
        return keywordSearchService.advancedSearch(query, topK);
    }

    // ==================== Гибридный поиск ====================

    /**
     * Гибридный поиск (этапы 1-4).
     */
    public List<MergedSearchResultDto> hybridSearch(String query, int topK,
                                                    double threshold,
                                                    double semanticWeight,
                                                    double keywordWeight) {
        return hybridSearchService.search(query, topK, threshold, semanticWeight, keywordWeight);
    }

    /**
     * Гибридный поиск с одним весом — возвращает SearchResultDto для совместимости.
     * (keywordWeight = 1 - semanticWeight)
     */
    public List<SearchResultDto> hybridSearch(String query, int topK,
                                              double threshold, double semanticWeight) {
        double keywordWeight = 1.0 - semanticWeight;
        return hybridSearchService.search(query, topK, threshold, semanticWeight, keywordWeight)
                .stream()
                .map(this::toSearchResultDto)
                .toList();
    }

    /**
     * Гибридный поиск с весами по умолчанию — возвращает SearchResultDto.
     */
    public List<SearchResultDto> hybridSearch(String query, int topK, double threshold) {
        return hybridSearchService.search(query, topK, threshold)
                .stream()
                .map(this::toSearchResultDto)
                .toList();
    }

    /**
     * Гибридный поиск — возвращает MergedSearchResultDto (полные данные).
     */
    public List<MergedSearchResultDto> hybridSearchMerged(String query, int topK,
                                                          double threshold,
                                                          double semanticWeight,
                                                          double keywordWeight) {
        return hybridSearchService.search(query, topK, threshold, semanticWeight, keywordWeight);
    }

    /**
     * Гибридный поиск с параметрами по умолчанию.
     */
    public List<MergedSearchResultDto> hybridSearch(String query, int topK) {
        return hybridSearchService.search(query, topK);
    }

    /**
     * Гибридный поиск в документе.
     */
    public List<MergedSearchResultDto> hybridSearchInDocument(String query, Long documentId,
                                                              int topK, double threshold,
                                                              double semanticWeight,
                                                              double keywordWeight) {
        return hybridSearchService.searchInDocument(
                query, documentId, topK, threshold, semanticWeight, keywordWeight);
    }

    /**
     * Гибридный поиск в документе с параметрами по умолчанию.
     */
    public List<MergedSearchResultDto> hybridSearchInDocument(String query, Long documentId, int topK) {
        return hybridSearchService.searchInDocument(query, documentId, topK);
    }

    // ==================== Полный Pipeline ====================

    /**
     * Полный pipeline поиска (этапы 1-5).
     */
    public List<FinalSearchResultDto> hybridSearchFinal(String query, int topK,
                                                        double threshold,
                                                        int maxChunksPerDocument) {
        return pipelineService.search(query, topK, threshold, maxChunksPerDocument);
    }

    /**
     * Полный pipeline с параметрами по умолчанию.
     */
    public List<FinalSearchResultDto> hybridSearchFinal(String query, int topK) {
        return pipelineService.search(query, topK);
    }

    // ==================== Конвертация ====================

    /**
     * Конвертирует MergedSearchResultDto в SearchResultDto.
     * Используется для обратной совместимости с контроллерами.
     */
    private SearchResultDto toSearchResultDto(MergedSearchResultDto merged) {
        return SearchResultDto.builder()
                .chunkId(merged.getChunkId())
                .documentId(merged.getDocumentId())
                .documentName(merged.getDocumentName())
                .chunkIndex(merged.getChunkIndex())
                .chunkText(merged.getChunkText())
                .metadata(merged.getMetadata())
                .createdAt(merged.getCreatedAt())
                .similarity(merged.getMergedScore()) // используем combined score как similarity
                .build();
    }
}