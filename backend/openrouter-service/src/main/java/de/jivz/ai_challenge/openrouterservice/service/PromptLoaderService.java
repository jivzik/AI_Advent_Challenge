package de.jivz.ai_challenge.openrouterservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.ai_challenge.openrouterservice.mcp.model.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service zum Laden und Verarbeiten von Prompts aus Markdown-Dateien.
 *
 * Prompts werden aus resources/prompts/ geladen und können Platzhalter enthalten:
 * - {{TOOLS_SECTION}} - Wird mit formatierten Tool-Definitionen ersetzt
 * - {{TOOL_RESULTS}} - Wird mit Tool-Ergebnissen ersetzt
 * - {{USER_MESSAGE}} - Wird mit der Benutzeranfrage ersetzt
 *
 * Kontext-Erkennung erfolgt über LLM statt Regex-Patterns.
 */
@Service
@Slf4j
public class PromptLoaderService {

    private final ObjectMapper objectMapper;
    private final Map<String, String> promptCache = new HashMap<>();


    @Value("classpath:prompts/system-tools.md")
    private Resource systemToolsPrompt;

    @Value("classpath:prompts/json-correction.md")
    private Resource jsonCorrectionPrompt;

    @Value("classpath:prompts/tool-results.md")
    private Resource toolResultsPrompt;

    @Value("classpath:prompts/context-docker.md")
    private Resource contextDockerPrompt;

    @Value("classpath:prompts/context-tasks.md")
    private Resource contextTasksPrompt;

    @Value("classpath:prompts/context-calendar.md")
    private Resource contextCalendarPrompt;

    @Value("classpath:prompts/context-detection.md")
    private Resource contextDetectionPrompt;

    @Value("classpath:prompts/context-developer.md")
    private Resource contextDeveloperPrompt;

    @Value("classpath:prompts/developer-search.md")
    private Resource developerSearchPrompt;

    @Value("classpath:prompts/developer-code-style.md")
    private Resource developerCodeStylePrompt;

    @Value("classpath:prompts/audio-transcription.md")
    private Resource audioTranscriptionPrompt;

