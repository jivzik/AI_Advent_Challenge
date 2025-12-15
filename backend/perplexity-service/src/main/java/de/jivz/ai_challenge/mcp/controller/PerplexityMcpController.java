package de.jivz.ai_challenge.mcp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.ai_challenge.mcp.model.McpTool;
import de.jivz.ai_challenge.mcp.model.McpToolExecutionRequest;
import de.jivz.ai_challenge.mcp.model.McpToolExecutionResponse;
import de.jivz.ai_challenge.mcp.service.PerplexityMcpClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Perplexity MCP operations
 */
@RestController
@RequestMapping("/perplexity")
@RequiredArgsConstructor
@Slf4j
public class PerplexityMcpController {

    private final PerplexityMcpClientService mcpClientService;
    private final ObjectMapper objectMapper;

    /**
     * GET /perplexity/tools
     * Returns list of available MCP tools from Perplexity MCP server
     */
    @GetMapping("/tools")
    public ResponseEntity<List<McpTool>> getTools() {
        try {
            log.info("Received request to list Perplexity MCP tools");

            if (!mcpClientService.isInitialized()) {
                log.error("MCP client is not initialized");
                return ResponseEntity.internalServerError().build();
            }

            List<McpTool> tools = mcpClientService.listTools();

            log.info("Returning {} Perplexity MCP tools", tools.size());
            return ResponseEntity.ok(tools);

        } catch (Exception e) {
            log.error("Error listing Perplexity MCP tools", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /perplexity/ask
     * Asks a question to Perplexity Sonar via MCP tool
     *
     * Request body: { "prompt": "your question" }
     * Returns: Answer from Perplexity as string or JSON
     */
    @PostMapping("/ask")
    public ResponseEntity<?> ask(@RequestBody Map<String, Object> requestBody) {
        try {
            String prompt = (String) requestBody.get("prompt");

            if (prompt == null || prompt.isBlank()) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "prompt is required")
                );
            }

            log.info("Received Perplexity ask request with prompt: {}",
                prompt.substring(0, Math.min(100, prompt.length())));

            if (!mcpClientService.isInitialized()) {
                return ResponseEntity.internalServerError().body(
                    Map.of("error", "MCP client is not initialized")
                );
            }

            // Prepare arguments for perplexity_ask tool
            Map<String, Object> arguments = new HashMap<>();
            arguments.put("prompt", prompt);

            // Optional parameters
            if (requestBody.containsKey("model")) {
                arguments.put("model", requestBody.get("model"));
            }
            if (requestBody.containsKey("temperature")) {
                arguments.put("temperature", requestBody.get("temperature"));
            }
            if (requestBody.containsKey("max_tokens")) {
                arguments.put("max_tokens", requestBody.get("max_tokens"));
            }

            // Execute perplexity_ask tool via MCP
            Object result = mcpClientService.executeTool("perplexity_ask", arguments);

            log.info("Perplexity MCP tool executed successfully");

            // Parse the result
            Map<String, Object> resultMap = objectMapper.convertValue(result, Map.class);

            // Extract content from MCP response
            if (resultMap.containsKey("content")) {
                List<Map<String, Object>> content = (List<Map<String, Object>>) resultMap.get("content");
                if (!content.isEmpty()) {
                    String textContent = (String) content.get(0).get("text");

                    // Try to parse as JSON
                    try {
                        Map<String, Object> perplexityResponse = objectMapper.readValue(textContent, Map.class);
                        return ResponseEntity.ok(perplexityResponse);
                    } catch (Exception e) {
                        // Return as string if not JSON
                        return ResponseEntity.ok(Map.of("answer", textContent));
                    }
                }
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error executing Perplexity ask", e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Failed to execute Perplexity ask: " + e.getMessage())
            );
        }
    }

    /**
     * POST /perplexity/search
     * Search for information using Perplexity via MCP tool
     *
     * Request body: { "query": "search query" }
     */
    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody Map<String, String> requestBody) {
        try {
            String query = requestBody.get("query");

            if (query == null || query.isBlank()) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "query is required")
                );
            }

            log.info("Received Perplexity search request: {}", query);

            if (!mcpClientService.isInitialized()) {
                return ResponseEntity.internalServerError().body(
                    Map.of("error", "MCP client is not initialized")
                );
            }

            // Execute perplexity_search tool via MCP
            Map<String, Object> arguments = Map.of("query", query);
            Object result = mcpClientService.executeTool("perplexity_search", arguments);

            log.info("Perplexity search executed successfully");

            // Parse the result
            Map<String, Object> resultMap = objectMapper.convertValue(result, Map.class);

            // Extract content from MCP response
            if (resultMap.containsKey("content")) {
                List<Map<String, Object>> content = (List<Map<String, Object>>) resultMap.get("content");
                if (!content.isEmpty()) {
                    String textContent = (String) content.get(0).get("text");

                    // Try to parse as JSON
                    try {
                        Map<String, Object> perplexityResponse = objectMapper.readValue(textContent, Map.class);
                        return ResponseEntity.ok(perplexityResponse);
                    } catch (Exception e) {
                        // Return as string if not JSON
                        return ResponseEntity.ok(Map.of("answer", textContent));
                    }
                }
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error executing Perplexity search", e);
            return ResponseEntity.internalServerError().body(
                Map.of("error", "Failed to execute Perplexity search: " + e.getMessage())
            );
        }
    }

    /**
     * POST /perplexity/execute
     * Generic endpoint to execute any MCP tool
     */
    @PostMapping("/execute")
    public ResponseEntity<McpToolExecutionResponse> executeTool(@RequestBody McpToolExecutionRequest request) {
        try {
            log.info("Received request to execute Perplexity MCP tool: {}", request.getToolName());

            if (!mcpClientService.isInitialized()) {
                return ResponseEntity.ok(McpToolExecutionResponse.builder()
                        .success(false)
                        .error("MCP client is not initialized")
                        .toolName(request.getToolName())
                        .build());
            }

            // Execute tool
            Object result = mcpClientService.executeTool(
                    request.getToolName(),
                    request.getArguments() != null ? request.getArguments() : new HashMap<>()
            );

            log.info("Tool executed successfully: {}", request.getToolName());
            return ResponseEntity.ok(McpToolExecutionResponse.builder()
                    .success(true)
                    .result(result)
                    .toolName(request.getToolName())
                    .build());

        } catch (Exception e) {
            log.error("Error executing tool: {}", request.getToolName(), e);
            return ResponseEntity.ok(McpToolExecutionResponse.builder()
                    .success(false)
                    .error("Execution error: " + e.getMessage())
                    .toolName(request.getToolName())
                    .build());
        }
    }

    /**
     * GET /perplexity/status
     * Check MCP server status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("initialized", mcpClientService.isInitialized());
        status.put("type", "Perplexity MCP Client (Node.js)");
        status.put("version", "1.0.0");

        try {
            if (mcpClientService.isInitialized()) {
                int toolCount = mcpClientService.listTools().size();
                status.put("toolCount", toolCount);
                status.put("status", "running");
            } else {
                status.put("status", "not_initialized");
            }
        } catch (Exception e) {
            status.put("status", "error");
            status.put("error", e.getMessage());
        }

        return ResponseEntity.ok(status);
    }
}

