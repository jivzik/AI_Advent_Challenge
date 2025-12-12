package de.jivz.ai_challenge.controller;
import de.jivz.ai_challenge.dto.ChatRequest;
import de.jivz.ai_challenge.dto.ChatResponse;
import de.jivz.ai_challenge.dto.CompressionInfo;
import de.jivz.ai_challenge.service.AgentService;
import de.jivz.ai_challenge.service.ConversationHistoryService;
import de.jivz.ai_challenge.service.DialogCompressionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

/**
 * REST Controller for chat endpoints.
 * Exception handling is delegated to GlobalExceptionHandler.
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final AgentService agentService;
    private final ConversationHistoryService historyService;
    private final DialogCompressionService compressionService;


    public ChatController(AgentService agentService,
                          ConversationHistoryService historyService, DialogCompressionService compressionService) {
        this.agentService = agentService;
        this.historyService = historyService;
        this.compressionService = compressionService;
    }
    /**
     * Processes a chat request and returns the AI response.
     * 
     * @param request the chat request containing the user's message
     * @return the chat response with the AI's reply
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat request from user: {}", request.getUserId());
        ChatResponse response = agentService.handle(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Clears the conversation history for a specific conversationId.
     *
     * @param conversationId the conversation identifier
     * @return confirmation message
     */
    @DeleteMapping("/conversation/{conversationId}")
    public ResponseEntity<Map<String, String>> clearConversation(@PathVariable String conversationId) {
        log.info("Clearing conversation history for conversationId: {}", conversationId);
        historyService.clearHistory(conversationId);
        return ResponseEntity.ok(Map.of(
                "status", "cleared",
                "conversationId", conversationId,
                "timestamp", new Date().toString()
        ));
    }

    /**
     * ⭐ NEW: Gets compression information for a conversation.
     * All business logic is in DialogCompressionService.
     *
     * @param conversationId the conversation identifier
     * @return compression information
     */
    @GetMapping("/compression-info/{conversationId}")
    public ResponseEntity<CompressionInfo> getCompressionInfo(
            @PathVariable String conversationId) {

        log.info("Getting compression info for conversationId: {}", conversationId);

        CompressionInfo info = compressionService.getCompressionInfo(conversationId);

        return ResponseEntity.ok(info);
    }


    /**
     * Gets statistics about active conversations.
     *
     * @return conversation statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(Map.of(
                "activeConversations", historyService.getConversationCount(),
                "timestamp", new Date().toString()
        ));
    }


    /**
     * Health check endpoint.
     *
     * @return service health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", new Date().toString()
        ));
    }
}
