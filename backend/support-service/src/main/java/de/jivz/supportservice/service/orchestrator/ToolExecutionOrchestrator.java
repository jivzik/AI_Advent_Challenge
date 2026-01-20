package de.jivz.supportservice.service.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.jivz.supportservice.dto.Message;
import de.jivz.supportservice.dto.ToolResponse;
import de.jivz.supportservice.mcp.MCPFactory;
import de.jivz.supportservice.mcp.model.MCPToolResult;
import de.jivz.supportservice.service.client.OpenRouterApiClient;
import de.jivz.supportservice.service.client.OllamaApiClient;
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
    private static final String PROVIDER_LOCAL = "local";
    private static final String PROVIDER_REMOTE = "remote";

    private final OpenRouterApiClient openRouterApiClient;
    private final OllamaApiClient ollamaApiClient;
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
        return executeToolLoop(messages, temperature, PROVIDER_REMOTE);
    }

    /**
     * Führt den Tool-Execution-Loop aus mit spezifischem LLM Provider.
     *
     * @param messages Die initialen Nachrichten
     * @param temperature Die Temperatur für LLM-Aufrufe
     * @param llmProvider "local" für Ollama oder "remote" für OpenRouter
     * @return Die finale Antwort
     */
    public String executeToolLoop(List<Message> messages, Double temperature, String llmProvider) {
        int iteration = 0;
        Set<String> sources = new LinkedHashSet<>();

        // Provider validieren
        String provider = (llmProvider != null && PROVIDER_LOCAL.equals(llmProvider))
                ? PROVIDER_LOCAL
                : PROVIDER_REMOTE;

        log.info("🚀 Starting tool loop with provider: {}", provider);

        // Für schwächere LLMs: Klarere initiale Anweisung hinzufügen
        addGuidanceForWeakLLMs(messages, provider);

        while (iteration < MAX_TOOL_ITERATIONS) {
            iteration++;
            log.info("🔄 Tool loop iteration: {} (provider: {})", iteration, provider);

            // Schritt 1: LLM aufrufen
            String llmResponse = callLlm(messages, temperature, provider);
            log.debug("📥 LLM raw response: {}", llmResponse);

            // Schritt 2: Response parsen
            ToolResponse parsed = parsingService.parseWithRetry(
                    llmResponse, messages, temperature);

            if (parsed == null) {
                log.error("❌ Failed to parse LLM response after retries");
                // Für schwächere LLMs: Erneuter Versuch mit expliziter Anleitung
                if (iteration == 1) {
                    log.info("💡 Attempting guided retry for weak LLM");
                    addExplicitFormatReminder(messages);
                    continue;
                }
                return "Entschuldigung, beim Verarbeiten der Antwort ist ein Fehler aufgetreten. Bitte versuchen Sie es erneut.";
            }

            // Schritt 3: Step prüfen und verarbeiten
            String step = parsed.getStep();
            boolean hasTools = hasToolCalls(parsed);

            log.debug("📊 Step: '{}', Tool-Calls: {}", step, hasTools ? parsed.getToolCalls().size() : 0);

            // Fall 1: Explizit "final" - sofort zurückgeben
            if (STEP_FINAL.equals(step)) {
                log.info("✅ Got final answer after {} iteration(s)", iteration);
                return formatFinalAnswer(parsed.getAnswer(), sources);
            }

            // Fall 2: "tool" oder Tool-Calls vorhanden - versuche Tools auszuführen
            if (STEP_TOOL.equals(step) || hasTools) {
                if (hasTools) {
                    log.info("🔧 Executing {} tool(s) (step: '{}')", parsed.getToolCalls().size(), step);
                    executeTools(parsed, messages, sources);

                    // Für schwächere LLMs: Erinnerung nach Tool-Ausführung
                    addToolResultsGuidance(messages, provider);
                    // Loop weitermachen für nächste Iteration
                    continue;
                } else {
                    // "tool" ohne tool_calls - als final behandeln
                    log.warn("⚠️ Step is '{}' but no valid tool_calls, treating as final", step);
                    return formatFinalAnswer(parsed.getAnswer() != null ? parsed.getAnswer() : llmResponse, sources);
                }
            }

            // Fall 3: Alle anderen Steps (ask_for_context, search_documents, etc.) - als final behandeln
            log.warn("🤷 Unknown step '{}' (LLM invented it), treating as final answer", step);
            return formatFinalAnswer(parsed.getAnswer() != null ? parsed.getAnswer() : llmResponse, sources);
        }

        log.error("❌ Max iterations ({}) reached in tool loop", MAX_TOOL_ITERATIONS);
        return "Entschuldigung, die maximale Anzahl an Iterationen wurde überschritten. Bitte formulieren Sie Ihre Anfrage um.";
    }

    /**
     * Fügt eine initiale Anleitung für schwächere LLMs hinzu.
     * Dies hilft besonders lokalen Modellen, das korrekte Format zu verstehen.
     */
    private void addGuidanceForWeakLLMs(List<Message> messages, String provider) {
        if (PROVIDER_LOCAL.equals(provider)) {
            // Lokale LLMs benötigen oft mehr Anleitung
            log.debug("💡 Adding extra guidance for local LLM");

            // Analysiere die letzte User-Nachricht für kontextspezifische Beispiele
            String lastUserMessage = getLastUserMessage(messages);
            if (lastUserMessage != null) {
                String contextualHint = buildContextualHint(lastUserMessage);
                if (contextualHint != null) {
                    messages.add(new Message("system", contextualHint));
                    log.debug("📝 Added contextual hint for local LLM");
                }
            }
        }
    }

    /**
     * Findet die letzte User-Nachricht in der Konversation.
     */
    private String getLastUserMessage(List<Message> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if ("user".equals(msg.getRole())) {
                return msg.getContent();
            }
        }
        return null;
    }

    /**
     * Erstellt einen kontextspezifischen Hinweis basierend auf der User-Anfrage.
     * Dies hilft schwächeren LLMs, die richtige Tool-Verwendung zu erkennen.
     */
    private String buildContextualHint(String userMessage) {
        String lower = userMessage.toLowerCase();

        // GitHub/Git-bezogene Anfragen
        if (lower.contains("issue") || lower.contains("github") || lower.contains("ticket")) {
            return "HINT: For GitHub/ticket operations, use tools like 'git:create_github_issue' or 'git:list_github_issues'. " +
                   "Example response: {\"step\":\"tool\",\"tool_calls\":[{\"name\":\"git:create_github_issue\",\"arguments\":{\"title\":\"...\",\"body\":\"...\"}}],\"answer\":\"\"}";
        }

        // Dokumenten-Suche
        if (lower.contains("search") || lower.contains("find") || lower.contains("dokumentation") ||
            lower.contains("documentation") || lower.contains("information about")) {
            return "HINT: For document search, use 'rag:search_documents'. " +
                   "Example: {\"step\":\"tool\",\"tool_calls\":[{\"name\":\"rag:search_documents\",\"arguments\":{\"query\":\"search term\"}}],\"answer\":\"\"}";
        }

        // Google Tasks/Calendar
        if (lower.contains("task") || lower.contains("calendar") || lower.contains("termin") ||
            lower.contains("aufgabe") || lower.contains("event")) {
            return "HINT: For tasks/calendar, use tools like 'google:tasks_list', 'google:calendar_list', 'google:tasks_create'. " +
                   "Example: {\"step\":\"tool\",\"tool_calls\":[{\"name\":\"google:tasks_list\",\"arguments\":{}}],\"answer\":\"\"}";
        }

        // Einfache Fragen - kein Tool nötig
        if (lower.contains("what is") || lower.contains("explain") || lower.contains("was ist") ||
            lower.contains("erkläre")) {
            return "HINT: For general questions, you can answer directly without tools. " +
                   "Example: {\"step\":\"final\",\"tool_calls\":[],\"answer\":\"Your answer here\"}";
        }

        return null;
    }

    /**
     * Fügt eine explizite Format-Erinnerung hinzu, wenn das Parsing fehlschlägt.
     */
    private void addExplicitFormatReminder(List<Message> messages) {
        String reminder = "IMPORTANT: You must respond with ONLY a JSON object. " +
                "Example: {\"step\":\"final\",\"tool_calls\":[],\"answer\":\"Your response here\"}\n" +
                "Do NOT use markdown blocks, do NOT add extra text. Just pure JSON.";
        messages.add(new Message("user", reminder));
        log.debug("📝 Added explicit format reminder");
    }

    /**
     * Fügt Anleitung nach Tool-Ausführung hinzu.
     */
    private void addToolResultsGuidance(List<Message> messages, String provider) {
        if (PROVIDER_LOCAL.equals(provider)) {
            // Lokale LLMs brauchen explizite Erinnerung, die Tool-Ergebnisse zu verarbeiten
            String guidance = "\nNow analyze the tool results above and provide your final answer. " +
                    "Format: {\"step\":\"final\",\"tool_calls\":[],\"answer\":\"Your complete answer based on the tool results\"}";
            Message lastMessage = messages.get(messages.size() - 1);
            if ("user".equals(lastMessage.getRole())) {
                lastMessage.setContent(lastMessage.getContent() + guidance);
                log.debug("💡 Added tool results guidance for local LLM");
            }
        }
    }

    /**
     * Ruft den entsprechenden LLM-Client basierend auf dem Provider auf.
     */
    private String callLlm(List<Message> messages, Double temperature, String provider) {
        if (PROVIDER_LOCAL.equals(provider)) {
            log.debug("🤖 Calling local Ollama LLM");
            return ollamaApiClient.sendChatRequest(messages, temperature, null);
        } else {
            log.debug("☁️ Calling remote OpenRouter LLM");
            return openRouterApiClient.sendChatRequest(messages, temperature, null);
        }
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
    @SuppressWarnings("unchecked")
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

                // Bei GitHub Issue-Erstellung die Issue-Nummer und URL speichern
                if ("git:create_github_issue".equals(toolCall.getName()) && result.getResult() != null) {
                    if (result.getResult() instanceof Map) {
                        Map<String, Object> resultMap = (Map<String, Object>) result.getResult();
                        Object issueNumber = resultMap.get("number");
                        Object issueUrl = resultMap.get("url");
                        if (issueNumber != null) {
                            // GitHub Issue als Ticket-Nummer speichern (Format: GH-123)
                            String ticketNumber = "GH-" + issueNumber;
                            ThreadLocalTicketContext.setTicketNumber(ticketNumber);
                            ThreadLocalTicketContext.setGitHubIssueUrl(issueUrl != null ? issueUrl.toString() : null);
                            log.info("🎫 Stored created GitHub issue: {} ({})", ticketNumber, issueUrl);
                        }
                    }
                }

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

