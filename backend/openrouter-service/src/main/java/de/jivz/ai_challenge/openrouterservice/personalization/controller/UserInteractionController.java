package de.jivz.ai_challenge.openrouterservice.personalization.controller;

import de.jivz.ai_challenge.openrouterservice.personalization.profile.dto.UserInteractionDTO;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.dto.UserInteractionRequestDTO;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.service.UserInteractionService;
import de.jivz.ai_challenge.openrouterservice.personalization.learning.service.LearningService;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.entity.AgentMemory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for managing user interactions
 */
@RestController
@RequestMapping("/api/personalization/interactions")
@RequiredArgsConstructor
@Slf4j
public class UserInteractionController {

    private final UserInteractionService interactionService;
    private final LearningService learningService;

    /**
     * Record a new user interaction
     * POST /api/personalization/interactions
     */
    @PostMapping
    public ResponseEntity<UserInteractionDTO> recordInteraction(
            @Valid @RequestBody UserInteractionRequestDTO request) {
        log.info("POST /api/personalization/interactions - userId: {}", request.getUserId());

        UserInteractionDTO recorded = interactionService.recordInteraction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(recorded);
    }

    /**
     * Get interactions for a user
     * GET /api/personalization/interactions/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<UserInteractionDTO>> getUserInteractions(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/personalization/interactions/user/{} - page: {}, size: {}", userId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserInteractionDTO> interactions = interactionService.getUserInteractions(userId, pageable);
        return ResponseEntity.ok(interactions);
    }

    /**
     * Get interactions by query type
     * GET /api/personalization/interactions/user/{userId}/type/{queryType}
     */
    @GetMapping("/user/{userId}/type/{queryType}")
    public ResponseEntity<Page<UserInteractionDTO>> getInteractionsByType(
            @PathVariable String userId,
            @PathVariable String queryType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/personalization/interactions/user/{}/type/{}", userId, queryType);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserInteractionDTO> interactions = interactionService.getInteractionsByType(userId, queryType, pageable);
        return ResponseEntity.ok(interactions);
    }

    /**
     * Get recent interactions
     * GET /api/personalization/interactions/user/{userId}/recent
     */
    @GetMapping("/user/{userId}/recent")
    public ResponseEntity<Page<UserInteractionDTO>> getRecentInteractions(
            @PathVariable String userId,
            @RequestParam(defaultValue = "7") int daysAgo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/personalization/interactions/user/{}/recent - days: {}", userId, daysAgo);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserInteractionDTO> interactions = interactionService.getRecentInteractions(userId, daysAgo, pageable);
        return ResponseEntity.ok(interactions);
    }

    /**
     * Get interactions with positive feedback
     * GET /api/personalization/interactions/user/{userId}/positive
     */
    @GetMapping("/user/{userId}/positive")
    public ResponseEntity<Page<UserInteractionDTO>> getPositiveFeedback(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/personalization/interactions/user/{}/positive", userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserInteractionDTO> interactions = interactionService.getPositiveFeedback(userId, pageable);
        return ResponseEntity.ok(interactions);
    }

    /**
     * Get interactions with negative feedback
     * GET /api/personalization/interactions/user/{userId}/negative
     */
    @GetMapping("/user/{userId}/negative")
    public ResponseEntity<Page<UserInteractionDTO>> getNegativeFeedback(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/personalization/interactions/user/{}/negative", userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserInteractionDTO> interactions = interactionService.getNegativeFeedback(userId, pageable);
        return ResponseEntity.ok(interactions);
    }

    /**
     * Update feedback for an interaction
     * PATCH /api/personalization/interactions/{interactionId}/feedback
     */
    @PatchMapping("/{interactionId}/feedback")
    public ResponseEntity<UserInteractionDTO> updateFeedback(
            @PathVariable Long interactionId,
            @RequestParam Integer feedback) {
        log.info("PATCH /api/personalization/interactions/{}/feedback - value: {}", interactionId, feedback);

        try {
            UserInteractionDTO updated = interactionService.updateFeedback(interactionId, feedback);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Error updating feedback: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get query type statistics
     * GET /api/personalization/interactions/user/{userId}/statistics
     */
    @GetMapping("/user/{userId}/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics(@PathVariable String userId) {
        log.info("GET /api/personalization/interactions/user/{}/statistics", userId);

        Map<String, Long> queryTypeStats = interactionService.getQueryTypeStatistics(userId);
        Double avgProcessingTime = interactionService.getAverageProcessingTime(userId);

        Map<String, Object> statistics = Map.of(
            "queryTypeStatistics", queryTypeStats,
            "averageProcessingTimeMs", avgProcessingTime
        );

        return ResponseEntity.ok(statistics);
    }

    /**
     * Get learned patterns for a user
     * GET /api/personalization/interactions/user/{userId}/patterns
     */
    @GetMapping("/user/{userId}/patterns")
    public ResponseEntity<List<Map<String, Object>>> getPatterns(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("GET /api/personalization/interactions/user/{}/patterns - limit: {}", userId, limit);

        try {
            List<AgentMemory> patterns = learningService.getTopPatterns(userId, limit);

            // Convert to simplified DTO
            List<Map<String, Object>> patternDtos = patterns.stream()
                    .map(pattern -> {
                        Map<String, Object> dto = new HashMap<>();
                        dto.put("id", pattern.getId());
                        dto.put("key", pattern.getKey());
                        dto.put("value", pattern.getValue());
                        dto.put("memoryType", pattern.getMemoryType());
                        dto.put("confidence", pattern.getConfidence());
                        dto.put("usageCount", pattern.getUsageCount());
                        dto.put("lastUsed", pattern.getLastUsed() != null ? pattern.getLastUsed().toString() : null);
                        return dto;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(patternDtos);
        } catch (Exception e) {
            log.error("Error getting patterns for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Delete an interaction
     * DELETE /api/personalization/interactions/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInteraction(@PathVariable Long id) {
        log.info("DELETE /api/personalization/interactions/{}", id);

        try {
            interactionService.deleteInteraction(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting interaction {}: {}", id, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }
}
