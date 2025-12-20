package de.jivz.ai_challenge.mcp;

import de.jivz.ai_challenge.exception.MCPServerNotFoundException;
import de.jivz.ai_challenge.mcp.model.MCPToolResult;
import de.jivz.ai_challenge.mcp.model.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MCPFactory {

    private final Map<String, MCPService> serviceMap;

    @Autowired
    public MCPFactory(List<MCPService> mcpServices) {
        log.info("Initializing MCPFactory with {} services", mcpServices.size());

        this.serviceMap = mcpServices.stream()
                .peek(service -> log.info("Registering MCP service: {}", service.getServerName()))
                .collect(Collectors.toMap(
                        MCPService::getServerName,
                        Function.identity()
                ));

        log.info("MCPFactory initialized with servers: {}", serviceMap.keySet());
    }

    /**
     * Роутит вызов инструмента к нужному MCP серверу
     *
     * @param fullToolName формат "server:tool" (например, "filesystem:read_file")
     * @param params параметры инструмента
     * @return результат выполнения
     */
    public MCPToolResult route(String fullToolName, Map<String, Object> params) {
        log.info("Routing tool call: {} with params: {}", fullToolName, params);

        String[] parts = fullToolName.split(":", 2);

        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "Invalid tool name format. Expected 'server:tool', got: " + fullToolName
            );
        }

        String serverName = parts[0];
        String toolName = parts[1];

        MCPService service = serviceMap.get(serverName);

        if (service == null) {
            throw new MCPServerNotFoundException(serverName);
        }

        return service.execute(toolName, params);
    }

    /**
     * Возвращает все доступные инструменты со всех серверов для передачи LLM
     */
    public List<ToolDefinition> getAllToolDefinitions() {
        return serviceMap.values().stream()
                .flatMap(service -> service.getToolDefinitions().stream())
                .collect(Collectors.toList());
    }

    /**
     * Возвращает список зарегистрированных серверов
     */
    public Set<String> getRegisteredServers() {
        return serviceMap.keySet();
    }
}
