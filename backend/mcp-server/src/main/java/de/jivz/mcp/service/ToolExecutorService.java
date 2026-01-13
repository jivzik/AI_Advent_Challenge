package de.jivz.mcp.service;

import de.jivz.mcp.model.ToolCallResponse;
import de.jivz.mcp.tools.ToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service für Tool-Ausführung.
 * Single Responsibility Principle: Nur verantwortlich für Koordination der Tool-Ausführung.
 * Open/Closed Principle: Geschlossen für Modifikation, offen für Erweiterung.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ToolExecutorService {

    private final ToolRegistry toolRegistry;

    /**
     * Führt ein MCP Tool nach Name aus.
     */
    public ToolCallResponse execute(String toolName, Map<String, Object> arguments) {
        log.info("Tool ausführen: {} mit Argumenten: {}", toolName, arguments);

        return toolRegistry.find(toolName)
                .map(tool -> executeToolSafely(tool, arguments))
                .orElseGet(() -> errorResponse("Unbekanntes Tool: " + toolName));
    }

    private ToolCallResponse executeToolSafely(de.jivz.mcp.tools.Tool tool, Map<String, Object> arguments) {
        try {
            Object result = tool.execute(arguments);
            log.info("Tool {} erfolgreich ausgeführt", tool.getName());
            return successResponse(result);
        } catch (Exception e) {
            log.error("Tool {} fehlgeschlagen: {}", tool.getName(), e.getMessage());
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

