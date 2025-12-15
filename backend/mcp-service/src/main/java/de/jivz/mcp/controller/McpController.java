package de.jivz.mcp.controller;

import de.jivz.mcp.model.McpTool;
import de.jivz.mcp.model.ToolExecutionRequest;
import de.jivz.mcp.model.ToolExecutionResponse;
import de.jivz.mcp.service.McpServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for MCP operations
 */
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@Slf4j
public class McpController {

    private final McpServerService mcpServerService;

    /**
     * Get list of available MCP tools
     *
     * GET /mcp/tools
     */
    @GetMapping("/tools")
    public ResponseEntity<List<McpTool>> getTools() {
        try {
            log.info("Received request to list MCP tools");
            List<McpTool> tools = mcpServerService.listTools();

            log.info("Returning {} tools", tools.size());
            return ResponseEntity.ok(tools);

        } catch (Exception e) {
            log.error("Error listing tools", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get a specific tool by name
     *
     * GET /mcp/tools/{toolName}
     */
    @GetMapping("/tools/{toolName}")
    public ResponseEntity<McpTool> getTool(@PathVariable String toolName) {
        log.info("Received request to get tool: {}", toolName);
        return mcpServerService.getTool(toolName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Execute a tool with given arguments
     *
     * POST /mcp/execute
     */
    @PostMapping("/execute")
    public ResponseEntity<ToolExecutionResponse> executeTool(@RequestBody ToolExecutionRequest request) {
        log.info("Received request to execute tool: {}", request.getToolName());

        try {
            // Validate tool exists
            if (mcpServerService.getTool(request.getToolName()).isEmpty()) {
                return ResponseEntity.ok(ToolExecutionResponse.builder()
                        .success(false)
                        .error("Tool not found: " + request.getToolName())
                        .toolName(request.getToolName())
                        .build());
            }

            // Execute tool
            Object result = mcpServerService.executeTool(
                    request.getToolName(),
                    request.getArguments() != null ? request.getArguments() : new HashMap<>()
            );

            log.info("Tool executed successfully: {}", request.getToolName());
            return ResponseEntity.ok(ToolExecutionResponse.builder()
                    .success(true)
                    .result(result)
                    .toolName(request.getToolName())
                    .build());

        } catch (IllegalArgumentException e) {
            log.error("Invalid arguments for tool: {}", request.getToolName(), e);
            return ResponseEntity.ok(ToolExecutionResponse.builder()
                    .success(false)
                    .error("Invalid arguments: " + e.getMessage())
                    .toolName(request.getToolName())
                    .build());

        } catch (Exception e) {
            log.error("Error executing tool: {}", request.getToolName(), e);
            return ResponseEntity.ok(ToolExecutionResponse.builder()
                    .success(false)
                    .error("Execution error: " + e.getMessage())
                    .toolName(request.getToolName())
                    .build());
        }
    }

    /**
     * Check MCP server status
     *
     * GET /mcp/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        int toolCount = mcpServerService.listTools().size();

        Map<String, Object> status = new HashMap<>();
        status.put("status", "running");
        status.put("type", "Java Native MCP Server");
        status.put("toolCount", toolCount);
        status.put("version", "1.0.0");

        return ResponseEntity.ok(status);
    }
}

