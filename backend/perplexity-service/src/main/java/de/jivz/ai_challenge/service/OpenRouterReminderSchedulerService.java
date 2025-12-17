package de.jivz.ai_challenge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.ai_challenge.dto.Message;
import de.jivz.ai_challenge.entity.ReminderSummary;
import de.jivz.ai_challenge.entity.ReminderSummary.Priority;
import de.jivz.ai_challenge.entity.ReminderSummary.SummaryType;
import de.jivz.ai_challenge.repository.ReminderSummaryRepository;
import de.jivz.ai_challenge.service.mcp.McpDto.McpTool;
import de.jivz.ai_challenge.service.mcp.McpDto.ToolExecutionResponse;
import de.jivz.ai_challenge.service.mcp.McpToolClient;
import de.jivz.ai_challenge.service.openrouter.OpenRouterToolClient;
import de.jivz.ai_challenge.service.openrouter.model.OpenRouterRequest;
import de.jivz.ai_challenge.service.openrouter.model.OpenRouterResponse;
import de.jivz.ai_challenge.service.openrouter.model.OpenRouterResponseWithMetrics;
import de.jivz.ai_challenge.service.strategy.OpenRouterToolsPromptStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenRouter Reminder Scheduler Service - Cron Job mit nativer Tool-Unterstützung.
 *
 * Im Unterschied zum Perplexity-basierten ReminderSchedulerService:
 * - Nutzt OpenRouter's native tool_calls Unterstützung
 * - Tools werden als Teil des API-Requests übergeben
 * - Keine JSON-Parsing-Tricks für Tool-Aufrufe nötig
 * - Unterstützt alle OpenRouter-kompatiblen Modelle (GPT-4, Claude, etc.)
 *
 * Konfiguration via application.properties:
 * - openrouter.reminder.scheduler.enabled=true
 * - openrouter.reminder.scheduler.cron=0 30 9 * * ?
 * - openrouter.reminder.scheduler.user-id=system
 * - openrouter.reminder.scheduler.model=openai/gpt-4-turbo
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OpenRouterReminderSchedulerService {

    private static final int MAX_TOOL_ITERATIONS = 10;
    private static final String FINISH_REASON_TOOL_CALLS = "tool_calls";
    private static final String FINISH_REASON_STOP = "stop";

    private final McpToolClient mcpToolClient;
    private final OpenRouterToolClient openRouterToolClient;
    private final ReminderSummaryRepository reminderRepository;
    private final OpenRouterToolsPromptStrategy promptStrategy;
    private final ObjectMapper objectMapper;

    @Value("${openrouter.reminder.scheduler.enabled:false}")
    private boolean schedulerEnabled;

    @Value("${openrouter.reminder.scheduler.user-id:system-openrouter}")
    private String defaultUserId;

    @Value("${openrouter.reminder.scheduler.temperature:0.3}")
    private double temperature;

    @Value("${openrouter.reminder.scheduler.model:#{null}}")
    private String model;

    /**
     * Haupt-Cron-Job: Läuft gemäß konfigurierter cron expression.
     *
     * Default: Jeden Tag um 9:30 Uhr (30 Min nach Perplexity-Job)
     * Kann in application.properties überschrieben werden:
     * openrouter.reminder.scheduler.cron=0 30 9 * * ?
     */
    @Scheduled(cron = "${openrouter.reminder.scheduler.cron:0 30 9 * * ?}")
    @Transactional
    public void scheduledReminderTask() {
        if (!schedulerEnabled) {
            log.debug("⏸️ OpenRouter reminder scheduler is disabled");
            return;
        }

        log.info("⏰ Starting OpenRouter scheduled reminder task at {}", LocalDateTime.now());

        try {
            ReminderSummary summary = executeReminderWorkflow(defaultUserId);

            if (summary != null) {
                log.info("✅ OpenRouter reminder task completed. Summary ID: {}, Title: {}",
                    summary.getId(), summary.getTitle());

                // Optional: Benachrichtigung triggern
                triggerNotification(summary);
            }

        } catch (Exception e) {
            log.error("❌ OpenRouter reminder task failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Manueller Trigger für den Reminder-Workflow.
     * Kann von einem Controller aufgerufen werden.
     *
     * @param userId Benutzer-ID für die Zusammenfassung
     * @return Die erstellte Zusammenfassung
     */
    @Transactional
    public ReminderSummary executeReminderWorkflow(String userId) {
        log.info("🚀 Executing OpenRouter reminder workflow for user: {}", userId);

        // 1. Aktuelle MCP Tools vom Backend holen
        List<McpTool> mcpTools = fetchCurrentTools();
        log.info("📋 Fetched {} MCP tools", mcpTools.size());

        // 2. Tools in OpenRouter-Format konvertieren
        List<OpenRouterRequest.Tool> openRouterTools = promptStrategy.convertToOpenRouterTools(mcpTools);
        log.debug("🔧 Converted to {} OpenRouter tools", openRouterTools.size());

        // 3. System-Prompt erstellen
        String systemPrompt = promptStrategy.buildSystemPrompt();
        log.debug("📝 Built system prompt ({} chars)", systemPrompt.length());

        // 4. Messages aufbauen
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", systemPrompt));
        messages.add(new Message("user",
            "Erstelle eine Zusammenfassung meiner aktuellen Aufgaben. " +
            "Identifiziere wichtige und überfällige Aufgaben. " +
            "Nutze die verfügbaren Tools um die Daten abzurufen."));

        // 5. Tool-Loop ausführen mit nativen Tool-Calls
        ToolLoopResult result = executeToolLoop(messages, openRouterTools);

        // 6. Zusammenfassung in DB speichern
        ReminderSummary summary = saveReminderSummary(userId, result);

        return summary;
    }

    /**
     * Holt die aktuelle Liste der MCP Tools.
     */
    private List<McpTool> fetchCurrentTools() {
        try {
            return mcpToolClient.getAllTools();
        } catch (Exception e) {
            log.warn("⚠️ Could not fetch MCP tools: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Der Tool-Loop mit nativer OpenRouter Tool-Unterstützung.
     * Führt iterativ Tools aus basierend auf finish_reason und tool_calls.
     */
    private ToolLoopResult executeToolLoop(List<Message> messages, List<OpenRouterRequest.Tool> tools) {
        int iteration = 0;
        StringBuilder rawDataBuilder = new StringBuilder();

        // Kopie der Messages für Mutationen
        List<Message> conversationMessages = new ArrayList<>(messages);

        while (iteration < MAX_TOOL_ITERATIONS) {
            iteration++;
            log.info("🔄 OpenRouter tool loop iteration: {}", iteration);

            // OpenRouter aufrufen mit Tools
            OpenRouterResponseWithMetrics response = callOpenRouter(conversationMessages, tools);
            log.debug("📥 OpenRouter response - finishReason: {}", response.getFinishReason());

            OpenRouterResponse.Message message = response.getMessage();

            // Prüfen ob Tool-Calls vorhanden sind
            if (FINISH_REASON_TOOL_CALLS.equals(response.getFinishReason()) ||
                (message != null && message.getToolCalls() != null && !message.getToolCalls().isEmpty())) {

                log.info("🔧 Processing {} tool calls", message.getToolCalls().size());

                // Assistant-Nachricht mit Tool-Calls zur Konversation hinzufügen
                String assistantContent = message.getContent() != null ? message.getContent() : "";
                conversationMessages.add(new Message("assistant", assistantContent));

                // Tool-Ergebnisse verarbeiten
                StringBuilder toolResults = new StringBuilder();
                toolResults.append("Tool-Ergebnisse:\n\n");

                for (OpenRouterResponse.ToolCall toolCall : message.getToolCalls()) {
                    String toolName = toolCall.getFunction().getName();
                    String toolArgs = toolCall.getFunction().getArguments();

                    log.info("📨 Executing tool: {} with args: {}", toolName, toolArgs);

                    String toolResult = executeMcpTool(toolName, toolArgs);
                    toolResults.append(String.format("TOOL_RESULT [%s]:\n%s\n\n", toolName, toolResult));
                    rawDataBuilder.append(toolResult).append("\n");

                    log.info("✅ Tool {} executed successfully", toolName);
                }

                // Tool-Ergebnisse als User-Nachricht hinzufügen
                conversationMessages.add(new Message("user", toolResults.toString().trim()));

            } else if (FINISH_REASON_STOP.equals(response.getFinishReason()) ||
                       (message != null && message.getContent() != null && !message.getContent().isEmpty())) {
                // Finale Antwort erhalten
                log.info("✅ Got final answer after {} iteration(s)", iteration);

                String finalAnswer = message != null && message.getContent() != null
                    ? message.getContent()
                    : response.getReply();

                SummaryInfo summaryInfo = parseSummaryInfo(finalAnswer);

                return new ToolLoopResult(
                    finalAnswer,
                    rawDataBuilder.toString(),
                    summaryInfo
                );
            } else {
                // Unerwarteter Zustand
                log.warn("⚠️ Unexpected response state, treating as final");
                return new ToolLoopResult(
                    response.getReply(),
                    rawDataBuilder.toString(),
                    null
                );
            }
        }

        log.error("❌ Max iterations reached");
        return new ToolLoopResult(
            "Maximale Iterationen erreicht",
            rawDataBuilder.toString(),
            null
        );
    }

    /**
     * Ruft OpenRouter API mit Tools auf.
     */
    private OpenRouterResponseWithMetrics callOpenRouter(List<Message> messages, List<OpenRouterRequest.Tool> tools) {
        try {
            return openRouterToolClient.requestCompletionWithMetrics(
                messages, temperature, model, tools
            );
        } catch (Exception e) {
            log.error("❌ Error calling OpenRouter: {}", e.getMessage());
            throw new RuntimeException("Failed to call OpenRouter API", e);
        }
    }

    /**
     * Führt ein MCP Tool aus.
     */
    private String executeMcpTool(String toolName, String argumentsJson) {
        try {
            Map<String, Object> arguments = parseToolArguments(argumentsJson);

            ToolExecutionResponse result = mcpToolClient.executeTool(toolName, arguments);

            if (result.isSuccess()) {
                return objectMapper.writeValueAsString(result.getResult());
            } else {
                return "ERROR: " + result.getError();
            }
        } catch (Exception e) {
            log.error("❌ Error executing tool {}: {}", toolName, e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * Parst Tool-Argumente aus JSON-String.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseToolArguments(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isBlank()) {
            return Map.of();
        }

        try {
            return objectMapper.readValue(argumentsJson, Map.class);
        } catch (JsonProcessingException e) {
            log.warn("⚠️ Failed to parse tool arguments: {}", e.getMessage());
            return Map.of();
        }
    }

    /**
     * Parst SummaryInfo aus der finalen Antwort.
     */
    private SummaryInfo parseSummaryInfo(String answer) {
        if (answer == null) {
            return null;
        }

        try {
            // Versuche JSON aus der Antwort zu extrahieren
            String cleaned = cleanJsonResponse(answer);
            JsonNode node = objectMapper.readTree(cleaned);

            return SummaryInfo.builder()
                .title(node.has("title") ? node.get("title").asText() : null)
                .totalItems(node.has("total_items") ? node.get("total_items").asInt() : null)
                .priority(node.has("priority") ? node.get("priority").asText() : null)
                .build();

        } catch (Exception e) {
            log.debug("Could not parse summary info from answer: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Bereinigt JSON von Markdown-Blöcken.
     */
    private String cleanJsonResponse(String response) {
        if (response == null) return null;

        String cleaned = response.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }

    /**
     * Speichert die Zusammenfassung in der Datenbank.
     */
    private ReminderSummary saveReminderSummary(String userId, ToolLoopResult result) {
        String title = "Aufgaben-Zusammenfassung (OpenRouter)";
        int itemsCount = 0;
        Priority priority = Priority.MEDIUM;

        // Versuche Metadaten aus dem SummaryInfo-Objekt zu extrahieren
        if (result.summaryInfo != null) {
            if (result.summaryInfo.title != null) {
                title = result.summaryInfo.title;
            }

            if (result.summaryInfo.totalItems != null) {
                itemsCount = result.summaryInfo.totalItems;
            }

            if (result.summaryInfo.priority != null) {
                try {
                    priority = Priority.valueOf(result.summaryInfo.priority);
                } catch (Exception e) {
                    // Default beibehalten
                }
            }
        }

        ReminderSummary summary = ReminderSummary.builder()
            .userId(userId)
            .summaryType(SummaryType.TASKS)
            .title(title)
            .content(result.answer)
            .rawData(result.rawData)
            .itemsCount(itemsCount)
            .priority(priority)
            .notified(false)
            .nextReminderAt(LocalDateTime.now().plusDays(1))
            .build();

        return reminderRepository.save(summary);
    }

    /**
     * Triggert eine Benachrichtigung für die Zusammenfassung.
     */
    private void triggerNotification(ReminderSummary summary) {
        log.info("📧 Notification triggered for summary: {} (Priority: {})",
            summary.getTitle(), summary.getPriority());

        // Markiere als benachrichtigt
        summary.setNotified(true);
        summary.setNotifiedAt(LocalDateTime.now());
        reminderRepository.save(summary);
    }

    /**
     * Holt alle OpenRouter-generierten Zusammenfassungen für einen Benutzer.
     */
    public List<ReminderSummary> getSummariesForUser(String userId) {
        return reminderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Internes Ergebnis-Objekt für den Tool-Loop.
     */
    private record ToolLoopResult(
        String answer,
        String rawData,
        SummaryInfo summaryInfo
    ) {}

    /**
     * Summary-Info DTO für Metadaten-Extraktion.
     */
    @lombok.Builder
    private record SummaryInfo(
        String title,
        Integer totalItems,
        String priority
    ) {}
}

