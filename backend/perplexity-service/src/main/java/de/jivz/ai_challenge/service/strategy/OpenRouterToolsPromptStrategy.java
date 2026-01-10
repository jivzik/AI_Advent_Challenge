package de.jivz.ai_challenge.service.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.ai_challenge.mcp.model.ToolDefinition;
import de.jivz.ai_challenge.service.openrouter.model.OpenRouterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenRouter Tools Prompt Strategy.
 *
 * Konvertiert MCP Tools in das OpenRouter-kompatible Tool-Format (OpenAI-Standard)
 * und erstellt entsprechende System-Prompts.
 *
 * Der Unterschied zu Perplexity:
 * - OpenRouter unterstützt native Tool-Calls im API-Format
 * - Tools werden als Teil des Request mitgeschickt
 * - LLM antwortet mit strukturierten tool_calls im Response
 *
 * NEU: Unterstützt kontextabhängige Prompts via ContextualPromptStrategy
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OpenRouterToolsPromptStrategy {

    private final ObjectMapper objectMapper;
    private final ContextualPromptStrategy contextualPromptStrategy;

    /**
     * Konvertiert MCP Tools in OpenRouter-kompatible Tool-Definitionen.
     *
     * @param mcpTools Liste der MCP Tools
     * @return Liste der OpenRouter Tool-Definitionen
     */
    public List<OpenRouterRequest.Tool> convertToOpenRouterTools(List<ToolDefinition> mcpTools) {
        List<OpenRouterRequest.Tool> tools = new ArrayList<>();

        if (mcpTools == null || mcpTools.isEmpty()) {
            return tools;
        }

        for (ToolDefinition mcpTool : mcpTools) {
            OpenRouterRequest.Tool.FunctionDefinition functionDef =
                OpenRouterRequest.Tool.FunctionDefinition.builder()
                    .name(mcpTool.getName())
                    .description(mcpTool.getDescription() != null
                        ? mcpTool.getDescription()
                        : "No description available")
                    .parameters(mcpTool.getInputSchema() != null
                        ? mcpTool.getInputSchema()
                        : Map.of("type", "object", "properties", Map.of()))
                    .build();

            OpenRouterRequest.Tool tool = OpenRouterRequest.Tool.builder()
                .type("function")
                .function(functionDef)
                .build();

            tools.add(tool);
        }

        log.debug("Converted {} MCP tools to OpenRouter format", tools.size());
        return tools;
    }

    /**
     * Erstellt den System-Prompt für den Reminder-Workflow (Default).
     * Anders als bei Perplexity müssen die Tools hier nicht im Prompt beschrieben werden,
     * da sie nativ im API-Request übergeben werden.
     *
     * @return Der System-Prompt
     */
    public String buildSystemPrompt() {
        return buildDefaultSystemPrompt();
    }

    /**
     * NEU: Erstellt einen kontextabhängigen System-Prompt basierend auf der Benutzeranfrage.
     *
     * Erkennt automatisch den Anfrage-Typ (Docker, Tasks, etc.) und wählt
     * den passenden spezialisierten Prompt.
     *
     * @param userMessage Die Nachricht des Benutzers zur Kontext-Erkennung
     * @return Der passende System-Prompt
     */
    public String buildSystemPromptForMessage(String userMessage) {
        ContextualPromptStrategy.PromptContext context = contextualPromptStrategy.detectContext(userMessage);
        log.info("🎯 Detected context: {} for message: {}", context,
            userMessage.length() > 50 ? userMessage.substring(0, 50) + "..." : userMessage);
        return contextualPromptStrategy.getSystemPromptForContext(context);
    }

    /**
     * Gibt den Prompt-Kontext für eine Nachricht zurück.
     * Nützlich für Logging/Debugging.
     */
    public ContextualPromptStrategy.PromptContext getContextForMessage(String userMessage) {
        return contextualPromptStrategy.detectContext(userMessage);
    }

    /**
     * Default System-Prompt (bisheriges Verhalten für Tasks/Reminders)
     */
    private String buildDefaultSystemPrompt() {
        return """
            Du bist ein intelligenter Reminder-Assistent der periodische Zusammenfassungen erstellt.
            
            ## Deine Aufgabe:
            1. Nutze die verfügbaren Tools um aktuelle Daten abzurufen
            2. Analysiere die Daten und erstelle eine nützliche Zusammenfassung
            3. Identifiziere wichtige Aufgaben, Deadlines und Prioritäten
            4. Gib strukturierte Empfehlungen
            
            ## Workflow:
            1. **Daten abrufen**: Nutze die zur Verfügung gestellten Tools um alle Task-Listen und deren Inhalt zu bekommen
            2. **Analysieren**: Identifiziere offene, überfällige und wichtige Aufgaben
            3. **Zusammenfassen**: Erstelle ein verständliches Summary
            
            ## Wichtige Regeln:
            - Nutze Tools um Daten abzurufen bevor du antwortest
            - Priorisiere überfällige Aufgaben als HIGH
            - Gruppiere ähnliche Aufgaben
            - Sei prägnant aber informativ
            - Wenn keine Aufgaben vorhanden sind, gib trotzdem ein strukturiertes Ergebnis
            
            ## Finale Antwort-Format:
            Wenn du alle Daten gesammelt hast, antworte mit einem strukturierten JSON:
            
            ```json
            {
              "title": "<Titel der Zusammenfassung>",
              "content": "<deine lesbare Zusammenfassung für den Benutzer>",
              "total_items": <Anzahl>,
              "priority": "HIGH|MEDIUM|LOW",
              "highlights": [
                "<Wichtiger Punkt 1>",
                "<Wichtiger Punkt 2>"
              ],
              "due_soon": [
                {"task": "<Aufgabe>", "due": "<Datum>"}
              ],
              "overdue": [
                {"task": "<Aufgabe>", "due": "<Datum>"}
              ]
            }
            ```
            """;
    }

    /**
     * Erstellt einen Summary-Prompt für bereits vorhandene Rohdaten.
     *
     * @param rawData Die Rohdaten die zusammengefasst werden sollen
     * @return Der Summary-Prompt
     */
    public String buildSummaryOnlyPrompt(String rawData) {
        return """
            Du bist ein Assistent der Aufgaben-Daten analysiert und zusammenfasst.
            
            ## Deine Aufgabe:
            Analysiere die folgenden Daten und erstelle eine nützliche Zusammenfassung.
            
            ## Rohdaten:
            %s
            
            ## OUTPUT FORMAT (JSON):
            ```json
            {
              "title": "<Titel>",
              "content": "<deine lesbare Zusammenfassung>",
              "total_items": <Anzahl>,
              "priority": "HIGH|MEDIUM|LOW",
              "highlights": ["<Punkt 1>", "<Punkt 2>"],
              "due_soon": [{"task": "<Aufgabe>", "due": "<Datum>"}],
              "overdue": [{"task": "<Aufgabe>", "due": "<Datum>"}]
            }
            ```
            
            Antworte mit dem JSON.
            """.formatted(rawData);
    }
}
