package de.jivz.rag.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.rag.dto.SearchResultDto;
import de.jivz.rag.repository.DocumentChunkRepository;
import de.jivz.rag.repository.entity.ChunkSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Сервис семантического поиска.
 * Выполняет векторный поиск по документам через pgvector.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SemanticSearchService {

    private final DocumentChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    /**
     * Семантический поиск по всем документам.
     */
    public List<SearchResultDto> search(String query, int topK, double threshold) {
        return search(query, topK, threshold, null);
    }

    /**
     * Семантический поиск по документам.
     */
    public List<SearchResultDto> search(String query, int topK, double threshold, Long documentId) {
        log.debug("Semantic search: query='{}', topK={}, threshold={}, documentId={}",
                query, topK, threshold, documentId);

        float[] queryEmbedding = generateQueryEmbedding(query);
        if (queryEmbedding == null) {
            return Collections.emptyList();
        }

        List<ChunkSearchResult> results = executeSearch(queryEmbedding, topK, threshold, documentId);
        List<SearchResultDto> searchResults = mapToSearchResults(results);

        log.debug("Semantic search found {} results", searchResults.size());
        return searchResults;
    }

    private float[] generateQueryEmbedding(String query) {
        float[] embedding = embeddingService.generateEmbedding(query);
        if (embedding == null) {
            log.warn("Failed to generate embedding for query");
        }
        return embedding;
    }

    private List<ChunkSearchResult> executeSearch(float[] embedding, int topK,
                                                  double threshold, Long documentId) {
        String embeddingStr = embeddingService.embeddingToString(embedding);

        if (documentId != null) {
            return chunkRepository.findSimilarChunksInDocumentProjection(
                    embeddingStr, documentId, topK, threshold);
        }
        return chunkRepository.findSimilarChunksProjection(embeddingStr, topK, threshold);
    }

    private List<SearchResultDto> mapToSearchResults(List<ChunkSearchResult> results) {
        return results.stream()
                .map(this::mapToSearchResult)
                .toList();
    }

    private SearchResultDto mapToSearchResult(ChunkSearchResult result) {
        return SearchResultDto.builder()
                .chunkId(result.getId())
                .documentId(result.getDocumentId())
                .documentName(result.getDocumentName())
                .chunkIndex(result.getChunkIndex())
                .chunkText(result.getChunkText())
                .metadata(parseMetadata(result.getMetadata()))
                .createdAt(result.getCreatedAt())
                .similarity(result.getSimilarity())
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(metadataJson, Map.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse metadata: {}", e.getMessage());
            return null;
        }
    }
}