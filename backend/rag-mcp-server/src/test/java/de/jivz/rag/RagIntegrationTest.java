package de.jivz.rag;

import de.jivz.rag.dto.DocumentDto;
import de.jivz.rag.dto.SearchResultDto;
import de.jivz.rag.entity.Document;
import de.jivz.rag.repository.DocumentChunkRepository;
import de.jivz.rag.repository.DocumentRepository;
import de.jivz.rag.service.RagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration тесты для RAG пайплайна.
 *
 * Тестирует: загрузка → индексация → поиск
 */
@SpringBootTest
@ActiveProfiles("test")
class RagIntegrationTest {

    @Autowired
    private RagService ragService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentChunkRepository chunkRepository;

    @BeforeEach
    void setUp() {
        // Очищаем базу перед каждым тестом
        chunkRepository.deleteAll();
        documentRepository.deleteAll();
    }

    @Test
    @DisplayName("Should get all documents")
    void shouldGetAllDocuments() {
        // Given - создаём документ напрямую
        Document doc = Document.builder()
                .fileName("test.txt")
                .fileType("TEXT")
                .fileSize(100L)
                .chunkCount(0)
                .status(Document.DocumentStatus.READY)
                .build();
        documentRepository.save(doc);

        // When
        List<DocumentDto> documents = ragService.getAllDocuments();

        // Then
        assertThat(documents).hasSize(1);
        assertThat(documents.get(0).getFileName()).isEqualTo("test.txt");
    }

    @Test
    @DisplayName("Should delete document by ID")
    void shouldDeleteDocumentById() {
        // Given
        Document doc = Document.builder()
                .fileName("to-delete.txt")
                .fileType("TEXT")
                .fileSize(50L)
                .status(Document.DocumentStatus.READY)
                .build();
        doc = documentRepository.save(doc);

        // When
        boolean deleted = ragService.deleteDocument(doc.getId());

        // Then
        assertThat(deleted).isTrue();
        assertThat(documentRepository.findById(doc.getId())).isEmpty();
    }

    @Test
    @DisplayName("Should delete document by name")
    void shouldDeleteDocumentByName() {
        // Given
        Document doc = Document.builder()
                .fileName("delete-by-name.txt")
                .fileType("TEXT")
                .fileSize(50L)
                .status(Document.DocumentStatus.READY)
                .build();
        documentRepository.save(doc);

        // When
        boolean deleted = ragService.deleteDocumentByName("delete-by-name.txt");

        // Then
        assertThat(deleted).isTrue();
        assertThat(documentRepository.findByFileName("delete-by-name.txt")).isEmpty();
    }

    @Test
    @DisplayName("Should return false when deleting non-existent document")
    void shouldReturnFalseWhenDeletingNonExistent() {
        boolean deleted = ragService.deleteDocument(999999L);
        assertThat(deleted).isFalse();
    }
}

