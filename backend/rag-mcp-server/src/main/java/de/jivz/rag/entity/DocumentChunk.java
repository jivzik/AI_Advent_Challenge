package de.jivz.rag.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity для хранения чанка документа с эмбеддингом.
 *
 * Эмбеддинг хранится как vector(768) в pgvector.
 */
@Entity
@Table(name = "document_chunks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @Column(name = "document_name", length = 500)
    private String documentName;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    @Column(name = "chunk_text", columnDefinition = "TEXT", nullable = false)
    private String chunkText;

    /**
     * Эмбеддинг вектор (768 размерность).
     * Хранится как vector в pgvector, но Hibernate verwaltet diese Spalte nicht direkt.
     * Wird über native Queries in Repository gespeichert und gelesen.
     */
    @Transient
    private String embedding;

    /**
     * Полнотекстовый вектор (tsvector) для keyword search.
     * Это GENERATED ALWAYS AS ... STORED колонка, которую поддерживает PostgreSQL.
     * Автоматически создается из chunk_text с использованием русской морфологии.
     * Транзиент, так как Hibernate не должна управлять этой колонкой.
     */
    @Transient
    private String textVector;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private Map<String, Object> metadata;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}