    public PromptLoaderService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        loadPrompts();
    }

    private void loadPrompts() {
        try {
            promptCache.put("system-tools", loadResource(systemToolsPrompt));
            promptCache.put("json-correction", loadResource(jsonCorrectionPrompt));
            promptCache.put("tool-results", loadResource(toolResultsPrompt));
            promptCache.put("context-docker", loadResource(contextDockerPrompt));
            promptCache.put("context-tasks", loadResource(contextTasksPrompt));
            promptCache.put("context-calendar", loadResource(contextCalendarPrompt));
            promptCache.put("context-detection", loadResource(contextDetectionPrompt));
            promptCache.put("context-developer", loadResource(contextDeveloperPrompt));
            promptCache.put("developer-search", loadResource(developerSearchPrompt));
            promptCache.put("developer-code-style", loadResource(developerCodeStylePrompt));
            promptCache.put("audio-transcription", loadResource(audioTranscriptionPrompt));
            log.info("Loaded {} prompts from resources", promptCache.size());
        } catch (IOException e) {
            log.error("Failed to load prompts", e);
            throw new RuntimeException("Failed to load prompts", e);
        }
    }

    private String loadResource(Resource resource) throws IOException {
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    /**
     * Erstellt den System-Prompt mit dynamisch eingefügten Tool-Definitionen.
     */
    public String buildSystemPromptWithTools(List<ToolDefinition> tools) {
        String template = promptCache.get("system-tools");
        String toolsSection = formatToolsForPrompt(tools);
        return template.replace("{{TOOLS_SECTION}}", toolsSection);
    }

    /**
     * Gibt den JSON-Korrektur-Prompt zurück.
     */
    public String getJsonCorrectionPrompt() {
        return promptCache.get("json-correction");
    }

    /**
     * Erstellt den Prompt für Tool-Ergebnisse.
     */
    public String buildToolResultsPrompt(String toolResults) {
        String template = promptCache.get("tool-results");
        return template.replace("{{TOOL_RESULTS}}", toolResults);
    }

    /**
     * Formatiert Tool-Definitionen für den Prompt.
     */
    private String formatToolsForPrompt(List<ToolDefinition> tools) {
        if (tools == null || tools.isEmpty()) {
            return "No MCP tools currently available.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## Available MCP Tools:\n\n");

        for (int i = 0; i < tools.size(); i++) {
            ToolDefinition tool = tools.get(i);
            sb.append(String.format("%d. **%s**\n", i + 1, tool.getName()));
            sb.append(String.format("   - Description: %s\n",
                    tool.getDescription() != null ? tool.getDescription() : "No description available"));

            if (tool.getInputSchema() != null && !tool.getInputSchema().isEmpty()) {
                try {
                    String schemaJson = objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(tool.getInputSchema());
                    sb.append(String.format("   - Input Schema:\n```json\n%s\n```\n", schemaJson));
                } catch (JsonProcessingException e) {
                    sb.append(String.format("   - Input Schema: %s\n", tool.getInputSchema()));
                }
            }
            sb.append("\n");
        }

        log.debug("Formatted {} MCP tools for prompt", tools.size());
        return sb.toString();
    }

    /**
     * Erstellt den Prompt für LLM-basierte Kontext-Erkennung.
     * Das LLM analysiert die Benutzeranfrage und wählt den passenden Kontext.
     *
     * @param userMessage Die Nachricht des Benutzers
     * @param toolsSection Die formatierte Tool-Sektion
     * @return Prompt für die Kontext-Erkennung
     */
    public String buildContextDetectionPrompt(String userMessage, String toolsSection) {
        String template = promptCache.get("context-detection");
        if (template == null) {
            log.warn("Context detection prompt not found, returning null");
            return null;
        }
        return template
                .replace("{{TOOLS_SECTION}}", toolsSection)
                .replace("{{USER_MESSAGE}}", userMessage);
    }

    /**
     * Erstellt den Prompt für LLM-basierte Kontext-Erkennung mit Tool-Definitionen.
     *
     * @param userMessage Die Nachricht des Benutzers
     * @param tools Die verfügbaren MCP-Tools
     * @return Prompt für die Kontext-Erkennung
     */
    public String buildContextDetectionPrompt(String userMessage, List<ToolDefinition> tools) {
        String toolsSection = formatToolsForPrompt(tools);
        return buildContextDetectionPrompt(userMessage, toolsSection);
    }

    /**
     * Erstellt den System-Prompt mit Tools UND kontextbezogenen Anweisungen.
     * Der Kontext wird vom LLM erkannt und hier direkt übergeben.
     *
     * @param tools Die verfügbaren MCP-Tools
     * @param context Der erkannte Kontext (docker, tasks, calendar, default)
     * @return Den vollständigen System-Prompt
     */
    public String buildSystemPromptWithToolsAndContext(List<ToolDefinition> tools, String context) {
        String basePrompt = buildSystemPromptWithTools(tools);

        if (context == null || "default".equals(context)) {
            return basePrompt;
        }

        String contextPrompt = getContextPrompt(context);
        if (contextPrompt != null) {
            log.info("🎯 Adding {} context prompt", context);
            return basePrompt + "\n\n## CONTEXT-SPECIFIC INSTRUCTIONS:\n\n" + contextPrompt;
        }

        return basePrompt;
    }

    /**
     * Gibt den kontextbezogenen Prompt zurück.
     */
    public String getContextPrompt(String context) {
        if (context == null) {
            return null;
        }
        String promptKey = "context-" + context;
        String prompt = promptCache.get(promptKey);
        if (prompt == null) {
            log.warn("No prompt found for context: {}", context);
        }
        return prompt;
    }

    /**
     * Lädt den Prompt-Cache neu (z.B. für Hot-Reload).
     */
    public void reloadPrompts() {
        promptCache.clear();
        loadPrompts();
        log.info("Prompts reloaded");
    }

    /**
     * Lädt einen Prompt nach Namen aus dem Cache.
     *
     * @param promptName Der Name des Prompts (ohne .md Extension)
     * @return Der geladene Prompt oder null wenn nicht gefunden
     */
    public String loadPrompt(String promptName) {
        String prompt = promptCache.get(promptName);
        if (prompt == null) {
            log.warn("Prompt not found in cache: {}", promptName);
        }
        return prompt;
    }

    /**
     * Füllt Template-Variablen in einem Prompt.
     * Ersetzt {{VAR_NAME}} mit entsprechenden Werten aus der Map.
     *
     * @param template Der Template-String mit Platzhaltern
     * @param variables Map mit Variablennamen und Werten
     * @return Der ausgefüllte Prompt
     */
    public String fillTemplate(String template, Map<String, String> variables) {
        if (template == null || variables == null) {
            return template;
        }

        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }

        return result;
    }

    /**
     * Erstellt den Prompt für Audio-Transkription.
     *
     * @param language Optional: Die gewünschte Sprache (z.B. "German", "English")
     * @return Der Audio-Transkriptions-Prompt
     */
    public String buildAudioTranscriptionPrompt(String language) {
        String template = promptCache.get("audio-transcription");
        if (template == null) {
            log.warn("Audio transcription prompt not found");
            return "Transcribe this audio to text. Return only the transcribed text.";
        }

        String languageText = language != null && !language.isEmpty()
                ? language
                : "the original language";

        return template.replace("{{LANGUAGE}}", languageText);
    }
}


