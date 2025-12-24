package de.jivz.rag.controller;

import de.jivz.rag.dto.DocumentDto;
import de.jivz.rag.service.RagFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * REST контроллер для индексации документов.
 *
 * Endpoints:
 * - POST /api/index/document - индексировать документ
 * - GET /api/index/documents - список документов
 * - DELETE /api/index/document/{name} - удалить по имени
 */
@RestController
@RequestMapping("/api/index")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class IndexController {

    private final RagFacade ragFacade;

    /**
     * Индексировать документ.
     *
     * POST /api/index/document
     * Content-Type: multipart/form-data
     */
    @PostMapping(value = "/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> indexDocument(@RequestParam("file") MultipartFile file) {
        long startTime = System.currentTimeMillis();

        log.info("📥 Index request: {} ({} bytes)", file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "File is empty"));
        }

        try {
            DocumentDto doc = ragFacade.uploadDocument(file);
            long processingTime = System.currentTimeMillis() - startTime;

            return ResponseEntity.ok(Map.of(
                    "documentId", doc.getId(),
                    "chunksCount", doc.getChunkCount(),
                    "status", "indexed",
                    "processingTime", formatTime(processingTime)
            ));
        } catch (Exception e) {
            log.error("❌ Indexing failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", "error",
                            "error", e.getMessage()
                    ));
        }
    }

    /**
     * Получить список всех проиндексированных документов.
     *
     * GET /api/index/documents
     */
    @GetMapping("/documents")
    public ResponseEntity<List<Map<String, Object>>> getIndexedDocuments() {
        List<DocumentDto> docs = ragFacade.getAllDocuments();

        List<Map<String, Object>> result = docs.stream()
                .map(doc -> Map.<String, Object>of(
                        "documentName", doc.getFileName(),
                        "chunksCount", doc.getChunkCount() != null ? doc.getChunkCount() : 0,
                        "createdAt", doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : ""
                ))
                .toList();

        return ResponseEntity.ok(result);
    }

    /**
     * Удалить документ по имени.
     *
     * DELETE /api/index/document/{name}
     */
    @DeleteMapping("/document/{name}")
    public ResponseEntity<?> deleteDocumentByName(@PathVariable String name) {
        log.info("🗑️ Delete request for document: {}", name);

        boolean deleted = ragFacade.deleteDocumentByName(name);

        if (deleted) {
            return ResponseEntity.ok(Map.of("status", "deleted"));
        }
        return ResponseEntity.notFound().build();
    }

    private String formatTime(long millis) {
        if (millis < 1000) {
            return millis + "ms";
        }
        return String.format("%.1fs", millis / 1000.0);
    }
}

