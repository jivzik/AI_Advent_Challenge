package de.jivz.ai_challenge.openrouterservice.controller;

import de.jivz.ai_challenge.openrouterservice.dto.ChatRequest;
import de.jivz.ai_challenge.openrouterservice.dto.ChatResponse;
import de.jivz.ai_challenge.openrouterservice.service.OpenRouterAiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller für OpenRouter Chat-Operationen mit Spring AI
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/openrouter/chat")
@Tag(name = "Chat", description = "Chat-Operationen mit OpenRouter")
public class OpenRouterChatController {

    private final OpenRouterAiChatService chatService;

    public OpenRouterChatController(OpenRouterAiChatService chatService) {
        this.chatService = chatService;
        log.info("OpenRouterChatController initialized");
    }

    /**
     * POST /api/v1/openrouter/chat/simple
     * Einfache Chat-Anfrage
     */
    @PostMapping("/simple")
    @Operation(summary = "Einfache Chat-Anfrage",
               description = "Sendet eine einfache Textnachricht an den LLM mit Standard-Konfiguration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Erfolgreiche Chat-Antwort",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatResponse.class))),
        @ApiResponse(responseCode = "500", description = "Interner Fehler")
    })
    public ResponseEntity<ChatResponse> simpleChat(
            @Parameter(description = "Die Benutzernachricht", required = true, example = "Hallo, wie heißt du?")
            @RequestParam String message) {
        log.info("Received simple chat request");

        try {
            ChatResponse response = chatService.chat(message);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Simple chat failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/v1/openrouter/chat/full
     * Chat-Anfrage mit allen Parametern
     */
    @PostMapping("/full")
    @Operation(summary = "Chat-Anfrage mit erweiterten Parametern",
               description = "Sendet eine Chat-Anfrage mit vollständiger Kontrolle über Parameter wie Modell, Temperatur und Tokens")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Erfolgreiche Chat-Antwort",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatResponse.class))),
        @ApiResponse(responseCode = "400", description = "Ungültige Request-Parameter"),
        @ApiResponse(responseCode = "500", description = "Interner Fehler")
    })
    public ResponseEntity<ChatResponse> fullChat(
            @Parameter(description = "Chat-Request mit allen Parametern", required = true)
            @RequestBody ChatRequest request) {
        log.info("Received full chat request - Model: {}", request.getModel());

        try {
            ChatResponse response = chatService.chatWithRequest(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Full chat failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/v1/openrouter/chat/json
     * Chat-Anfrage mit JSON-Response
     */
    @PostMapping("/json")
    @Operation(summary = "Chat-Anfrage mit JSON-Response",
               description = "Sendet eine Chat-Anfrage und formatiert die Antwort als strukturiertes JSON")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Erfolgreiche Chat-Antwort im JSON-Format",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatResponse.class))),
        @ApiResponse(responseCode = "500", description = "Interner Fehler")
    })
    public ResponseEntity<ChatResponse> jsonChat(
            @Parameter(description = "Die Benutzernachricht", required = true, example = "Gib mir eine Antwort im JSON-Format")
            @RequestParam String message) {
        log.info("Received JSON chat request");

        try {
            String jsonPrompt = """
                    Antwort im folgenden JSON-Format:
                    {
                        "response": "Deine Antwort hier",
                        "status": "success"
                    }
                    
                    Benutzernachricht: %s
                    """.formatted(message);

            ChatResponse response = chatService.chat(jsonPrompt);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("JSON chat failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    /**
     * GET /api/v1/openrouter/chat/health
     * Health Check Endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health Check",
               description = "Überprüft ob der OpenRouter Chat Service läuft")
    @ApiResponse(responseCode = "200", description = "Service läuft",
                content = @Content(mediaType = "text/plain", schema = @Schema(type = "string")))
    public ResponseEntity<String> health() {
        log.info("Health check requested");
        return ResponseEntity.ok("OpenRouter Chat Service is running");
    }
}

