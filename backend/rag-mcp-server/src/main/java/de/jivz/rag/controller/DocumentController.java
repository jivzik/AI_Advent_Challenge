package de.jivz.rag.controller;

import de.jivz.rag.dto.DocumentDto;
import de.jivz.rag.dto.SearchRequest;
import de.jivz.rag.dto.SearchResultDto;
import de.jivz.rag.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * REST контроллер для работы с документами.
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DocumentController {

    private final RagService ragService;

    /**
     * Загрузка документа.
     *
     * POST /api/documents/upload
     * Content-Type: multipart/form-data
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {
        log.info("📥 Received upload request: {} ({} bytes)",
                file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "File is empty"));
        }

        try {
            DocumentDto doc = ragService.uploadDocument(file);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "document", doc,
                    "message", "Document uploaded and processed successfully"
            ));
        } catch (Exception e) {
            log.error("❌ Upload failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage()
                    ));
        }
    }

    /**
     * Получить список всех документов.
     *
     * GET /api/documents
     */
    @GetMapping
    public ResponseEntity<List<DocumentDto>> getAllDocuments() {
        return ResponseEntity.ok(ragService.getAllDocuments());
    }

    /**
     * Получить документ по ID.
     *
     * GET /api/documents/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDocument(@PathVariable Long id) {
        DocumentDto doc = ragService.getDocument(id);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(doc);
    }

    /**
     * Удалить документ.
     *
     * DELETE /api/documents/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        boolean deleted = ragService.deleteDocument(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Document deleted successfully"
            ));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Семантический поиск по документам.
     *
     * POST /api/documents/search
     */
    @PostMapping("/search")
    public ResponseEntity<List<SearchResultDto>> search(@RequestBody SearchRequest request) {
        log.info("🔍 Search request: query='{}', topK={}, threshold={}",
                request.getQuery(), request.getTopK(), request.getThreshold());

        if (request.getQuery() == null || request.getQuery().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        List<SearchResultDto> results = ragService.search(
                request.getQuery(),
                request.getTopK() != null ? request.getTopK() : 5,
                request.getThreshold() != null ? request.getThreshold() : 0.5,
                request.getDocumentId()
        );

        return ResponseEntity.ok(results);
    }

    /**
     * Быстрый поиск через GET параметры.
     *
     * GET /api/documents/search?query=...&topK=5&threshold=0.5
     */
    @GetMapping("/search")
    public ResponseEntity<List<SearchResultDto>> searchGet(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK,
            @RequestParam(defaultValue = "0.5") double threshold,
            @RequestParam(required = false) Long documentId) {

        List<SearchResultDto> results = ragService.search(query, topK, threshold, documentId);
        return ResponseEntity.ok(results);
    }
}

