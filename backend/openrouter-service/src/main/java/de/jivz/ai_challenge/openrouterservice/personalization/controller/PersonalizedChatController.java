package de.jivz.ai_challenge.openrouterservice.personalization.controller;
import de.jivz.ai_challenge.openrouterservice.dto.ChatRequest;
import de.jivz.ai_challenge.openrouterservice.dto.ChatResponse;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.dto.PersonalizedChatRequest;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.dto.PersonalizedChatResponse;
import de.jivz.ai_challenge.openrouterservice.personalization.learning.service.LearningService;
import de.jivz.ai_challenge.openrouterservice.personalization.service.PersonalizationService;
import de.jivz.ai_challenge.openrouterservice.service.OpenRouterAiChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/chat/personalized")
@RequiredArgsConstructor
@Slf4j
public class PersonalizedChatController {
    private final OpenRouterAiChatService openRouterAiChatService;
    private final PersonalizationService personalizationService;
    private final LearningService learningService;


    @PostMapping
    public ResponseEntity<PersonalizedChatResponse> chatPersonalized(
            @RequestParam String userId,
            @Valid @RequestBody PersonalizedChatRequest request) {
        log.info("POST /api/chat/personalized - userId: {}, useProfile: {}", 
                userId, request.getUseProfile());
        long startTime = System.currentTimeMillis();
        try {
            String systemPrompt = null;
            Map<String, Object> profileContext = null;
            List<String> usedMemory = List.of();
            if (request.getUseProfile() != null && request.getUseProfile()) {
                profileContext = personalizationService.getPersonalizationContext(userId);
                systemPrompt = personalizationService.buildPersonalizedPrompt(userId);
                if (profileContext.containsKey("recentMemories")) {
                    usedMemory = (List<String>) profileContext.get("recentMemories");
                }
            }
            // Baue die vollständige Message mit optionalem System Prompt
            String fullMessage = request.getMessage();
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                fullMessage = systemPrompt + "\n\nUser: " + request.getMessage();
            }

            ChatRequest chatRequest = ChatRequest.builder()
                    .message(fullMessage)
                    .temperature(0.7)
                    .maxTokens(2000)
                    .build();
            ChatResponse chatResponse =
                    openRouterAiChatService.chatWithRequest(chatRequest);
            if (chatResponse == null || chatResponse.getReply() == null) {
                log.error("Chat request failed: No response from OpenRouter");
                return ResponseEntity.internalServerError().body(
                    PersonalizedChatResponse.builder()
                        .response("Error: No response from OpenRouter")
                        .usedProfile(false)
                        .build()
                );
            }
            learningService.logInteraction(
                    userId,
                    request.getMessage(),
                    chatResponse.getReply(),
                    "chat",
                    (int) (System.currentTimeMillis() - startTime),
                    chatResponse.getOutputTokens() != null ? chatResponse.getOutputTokens() : 0,
                    chatResponse.getModel() != null ? chatResponse.getModel() : "unknown"
            );
            long processingTime = System.currentTimeMillis() - startTime;
            PersonalizedChatResponse response = PersonalizedChatResponse.builder()
                    .response(chatResponse.getReply())
                    .usedProfile(request.getUseProfile() != null && request.getUseProfile())
                    .usedMemory(usedMemory)
                    .profileContext(profileContext)
                    .processingTimeMs((int) processingTime)
                    .tokensUsed(chatResponse.getOutputTokens() != null ? chatResponse.getOutputTokens() : 0)
                    .build();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in personalized chat: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                PersonalizedChatResponse.builder()
                    .response("Internal server error: " + e.getMessage())
                    .usedProfile(false)
                    .build()
            );
        }
    }
    @PostMapping("/feedback")
    public ResponseEntity<Map<String, String>> submitFeedback(
            @RequestBody Map<String, Object> payload) {
        log.info("POST /api/chat/feedback - payload: {}", payload);
        try {
            if (!payload.containsKey("interactionId") || !payload.containsKey("feedback")) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Missing required fields: interactionId and feedback")
                );
            }
            Long interactionId = ((Number) payload.get("interactionId")).longValue();
            Integer feedback = ((Number) payload.get("feedback")).intValue();
            if (feedback != 1 && feedback != -1) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Feedback must be 1 (positive) or -1 (negative)")
                );
            }
            learningService.updateFeedback(interactionId, feedback);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Feedback recorded"
            ));
        } catch (Exception e) {
            log.error("Error recording feedback: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Internal server error: " + e.getMessage())
            );
        }
    }
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(@RequestParam String userId) {
        log.info("GET /api/chat/stats - userId: {}", userId);
        try {
            Map<String, Object> stats = personalizationService.getPersonalizationStats(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting stats for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Error retrieving statistics: " + e.getMessage())
            );
        }
    }
}
