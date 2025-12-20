/*
package de.jivz.ai_challenge.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.ai_challenge.dto.Message;
import de.jivz.ai_challenge.dto.ReminderSummaryJsonDto;
import de.jivz.ai_challenge.dto.SonarToolDto;
import de.jivz.ai_challenge.dto.SonarToolDto.ToolCall;
import de.jivz.ai_challenge.entity.ReminderSummary;
import de.jivz.ai_challenge.entity.ReminderSummary.Priority;
import de.jivz.ai_challenge.entity.ReminderSummary.SummaryType;
import de.jivz.ai_challenge.repository.ReminderSummaryRepository;
import de.jivz.ai_challenge.mcp.model.McpDto.McpTool;
import de.jivz.ai_challenge.mcp.model.McpDto.ToolExecutionResponse;
import de.jivz.ai_challenge.mcp.model.McpToolClient;
import de.jivz.ai_challenge.service.perplexity.PerplexityToolClient;
import de.jivz.ai_challenge.service.strategy.ReminderToolsPromptStrategy;
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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

*/
/**
 * REFACTORED: ReminderSchedulerService mit direktem JSON-Output
 *
 * Keine Normalisierung, kein Parsing von verschiedenen Formaten!
 * Die Models geben DIREKT das richtige JSON-Format aus.
 *
 * Workflow:
 * 1. Erstelle Messages mit JSON-Schema-Prompt
 * 2. Rufe Sonar auf → erhalte JSON
 * 3. Deserialisiere direkt zu ReminderSummaryJsonDto
 * 4. Speichere in DB
 *
 * Das ist alles! Keine Parser, keine Normalisierung nötig!
 *//*

@Service
@Slf4j
@RequiredArgsConstructor
public class ReminderSchedulerServiceRefactored {

    private final McpToolClient mcpToolClient;
    private final PerplexityToolClient perplexityToolClient;
    private final ReminderSummaryRepository reminderRepository;
    private final ObjectMapper objectMapper;
    private final ReminderToolsPromptStrategy reminderToolsPromptStrategy;

    @Value("${reminder.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    @Value("${reminder.scheduler.user-id:system}")
    private String defaultUserId;

    @Value("${reminder.scheduler.temperature:0.1}")
    private double temperature;

    */
/**
     * Scheduled Task: Erstelle Reminder-Zusammenfassung
     *//*

    @Scheduled(cron = "${reminder.scheduler.cron:0 0 9 * * ?}")
    @Transactional
    public void scheduledReminderTask() {
        if (!schedulerEnabled) {
            log.debug("⏸️ Scheduler deaktiviert");
            return;
        }

        log.info("⏰ Starte Reminder-Workflow um {}", LocalDateTime.now());

        try {
            ReminderSummary summary = executeReminderWorkflow(defaultUserId);
            if (summary != null) {
                log.info("✅ Reminder erstellt: {} (ID: {})", summary.getTitle(), summary.getId());
            }
        } catch (Exception e) {
            log.error("❌ Fehler beim Reminder: {}", e.getMessage(), e);
        }
    }

    */
/**
     * Führe den kompletten Workflow aus
     *//*

    @Transactional
    public ReminderSummary executeReminderWorkflow(String userId) {
        log.info("🚀 Executing reminder workflow for user: {}", userId);

        // 1. Hole aktuelle MCP Tools
        List<McpTool> tools = fetchCurrentTools();

        // 2. Baue Messages mit JSON-Schema-Prompt
        List<Message> messages = buildJsonMessages(tools);

        // 3. Führe Tool-Loop aus
        String jsonResponse = executeToolLoop(messages);

        // 4. Parse JSON → DTO (KEIN Parser, direktes Jackson-Parsing!)
        ReminderSummaryJsonDto summaryDto = parseJsonResponse(jsonResponse);

        // 5. Speichere in DB
        return saveReminderSummary(userId, summaryDto);
    }

    */
/**
     * Baue Messages mit Tool-Prompt
     *//*

    private List<Message> buildJsonMessages(List<McpTool> tools) {
        List<Message> messages = new ArrayList<>();

        // System-Prompt mit MCP Tools
        String systemPrompt = reminderToolsPromptStrategy.buildDynamicSystemPrompt(tools);
        messages.add(new Message("system", systemPrompt));

        // User-Prompt
        messages.add(new Message("user",
            "Erstelle eine detaillierte Zusammenfassung meiner Aufgaben. " +
            "Nutze die verfügbaren Tools um meine Aufgaben zu laden und dann " +
            "eine strukturierte Zusammenfassung als JSON zu erstellen."));

        return messages;
    }

    */
