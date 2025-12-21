package de.jivz.ai_challenge.openrouterservice.controller;

import de.jivz.ai_challenge.openrouterservice.dto.ChatRequest;
import de.jivz.ai_challenge.openrouterservice.dto.ChatResponse;
import de.jivz.ai_challenge.openrouterservice.mcp.MCPFactory;
import de.jivz.ai_challenge.openrouterservice.mcp.model.ToolDefinition;
import de.jivz.ai_challenge.openrouterservice.service.ChatWithToolsService;
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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST Controller für Chat mit MCP Tool-Unterstützung.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/openrouter/tools")
@Tag(name = "Chat with Tools", description = "Chat-Operationen mit MCP Tool-Unterstützung")
public class ChatWithToolsController {

    private final ChatWithToolsService chatWithToolsService;
    private final MCPFactory mcpFactory;

    public ChatWithToolsController(ChatWithToolsService chatWithToolsService, MCPFactory mcpFactory) {
        this.chatWithToolsService = chatWithToolsService;
        this.mcpFactory = mcpFactory;
        log.info("ChatWithToolsController initialized");
    }

    /**
     * POST /api/v1/openrouter/tools/chat
     * Chat mit automatischer Tool-Verwendung
     */
    @PostMapping("/chat")
    @Operation(summary = "Chat mit Tool-Unterstützung",
               description = "Sendet eine Chat-Anfrage die automatisch MCP Tools verwenden kann")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Erfolgreiche Chat-Antwort",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = ChatResponse.class))),
        @ApiResponse(responseCode = "500", description = "Interner Fehler")
    })
    public ResponseEntity<ChatResponse> chatWithTools(
            @Parameter(description = "Chat-Request", required = true)
            @RequestBody ChatRequest request) {
        log.info("Received chat with tools request");

        try {
            ChatResponse response = chatWithToolsService.chatWithTools(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Chat with tools failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/v1/openrouter/tools/chat/simple
     * Einfache Chat-Anfrage mit Tools
     */
    @PostMapping("/chat/simple")
    @Operation(summary = "Einfache Chat-Anfrage mit Tools",
               description = "Sendet eine einfache Textnachricht die automatisch MCP Tools verwenden kann")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Erfolgreiche Chat-Antwort",
                    content = @Content(mediaType = "application/json",
                                     schema = @Schema(implementation = ChatResponse.class))),
        @ApiResponse(responseCode = "500", description = "Interner Fehler")
    })
    public ResponseEntity<ChatResponse> simpleChatWithTools(
            @Parameter(description = "Die Benutzernachricht", required = true,
                      example = "List my Google Tasks")
            @RequestParam String message) {
        log.info("Received simple chat with tools request");

        try {
            ChatResponse response = chatWithToolsService.chatWithTools(message);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Simple chat with tools failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/v1/openrouter/tools/available
     * Listet alle verfügbaren MCP Tools auf
     */
    @GetMapping("/available")
    @Operation(summary = "Verfügbare Tools auflisten",
               description = "Gibt eine Liste aller verfügbaren MCP Tools zurück")
    @ApiResponse(responseCode = "200", description = "Liste der Tools")
    public ResponseEntity<List<ToolDefinition>> getAvailableTools() {
        log.info("Getting available tools");
        List<ToolDefinition> tools = mcpFactory.getAllToolDefinitions();
        return ResponseEntity.ok(tools);
    }

    /**
     * GET /api/v1/openrouter/tools/servers
     * Listet alle registrierten MCP Server auf
     */
    @GetMapping("/servers")
    @Operation(summary = "Registrierte Server auflisten",
               description = "Gibt eine Liste aller registrierten MCP Server zurück")
    @ApiResponse(responseCode = "200", description = "Liste der Server")
    public ResponseEntity<Map<String, Object>> getRegisteredServers() {
        log.info("Getting registered servers");
        Set<String> servers = mcpFactory.getRegisteredServers();
        return ResponseEntity.ok(Map.of(
                "servers", servers,
                "count", servers.size(),
                "hasTools", mcpFactory.hasTools()
        ));
    }

    /**
     * GET /api/v1/openrouter/tools/health
     * Health Check
     */
    @GetMapping("/health")
    @Operation(summary = "Health Check",
               description = "Überprüft ob der ChatWithTools Service läuft")
    @ApiResponse(responseCode = "200", description = "Service läuft")
    public ResponseEntity<Map<String, Object>> health() {
        log.info("Health check requested");
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "ChatWithToolsService",
                "registeredServers", mcpFactory.getRegisteredServers().size(),
                "availableTools", mcpFactory.getAllToolDefinitions().size()
        ));
    }
}

