package de.jivz.ai_challenge.openrouterservice.controller;

import de.jivz.ai_challenge.openrouterservice.dto.ChatRequest;
import de.jivz.ai_challenge.openrouterservice.dto.ChatResponse;
import de.jivz.ai_challenge.openrouterservice.dto.Message;
import de.jivz.ai_challenge.openrouterservice.service.ChatWithToolsService;
import de.jivz.ai_challenge.openrouterservice.service.ConversationHistoryService;
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

import java.time.LocalDateTime;
import java.util.*;

/**
 * REST Controller für OpenRouter Chat-Operationen mit Spring AI
 *
 * Features:
 * - Einfache und erweiterte Chat-Anfragen
 * - JSON-Response Formatting
 * - Conversation History Management (neu!)
 * - Conversation Listing mit Sidebar Integration (neu!)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/openrouter/chat")
@Tag(name = "Chat", description = "Chat-Operationen mit OpenRouter und Conversation Management")
public class OpenRouterChatController {

    private final OpenRouterAiChatService chatService;
    private final ChatWithToolsService chatWithToolsService;
    private final ConversationHistoryService historyService;

    public OpenRouterChatController(
            OpenRouterAiChatService chatService,
            ChatWithToolsService chatWithToolsService,
            ConversationHistoryService historyService) {
        this.chatService = chatService;
        this.chatWithToolsService = chatWithToolsService;
        this.historyService = historyService;
        log.info("OpenRouterChatController initialized with conversation management");
    }

    // ============== BASIC CHAT ENDPOINTS ==============

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
    @Operation(summary = "Chat-Anfrage mit erweiterten Parametern (WITH HISTORY PERSISTENCE)",
               description = "Sendet eine Chat-Anfrage mit vollständiger Kontrolle über Parameter. Speichert Historie in PostgreSQL!")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Erfolgreiche Chat-Antwort",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatResponse.class))),
        @ApiResponse(responseCode = "400", description = "Ungültige Request-Parameter"),
        @ApiResponse(responseCode = "500", description = "Interner Fehler")
    })
    public ResponseEntity<ChatResponse> fullChat(
            @Parameter(description = "Chat-Request mit allen Parametern", required = true)
            @RequestBody ChatRequest request) {
        log.info("Received full chat request - Model: {}, ConversationId: {}",
                 request.getModel(), request.getConversationId());

        try {
            // ✅ WICHTIG: Verwende ChatWithToolsService für Persistierung in DB!
            // Falls conversationId nicht gesetzt, generiere eine neue
            if (request.getConversationId() == null || request.getConversationId().isBlank()) {
                request.setConversationId("openrouter-" + System.currentTimeMillis() +
                        "-" + System.nanoTime() % 1000000);
                log.info("Generated new conversationId: {}", request.getConversationId());
            }

            ChatResponse response = chatWithToolsService.chatWithTools(request);
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

    // ============== CONVERSATION MANAGEMENT ENDPOINTS (NEW) ==============

    /**
     * GET /api/v1/openrouter/chat/conversations
     * Получить список всех конверсаций (для сайдбара)
     */
    @GetMapping("/conversations")
    @Operation(summary = "Получить список конверсаций",
               description = "Возвращает список всех конверсаций с метаинформацией для отображения в сайдбаре")
    @ApiResponse(responseCode = "200", description = "Список конверсаций",
                content = @Content(mediaType = "application/json"))
    public ResponseEntity<Map<String, Object>> getConversations() {
        log.info("Received getConversations request");

        try {
            Map<String, Object> response = new HashMap<>();

            // Получаем все конверсации из БД через MemoryRepository
            // findAllConversationIds() возвращает список всех уникальных conversation_id
            List<String> allConvIds = chatWithToolsService.getAllConversationIds();

            List<Map<String, Object>> conversations = new ArrayList<>();
            for (String convId : allConvIds) {
                try {
                    // Получаем историю для каждой конверсации
                    var messages = historyService.getHistory(convId);
                    if (messages != null && !messages.isEmpty()) {
                        Map<String, Object> summary = new HashMap<>();
                        summary.put("conversationId", convId);
                        // Первое сообщение как превью
                        summary.put("firstMessage", truncateMessage(messages.get(0).getContent()));
                        // Последнее время - берём из последнего сообщения (примерно)
                        summary.put("lastMessageTime", System.currentTimeMillis());
                        summary.put("messageCount", messages.size());
                        summary.put("hasCompression", false);

                        conversations.add(summary);
                    }
                } catch (Exception e) {
                    log.warn("Error processing conversation {}: {}", convId, e.getMessage());
                }
            }

            response.put("conversations", conversations);
            response.put("count", conversations.size());
            response.put("status", "success");

            log.info("📋 Returning {} conversations", conversations.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get conversations: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get conversations"));
        }
    }

    /**
     * Вспомогательный метод для усечения текста сообщения
     */
    private String truncateMessage(String message) {
        if (message == null) return "";
        int maxLen = 50;
        return message.length() > maxLen ? message.substring(0, maxLen) + "..." : message;
    }

    /**
     * GET /api/v1/openrouter/chat/conversations/{conversationId}/history
     * Получить историю конверсации
     */
    @GetMapping("/conversations/{conversationId}/history")
    @Operation(summary = "Получить историю конверсации",
               description = "Возвращает все сообщения конверсации с фильтрацией по conversationId")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "История конверсации",
                    content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Конверсация не найдена")
    })
    public ResponseEntity<Map<String, Object>> getConversationHistory(
            @Parameter(description = "ID конверсации", required = true, example = "conv-123")
            @PathVariable String conversationId) {
        log.info("Received getConversationHistory request for conversationId: {}", conversationId);

        try {
            // Загружаем историю из сервиса
            List<Message> history = historyService.getHistory(conversationId);

            Map<String, Object> response = new HashMap<>();
            response.put("conversationId", conversationId);
            response.put("messages", history);
            response.put("messageCount", history.size());
            response.put("status", "success");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get conversation history: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get history"));
        }
    }

    /**
     * DELETE /api/v1/openrouter/chat/conversations/{conversationId}
     * Удалить конверсацию
     */
    @DeleteMapping("/conversations/{conversationId}")
    @Operation(summary = "Удалить конверсацию",
               description = "Удаляет конверсацию и всю её историю из БД")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Конверсация успешно удалена"),
        @ApiResponse(responseCode = "404", description = "Конверсация не найдена"),
        @ApiResponse(responseCode = "500", description = "Ошибка при удалении")
    })
    public ResponseEntity<Map<String, Object>> deleteConversation(
            @Parameter(description = "ID конверсации", required = true, example = "conv-123")
            @PathVariable String conversationId) {
        log.info("Received deleteConversation request for conversationId: {}", conversationId);

        try {
            historyService.clearHistory(conversationId);

            Map<String, Object> response = new HashMap<>();
            response.put("conversationId", conversationId);
            response.put("message", "Conversation deleted successfully");
            response.put("status", "success");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to delete conversation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete conversation"));
        }
    }

    /**
     * POST /api/v1/openrouter/chat/conversations/{conversationId}/clear
     * Очистить историю конверсации
     */
    @PostMapping("/conversations/{conversationId}/clear")
    @Operation(summary = "Очистить историю конверсации",
               description = "Удаляет всю историю из конверсации, но сохраняет саму конверсацию")
    @ApiResponse(responseCode = "200", description = "История успешно очищена")
    public ResponseEntity<Map<String, Object>> clearConversationHistory(
            @Parameter(description = "ID конверсации", required = true, example = "conv-123")
            @PathVariable String conversationId) {
        log.info("Received clearConversationHistory request for conversationId: {}", conversationId);

        try {
            historyService.clearHistory(conversationId);

            Map<String, Object> response = new HashMap<>();
            response.put("conversationId", conversationId);
            response.put("message", "Conversation history cleared successfully");
            response.put("status", "success");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to clear conversation history: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to clear history"));
        }
    }

    // ============== UTILITY ENDPOINTS ==============

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