/**
     * Führe Tool-Loop aus - iterativ bis JSON-Response oder max Iterationen
     *
     * Workflow:
     * 1. Rufe Sonar auf
     * 2. Wenn JSON → fertig, return
     * 3. Wenn Tool-Calls → führe MCP Tools aus, füge Results zu messages hinzu
     * 4. Wiederhole
     *//*

    private String executeToolLoop(List<Message> messages) {
        int iteration = 0;
        final int MAX_ITERATIONS = 10;
        final String STEP_TOOL = "tool";
        final String STEP_FINAL = "final";

        while (iteration < MAX_ITERATIONS) {
            iteration++;
            log.info("🔄 Tool-Loop Iteration {} | Temp: {} | Max: {}", iteration, temperature, MAX_ITERATIONS);

            // Rufe Sonar auf
            String sonarResponse = callSonar(messages);
            log.info("📥 Response (erste 200 chars): {}", sonarResponse.substring(0, Math.min(200, sonarResponse.length())));

            // Versuche zu parsen als Tool-Response
            SonarToolDto.SonarToolResponse parsed = null;
            try {
                String cleaned = cleanSonarResponse(sonarResponse);
                parsed = objectMapper.readValue(cleaned, SonarToolDto.SonarToolResponse.class);
                log.debug("✅ Parsed as Tool-Response: step={}", parsed.getStep());
            } catch (JsonProcessingException e) {
                // Nicht als Tool-Response parsbar - könnte reines JSON sein
                log.debug("⚠️ Kein Tool-Response Format: {}", e.getMessage());
            }

            // Prüfe ob es reines JSON ist (für finale Zusammenfassung)
            if (sonarResponse.trim().startsWith("{") && parsed == null) {
                log.info("✅ JSON Response erhalten nach {} Iteration(en)", iteration);
                return sonarResponse;
            }

            // Falls parsbar: prüfe auf Finale oder Tool-Calls
            if (parsed != null) {
                log.info("📊 Parsed Response Step: {}", parsed.getStep());

                if (STEP_FINAL.equals(parsed.getStep())) {
                    log.info("✅ Finaler Schritt erreicht nach {} Iteration(en)", iteration);
                    // Rückgabe der Antwort als JSON für die nächste Stufe
                    return parsed.getAnswer() != null ? parsed.getAnswer() : sonarResponse;
                }

                if (STEP_TOOL.equals(parsed.getStep())) {
                    int toolCount = parsed.getToolCalls() != null ? parsed.getToolCalls().size() : 0;

                    if (toolCount > 0) {
                        log.info("🛠️ Tool-Calls FOUND: {} Tools werden aufgerufen!", toolCount);
                        messages.add(new Message("assistant", sonarResponse));

                        // Führe alle Tool-Calls aus
                        StringBuilder toolResults = new StringBuilder();
                        toolResults.append("Результаты вызовов инструментов:\n\n");

                        for (ToolCall toolCall : parsed.getToolCalls()) {
                            try {
                                log.info("🔨 Executing Tool: {} with args: {}", toolCall.getName(), toolCall.getArguments());
                                String toolResult = executeMcpTool(toolCall);
                                toolResults.append(String.format("TOOL_RESULT %s:\n%s\n\n",
                                    toolCall.getName(), toolResult));
                                log.info("✅ Tool erfolgreich: {}", toolCall.getName());
                            } catch (Exception e) {
                                String error = "ERROR: " + e.getMessage();
                                toolResults.append(String.format("TOOL_RESULT %s:\n%s\n\n",
                                    toolCall.getName(), error));
                                log.error("❌ Tool-Fehler [{}]: {}", toolCall.getName(), e.getMessage());
                            }
                        }

                        messages.add(new Message("user", toolResults.toString().trim()));
                        log.info("➡️ Tool-Results zurück zum Model, iteriere weiter...");
                        continue;
                    } else {
                        log.warn("⚠️ Step=tool aber keine toolCalls vorhanden! Behandle als final.");
                    }
                }
            }

            // Falls nichts davon: Behandele als finales JSON
            log.info("✅ Keine Tool-Calls, behandele als finales JSON nach {} Iteration(en)", iteration);
            return sonarResponse;
        }

        log.error("❌ Max Iterationen ({}) erreicht", MAX_ITERATIONS);
        throw new RuntimeException("Failed to get JSON response after " + MAX_ITERATIONS + " iterations");
    }

    */
