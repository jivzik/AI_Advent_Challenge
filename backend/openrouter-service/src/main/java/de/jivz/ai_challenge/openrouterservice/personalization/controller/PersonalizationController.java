package de.jivz.ai_challenge.openrouterservice.personalization.controller;

import de.jivz.ai_challenge.openrouterservice.personalization.service.PersonalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for personalization features
 */
@RestController
@RequestMapping("/api/personalization")
@RequiredArgsConstructor
@Slf4j
public class PersonalizationController {

    private final PersonalizationService personalizationService;

    /**
     * Get complete personalization context for a user
     * GET /api/personalization/context/{userId}
     */
    @GetMapping("/context/{userId}")
    public ResponseEntity<Map<String, Object>> getPersonalizationContext(@PathVariable String userId) {
        log.info("GET /api/personalization/context/{}", userId);

        Map<String, Object> context = personalizationService.getPersonalizationContext(userId);
        return ResponseEntity.ok(context);
    }

    /**
     * Get personalized system prompt for a user
     * GET /api/personalization/prompt/{userId}
     */
    @GetMapping("/prompt/{userId}")
    public ResponseEntity<Map<String, String>> getPersonalizedPrompt(@PathVariable String userId) {
        log.info("GET /api/personalization/prompt/{}", userId);

        String prompt = personalizationService.buildPersonalizedPrompt(userId);
        return ResponseEntity.ok(Map.of("prompt", prompt));
    }

    /**
     * Trigger learning process for a user
     * POST /api/personalization/learn/{userId}
     */
    @PostMapping("/learn/{userId}")
    public ResponseEntity<Map<String, String>> triggerLearning(@PathVariable String userId) {
        log.info("POST /api/personalization/learn/{}", userId);

        personalizationService.triggerLearning(userId);
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Learning process triggered for user: " + userId
        ));
    }

    /**
     * Check if a user has a personalization profile
     * GET /api/personalization/has-profile/{userId}
     */
    @GetMapping("/has-profile/{userId}")
    public ResponseEntity<Map<String, Boolean>> hasProfile(@PathVariable String userId) {
        log.info("GET /api/personalization/has-profile/{}", userId);

        boolean hasProfile = personalizationService.hasProfile(userId);
        return ResponseEntity.ok(Map.of("hasProfile", hasProfile));
    }
}
