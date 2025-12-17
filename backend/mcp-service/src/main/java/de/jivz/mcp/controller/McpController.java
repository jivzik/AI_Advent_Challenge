package de.jivz.mcp.controller;

import de.jivz.mcp.client.PerplexityServiceClient;
import de.jivz.mcp.client.PerplexityServiceDto.PerplexityRequest;
import de.jivz.mcp.client.PerplexityServiceDto.PerplexityResponse;
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
    private final PerplexityServiceClient perplexityServiceClient;

    /**
     * Get list of available MCP tools
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

            // Check if the result indicates an error (provider returned success: false)
            boolean isSuccess = true;
            String errorMessage = null;
            if (result instanceof Map<?, ?> resultMap) {
                Object successValue = resultMap.get("success");
                if (Boolean.FALSE.equals(successValue)) {
                    isSuccess = false;
                    Object errorValue = resultMap.get("error");
                    errorMessage = errorValue != null ? errorValue.toString() : "Tool execution failed";
                }
            }

            if (isSuccess) {
                log.info("Tool executed successfully: {}", request.getToolName());
                return ResponseEntity.ok(ToolExecutionResponse.builder()
                        .success(true)
                        .result(result)
                        .toolName(request.getToolName())
                        .build());
            } else {
                log.warn("Tool execution returned error: {} - {}", request.getToolName(), errorMessage);
                return ResponseEntity.ok(ToolExecutionResponse.builder()
                        .success(false)
                        .error(errorMessage)
                        .result(result)
                        .toolName(request.getToolName())
                        .build());
            }

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
     * GET /mcp/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "running");
        status.put("type", "Multi-Provider MCP Server");
        status.put("version", "2.0.0");

        // Add provider statistics
        Map<String, Object> stats = mcpServerService.getStatistics();
        status.putAll(stats);

        return ResponseEntity.ok(status);
    }

    /**
     * Get list of all registered providers
     *
     * GET /mcp/providers
     */
    @GetMapping("/providers")
    public ResponseEntity<Map<String, Object>> getProviders() {
        try {
            log.info("Received request to list providers");
            List<String> providers = mcpServerService.listProviders();
            Map<String, Object> stats = mcpServerService.getStatistics();

            Map<String, Object> response = new HashMap<>();
            response.put("providers", providers);
            response.put("statistics", stats);

            log.info("Returning {} providers", providers.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error listing providers", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get tools from a specific provider
     *
     * GET /mcp/providers/{providerName}/tools
     */
    @GetMapping("/providers/{providerName}/tools")
    public ResponseEntity<Map<String, Object>> getToolsByProvider(@PathVariable String providerName) {
        try {
            log.info("Received request to list tools for provider: {}", providerName);
            List<McpTool> tools = mcpServerService.listToolsByProvider(providerName);

            Map<String, Object> response = new HashMap<>();
            response.put("provider", providerName);
            response.put("tool_count", tools.size());
            response.put("tools", tools);

            log.info("Returning {} tools for provider: {}", tools.size(), providerName);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid provider: {}", providerName, e);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("Error listing tools for provider: {}", providerName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Ask Perplexity with MCP Tools
     * Perplexity kann dabei google-service und andere MCP-Tools nutzen
     *
     * POST /mcp/perplexity/ask
     */
    @PostMapping("/perplexity/ask")
    public ResponseEntity<PerplexityResponse> askPerplexity(@RequestBody PerplexityRequest request) {
        try {
            log.info("Received Perplexity request: {} (useTools: {})",
                    request.getQuery(), request.isUseTools());

            PerplexityResponse response = perplexityServiceClient.askWithTools(
                    request.getQuery(),
                    request.isUseTools()
            );

            log.info("Perplexity response received. Tools used: {}", response.getToolsUsed());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error asking Perplexity", e);
            return ResponseEntity.ok(PerplexityResponse.builder()
                    .answer("Error: " + e.getMessage())
                    .success(false)
                    .error(e.getMessage())
                    .build());
        }
    }

    /**
     * Search with Perplexity
     *
     * POST /mcp/perplexity/search
     */
    @PostMapping("/perplexity/search")
    public ResponseEntity<PerplexityResponse> searchPerplexity(@RequestBody Map<String, String> request) {
        try {
            String query = request.get("query");
            log.info("Received Perplexity search request: {}", query);

            PerplexityResponse response = perplexityServiceClient.search(query);

            log.info("Perplexity search response received");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error searching with Perplexity", e);
            return ResponseEntity.ok(PerplexityResponse.builder()
                    .answer("Error: " + e.getMessage())
                    .success(false)
                    .error(e.getMessage())
                    .build());
        }
    }
}