/**
     * Führe einen MCP Tool aus
     *//*

    private String executeMcpTool(ToolCall toolCall) {
        try {
            ToolExecutionResponse result = mcpToolClient.executeTool(
                toolCall.getName(),
                toolCall.getArguments() != null ? toolCall.getArguments() : Map.of()
            );

            if (result.isSuccess()) {
                return objectMapper.writeValueAsString(result.getResult());
            } else {
                return "ERROR: " + result.getError();
            }
        } catch (Exception e) {
            log.error("❌ Tool-Ausführungsfehler {}: {}", toolCall.getName(), e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }

    */
/**
     * Bereinige Sonar-Response von Markdown
     *//*

    private String cleanSonarResponse(String response) {
        if (response == null) return "{}";

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

    */
/**
     * Rufe Sonar API auf
     *//*

    private String callSonar(List<Message> messages) {
        try {
            var response = perplexityToolClient.requestCompletionWithMetrics(
                messages, temperature, null
            );
            return response.getReply();
        } catch (Exception e) {
            log.error("❌ Sonar API Error: {}", e.getMessage());
            throw new RuntimeException("Sonar API call failed", e);
        }
    }

    */
/**
     * Parse JSON-Response → ReminderSummaryJsonDto
     *
     * Обрабатывает два формата:
     * 1. Tool-Response от Model: {"step":"final","tool_calls":[],"answer":"...","summary":{...}}
     * 2. Финальный JSON: {...} (прямой ReminderSummaryJsonDto)
     *
     * Erweiterte Fehlerbehandlung:
     * - Automatische JSON-Reparatur (ungültige Quotes, fehlende Kommas)
     * - Lenient Jackson-Parsing (ignoriert unbekannte Felder)
     * - Detailliertes Logging für Debug
     *//*

    private ReminderSummaryJsonDto parseJsonResponse(String jsonResponse) {
        try {
            // 1. Entferne Markdown-Wrapper falls vorhanden
            String cleanJson = cleanJsonString(jsonResponse);

            log.info("📋 Rohes JSON vor Reparatur (erste 500 chars):\n{}",
                cleanJson.substring(0, Math.min(500, cleanJson.length())));

            // 2. Versuche Original-JSON zu parsen (best case)
            try {
                ReminderSummaryJsonDto dto = parseJsonWithLenient(cleanJson);
                log.info("✅ JSON erfolgreich geparst (Versuch 1): {} Tasks", dto.getTotalItems());
                logDtoState(dto);
                return dto;
            } catch (Exception e1) {
                log.warn("⚠️ Erstes Parse fehlgeschlagen: {}", e1.getMessage());

                // 2b. Versuche zu parsen als Tool-Response mit "summary" Feld
                try {
                    SonarToolDto.SonarToolResponse toolResponse = objectMapper.readValue(cleanJson, SonarToolDto.SonarToolResponse.class);

                    if (toolResponse.getSummary() != null) {
                        log.info("✅ Parsiert als Tool-Response, extrahiere summary Feld");
                        ReminderSummaryJsonDto dto = toolResponse.getSummary();
                        logDtoState(dto);
                        return dto;
                    }
                } catch (Exception e2) {
                    log.debug("⚠️ Nicht als Tool-Response parsbar: {}", e2.getMessage());
                }

                log.warn("⚠️ Parsierung fehlgeschlagen, versuche Reparatur...");

                // 3. Versuche automatische JSON-Reparatur
                String repairedJson = attemptJsonRepair(cleanJson);

                if (!repairedJson.equals(cleanJson)) {
                    log.debug("🔧 JSON repariert");
                    log.debug("📋 Repariertes JSON (erste 500 chars):\n{}",
                        repairedJson.substring(0, Math.min(500, repairedJson.length())));

                    try {
                        ReminderSummaryJsonDto dto = parseJsonWithLenient(repairedJson);
                        log.info("✅ JSON erfolgreich geparst (nach Reparatur): {} Tasks", dto.getTotalItems());
                        logDtoState(dto);
                        return dto;
                    } catch (Exception e3) {
                        log.warn("⚠️ Parse nach Reparatur fehlgeschlagen: {}", e3.getMessage());
                        throw e3;
                    }
                } else {
                    throw e1;
                }
            }

        } catch (Exception e) {
            log.error("❌ JSON Parsing Error: {}", e.getMessage());
            log.error("📋 Fehlertyp: {}", e.getClass().getSimpleName());
            log.error("🔍 Full stack trace:", e);

            // Fallback: Erstelle leeres DTO damit nicht alles crasht
            log.warn("⚠️ Fallback: Erstelle leeres ReminderSummaryJsonDto");
            return ReminderSummaryJsonDto.builder()
                .title("Aufgaben-Zusammenfassung")
                .summary("Fehler beim Parsen der Zusammenfassung")
                .totalItems(0)
                .priority("MEDIUM")
                .highlights(List.of())
                .activeTasks(List.of())
                .dueSoon(List.of())
                .overdue(List.of())
                .build();
        }
    }

    */
