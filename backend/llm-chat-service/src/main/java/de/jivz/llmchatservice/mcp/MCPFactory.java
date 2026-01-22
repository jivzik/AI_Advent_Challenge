package de.jivz.llmchatservice.mcp;



import de.jivz.llmchatservice.mcp.model.MCPToolResult;
import de.jivz.llmchatservice.mcp.model.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory für MCP Services.
 * Routet Tool-Aufrufe zum richtigen MCP Server.
 */
@Component
@Slf4j
public class MCPFactory {

    private final Map<String, MCPService> serviceMap;

    @Autowired
    public MCPFactory(Optional<List<MCPService>> mcpServices) {
        List<MCPService> services = mcpServices.orElse(List.of());
        log.info("Initializing MCPFactory with {} services", services.size());

        this.serviceMap = services.stream()
                .peek(service -> log.info("Registering MCP service: {}", service.getServerName()))
                .collect(Collectors.toMap(
                        MCPService::getServerName,
                        Function.identity()
                ));

        log.info("MCPFactory initialized with servers: {}", serviceMap.keySet());
    }

    /**
     * Routet einen Tool-Aufruf zum richtigen MCP Server.
     *
     * @param fullToolName Format "server:tool" (z.B. "google:tasks_list")
     * @param params Parameter für das Tool
     * @return Ergebnis der Ausführung
     */
    public MCPToolResult route(String fullToolName, Map<String, Object> params) {
        log.info("Routing tool call: {} with params: {}", fullToolName, params);

        String[] parts = fullToolName.split(":", 2);

        if (parts.length != 2) {
            return MCPToolResult.builder()
                    .success(false)
                    .error("Invalid tool name format. Expected 'server:tool', got: " + fullToolName)
                    .toolName(fullToolName)
                    .timestamp(System.currentTimeMillis())
                    .build();
        }

        String serverName = parts[0];
        String toolName = parts[1];

        MCPService service = serviceMap.get(serverName);

        if (service == null) {
            log.error("MCP server not found: {}", serverName);
            return MCPToolResult.builder()
                    .success(false)
                    .error("MCP server not found: " + serverName + ". Available: " + serviceMap.keySet())
                    .toolName(fullToolName)
                    .timestamp(System.currentTimeMillis())
                    .build();
        }

        return service.execute(toolName, params);
    }

    /**
     * Gibt alle verfügbaren Tool-Definitionen von allen Servern zurück.
     */
    public List<ToolDefinition> getAllToolDefinitions() {
        return serviceMap.values().stream()
                .flatMap(service -> service.getToolDefinitions().stream())
                .collect(Collectors.toList());
    }

    /**
     * Gibt die Liste der registrierten Server zurück.
     */
    public Set<String> getRegisteredServers() {
        return serviceMap.keySet();
    }

    /**
     * Prüft ob Tools verfügbar sind.
     */
    public boolean hasTools() {
        return !serviceMap.isEmpty();
    }
}