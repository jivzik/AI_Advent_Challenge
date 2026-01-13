package de.jivz.mcp.controller;

import de.jivz.mcp.model.ToolCallRequest;
import de.jivz.mcp.model.ToolCallResponse;
import de.jivz.mcp.model.ToolDefinition;
import de.jivz.mcp.service.McpServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller für MCP-Operationen.
 */
@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
@Slf4j
public class McpToolsController {

    private final McpServerService mcpServerService;

    /**
     * Liste verfügbarer MCP Tools abrufen.
     * GET /tools
     */
    @GetMapping
    public ResponseEntity<List<ToolDefinition>> getTools() {
        try {
            log.info("Request: MCP Tools auflisten");
            List<ToolDefinition> tools = mcpServerService.listTools();

            log.info("{} Tools zurückgegeben", tools.size());
            return ResponseEntity.ok(tools);

        } catch (Exception e) {
            log.error("Fehler beim Auflisten der Tools", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Выполнить MCP инструмент.
     * <p>
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

        return ResponseEntity.ok(mcpServerService.executeTool(toolName, request.getArguments()));
    }
}