/**
     * Logge DTO-State für Debugging
     *//*

    private void logDtoState(ReminderSummaryJsonDto dto) {
        if (dto == null) {
            log.warn("⚠️ DTO is NULL!");
            return;
        }

        log.info("📊 DTO State:");
        log.info("  - Title: {}", dto.getTitle());
        log.info("  - Summary: {} chars", dto.getSummary() != null ? dto.getSummary().length() : "NULL");
        log.info("  - Total Items: {}", dto.getTotalItems());
        log.info("  - Priority: {}", dto.getPriority());
        log.info("  - Highlights: {}", dto.getHighlights() != null ? dto.getHighlights().size() : "NULL");
        log.info("  - Active Tasks: {}", dto.getActiveTasks() != null ? dto.getActiveTasks().size() : "NULL");
        log.info("  - Due Soon: {}", dto.getDueSoon() != null ? dto.getDueSoon().size() : "NULL");
        log.info("  - Overdue: {}", dto.getOverdue() != null ? dto.getOverdue().size() : "NULL");
    }

    */
/**
     * Parse JSON mit lenient Jackson-Konfiguration
     *//*

    private ReminderSummaryJsonDto parseJsonWithLenient(String jsonString) throws Exception {
        ObjectMapper lenientMapper = objectMapper.copy();

        // Aktiviere lenient parsing
        lenientMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        lenientMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        lenientMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return lenientMapper.readValue(jsonString, ReminderSummaryJsonDto.class);
    }

    */
/**
     * Versuche, häufige JSON-Fehler automatisch zu reparieren
     *
     * Bekannte Fehler:
     * - Fehlende schließende Anführungszeichen
     * - Ungültige Escape-Sequenzen
     * - Fehlende Kommas zwischen Objekten
     *//*

    private String attemptJsonRepair(String json) {
        String repaired = json;

        // Reparatur 1: Fehlende Anführungszeichen in String-Werten
        // Pattern: Attribut mit nicht geschlossenem String-Wert
        repaired = fixUnclosedStrings(repaired);

        // Reparatur 2: Doppelte Backslashes (Unicode-Escape-Fehler)
        repaired = fixDoubleBackslashes(repaired);

        // Reparatur 3: Fehlende Kommas zwischen Objekten in Arrays
        repaired = fixMissingCommasBetweenObjects(repaired);

        // Reparatur 4: Entferne trailing Kommas
        repaired = fixTrailingCommas(repaired);

        if (!repaired.equals(json)) {
            log.debug("🔧 JSON-Reparaturen angewendet");
        }

        return repaired;
    }

    */
