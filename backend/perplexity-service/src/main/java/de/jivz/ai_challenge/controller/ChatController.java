package de.jivz.ai_challenge.controller;
import de.jivz.ai_challenge.dto.ChatRequest;
import de.jivz.ai_challenge.dto.ChatResponse;
import de.jivz.ai_challenge.service.AgentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
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

    public ChatController(AgentService agentService) {
        this.agentService = agentService;
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
     * Health check endpoint.
     * 
     * @return service health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString()
        ));
    }
}
