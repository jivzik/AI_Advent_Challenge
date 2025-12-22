package de.jivz.rag.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.rag.dto.DocumentDto;
import de.jivz.rag.dto.FinalRankingConfig;
import de.jivz.rag.dto.FinalSearchResultDto;
import de.jivz.rag.dto.MergedSearchResultDto;
import de.jivz.rag.dto.RerankingStrategyConfig;
import de.jivz.rag.dto.SearchResultDto;
import de.jivz.rag.entity.Document;
import de.jivz.rag.entity.DocumentChunk;
import de.jivz.rag.repository.DocumentChunkRepository;
import de.jivz.rag.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Главный сервис RAG - координирует загрузку, chunking, embedding и поиск.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RagService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentParserService parserService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final KeywordSearchService keywordSearchService;
    private final SearchResultMergingService mergingService;
    private final SearchResultRerankingService rerankingService;
    private final FinalSearchResultService finalSearchService;
    private final ObjectMapper objectMapper;

    /**
     * Загружает и обрабатывает документ.
     *
     * Pipeline:
     * 1. Сохраняем метаданные документа
     * 2. Извлекаем текст
     * 3. Разбиваем на чанки
     * 4. Генерируем эмбеддинги
     * 5. Сохраняем в pgvector
     */
    @Transactional
    public DocumentDto uploadDocument(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        log.info("📥 Uploading document: {}", fileName);

        // 1. Создаём запись о документе
        Document document = Document.builder()
                .fileName(fileName)
                .fileType(parserService.getFileType(fileName))
                .fileSize(file.getSize())
                .status(Document.DocumentStatus.PROCESSING)
                .build();
        document = documentRepository.save(document);
        log.info("📄 Created document record: id={}", document.getId());

        try {
            // 2. Извлекаем текст
            String text = parserService.extractText(file);
            log.info("📝 Extracted {} characters", text.length());

            document.setStatus(Document.DocumentStatus.CHUNKED);
            documentRepository.save(document);

            // 3. Разбиваем на чанки
            List<String> chunks = chunkingService.chunkText(text);
            log.info("🔪 Created {} chunks", chunks.size());

            document.setStatus(Document.DocumentStatus.EMBEDDING);
            documentRepository.save(document);

            // 4. Генерируем эмбеддинги (batch)
            List<float[]> embeddings = embeddingService.generateEmbeddings(chunks);
            log.info("🧠 Generated {} embeddings", embeddings.size());

            // 5. Сохраняем чанки с эмбеддингами
            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);
                float[] embedding = i < embeddings.size() ? embeddings.get(i) : null;

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("chunkSize", chunkText.length());
                metadata.put("position", i);
                metadata.put("totalChunks", chunks.size());

                saveChunkWithEmbedding(
                        document.getId(),
                        fileName,
                        i,
                        chunkText,
                        embedding,
                        metadata
                );
            }

            // 6. Обновляем статус документа
            document.setChunkCount(chunks.size());
            document.setStatus(Document.DocumentStatus.READY);
            documentRepository.save(document);

            log.info("✅ Document processed successfully: {} ({} chunks)", fileName, chunks.size());
            return DocumentDto.fromEntity(document);

        } catch (Exception e) {
            log.error("❌ Error processing document: {}", e.getMessage(), e);
            document.setStatus(Document.DocumentStatus.ERROR);
            documentRepository.save(document);
            throw new RuntimeException("Failed to process document: " + e.getMessage(), e);
        }
    }

    /**
     * Сохраняет чанк с эмбеддингом через native query.
     */
    private void saveChunkWithEmbedding(Long documentId, String documentName,
                                        int chunkIndex, String chunkText,
                                        float[] embedding, Map<String, Object> metadata) {
        try {
            String embeddingStr = embeddingService.embeddingToString(embedding);
            String metadataJson = objectMapper.writeValueAsString(metadata);

            chunkRepository.saveWithEmbedding(
                    documentId,
                    documentName,
                    chunkIndex,
                    chunkText,
                    embeddingStr,
                    metadataJson
            );
        } catch (JsonProcessingException e) {
            log.error("❌ Error serializing metadata: {}", e.getMessage());
            throw new RuntimeException("Failed to serialize chunk metadata", e);
        }
    }

    /**
     * Семантический поиск по документам.
     */
    public List<SearchResultDto> search(String query, int topK, double threshold, Long documentId) {
        log.info("🔍 Searching for: '{}' (topK={}, threshold={}, docId={})",
                query, topK, threshold, documentId);

        // 1. Генерируем эмбеддинг для запроса
        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        if (queryEmbedding == null) {
            log.error("❌ Failed to generate embedding for query");
            return List.of();
        }

        String embeddingStr = embeddingService.embeddingToString(queryEmbedding);

        // 2. Выполняем поиск в pgvector
        List<Object[]> results;
        if (documentId != null) {
            results = chunkRepository.findSimilarChunksInDocument(
                    embeddingStr, documentId, topK, threshold);
        } else {
            results = chunkRepository.findSimilarChunks(embeddingStr, topK, threshold);
        }

        // 3. Преобразуем результаты
        List<SearchResultDto> searchResults = new ArrayList<>();
        for (Object[] row : results) {
            SearchResultDto dto = mapToSearchResult(row);
            searchResults.add(dto);
        }

        log.info("✅ Found {} results", searchResults.size());
        return searchResults;
    }

    /**
     * Маппинг результата native query в DTO.
     */
    @SuppressWarnings("unchecked")
    private SearchResultDto mapToSearchResult(Object[] row) {
        // Порядок колонок из query:
        // id, document_id, document_name, chunk_index, chunk_text, metadata, created_at, similarity
        return SearchResultDto.builder()
                .chunkId(((Number) row[0]).longValue())
                .documentId(row[1] != null ? ((Number) row[1]).longValue() : null)
                .documentName((String) row[2])
                .chunkIndex(row[3] != null ? ((Number) row[3]).intValue() : null)
                .chunkText((String) row[4])
                .metadata(parseMetadata(row[5]))
                .createdAt(row[6] != null ? ((Timestamp) row[6]).toLocalDateTime() : null)
                .similarity(row[7] != null ? ((Number) row[7]).doubleValue() : null)
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMetadata(Object metadataObj) {
        if (metadataObj == null) return null;
        if (metadataObj instanceof Map) {
            return (Map<String, Object>) metadataObj;
        }
        if (metadataObj instanceof String) {
            try {
                return objectMapper.readValue((String) metadataObj, Map.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse metadata: {}", e.getMessage());
                return null;
            }
        }
        return null;
    }

    /**
     * Получить все документы.
     */
    public List<DocumentDto> getAllDocuments() {
        return documentRepository.findAll().stream()
                .map(DocumentDto::fromEntity)
                .toList();
    }

    /**
     * Получить документ по ID.
     */
    public DocumentDto getDocument(Long id) {
        return documentRepository.findById(id)
                .map(DocumentDto::fromEntity)
                .orElse(null);
    }

    /**
     * Удалить документ и все его чанки.
     */
    @Transactional
    public boolean deleteDocument(Long id) {
        if (documentRepository.existsById(id)) {
            documentRepository.deleteById(id); // Каскадно удалит чанки
            log.info("🗑️ Deleted document: id={}", id);
            return true;
        }
        return false;
    }

    /**
     * Удалить документ по имени.
     */
    @Transactional
    public boolean deleteDocumentByName(String fileName) {
        return documentRepository.findByFileName(fileName)
                .map(doc -> {
                    documentRepository.delete(doc);
                    log.info("🗑️ Deleted document: name={}", fileName);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Получить чанки документа.
     */
    public List<DocumentChunk> getDocumentChunks(Long documentId) {
        return chunkRepository.findByDocumentId(documentId);
    }

    /**
     * Полнотекстовый поиск по ключевым словам.
     *
     * Использует встроенный FTS PostgreSQL для быстрого поиска.
     * Поддерживает русский язык с морфологической нормализацией.
     *
     * @param query текст для поиска
     * @param topK максимальное количество результатов
     * @return список результатов, отсортированных по релевантности
     */
    public List<SearchResultDto> keywordSearch(String query, int topK) {
        log.info("🔍 Keyword search: query='{}', topK={}", query, topK);
        return keywordSearchService.keywordSearch(query, topK);
    }

    /**
     * Полнотекстовый поиск в конкретном документе.
     *
     * @param query текст для поиска
     * @param documentId ID документа
     * @param topK максимальное количество результатов
     * @return список результатов из документа
     */
    public List<SearchResultDto> keywordSearchInDocument(String query, Long documentId, int topK) {
        log.info("🔍 Keyword search in document: query='{}', docId={}, topK={}",
                query, documentId, topK);
        return keywordSearchService.keywordSearchInDocument(query, documentId, topK);
    }

    /**
     * Расширенный поиск с поддержкой операторов.
     *
     * Поддерживает:
     * - & (AND): оба слова должны присутствовать
     * - | (OR): хотя бы одно слово
     * - ! (NOT): исключить слово
     *
     * @param query tsquery выражение
     * @param topK максимальное количество результатов
     * @return список результатов
     */
    public List<SearchResultDto> advancedKeywordSearch(String query, int topK) {
        log.info("🔍 Advanced keyword search: query='{}', topK={}", query, topK);
        return keywordSearchService.advancedKeywordSearch(query, topK);
    }

    /**
     * Поиск с расширенным ранжированием (ts_rank_cd).
     *
     * Более точное вычисление релевантности:
     * - Учитывает TF (частота слов в документе)
     * - Учитывает IDF (редкость слов в коллекции)
     * - Учитывает длину документа
     * - Учитывает близость слов друг к другу
     *
     * @param query текст для поиска
     * @param topK максимальное количество результатов
     * @return список результатов с улучшенным ранжированием
     */
    public List<SearchResultDto> advancedRankedKeywordSearch(String query, int topK) {
        log.info("🔍 Advanced ranked keyword search: query='{}', topK={}", query, topK);
        return keywordSearchService.advancedSearch(query, topK);
    }

    // ============ ЭТАП 4: Reranking (Переранжирование) ============

    /**
     * Переранжирует результаты поиска с использованием выбранной стратегии.
     *
     * ЭТАП 4: Reranking (переранжирование)
     *
     * Цель:
     * Вычислить финальный combined score для каждого чанка.
     *
     * Поддерживаемые стратегии:
     * 1. WEIGHTED_SUM (по умолчанию) - взвешенная сумма
     *    combined_score = semantic_weight × semantic_score + keyword_weight × keyword_score
     *
     * 2. MAX_SCORE - максимум из двух оценок
     *    combined_score = max(semantic_score, keyword_score)
     *
     * 3. RRF (Reciprocal Rank Fusion) - на основе позиций
     *    RRF_score = Σ(1 / (k + rank_i))
     *    Более robust, менее чувствительна к масштабам scores
     *
     * @param results результаты для переранжирования
     * @param config конфигурация стратегии
     * @return переранжированные результаты
     */
    public List<MergedSearchResultDto> rerankResults(
            List<MergedSearchResultDto> results,
            RerankingStrategyConfig config) {

        log.info("🔄 Reranking {} results using strategy: {}",
                results != null ? results.size() : 0,
                config.getStrategy());

        return rerankingService.rerank(results, config);
    }

    /**
     * Переранжирует результаты с использованием стратегии WEIGHTED_SUM.
     *
     * Рекомендация:
     * - Для общих вопросов: semantic_weight больше (0.7/0.3)
     * - Для точного поиска: keyword_weight больше (0.3/0.7)
     * - Balanced: 0.5/0.5
     *
     * @param results результаты для переранжирования
     * @param semanticWeight вес семантического поиска (0.0 - 1.0)
     * @param keywordWeight вес ключевого поиска (0.0 - 1.0)
     * @return переранжированные результаты
     */
    public List<MergedSearchResultDto> rerankWeightedSum(
            List<MergedSearchResultDto> results,
            double semanticWeight,
            double keywordWeight) {

        log.info("🔄 Reranking with WEIGHTED_SUM: semantic_weight={}, keyword_weight={}",
                semanticWeight, keywordWeight);

        return rerankingService.rerankWeightedSum(results, semanticWeight, keywordWeight);
    }

    /**
     * Переранжирует результаты с использованием стратегии WEIGHTED_SUM по умолчанию (0.6/0.4).
     *
     * @param results результаты для переранжирования
     * @return переранжированные результаты
     */
    public List<MergedSearchResultDto> rerankWeightedSum(List<MergedSearchResultDto> results) {
        return rerankingService.rerankDefault(results);
    }

    /**
     * Переранжирует результаты с использованием стратегии MAX_SCORE.
     *
     * Логика:
     * combined_score = max(semantic_score, keyword_score)
     *
     * Используется, когда нужно отдать приоритет лучшему результату из двух методов,
     * независимо от того, какой метод его дал.
     *
     * @param results результаты для переранжирования
     * @return переранжированные результаты
     */
    public List<MergedSearchResultDto> rerankMaxScore(List<MergedSearchResultDto> results) {
        log.info("🔄 Reranking with MAX_SCORE");
        return rerankingService.rerankMaxScore(results);
    }

    /**
     * Переранжирует результаты с использованием стратегии RRF (Reciprocal Rank Fusion).
     *
     * Концепция:
     * Не использует raw scores, а позиции в ранжированных списках.
     *
     * Формула для каждого чанка:
     * RRF_score = Σ(1 / (k + rank_i))
     *
     * где:
     * - k = 60 (константа, обычно 60)
     * - rank_i = позиция в списке i (semantic или keyword)
     *
     * RRF более robust к различным масштабам scores и менее чувствительна к экстремальным значениям.
     *
     * @param results результаты для переранжирования
     * @param k константа k для RRF
     * @return переранжированные результаты
     */
    public List<MergedSearchResultDto> rerankRRF(List<MergedSearchResultDto> results, int k) {
        log.info("🔄 Reranking with RRF: k={}", k);
        return rerankingService.rerankRRF(results, k);
    }

    /**
     * Переранжирает результаты с использованием стратегии RRF с k=60 по умолчанию.
     *
     * @param results результаты для переранжирования
     * @return переранжированные результаты
     */
    public List<MergedSearchResultDto> rerankRRF(List<MergedSearchResultDto> results) {
        return rerankingService.rerankRRF(results);
    }

    /**
     * Гибридный поиск - объединение семантического и ключевого поиска.
     *
     * ЭТАП 3: Объединение результатов (Merging)
     *
     * Алгоритм:
     * 1. Выполнить семантический поиск
     * 2. Выполнить ключевой поиск
     * 3. Объединить результаты в один список без дубликатов
     * 4. Вычислить комбинированную оценку для каждого чанка
     * 5. Отсортировать по комбинированной оценке
     *
     * @param query текст для поиска
     * @param topK максимальное количество результатов
     * @param threshold минимальная оценка для семантического поиска
     * @param semanticWeight вес семантического поиска (0.0-1.0)
     * @param keywordWeight вес ключевого поиска (0.0-1.0)
     * @return объединённые результаты
     */
    public List<MergedSearchResultDto> hybridSearch(
            String query,
            int topK,
            double threshold,
            double semanticWeight,
            double keywordWeight) {

        log.info("🔀 Hybrid search: query='{}', topK={}, threshold={}, weights=({}/{})",
                query, topK, threshold, semanticWeight, keywordWeight);

        // Шаг 1: Семантический поиск
        List<SearchResultDto> semanticResults = search(query, topK, threshold, null);
        log.info("  📊 Semantic search found {} results",
                semanticResults != null ? semanticResults.size() : 0);

        // Шаг 2: Ключевой поиск
        List<SearchResultDto> keywordResults = keywordSearch(query, topK);
        log.info("  📊 Keyword search found {} results",
                keywordResults != null ? keywordResults.size() : 0);

        // Шаг 3: Объединение результатов (ЭТАП 3: Merging)
        List<MergedSearchResultDto> mergedResults = mergingService.mergeResults(
                semanticResults, keywordResults, semanticWeight, keywordWeight, topK);
        log.info("  ✅ Merged {} results", mergedResults.size());

        // Шаг 4: Переранжирование с использованием стратегии WEIGHTED_SUM (ЭТАП 4: Reranking)
        List<MergedSearchResultDto> rerankedResults = rerankingService.rerankWeightedSum(
                mergedResults, semanticWeight, keywordWeight);

        log.info("✅ Hybrid search completed: {} final results", rerankedResults.size());
        return rerankedResults;
    }

    /**
     * Гибридный поиск с параметрами по умолчанию (semantic: 0.6, keyword: 0.4).
     *
     * @param query текст для поиска
     * @param topK максимальное количество результатов
     * @param threshold минимальная оценка для семантического поиска
     * @return объединённые результаты
     */
    public List<MergedSearchResultDto> hybridSearch(
            String query,
            int topK,
            double threshold) {
        return hybridSearch(query, topK, threshold, 0.6, 0.4);
    }

    /**
     * Гибридный поиск с параметрами по умолчанию (threshold: 0.5).
     *
     * @param query текст для поиска
     * @param topK максимальное количество результатов
     * @return объединённые результаты
     */
    public List<MergedSearchResultDto> hybridSearch(
            String query,
            int topK) {
        return hybridSearch(query, topK, 0.5);
    }

    /**
     * Гибридный поиск в конкретном документе.
     *
     * ЭТАП 3-4: Объединение и переранжирование результатов
     *
     * @param query текст для поиска
     * @param documentId ID документа
     * @param topK максимальное количество результатов
     * @param threshold минимальная оценка для семантического поиска
     * @param semanticWeight вес семантического поиска
     * @param keywordWeight вес ключевого поиска
     * @return объединённые и переранжированные результаты
     */
    public List<MergedSearchResultDto> hybridSearchInDocument(
            String query,
            Long documentId,
            int topK,
            double threshold,
            double semanticWeight,
            double keywordWeight) {

        log.info("🔀 Hybrid search in document: query='{}', docId={}, topK={}, threshold={}, weights=({}/{})",
                query, documentId, topK, threshold, semanticWeight, keywordWeight);

        // Шаг 1: Семантический поиск в документе
        List<SearchResultDto> semanticResults = search(query, topK, threshold, documentId);
        log.info("  📊 Semantic search in document found {} results",
                semanticResults != null ? semanticResults.size() : 0);

        // Шаг 2: Ключевой поиск в документе
        List<SearchResultDto> keywordResults = keywordSearchInDocument(query, documentId, topK);
        log.info("  📊 Keyword search in document found {} results",
                keywordResults != null ? keywordResults.size() : 0);

        // Шаг 3: Объединение результатов (ЭТАП 3: Merging)
        List<MergedSearchResultDto> mergedResults = mergingService.mergeResults(
                semanticResults, keywordResults, semanticWeight, keywordWeight, topK);
        log.info("  ✅ Merged {} results", mergedResults.size());

        // Шаг 4: Переранжирование (ЭТАП 4: Reranking)
        List<MergedSearchResultDto> rerankedResults = rerankingService.rerankWeightedSum(
                mergedResults, semanticWeight, keywordWeight);

        log.info("✅ Hybrid search in document completed: {} final results", rerankedResults.size());
        return rerankedResults;
    }

    /**
     * Гибридный поиск в документе с параметрами по умолчанию.
     *
     * @param query текст для поиска
     * @param documentId ID документа
     * @param topK максимальное количество результатов
     * @return объединённые результаты
     */
    public List<MergedSearchResultDto> hybridSearchInDocument(
            String query,
            Long documentId,
            int topK) {
        return hybridSearchInDocument(query, documentId, topK, 0.5, 0.6, 0.4);
    }

    // ============ ЭТАП 5: Финальная сортировка и фильтрация ============

    /**
     * Выполняет финальную сортировку и фильтрацию результатов.
     *
     * ЭТАП 5: Финальная сортировка и фильтрация
     *
     * Функции:
     * 1. Сортировка по combined_score (убывание)
     * 2. Фильтрация по минимальному порогу
     * 3. Удаление дубликатов (max N чанков с одного документа)
     * 4. Ограничение на топ-K результатов
     * 5. Добавление метаданных (ранг, процентиль, источник)
     *
     * @param mergedResults объединённые результаты (выход ЭТАП 4)
     * @param config конфигурация сортировки и фильтрации
     * @return финальные результаты с метаданными
     */
    public List<FinalSearchResultDto> finalizeResults(
            List<MergedSearchResultDto> mergedResults,
            FinalRankingConfig config) {

        log.info("🎯 ЭТАП 5: Finalizing {} results", mergedResults.size());
        return finalSearchService.finalizeResults(mergedResults, config);
    }

    /**
     * Финализирует результаты с параметрами по умолчанию.
     *
     * Default: threshold=0.3, topK=10
     *
     * @param mergedResults объединённые результаты
     * @return финализированные результаты
     */
    public List<FinalSearchResultDto> finalizeDefault(List<MergedSearchResultDto> mergedResults) {
        log.info("🎯 ЭТАП 5: Finalizing {} results (default config)", mergedResults.size());
        return finalSearchService.finalizeDefault(mergedResults);
    }

    /**
     * Финализирует результаты с пользовательским порогом и topK.
     *
     * @param mergedResults объединённые результаты
     * @param minScoreThreshold минимальный порог (0.0 - 1.0)
     * @param topK максимальное количество результатов
     * @return финализированные результаты
     */
    public List<FinalSearchResultDto> finalizeWithThreshold(
            List<MergedSearchResultDto> mergedResults,
            double minScoreThreshold,
            int topK) {

        log.info("🎯 ЭТАП 5: Finalizing with threshold={}, topK={}", minScoreThreshold, topK);
        return finalSearchService.finalizeWithThreshold(mergedResults, minScoreThreshold, topK);
    }

    /**
     * Финализирует результаты с разнообразностью (ограничение на макс чанков с документа).
     *
     * Полезно для получения результатов с разных документов.
     *
     * @param mergedResults объединённые результаты
     * @param topK максимальное количество результатов
     * @param maxChunksPerDocument максимум чанков с одного документа
     * @return финализированные результаты (diversified)
     */
    public List<FinalSearchResultDto> finalizeWithDiversification(
            List<MergedSearchResultDto> mergedResults,
            int topK,
            int maxChunksPerDocument) {

        log.info("🎯 ЭТАП 5: Finalizing with diversification: topK={}, maxPerDoc={}",
                topK, maxChunksPerDocument);
        return finalSearchService.finalizeWithDiversification(
                mergedResults, topK, maxChunksPerDocument);
    }

    /**
     * Финализирует результаты с удалением дубликатов.
     *
     * @param mergedResults объединённые результаты
     * @param topK максимальное количество результатов
     * @return финализированные результаты без дубликатов
     */
    public List<FinalSearchResultDto> finalizeWithDeduplication(
            List<MergedSearchResultDto> mergedResults,
            int topK) {

        log.info("🎯 ЭТАП 5: Finalizing with deduplication: topK={}", topK);
        return finalSearchService.finalizeWithDeduplication(mergedResults, topK);
    }

    // ============ Полный pipeline гибридного поиска с финализацией ============

    /**
     * Выполняет полный pipeline гибридного поиска со всеми этапами:
     * ЭТАП 1: Embedding (семантический поиск)
     * ЭТАП 2: Keyword Search (полнотекстовый поиск)
     * ЭТАП 3: Merging (объединение результатов)
     * ЭТАП 4: Reranking (переранжирование)
     * ЭТАП 5: Finalization (финальная сортировка и фильтрация)
     *
     * @param query текст для поиска
     * @param topK максимальное количество финальных результатов
     * @param threshold минимальный порог для финализации
     * @param maxChunksPerDocument максимум чанков с одного документа (для разнообразия)
     * @return финальные результаты (отсортированные, отфильтрованные, с метаданными)
     */
    public List<FinalSearchResultDto> hybridSearchFinal(
            String query,
            int topK,
            double threshold,
            int maxChunksPerDocument) {

        log.info("🔀 ПОЛНЫЙ PIPELINE: query='{}', topK={}, threshold={}, maxPerDoc={}",
                query, topK, threshold, maxChunksPerDocument);

        // ЭТАП 1-4: Гибридный поиск с переранжированием
        List<MergedSearchResultDto> merged = hybridSearch(query, topK * 2, 0.0, 0.6, 0.4);

        // ЭТАП 5: Финализация
        FinalRankingConfig config = FinalRankingConfig.builder()
                .minScoreThreshold(threshold)
                .topK(topK)
                .maxChunksPerDocument(maxChunksPerDocument)
                .build();

        return finalSearchService.finalizeResults(merged, config);
    }

    /**
     * Выполняет полный pipeline с параметрами по умолчанию.
     *
     * Default: threshold=0.3, maxChunksPerDocument=2
     *
     * @param query текст для поиска
     * @param topK максимальное количество финальных результатов
     * @return финальные результаты
     */
    public List<FinalSearchResultDto> hybridSearchFinal(String query, int topK) {
        return hybridSearchFinal(query, topK, 0.3, 2);
    }

    /**
     * Гибридный поиск, комбинирующий семантический и ключевой поиск.
     *
     * @param query Поисковый запрос
     * @param topK Количество результатов
     * @param threshold Порог сходства для семантического поиска
     * @param semanticWeight Вес семантического поиска (0.0-1.0), остаток идет на ключевой поиск
     * @return Список объединенных результатов
     */
    public List<SearchResultDto> hybridSearch(String query, int topK, double threshold, double semanticWeight) {
        log.info("🔄 Hybrid search: query='{}', topK={}, threshold={}, semanticWeight={}",
                query, topK, threshold, semanticWeight);

        // Клиппируем вес для корректности
        double semanticW = Math.max(0.0, Math.min(1.0, semanticWeight));
        double keywordW = 1.0 - semanticW;

        // 1. Выполняем семантический поиск
        List<SearchResultDto> semanticResults = new ArrayList<>();
        if (semanticW > 0.01) { // Только если вес достаточно большой
            log.debug("📊 Semantic search weight: {}%", Math.round(semanticW * 100));
            semanticResults = search(query, topK * 2, threshold, null); // Берем больше для объединения
        }

        // 2. Выполняем ключевой поиск
        List<SearchResultDto> keywordResults = new ArrayList<>();
        if (keywordW > 0.01) { // Только если вес достаточно большой
            log.debug("📊 Keyword search weight: {}%", Math.round(keywordW * 100));
            keywordResults = keywordSearch(query, topK * 2); // Берем больше для объединения
        }

        // 3. Объединяем результаты с взвешиванием
        Map<Long, SearchResultDto> mergedResults = new HashMap<>();

        // Добавляем семантические результаты с взвешиванием
        for (SearchResultDto result : semanticResults) {
            double weighted = (result.getSimilarity() != null ? result.getSimilarity() : 0.0) * semanticW;
            result.setSimilarity(weighted);
            mergedResults.put(result.getChunkId(), result);
        }

        // Добавляем/усредняем ключевые результаты
        for (SearchResultDto keywordResult : keywordResults) {
            Long chunkId = keywordResult.getChunkId();
            double weighted = (keywordResult.getSimilarity() != null ? keywordResult.getSimilarity() : 0.0) * keywordW;

            if (mergedResults.containsKey(chunkId)) {
                // Документ уже есть - усредняем оценки
                SearchResultDto existing = mergedResults.get(chunkId);
                double avgScore = (existing.getSimilarity() != null ? existing.getSimilarity() : 0.0) + weighted;
                existing.setSimilarity(avgScore);
            } else {
                // Новый документ - добавляем с ключевым весом
                keywordResult.setSimilarity(weighted);
                mergedResults.put(chunkId, keywordResult);
            }
        }

        // 4. Сортируем по финальной оценке и берем топ-K
        List<SearchResultDto> finalResults = mergedResults.values().stream()
                .sorted((a, b) -> Double.compare(
                        b.getSimilarity() != null ? b.getSimilarity() : 0.0,
                        a.getSimilarity() != null ? a.getSimilarity() : 0.0
                ))
                .limit(topK)
                .toList();

        log.info("✅ Hybrid search found {} results (semantic: {}, keyword: {})",
                finalResults.size(), semanticResults.size(), keywordResults.size());

        return finalResults;
    }
}
