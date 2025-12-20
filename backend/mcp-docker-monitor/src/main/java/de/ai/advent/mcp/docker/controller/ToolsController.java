package de.ai.advent.mcp.docker.controller;

import de.ai.advent.mcp.docker.model.PerplexityFunctionDefinition;
import de.ai.advent.mcp.docker.model.ToolCallRequest;
import de.ai.advent.mcp.docker.model.ToolCallResponse;
import de.ai.advent.mcp.docker.service.DockerService;
import de.ai.advent.mcp.docker.service.ToolsDefinitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Docker monitoring tools
 *
 * Provides endpoints for:
 * 1. Listing available tools in Perplexity format
 * 2. Calling tools with parameters
 * 3. Health checks
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class ToolsController {

    private final DockerService dockerService;
    private final ToolsDefinitionService toolsDefinitionService;

    public ToolsController(DockerService dockerService, ToolsDefinitionService toolsDefinitionService) {
        this.dockerService = dockerService;
        this.toolsDefinitionService = toolsDefinitionService;
    }

    /**
     * GET /api/tools/list
     * Returns available tools in Perplexity function calling format
     */
    @GetMapping("/tools")
    public ResponseEntity<List<PerplexityFunctionDefinition>> listTools() {
        log.info("Listing available tools");
        List<PerplexityFunctionDefinition> tools = toolsDefinitionService.getToolDefinitions();
        return ResponseEntity.ok(tools);
    }

    /**
     * POST /api/tools/call
     * Executes a tool with given arguments
     */
    @PostMapping("/tools/execute")
    public ResponseEntity<ToolCallResponse> callTool(@RequestBody ToolCallRequest request) {
        log.info("Calling tool: {} with arguments: {}", request.getToolName(), request.getArguments());

        try {
            Object result = executeTool(request.getToolName(), request.getArguments());
            log.info("Tool {} executed successfully", request.getToolName());
            ToolCallResponse response = new ToolCallResponse(true, result, request.getToolName());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error executing tool: {}", request.getToolName(), e);
            ToolCallResponse response = new ToolCallResponse(
                    false,
                    e.getMessage(),
                    request.getToolName()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * GET /api/health
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = Map.of(
            "status", "UP",
            "message", "Docker Monitor service is running",
            "timestamp", System.currentTimeMillis()
        );
        return ResponseEntity.ok(health);
    }

    /**
     * Executes tool based on name and arguments
     */
    private Object executeTool(String toolName, Map<String, Object> arguments) throws Exception {
        return switch (toolName) {
            case "list_containers" ->
                dockerService.listContainers();

            case "get_container_logs" -> {
                String containerId = getArgument(arguments, "container_name", String.class);
                int tail = getArgument(arguments, "tail", Integer.class, 100);
                yield dockerService.getContainerLogs(containerId, tail, null);
            }

            case "get_container_logs_since" -> {
                String containerId = getArgument(arguments, "container_name", String.class);
                int tail = getArgument(arguments, "tail", Integer.class, 100);
                String since = getArgument(arguments, "since", String.class, "");
                yield dockerService.getContainerLogs(containerId, tail, since);
            }

            case "check_container_health" -> {
                String containerId = getArgument(arguments, "container_name", String.class);
                yield dockerService.checkContainerHealth(containerId);
            }

            case "summarize_all" ->
                dockerService.summarizeAll();

            default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
        };
    }

    /**
     * Helper method to extract typed argument from map
     */
    @SuppressWarnings("unchecked")
    private <T> T getArgument(Map<String, Object> arguments, String key, Class<T> type) throws Exception {
        Object value = arguments.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required argument: " + key);
        }
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("Invalid type for argument " + key + ": expected " +
                type.getSimpleName() + ", got " + value.getClass().getSimpleName());
        }
        return (T) value;
    }

    /**
     * Helper method to extract typed argument with default value
     */
    @SuppressWarnings("unchecked")
    private <T> T getArgument(Map<String, Object> arguments, String key, Class<T> type, T defaultValue) {
        Object value = arguments.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (!type.isInstance(value)) {
            log.warn("Invalid type for argument {}: expected {}, got {}",
                key, type.getSimpleName(), value.getClass().getSimpleName());
            return defaultValue;
        }
        return (T) value;
    }
}

