package de.jivz.rag.mcp;

import de.jivz.rag.mcp.McpModels.ToolCallResponse;
import de.jivz.rag.mcp.tools.Tool;
import de.jivz.rag.mcp.tools.ToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Сервис выполнения MCP инструментов.
 *
 * Единственная ответственность:
 * Координация выполнения — логирование и делегирование.
 *
 * Вся логика инструментов инкапсулирована в классах Tool.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ToolExecutorService {

    private final ToolRegistry toolRegistry;

    /**
     * Выполняет MCP инструмент по имени.
     */
    public ToolCallResponse execute(String toolName, Map<String, Object> arguments) {
        log.info("Executing tool: {} with args: {}", toolName, arguments);

        return toolRegistry.find(toolName)
                .map(tool -> executeToolSafely(tool, arguments))
                .orElseGet(() -> errorResponse("Unknown tool: " + toolName));
    }

    private ToolCallResponse executeToolSafely(Tool tool, Map<String, Object> arguments) {
        try {
            Object result = tool.execute(arguments);
            log.info("Tool {} executed successfully", tool.getName());
            return successResponse(result);
        } catch (Exception e) {
            log.error("Tool {} failed: {}", tool.getName(), e.getMessage());
            return errorResponse(e.getMessage());
        }
    }

    private ToolCallResponse successResponse(Object result) {
        return ToolCallResponse.builder()
                .success(true)
                .result(result)
                .build();
    }

    private ToolCallResponse errorResponse(String error) {
        return ToolCallResponse.builder()
                .success(false)
                .error(error)
                .build();
    }
}