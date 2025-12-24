package de.jivz.rag.repository;

import de.jivz.rag.repository.entity.ChunkSearchResult;
import de.jivz.rag.repository.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Репозиторий для работы с чанками документов.
 * Поддерживает векторный поиск через pgvector и полнотекстовый поиск через PostgreSQL FTS.
 */
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

    // ==================== VECTOR SEARCH (Semantic) ====================

    /**
     * Поиск похожих чанков по косинусному сходству.
     * Использует projection ChunkSearchResult для правильного маппинга similarity.
     */
    @Query(value = """
        SELECT c.id as id,
            c.document_id as documentId,
            c.document_name as documentName,
            c.chunk_index as chunkIndex,
            c.chunk_text as chunkText,
            c.metadata as metadata,
            c.created_at as createdAt,
            1 - (c.embedding <=> cast(:queryEmbedding as vector)) as similarity
        FROM document_chunks c
        WHERE c.embedding IS NOT NULL
          AND 1 - (c.embedding <=> cast(:queryEmbedding as vector)) >= :threshold
        ORDER BY c.embedding <=> cast(:queryEmbedding as vector)
        LIMIT :topK
        """, nativeQuery = true)
    List<ChunkSearchResult> findSimilarChunksProjection(
            @Param("queryEmbedding") String queryEmbedding,
            @Param("topK") int topK,
            @Param("threshold") double threshold
    );

    /**
     * Поиск похожих чанков в конкретном документе.
     */
    @Query(value = """
        SELECT c.id as id,
            c.document_id as documentId,
            c.document_name as documentName,
            c.chunk_index as chunkIndex,
            c.chunk_text as chunkText,
            c.metadata as metadata,
            c.created_at as createdAt,
            1 - (c.embedding <=> cast(:queryEmbedding as vector)) as similarity
        FROM document_chunks c
        WHERE c.embedding IS NOT NULL
          AND c.document_id = :documentId
          AND 1 - (c.embedding <=> cast(:queryEmbedding as vector)) >= :threshold
        ORDER BY c.embedding <=> cast(:queryEmbedding as vector)
        LIMIT :topK
        """, nativeQuery = true)
    List<ChunkSearchResult> findSimilarChunksInDocumentProjection(
            @Param("queryEmbedding") String queryEmbedding,
            @Param("documentId") Long documentId,
            @Param("topK") int topK,
            @Param("threshold") double threshold
    );

    @Query("SELECT COUNT(c) FROM DocumentChunk c WHERE c.document.id = :documentId")
    long countByDocumentId(@Param("documentId") Long documentId);

    // ==================== FULL-TEXT SEARCH (Keyword) ====================

    /**
     * Полнотекстовый поиск по ключевым словам.
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
     */
    @Query(value = """
        SELECT c.id,
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
     * Расширенный поиск с поддержкой операторов (AND, OR, NOT).
     */
    @Query(value = """
        SELECT c.id,
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
     * Поиск с расширенным ранжированием (ts_rank_cd).
     */
    @Query(value = """
        SELECT c.id,
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

    // ==================== LEGACY METHODS (для обратной совместимости) ====================

    /**
     * @deprecated Используй findSimilarChunksProjection — этот метод не маппит similarity!
     */
    @Deprecated
    @Query(value = """
        SELECT c.id,
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
     * @deprecated Используй findSimilarChunksInDocumentProjection
     */
    @Deprecated
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
}