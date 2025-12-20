package de.ai.advent.mcp.docker.service;

import de.ai.advent.mcp.docker.model.PerplexityFunctionDefinition;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for defining MCP tools in Perplexity format
 */
@Service
public class ToolsDefinitionService {

    /**
     * Returns all available tool definitions in Perplexity function calling format
     */
    public List<PerplexityFunctionDefinition> getToolDefinitions() {
        List<PerplexityFunctionDefinition> tools = new ArrayList<>();

        // Tool 1: list_containers - NO parameters
        tools.add(createTool(
            "list_containers",
            "List all Docker containers on the configured remote server. Returns container ID, name, image, status, and uptime.",
            Map.of(
                "properties", Map.of(),
                "required", List.of()
            )
        ));

        // Tool 2: get_container_logs
        tools.add(createTool(
            "get_container_logs",
            "Get logs from a specific Docker container. Returns the last N lines of container logs.",
            Map.of(
                "properties", Map.of(
                    "container_name", Map.of(
                        "type", "string",
                        "description", "Docker container ID or name"
                    ),
                    "tail", Map.of(
                        "type", "integer",
                        "description", "Number of log lines to return (default: 100)",
                        "default", 100
                    )
                ),
                "required", List.of("container_name")
            )
        ));

        // Tool 3: get_container_logs_since
        tools.add(createTool(
            "get_container_logs_since",
            "Get container logs since a specific time or with tail limit. Supports both parameters.",
            Map.of(
                "properties", Map.of(
                    "container_name", Map.of(
                        "type", "string",
                        "description", "Docker container ID or name"
                    ),
                    "tail", Map.of(
                        "type", "integer",
                        "description", "Number of log lines to return (default: 100)",
                        "default", 100
                    ),
                    "since", Map.of(
                        "type", "string",
                        "description", "Show logs since timestamp (e.g., '10m', '1h', '2024-01-19T10:00:00')"
                    )
                ),
                "required", List.of("container_name")
            )
        ));

        // Tool 4: check_container_health
        tools.add(createTool(
            "check_container_health",
            "Check the health status of a specific container. Returns status, restart count, recent errors, and uptime.",
            Map.of(
                "properties", Map.of(
                    "container_name", Map.of(
                        "type", "string",
                        "description", "Docker container ID or name"
                    )
                ),
                "required", List.of("container_name")
            )
        ));

        // Tool 5: summarize_all - NO parameters
        tools.add(createTool(
            "summarize_all",
            "Get a comprehensive summary of all containers on the server. Highlights problematic containers with errors or issues.",
            Map.of(
                "properties", Map.of(),
                "required", List.of()
            )
        ));

        return tools;
    }

    /**
     * Helper method to create a PerplexityFunctionDefinition
     */
    private PerplexityFunctionDefinition createTool(String name, String description, Map<String, Object> inputSchema) {
        return PerplexityFunctionDefinition.builder()
                .name(name)
                .description(description)
                .inputSchema(inputSchema)
                .build();
    }
}

