package de.jivz.rag.service;

import de.jivz.rag.dto.DocumentDto;
import de.jivz.rag.repository.entity.Document;
import de.jivz.rag.repository.entity.DocumentChunk;
import de.jivz.rag.repository.DocumentChunkRepository;
import de.jivz.rag.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервис управления документами.
 *
 * Единственная ответственность (SRP):
 * CRUD операции с документами и их чанками.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentManagementService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;

    /**
     * Получить все документы.
     *
     * @return список всех документов
     */
    public List<DocumentDto> getAllDocuments() {
        return documentRepository.findAll().stream()
                .map(DocumentDto::fromEntity)
                .toList();
    }

    /**
     * Получить документ по ID.
     *
     * @param id идентификатор документа
     * @return Optional с документом или пустой Optional
     */
    public Optional<DocumentDto> getDocument(Long id) {
        return documentRepository.findById(id)
                .map(DocumentDto::fromEntity);
    }

    /**
     * Получить документ по имени файла.
     *
     * @param fileName имя файла
     * @return Optional с документом или пустой Optional
     */
    public Optional<DocumentDto> getDocumentByFileName(String fileName) {
        return documentRepository.findByFileName(fileName)
                .map(DocumentDto::fromEntity);
    }

    /**
     * Проверить существование документа.
     *
     * @param id идентификатор документа
     * @return true если документ существует
     */
    public boolean documentExists(Long id) {
        return documentRepository.existsById(id);
    }

    /**
     * Удалить документ по ID.
     * Каскадно удаляет все чанки документа.
     *
     * @param id идентификатор документа
     * @return true если документ был удалён
     */
    @Transactional
    public boolean deleteDocument(Long id) {
        if (!documentRepository.existsById(id)) {
            log.debug("Document not found for deletion: id={}", id);
            return false;
        }

        documentRepository.deleteById(id);
        log.info("Deleted document: id={}", id);
        return true;
    }

    /**
     * Удалить документ по имени файла.
     * Каскадно удаляет все чанки документа.
     *
     * @param fileName имя файла
     * @return true если документ был удалён
     */
    @Transactional
    public boolean deleteDocumentByFileName(String fileName) {
        return documentRepository.findByFileName(fileName)
                .map(doc -> {
                    documentRepository.delete(doc);
                    log.info("Deleted document: name={}", fileName);
                    return true;
                })
                .orElseGet(() -> {
                    log.debug("Document not found for deletion: name={}", fileName);
                    return false;
                });
    }

    /**
     * Получить чанки документа.
     *
     * @param documentId идентификатор документа
     * @return список чанков документа
     */
    public List<DocumentChunk> getDocumentChunks(Long documentId) {
        return chunkRepository.findByDocumentId(documentId);
    }

    /**
     * Получить количество чанков документа.
     *
     * @param documentId идентификатор документа
     * @return количество чанков
     */
    public long getChunkCount(Long documentId) {
        return chunkRepository.countByDocumentId(documentId);
    }

    /**
     * Получить все документы со статусом READY.
     *
     * @return список готовых документов
     */
    public List<DocumentDto> getReadyDocuments() {
        return documentRepository.findByStatus(Document.DocumentStatus.READY).stream()
                .map(DocumentDto::fromEntity)
                .toList();
    }

    /**
     * Получить все документы со статусом ERROR.
     *
     * @return список документов с ошибками
     */
    public List<DocumentDto> getFailedDocuments() {
        return documentRepository.findByStatus(Document.DocumentStatus.ERROR).stream()
                .map(DocumentDto::fromEntity)
                .toList();
    }
}