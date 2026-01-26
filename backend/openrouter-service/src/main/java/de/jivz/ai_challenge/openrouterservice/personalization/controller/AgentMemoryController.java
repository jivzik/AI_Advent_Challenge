package de.jivz.ai_challenge.openrouterservice.personalization.controller;

import de.jivz.ai_challenge.openrouterservice.personalization.profile.dto.AgentMemoryDTO;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.dto.AgentMemoryRequestDTO;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.service.AgentMemoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing agent memory
 */
@RestController
@RequestMapping("/api/personalization/memory")
@RequiredArgsConstructor
@Slf4j
public class AgentMemoryController {

    private final AgentMemoryService memoryService;

    /**
     * Store or update a memory entry
     * POST /api/personalization/memory
     */
    @PostMapping
    public ResponseEntity<AgentMemoryDTO> storeMemory(
            @Valid @RequestBody AgentMemoryRequestDTO request) {
        log.info("POST /api/personalization/memory - userId: {}, key: {}",
                 request.getUserId(), request.getKey());

        AgentMemoryDTO stored = memoryService.storeMemory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(stored);
    }

    /**
     * Get a specific memory entry
     * GET /api/personalization/memory/user/{userId}/key/{key}
     */
    @GetMapping("/user/{userId}/key/{key}")
    public ResponseEntity<AgentMemoryDTO> getMemory(
            @PathVariable String userId,
            @PathVariable String key) {
        log.info("GET /api/personalization/memory/user/{}/key/{}", userId, key);

        return memoryService.getMemory(userId, key)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all memory entries for a user
     * GET /api/personalization/memory/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AgentMemoryDTO>> getAllMemories(@PathVariable String userId) {
        log.info("GET /api/personalization/memory/user/{}", userId);

        List<AgentMemoryDTO> memories = memoryService.getAllMemories(userId);
        return ResponseEntity.ok(memories);
    }

    /**
     * Get memory entries by type
     * GET /api/personalization/memory/user/{userId}/type/{memoryType}
     */
    @GetMapping("/user/{userId}/type/{memoryType}")
    public ResponseEntity<List<AgentMemoryDTO>> getMemoriesByType(
            @PathVariable String userId,
            @PathVariable String memoryType) {
        log.info("GET /api/personalization/memory/user/{}/type/{}", userId, memoryType);

        List<AgentMemoryDTO> memories = memoryService.getMemoriesByType(userId, memoryType);
        return ResponseEntity.ok(memories);
    }

    /**
     * Get high-confidence memories
     * GET /api/personalization/memory/user/{userId}/high-confidence
     */
    @GetMapping("/user/{userId}/high-confidence")
    public ResponseEntity<List<AgentMemoryDTO>> getHighConfidenceMemories(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0.7") Double minConfidence) {
        log.info("GET /api/personalization/memory/user/{}/high-confidence - min: {}",
                 userId, minConfidence);

        List<AgentMemoryDTO> memories = memoryService.getHighConfidenceMemories(userId, minConfidence);
        return ResponseEntity.ok(memories);
    }

    /**
     * Get most used memories
     * GET /api/personalization/memory/user/{userId}/most-used
     */
    @GetMapping("/user/{userId}/most-used")
    public ResponseEntity<List<AgentMemoryDTO>> getMostUsedMemories(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("GET /api/personalization/memory/user/{}/most-used - limit: {}", userId, limit);

        List<AgentMemoryDTO> memories = memoryService.getMostUsedMemories(userId, limit);
        return ResponseEntity.ok(memories);
    }

    /**
     * Increment usage count for a memory entry
     * POST /api/personalization/memory/user/{userId}/key/{key}/increment
     */
    @PostMapping("/user/{userId}/key/{key}/increment")
    public ResponseEntity<AgentMemoryDTO> incrementUsage(
            @PathVariable String userId,
            @PathVariable String key) {
        log.info("POST /api/personalization/memory/user/{}/key/{}/increment", userId, key);

        return memoryService.incrementUsage(userId, key)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update confidence level for a memory entry
     * PATCH /api/personalization/memory/user/{userId}/key/{key}/confidence
     */
    @PatchMapping("/user/{userId}/key/{key}/confidence")
    public ResponseEntity<AgentMemoryDTO> updateConfidence(
            @PathVariable String userId,
            @PathVariable String key,
            @RequestParam Double confidence) {
        log.info("PATCH /api/personalization/memory/user/{}/key/{}/confidence - value: {}",
                 userId, key, confidence);

        return memoryService.updateConfidence(userId, key, confidence)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a memory entry
     * DELETE /api/personalization/memory/user/{userId}/key/{key}
     */
    @DeleteMapping("/user/{userId}/key/{key}")
    public ResponseEntity<Void> deleteMemory(
            @PathVariable String userId,
            @PathVariable String key) {
        log.info("DELETE /api/personalization/memory/user/{}/key/{}", userId, key);

        memoryService.deleteMemory(userId, key);
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete all memory entries for a user
     * DELETE /api/personalization/memory/user/{userId}
     */
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteAllMemories(@PathVariable String userId) {
        log.info("DELETE /api/personalization/memory/user/{}", userId);

        memoryService.deleteAllMemories(userId);
        return ResponseEntity.noContent().build();
    }
}
