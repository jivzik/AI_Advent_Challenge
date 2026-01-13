package de.jivz.ai_challenge.mcp;

import de.jivz.ai_challenge.exception.MCPExecutionException;
import de.jivz.ai_challenge.exception.MCPToolNotFoundException;
import de.jivz.ai_challenge.mcp.model.MCPExecuteRequest;
import de.jivz.ai_challenge.mcp.model.MCPToolResult;
import de.jivz.ai_challenge.mcp.model.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

            log.info("Tool execution result: {}", result);
            return result;

        } catch (Exception e) {
            log.error("Error executing tool {}:{}", serverName, toolName, e);
            throw new MCPExecutionException(
                    String.format("Failed to execute %s:%s", serverName, toolName),
                    e
            );
        }
    }

    @Override
    public List<ToolDefinition> getToolDefinitions() {
        if (cachedToolDefinitions != null) {
            return cachedToolDefinitions;
        }

        log.info("Executing tool: {} for getting tools list", serverName);

        try {
            List<Map<String, Object>> remoteTools = webClient.get()
                    .uri("/api/tools")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .collectList()
                    .block();

            cachedToolDefinitions = remoteTools.stream()
                    .map(tool -> {
                        String toolName = (String) tool.get("name");

                        // Убираем любой префикс (все после последнего ":")
                        String cleanToolName = toolName.contains(":")
                                ? toolName.substring(toolName.lastIndexOf(":") + 1)
                                : toolName;

                        // Добавляем наш префикс
                        String finalName = serverName + ":" + cleanToolName;

                        return ToolDefinition.builder()
                                .name(finalName)
                                .description((String) tool.get("description"))
                                .inputSchema((Map<String, Object>) tool.get("inputSchema"))
                                .build();
                    })
                    .collect(Collectors.toList());

            log.info("Loaded {} tools from {}", cachedToolDefinitions.size(), serverName);
            return cachedToolDefinitions;

        } catch (Exception e) {
            log.warn("Failed to fetch tool definitions from server '{}': {} - Server may be temporarily unavailable",
                    serverName, e.getMessage());
            return List.of();
        }
    }

    /**
     * Метод для инвалидации кэша (если нужно обновить список инструментов)
     */
    public void refreshToolDefinitions() {
        cachedToolDefinitions = null;
    }

    /**
     * Валидирует, что данный инструмент поддерживается этим сервером
     */
/*    protected void validateTool(String toolName) {
        boolean supported = getToolDefinitions().stream()
                .anyMatch(def -> def.getName().endsWith(":" + toolName));

        if (!supported) {
            throw new MCPToolNotFoundException(serverName + ":" + toolName);
        }
    }*/
}