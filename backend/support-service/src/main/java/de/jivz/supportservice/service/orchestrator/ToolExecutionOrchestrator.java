package de.jivz.supportservice.service.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.jivz.supportservice.dto.Message;
import de.jivz.supportservice.dto.ToolResponse;
import de.jivz.supportservice.mcp.MCPFactory;
import de.jivz.supportservice.mcp.model.MCPToolResult;
import de.jivz.supportservice.service.client.OpenRouterApiClient;
import de.jivz.supportservice.service.parser.ResponseParsingService;
import de.jivz.supportservice.service.source.SourceExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orchestrator für den Tool-Execution-Loop.
 * Koordiniert den iterativen Prozess: LLM → Tools → LLM → ... → Final Answer
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ToolExecutionOrchestrator {

    private static final int MAX_TOOL_ITERATIONS = 10;
    private static final String STEP_TOOL = "tool";
    private static final String STEP_FINAL = "final";
    private static final String RAG_SEARCH_TOOL = "rag:search_documents";

    private final OpenRouterApiClient apiClient;
    private final ResponseParsingService parsingService;
    private final MCPFactory mcpFactory;
    private final SourceExtractionService sourceExtractionService;
    private final ObjectMapper objectMapper;

    /**
     * Führt den Tool-Execution-Loop aus.
     *
     * @param messages Die initialen Nachrichten
     * @param temperature Die Temperatur für LLM-Aufrufe
     * @return Die finale Antwort
     */
    public String executeToolLoop(List<Message> messages, Double temperature) {
        int iteration = 0;
        Set<String> sources = new LinkedHashSet<>();

        while (iteration < MAX_TOOL_ITERATIONS) {
            iteration++;
            log.info("🔄 Tool loop iteration: {}", iteration);

            // Schritt 1: OpenRouter aufrufen
            String openRouterResponse = apiClient.sendChatRequest(messages, temperature, null);
            log.debug("📥 OpenRouter raw response: {}", openRouterResponse);

            // Schritt 2: Response parsen
            ToolResponse parsed = parsingService.parseWithRetry(
                    openRouterResponse, messages, temperature);

            if (parsed == null) {
                log.error("❌ Failed to parse OpenRouter response after retries");
                return "Entschuldigung, beim Verarbeiten der Antwort ist ein Fehler aufgetreten. Bitte versuchen Sie es erneut.";
            }

            // Schritt 3: Step prüfen
            if (STEP_FINAL.equals(parsed.getStep())) {
                log.info("✅ Got final answer after {} iteration(s)", iteration);
                return formatFinalAnswer(parsed.getAnswer(), sources);
            }

            // Schritt 4: Tools ausführen
            if (STEP_TOOL.equals(parsed.getStep()) && hasToolCalls(parsed)) {
                executeTools(parsed, messages, sources);
            } else {
                log.warn("⚠️ Unknown step or empty tool_calls, treating as final");
                return formatFinalAnswer(parsed.getAnswer() != null ?
                        parsed.getAnswer() : openRouterResponse, sources);
            }
        }

        log.error("❌ Max iterations ({}) reached in tool loop", MAX_TOOL_ITERATIONS);
        return "Entschuldigung, die maximale Anzahl an Iterationen wurde überschritten. Bitte formulieren Sie Ihre Anfrage um.";
    }

    /**
     * Führt alle Tool-Calls aus und fügt die Ergebnisse zu den Nachrichten hinzu.
     */
    private void executeTools(ToolResponse parsed, List<Message> messages, Set<String> sources) {
        // Antwort des Modells als assistant hinzufügen
        messages.add(new Message("assistant", objectMapper.valueToTree(parsed).toString()));

        StringBuilder allToolResults = new StringBuilder();
        allToolResults.append("Tool execution results:\n\n");

        for (ToolResponse.ToolCall toolCall : parsed.getToolCalls()) {
            String toolResult = executeSingleTool(toolCall);

            // Quellen aus RAG-Ergebnissen extrahieren
            if (RAG_SEARCH_TOOL.equals(toolCall.getName())) {
                sourceExtractionService.extractSourcesFromRagResult(toolResult, sources);
            }

            allToolResults.append(String.format("TOOL_RESULT %s:\n%s\n\n",
                    toolCall.getName(), toolResult));
            log.info("📨 Executed tool: {}", toolCall.getName());
        }

        // Ergebnisse als user-Nachricht hinzufügen
        messages.add(new Message("user", allToolResults.toString().trim()));
        log.info("📨 Added tool results as user message");
    }

    /**
     * Führt ein einzelnes MCP-Tool aus.
     */
    private String executeSingleTool(ToolResponse.ToolCall toolCall) {
        log.info("🔧 Executing MCP tool: {} with args: {}",
                toolCall.getName(), toolCall.getArguments());

        try {
            MCPToolResult result = mcpFactory.route(
                    toolCall.getName(),
                    toolCall.getArguments() != null ? toolCall.getArguments() : Map.of()
            );

            if (result.isSuccess()) {
                log.info("✅ MCP tool {} executed successfully", toolCall.getName());
                return objectMapper.writeValueAsString(result.getResult());
            } else {
                log.warn("⚠️ MCP tool {} returned error: {}",
                        toolCall.getName(), result.getError());
                return String.format("ERROR: %s", result.getError());
            }

        } catch (Exception e) {
            log.error("❌ Error executing MCP tool {}: {}",
                    toolCall.getName(), e.getMessage());
            return String.format("ERROR: %s", e.getMessage());
        }
    }

    /**
     * Formatiert die finale Antwort mit Quellen.
     */
    private String formatFinalAnswer(String answer, Set<String> sources) {
        if (answer == null) {
            answer = "";
        }

        if (!sources.isEmpty()) {
            return sourceExtractionService.appendSources(answer, sources);
        }

        return answer;
    }

    /**
     * Prüft ob Tool-Calls vorhanden sind.
     */
    private boolean hasToolCalls(ToolResponse parsed) {
        return parsed.getToolCalls() != null && !parsed.getToolCalls().isEmpty();
    }
}