/**
     * Repariere fehlende Anführungszeichen in String-Werten
     *
     * Behandelt:
     * - Enum-Werte: "urgency": HIGH  →  "urgency": "HIGH"
     * - Kategorien: "category": SPORTS  →  "category": "SPORTS"
     * - Boolean-ähnliche Werte
     * - Ungültige Zeichenfolgen in Strings
     * - Nicht geschlossene String-Werte vor Delimitern
     *//*

    private String fixUnclosedStrings(String json) {
        String repaired = json;

        // Muster 1: Nicht zitierte Enum-Werte
        // "field": VALUE[,}] → "field": "VALUE"[,}]
        Pattern enumPattern = Pattern.compile("\"(\\w+)\"\\s*:\\s*(HIGH|MEDIUM|LOW|SPORTS|WORK|HEALTH|FAMILY|OTHER)([,\\}\\]])");
        Matcher matcher = enumPattern.matcher(repaired);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String fieldName = matcher.group(1);
            String enumValue = matcher.group(2);
            String suffix = matcher.group(3);

            log.debug("🔧 Repariere Enum: {} → \"{}\"", enumValue, enumValue);
            matcher.appendReplacement(sb, "\"" + fieldName + "\" : \"" + enumValue + "\"" + suffix);
        }
        matcher.appendTail(sb);
        repaired = sb.toString();

        // Muster 2: Ungültige Escape-Sequenzen in Strings
        // \"value zu "value (falls Escape fehlerhaft ist)
        repaired = repaired.replaceAll("\\\\\"([^\"]*?)\\\\\"", "\"$1\"");

        // Muster 3: String-Werte ohne Abschluss-Quote vor Komma oder schließender Klammer
        // Beispiel: "urgency": "HIGH,  (ohne schließendes ") oder "urgency": "value}
        repaired = fixUnclosedStringValues(repaired);

        return repaired;
    }

    */
/**
     * Repariere String-Werte die mit einem öffnenden Quote anfangen aber nicht geschlossen sind
     *
     * Dies ist ein häufiger Fehler bei LLM-generierten JSON:
     * "field": "value,  (statt "field": "value",)
     * "urgency": "HIGH}   (statt "urgency": "HIGH"}  oder "urgency": "HIGH",)
     *//*

    private String fixUnclosedStringValues(String json) {
        String result = json;

        // Pattern 1: "field": "value[,}] wobei das Abschluss-Quote fehlt
        // Erkennt: öffnendes Quote nach : , aber dann kein schließendes Quote vor , oder }
        Pattern unclosedValuePattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]+?)([,\\}\\]])(\\s*[,\\}\\]])");
        Matcher matcher = unclosedValuePattern.matcher(result);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String fieldName = matcher.group(1);
            String fieldValue = matcher.group(2);
            String delimiter = matcher.group(3);
            String suffix = matcher.group(4);

            // Wenn das Suffix auch ein Delimiter ist, sind wir wahrscheinlich auf einen fehlenden Quote gestoßen
            if (delimiter.matches("[,\\}\\]]") && suffix.matches("\\s*[,\\}\\]]")) {
                log.debug("🔧 Repariere nicht geschlossenen String: {} → {}\"{}\"{}",
                    fieldName, fieldName, fieldValue, suffix);
                matcher.appendReplacement(sb, "\"" + fieldName + "\" : \"" + fieldValue + "\"" + suffix);
            } else {
                matcher.appendReplacement(sb, "$0");
            }
        }
        matcher.appendTail(sb);
        result = sb.toString();

        return result;
    }

    */
/**
     * Repariere unvollständig geschlossene String-Werte (legacy)
     * Beispiel: "urgency": "HIGH,  → "urgency": "HIGH",
     *//*

    private String fixIncompleteStrings(String json) {
        // Pattern: öffnendes Quote aber kein schließendes Quote vor Delimiter
        // "fieldname": "value[,}] ohne schließendes "

        // Nur wenn wir mehrere aufeinanderfolgende nicht-Quote-Zeichen nach : haben
        String result = json;

        // Ersetze Muster wie: "field": "value,  mit "field": "value",
        result = result.replaceAll("\"([^\"]*?)\"\\s*:\\s*\"([^\"]*?)([,\\}\\]])(?=[,\\}])", "\"$1\" : \"$2\"$3");

        return result;
    }

    */
/**
     * Repariere doppelte Backslashes (häufig bei Unicode-Escapes)
     *//*

    private String fixDoubleBackslashes(String json) {
        // Ersetze \\ mit \ (außer vor speziellen Zeichen wie ")
        return json.replaceAll("\\\\\\\\([^\"tnrfb\\\\])", "\\\\$1");
    }

    */
/**
     * Repariere fehlende Kommas zwischen Objekten in Arrays
     * Beispiel: }{ → },{
     *//*

    private String fixMissingCommasBetweenObjects(String json) {
        return json.replaceAll("\\}\\s*\\{", "},{");
    }

    */
/**
     * Entferne trailing Kommas (vor } oder ])
     *//*

    private String fixTrailingCommas(String json) {
        return json.replaceAll(",\\s*([\\}\\]])", "$1");
    }

    */
