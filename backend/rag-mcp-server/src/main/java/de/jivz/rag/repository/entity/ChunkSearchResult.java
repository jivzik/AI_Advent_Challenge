package de.jivz.rag.repository.entity;

import java.time.LocalDateTime;

/**
 * Projection для результатов similarity search.
 *
 * Используется для маппинга native query с вычисляемым полем similarity.
 * Spring Data автоматически создаёт реализацию этого интерфейса.
 */
public interface ChunkSearchResult {

    Long getId();

    Long getDocumentId();

    String getDocumentName();

    Integer getChunkIndex();

    String getChunkText();

    String getMetadata();

    LocalDateTime getCreatedAt();

    /**
     * Вычисляемое поле similarity из SQL запроса.
     */
    Double getSimilarity();
}