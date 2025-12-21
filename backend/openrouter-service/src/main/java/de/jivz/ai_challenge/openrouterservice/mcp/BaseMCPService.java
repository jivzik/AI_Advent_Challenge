package de.jivz.ai_challenge.openrouterservice.mcp;

import de.jivz.ai_challenge.openrouterservice.mcp.model.MCPExecuteRequest;
import de.jivz.ai_challenge.openrouterservice.mcp.model.MCPToolResult;
import de.jivz.ai_challenge.openrouterservice.mcp.model.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstrakte Basisklasse für MCP Service Implementierungen.
 * Enthält gemeinsame Logik für WebClient-basierte MCP-Kommunikation.
 */
@Slf4j
public abstract class BaseMCPService implements MCPService {

    protected final WebClient webClient;
    protected final String serverName;
    private List<ToolDefinition> cachedToolDefinitions;

    protected BaseMCPService(WebClient webClient, String serverName) {
        this.webClient = webClient;
        this.serverName = serverName;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public MCPToolResult execute(String toolName, Map<String, Object> params) {
        log.info("Executing tool: {}:{} with params: {}", serverName, toolName, params);

        try {
            MCPExecuteRequest request = MCPExecuteRequest.builder()
                    .toolName(toolName)
                    .arguments(params)
                    .build();

            MCPToolResult result = webClient.post()
                    .uri("/api/tools/execute")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(BodyInserters.fromValue(request))
                    .retrieve()
                    .bodyToMono(MCPToolResult.class)
                    .block();

            log.info("Tool execution result: success={}", result != null && result.isSuccess());
            return result;

        } catch (Exception e) {
            log.error("Error executing tool {}:{}", serverName, toolName, e);
            return MCPToolResult.builder()
                    .success(false)
                    .error("Failed to execute " + serverName + ":" + toolName + " - " + e.getMessage())
                    .toolName(serverName + ":" + toolName)
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }

    @Override
    public List<ToolDefinition> getToolDefinitions() {
        if (cachedToolDefinitions != null) {
            return cachedToolDefinitions;
        }

        log.info("Fetching tool definitions from server: {}", serverName);

        try {
            List<Map<String, Object>> remoteTools = webClient.get()
                    .uri("/api/tools")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .collectList()
                    .block();

            if (remoteTools == null || remoteTools.isEmpty()) {
                log.warn("No tools found for server: {}", serverName);
                return List.of();
            }

            cachedToolDefinitions = remoteTools.stream()
                    .map(tool -> ToolDefinition.builder()
                            .name(serverName + ":" + tool.get("name"))
                            .description((String) tool.get("description"))
                            .inputSchema(tool.get("inputSchema") instanceof Map
                                    ? (Map<String, Object>) tool.get("inputSchema")
                                    : Map.of())
                            .build())
                    .collect(Collectors.toList());

            log.info("Cached {} tool definitions for server: {}", cachedToolDefinitions.size(), serverName);
            return cachedToolDefinitions;

        } catch (Exception e) {
            log.error("Error fetching tool definitions from server: {}", serverName, e);
            return List.of();
        }
    }

    /**
     * Invalidiert den Tool-Cache (z.B. nach Server-Neustart).
     */
    public void invalidateCache() {
        cachedToolDefinitions = null;
        log.info("Tool cache invalidated for server: {}", serverName);
    }
}

