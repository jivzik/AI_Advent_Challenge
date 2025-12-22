package de.jivz.rag.controller;

import de.jivz.rag.mcp.McpModels.*;
import de.jivz.rag.mcp.ToolExecutorService;
import de.jivz.rag.mcp.ToolsDefinitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * MCP контроллер - предоставляет tools для интеграции с perplexity-service.
 *
 * Endpoints:
 * - GET /api/tools - список доступных инструментов
 * - POST /api/tools/execute - выполнение инструмента
 */
@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class McpToolsController {

    private final ToolsDefinitionService toolsDefinitionService;
    private final ToolExecutorService toolExecutorService;

    /**
     * Получить список всех доступных MCP инструментов.
     *
     * GET /api/tools
     */
    @GetMapping
    public ResponseEntity<List<ToolDefinition>> getTools() {
        log.info("📋 Returning list of available tools");
        return ResponseEntity.ok(toolsDefinitionService.getToolDefinitions());
    }

    /**
     * Выполнить MCP инструмент.
     *
     * POST /api/tools/execute
     * Body: { "name": "search_documents", "arguments": { "query": "..." } }
     * или: { "toolName": "search_documents", "arguments": { "query": "..." } }
     */
    @PostMapping("/execute")
    public ResponseEntity<ToolCallResponse> executeTool(@RequestBody ToolCallRequest request) {
        String toolName = request.getEffectiveName();
        log.info("🔧 Executing tool: {}", toolName);

        if (toolName == null || toolName.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ToolCallResponse.builder()
                            .success(false)
                            .error("Tool name is required (use 'name' or 'toolName' field)")
                            .build());
        }

        ToolCallResponse response = toolExecutorService.execute(
                toolName,
                request.getArguments() != null ? request.getArguments() : Map.of()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Health check для MCP сервера.
     *
     * GET /api/tools/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "rag-mcp-server",
                "toolsCount", toolsDefinitionService.getToolDefinitions().size()
        ));
    }
}

