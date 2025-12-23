package de.jivz.rag.repository;

import de.jivz.rag.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentId(Long documentId);

    List<DocumentChunk> findByDocumentName(String documentName);

    @Modifying
    @Transactional
    @Query("DELETE FROM DocumentChunk c WHERE c.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") Long documentId);

    @Modifying
    @Transactional
    @Query("DELETE FROM DocumentChunk c WHERE c.documentName = :documentName")
    void deleteByDocumentName(@Param("documentName") String documentName);

    /**
     * Сохраняет чанк с эмбеддингом через native query.
     * Использует pgvector для хранения вектора.
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO document_chunks 
        (document_id, document_name, chunk_index, chunk_text, embedding, metadata, created_at)
        VALUES 
        (:documentId, :documentName, :chunkIndex, :chunkText, 
         cast(:embedding as vector), 
         cast(:metadata as jsonb), 
         NOW())
        """, nativeQuery = true)
    void saveWithEmbedding(
            @Param("documentId") Long documentId,
            @Param("documentName") String documentName,
            @Param("chunkIndex") Integer chunkIndex,
            @Param("chunkText") String chunkText,
            @Param("embedding") String embedding,
            @Param("metadata") String metadata
    );

    /**
     * Поиск похожих чанков по косинусному сходству.
     *
     * @param queryEmbedding эмбеддинг запроса в формате "[0.1, 0.2, ...]"
     * @param topK количество результатов
     * @param threshold минимальный порог сходства (0.0 - 1.0)
     * @return список чанков, отсортированных по релевантности
     */
    @Query(value = """
        SELECT 
            c.id,
            c.document_id,
            c.document_name,
            c.chunk_index,
            c.chunk_text,
            c.metadata,
            c.created_at,
            1 - (c.embedding <=> cast(:queryEmbedding as vector)) as similarity
        FROM document_chunks c
        WHERE c.embedding IS NOT NULL
          AND 1 - (c.embedding <=> cast(:queryEmbedding as vector)) >= :threshold
        ORDER BY c.embedding <=> cast(:queryEmbedding as vector)
        LIMIT :topK
        """, nativeQuery = true)
    List<DocumentChunk> findSimilarChunks(
            @Param("queryEmbedding") String queryEmbedding,
            @Param("topK") int topK,
            @Param("threshold") double threshold
    );

    /**
     * Поиск похожих чанков в конкретном документе.
     */
    @Query(value = """
        SELECT 
            c.id,
            c.document_id,
            c.document_name,
            c.chunk_index,
            c.chunk_text,
            c.metadata,
            c.created_at,
            1 - (c.embedding <=> cast(:queryEmbedding as vector)) as similarity
        FROM document_chunks c
        WHERE c.embedding IS NOT NULL
          AND c.document_id = :documentId
          AND 1 - (c.embedding <=> cast(:queryEmbedding as vector)) >= :threshold
        ORDER BY c.embedding <=> cast(:queryEmbedding as vector)
        LIMIT :topK
        """, nativeQuery = true)
    List<DocumentChunk> findSimilarChunksInDocument(
            @Param("queryEmbedding") String queryEmbedding,
            @Param("documentId") Long documentId,
            @Param("topK") int topK,
            @Param("threshold") double threshold
    );

    @Query("SELECT COUNT(c) FROM DocumentChunk c WHERE c.document.id = :documentId")
    long countByDocumentId(@Param("documentId") Long documentId);

    /**
     * Полнотекстовый поиск по ключевым словам.
     *
     * Использует PostgreSQL FTS (Full-Text Search) для быстрого поиска.
     * Поддерживает русский язык с морфологической нормализацией.
     *
     * @param query текст для поиска (будет конвертирован в tsquery)
     * @param topK количество результатов
     * @return список чанков, отсортированных по релевантности (ts_rank)
     */
    @Query(value = """
        SELECT 
            c.id,
            c.document_id,
            c.document_name,
            c.chunk_index,
            c.chunk_text,
            c.metadata,
            c.created_at,
            ts_rank(c.text_vector, query) as relevance_score
        FROM document_chunks c,
             plainto_tsquery('simple', :query) query
        WHERE c.text_vector @@ query
        ORDER BY ts_rank(c.text_vector, query) DESC,
                 c.created_at DESC
        LIMIT :topK
        """, nativeQuery = true)
    List<Object[]> searchByKeywords(
            @Param("query") String query,
            @Param("topK") int topK
    );

    /**
     * Полнотекстовый поиск в пределах одного документа.
     *
     * @param query текст для поиска
     * @param documentId ID документа
     * @param topK количество результатов
     * @return список чанков, отсортированных по релевантности
     */
    @Query(value = """
        SELECT 
            c.id,
            c.document_id,
            c.document_name,
            c.chunk_index,
            c.chunk_text,
            c.metadata,
            c.created_at,
            ts_rank(c.text_vector, query) as relevance_score
        FROM document_chunks c,
             plainto_tsquery('simple', :query) query
        WHERE c.text_vector @@ query
          AND c.document_id = :documentId
        ORDER BY ts_rank(c.text_vector, query) DESC,
                 c.created_at DESC
        LIMIT :topK
        """, nativeQuery = true)
    List<Object[]> searchByKeywordsInDocument(
            @Param("query") String query,
            @Param("documentId") Long documentId,
            @Param("topK") int topK
    );

    /**
     * Полнотекстовый поиск с поддержкой различных операторов.
     *
     * Поддерживает:
     * - Точное совпадение слова: "word"
     * - OR операцию: "word1 | word2"
     * - AND операцию: "word1 & word2"
     * - NOT операцию: "word1 & !word2"
     *
     * @param query текст с операторами tsquery
     * @param topK количество результатов
     * @return список чанков с релевантностью
     */
    @Query(value = """
        SELECT 
            c.id,
            c.document_id,
            c.document_name,
            c.chunk_index,
            c.chunk_text,
            c.metadata,
            c.created_at,
            ts_rank(c.text_vector, query) as relevance_score
        FROM document_chunks c,
             to_tsquery('simple', :query) query
        WHERE c.text_vector @@ query
        ORDER BY ts_rank(c.text_vector, query) DESC,
                 c.created_at DESC
        LIMIT :topK
        """, nativeQuery = true)
    List<Object[]> searchByAdvancedQuery(
            @Param("query") String query,
            @Param("topK") int topK
    );

    /**
     * Поиск с расширенными возможностями и ранжированием.
     *
     * ts_rank_cd использует больше параметров для расчета релевантности:
     * - Частота слов в документе (TF)
     * - Редкость слов в коллекции (IDF)
     * - Длина документа
     * - Близость слов друг к другу
     *
     * @param query текст для поиска
     * @param topK количество результатов
     * @return список результатов с покрытой релевантностью
     */
    @Query(value = """
        SELECT 
            c.id,
            c.document_id,
            c.document_name,
            c.chunk_index,
            c.chunk_text,
            c.metadata,
            c.created_at,
            ts_rank_cd(c.text_vector, query, 32) as relevance_score
        FROM document_chunks c,
             plainto_tsquery('russian', :query) query
        WHERE c.text_vector @@ query
        ORDER BY relevance_score DESC,
                 c.created_at DESC
        LIMIT :topK
        """, nativeQuery = true)
    List<Object[]> searchByKeywordsAdvanced(
            @Param("query") String query,
            @Param("topK") int topK
    );
}