/**
     * Entferne Markdown-Wrapper von JSON und normalisiere Format
     *
     * Behandelt:
     * - ```json ``` - Markdown-Blöcke
     * - Führende/nachfolgende Whitespace
     * - Häufige Text um JSON
     * - Encoding-Fehler
     *//*

    private String cleanJsonString(String json) {
        if (json == null || json.trim().isEmpty()) {
            log.warn("⚠️ Leere JSON-Response erhalten");
            return "{}";
        }

        String cleaned = json.trim();

        // Entferne Markdown-Blöcke
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        cleaned = cleaned.trim();

        // Entferne häufige Wrapper-Texte (falls LLM zusätzlichen Text hinzufügt)
        if (cleaned.startsWith("json")) {
            cleaned = cleaned.substring(4).trim();
        }

        // Entferne Text vor erstem {
        int jsonStart = cleaned.indexOf('{');
        if (jsonStart > 0) {
            log.debug("⚠️ Text vor JSON gefunden, entferne Zeichen 0-{}", jsonStart);
            cleaned = cleaned.substring(jsonStart);
        }

        // Entferne Text nach letztem }
        int jsonEnd = cleaned.lastIndexOf('}');
        if (jsonEnd >= 0 && jsonEnd < cleaned.length() - 1) {
            log.debug("⚠️ Text nach JSON gefunden, entferne Zeichen nach {}", jsonEnd);
            cleaned = cleaned.substring(0, jsonEnd + 1);
        }

        cleaned = cleaned.trim();

        log.debug("📋 Bereinigte JSON-Länge: {} Zeichen", cleaned.length());

        return cleaned;
    }

    */
/**
     * Speichere ReminderSummaryJsonDto in DB
     *
     * Mit null-safety checks!
     *//*

    private ReminderSummary saveReminderSummary(String userId, ReminderSummaryJsonDto dto) {
        try {
            // Validiere und setze Default-Werte wenn nötig
            String title = dto.getTitle() != null ? dto.getTitle() : "Aufgaben-Zusammenfassung";
            Integer itemsCount = dto.getTotalItems() != null ? dto.getTotalItems() : 0;

            Priority priority = Priority.MEDIUM;  // Default
            if (dto.getPriority() != null && !dto.getPriority().isEmpty()) {
                try {
                    priority = Priority.valueOf(dto.getPriority().toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("⚠️ Ungültige Priority '{}', verwende MEDIUM", dto.getPriority());
                    priority = Priority.MEDIUM;
                }
            } else {
                log.debug("⚠️ Priority ist null/leer, verwende MEDIUM");
            }

            log.info("📊 Speichere Reminder: Title={}, Items={}, Priority={}",
                title, itemsCount, priority);

            // Baue Entity aus DTO
            ReminderSummary entity = ReminderSummary.builder()
                .userId(userId)
                .summaryType(SummaryType.TASKS)
                .title(title)
                .content(dto.toMarkdownString())  // Speichere als Markdown für schöne Anzeige
                .itemsCount(itemsCount)
                .priority(priority)
                .notified(false)
                .nextReminderAt(LocalDateTime.now().plusDays(1))
                .build();

            ReminderSummary saved = reminderRepository.save(entity);

            log.info("✅ Reminder gespeichert: ID={}, Title={}, Priority={}, Items={}",
                saved.getId(), saved.getTitle(), saved.getPriority(), saved.getItemsCount());
            return saved;

        } catch (Exception e) {
            log.error("❌ Fehler beim Speichern des Reminders: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save reminder: " + e.getMessage(), e);
        }
    }

    */
/**
     * Hole aktuelle MCP Tools
     *//*

    private List<McpTool> fetchCurrentTools() {
        try {
            return mcpToolClient.getAllTools();
        } catch (Exception e) {
            log.warn("⚠️ Fehler beim Abrufen von Tools: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    */
/**
     * Manueller Trigger für Reminder-Workflow
     *//*

    @Transactional
    public ReminderSummary triggerReminder(String userId) {
        return executeReminderWorkflow(userId);
    }

    */
/**
     * Hole alle Summaries für Benutzer
     *//*

    public List<ReminderSummary> getSummariesForUser(String userId) {
        return reminderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    */
/**
     * Hole neueste Summary
     *//*

    public ReminderSummary getLatestSummary() {
        return reminderRepository.findTopByOrderByCreatedAtDesc().orElse(null);
    }
}

*/
