package de.jivz.ai_challenge.openrouterservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.ai_challenge.openrouterservice.dto.*;
import de.jivz.ai_challenge.openrouterservice.mcp.MCPFactory;
import de.jivz.ai_challenge.openrouterservice.mcp.model.MCPToolResult;
import de.jivz.ai_challenge.openrouterservice.mcp.model.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ChatWithToolsService - Implementiert den Tool-Execution-Loop für OpenRouter.
 *
 * Workflow:
 * 1. Nachrichten zusammenstellen (system + user)
 * 2. An OpenRouter senden
 * 3. Antwort als JSON parsen (step, tool_calls, answer)
 * 4. Wenn step == "final" → Antwort zurückgeben
 * 5. Wenn step == "tool" → MCP Tools aufrufen, Ergebnisse hinzufügen, wiederholen
 *
 * Features:
 * - Automatische Erkennung der Notwendigkeit von MCP-Tool-Aufrufen
 * - Zyklische Verarbeitung bis zur finalen Antwort
 * - Fehlerbehandlung mit Retry
 * - Unterstützung aller registrierten MCP-Tools
 */
@Service
@Slf4j
public class ChatWithToolsService {

    private static final int MAX_TOOL_ITERATIONS = 10;
    private static final String STEP_TOOL = "tool";
    private static final String STEP_FINAL = "final";

    private final WebClient webClient;
    private final MCPFactory mcpFactory;
    private final PromptLoaderService promptLoader;
    private final ObjectMapper objectMapper;
    private final de.jivz.ai_challenge.openrouterservice.config.OpenRouterProperties properties;
    private final ConversationHistoryService historyService;

    public ChatWithToolsService(
            @Qualifier("openRouterWebClient") WebClient webClient,
            MCPFactory mcpFactory,
            PromptLoaderService promptLoader,
            ObjectMapper objectMapper,
            de.jivz.ai_challenge.openrouterservice.config.OpenRouterProperties properties,
            ConversationHistoryService historyService) {
        this.webClient = webClient;
        this.mcpFactory = mcpFactory;
        this.promptLoader = promptLoader;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.historyService = historyService;
        log.info("ChatWithToolsService initialized");
    }

    /**
     * Hauptmethode: Verarbeitet eine Anfrage mit MCP Tool-Unterstützung.
     *
     * @param request ChatRequest vom Benutzer
     * @return ChatResponse mit der finalen Antwort
     */
    public ChatResponse chatWithTools(ChatRequest request) {
        log.info("🚀 Starting chat with tools - Message: {}",
                request.getMessage().substring(0, Math.min(50, request.getMessage().length())) + "...");

        String conversationId = request.getConversationId();
        String userPrompt = request.getMessage();

        // 1. Nachrichten zusammenstellen (system + history + user)
        List<Message> messages = buildMessages(conversationId, userPrompt);

        // 2. Tool-Loop starten
        String finalAnswer = executeToolLoop(messages, request.getTemperature());

        // 3. In Historie speichern
        saveToHistory(conversationId, userPrompt, finalAnswer);

        // 4. Response erstellen
        return ChatResponse.builder()
                .reply(finalAnswer)
                .model(properties.getDefaultModel())
                .responseTimeMs(System.currentTimeMillis())
                .finishReason("stop")
                .build();
    }

    /**
     * Einfache Chat-Anfrage mit Tool-Unterstützung.
     */
    public ChatResponse chatWithTools(String message) {
        ChatRequest request = ChatRequest.builder()
                .message(message)
                .temperature(properties.getDefaultTemperature())
                .build();
        return chatWithTools(request);
    }

    /**
     * Haupt-Loop: OpenRouter → MCP → OpenRouter → ... → final
     */
    private String executeToolLoop(List<Message> messages, Double temperature) {
        int iteration = 0;

        while (iteration < MAX_TOOL_ITERATIONS) {
            iteration++;
            log.info("🔄 Tool loop iteration: {}", iteration);

            // ====== SCHRITT 1: Anfrage an OpenRouter ======
            String openRouterResponse = callOpenRouter(messages, temperature);
            log.debug("📥 OpenRouter raw response: {}", openRouterResponse);

            // ====== SCHRITT 2: JSON-Antwort parsen ======
            ToolResponse parsed = parseToolResponse(openRouterResponse, messages, temperature);

            if (parsed == null) {
                log.error("❌ Failed to parse OpenRouter response after retries");
                return "Sorry, an error occurred while processing the response. Please try again.";
            }

            // ====== SCHRITT 3: Step prüfen ======
            if (STEP_FINAL.equals(parsed.getStep())) {
                log.info("✅ Got final answer after {} iteration(s)", iteration);
                return parsed.getAnswer() != null ? parsed.getAnswer() : "";
            }

            if (STEP_TOOL.equals(parsed.getStep()) && parsed.getToolCalls() != null && !parsed.getToolCalls().isEmpty()) {
                // ====== SCHRITT 4: MCP Tools aufrufen ======

                // Antwort des Modells als assistant hinzufügen
                messages.add(new Message("assistant", openRouterResponse));

                // Alle Tool-Ergebnisse sammeln
                StringBuilder allToolResults = new StringBuilder();
                allToolResults.append("Tool execution results:\n\n");

                for (ToolResponse.ToolCall toolCall : parsed.getToolCalls()) {
                    String toolResult = executeMcpTool(toolCall);
                    allToolResults.append(String.format("TOOL_RESULT %s:\n%s\n\n",
                            toolCall.getName(), toolResult));
                    log.info("📨 Executed tool: {}", toolCall.getName());
                }

                // Ergebnisse als user-Nachricht hinzufügen
                messages.add(new Message("user", allToolResults.toString().trim()));
                log.info("📨 Added tool results as user message");

                // Weiter im Loop - erneut an OpenRouter
            } else {
                // Unbekannter Step oder leere tool_calls
                log.warn("⚠️ Unknown step or empty tool_calls, treating as final");
                return parsed.getAnswer() != null ? parsed.getAnswer() : openRouterResponse;
            }
        }

        log.error("❌ Max iterations ({}) reached in tool loop", MAX_TOOL_ITERATIONS);
        return "Sorry, maximum number of iterations exceeded. Please rephrase your request.";
    }

    /**
     * Erstellt die Nachrichtenliste für OpenRouter.
     */
    private List<Message> buildMessages(String conversationId, String userPrompt) {
        List<Message> messages = new ArrayList<>();

        // 1. MCP Tools abrufen
        List<ToolDefinition> tools = mcpFactory.getAllToolDefinitions();
        log.info("📋 Got {} MCP tools", tools.size());

        // 2. Kontext durch LLM erkennen lassen
        String context = detectContextViaLlm(userPrompt, tools);
        log.info("🎯 Detected context: {}", context);

        // 3. System-Prompt mit Tools UND erkanntem Kontext erstellen
        String systemPrompt = promptLoader.buildSystemPromptWithToolsAndContext(tools, context);
        messages.add(new Message("system", systemPrompt));

        // 4. Konversationshistorie laden und hinzufügen (wenn conversationId vorhanden)
        if (conversationId != null && !conversationId.isBlank()) {
            List<Message> history = historyService.getHistory(conversationId);
            if (history != null && !history.isEmpty()) {
                messages.addAll(history);
                log.info("📝 Loaded {} messages from history for conversationId: {}", history.size(), conversationId);
            }
        }

        // 5. User-Nachricht hinzufügen
        messages.add(new Message("user", userPrompt));

        log.info("📝 Built {} messages for OpenRouter", messages.size());
        return messages;
    }

    /**
     * Erkennt den Kontext durch einen schnellen LLM-Aufruf.
     * Verwendet ein kleines, schnelles Modell für die Klassifizierung.
     *
     * @param userMessage Die Benutzeranfrage
     * @param tools Die verfügbaren MCP-Tools
     * @return Der erkannte Kontext (docker, tasks, calendar, default)
     */
    private String detectContextViaLlm(String userMessage, List<ToolDefinition> tools) {
        try {
            String detectionPrompt = promptLoader.buildContextDetectionPrompt(userMessage, tools);
            if (detectionPrompt == null) {
                log.warn("Context detection prompt not available, using default");
                return "default";
            }

            // Schneller LLM-Aufruf nur für Kontext-Erkennung
            List<Message> messages = List.of(
                    new Message("system", "You are a context classifier. Respond only with JSON."),
                    new Message("user", detectionPrompt)
            );

            String response = callOpenRouterForContextDetection(messages);
            log.debug("Context detection response: {}", response);

            // JSON parsen und context extrahieren
            return parseContextFromResponse(response);
        } catch (Exception e) {
            log.error("Error detecting context via LLM: {}", e.getMessage());
            return "default";
        }
    }

    /**
     * Schneller OpenRouter-Aufruf für Kontext-Erkennung.
     * Verwendet niedrige max_tokens für schnelle Antwort.
     */
    private String callOpenRouterForContextDetection(List<Message> messages) {
        List<OpenRouterApiRequest.ChatMessage> apiMessages = messages.stream()
                .map(m -> OpenRouterApiRequest.ChatMessage.builder()
                        .role(m.getRole())
                        .content(m.getContent())
                        .build())
                .collect(Collectors.toList());

        OpenRouterApiRequest request = OpenRouterApiRequest.builder()
                .model(properties.getDefaultModel())
                .messages(apiMessages)
                .temperature(0.1) // Niedrige Temperatur für deterministische Antwort
                .maxTokens(100)   // Nur wenig Tokens für JSON-Antwort
                .build();

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenRouterApiResponse.class)
                .map(resp -> resp.getChoices().get(0).getMessage().getContent())
                .block();
    }

    /**
     * Extrahiert den Kontext aus der LLM-Antwort.
     */
    private String parseContextFromResponse(String response) {
        if (response == null || response.isBlank()) {
            return "default";
        }

        try {
            // Versuche JSON zu parsen
            var node = objectMapper.readTree(response);
            String context = node.path("context").asText("default");
            double confidence = node.path("confidence").asDouble(0.0);

            log.info("🎯 Context: {} (confidence: {})", context, confidence);

            // Nur akzeptieren wenn Confidence hoch genug
            if (confidence >= 0.5) {
                return context;
            }
            return "default";
        } catch (JsonProcessingException e) {
            log.warn("Could not parse context response: {}", response);
            return "default";
        }
    }

    /**
     * Ruft die OpenRouter API auf.
     */
    private String callOpenRouter(List<Message> messages, Double temperature) {
        log.info("📤 Calling OpenRouter with {} messages", messages.size());

        try {
            // Messages in das OpenRouter-Format konvertieren
            List<OpenRouterApiRequest.ChatMessage> apiMessages = messages.stream()
                    .map(m -> OpenRouterApiRequest.ChatMessage.builder()
                            .role(m.getRole())
                            .content(m.getContent())
                            .build())
                    .collect(Collectors.toList());

            OpenRouterApiRequest request = OpenRouterApiRequest.builder()
                    .model(properties.getDefaultModel())
                    .messages(apiMessages)
                    .temperature(temperature != null ? temperature : properties.getDefaultTemperature())
                    .maxTokens(properties.getDefaultMaxTokens())
                    .topP(properties.getDefaultTopP())
                    .build();

            OpenRouterApiResponse response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, r -> r.bodyToMono(String.class)
                            .flatMap(body -> Mono.error(new RuntimeException("Client error: " + body))))
                    .onStatus(HttpStatusCode::is5xxServerError, r -> r.bodyToMono(String.class)
                            .flatMap(body -> Mono.error(new RuntimeException("Server error: " + body))))
                    .bodyToMono(OpenRouterApiResponse.class)
                    .block();

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new RuntimeException("Empty response from OpenRouter");
            }

            String reply = response.getChoices().get(0).getMessage().getContent();
            log.info("📥 OpenRouter response received");
            return reply;

        } catch (Exception e) {
            log.error("❌ Error calling OpenRouter: {}", e.getMessage());
            throw new RuntimeException("Failed to call OpenRouter API", e);
        }
    }

    /**
     * Parst die OpenRouter-Antwort als JSON.
     * Bei Fehler wird ein Retry mit Korrektur-Prompt gemacht.
     */
    private ToolResponse parseToolResponse(String response, List<Message> messages, Double temperature) {
        String cleanedResponse = cleanJsonResponse(response);

        try {
            return objectMapper.readValue(cleanedResponse, ToolResponse.class);
        } catch (JsonProcessingException e) {
            log.warn("⚠️ Failed to parse JSON: {}", e.getMessage());

            // Prüfen ob es kein JSON ist - dann als finale Text-Antwort behandeln
            if (cleanedResponse != null && !cleanedResponse.trim().startsWith("{")) {
                log.info("📝 Response is not JSON, treating as final text answer");
                return ToolResponse.builder()
                        .step(STEP_FINAL)
                        .answer(response)
                        .toolCalls(List.of())
                        .build();
            }

            // Retry mit Korrektur-Prompt
            log.warn("⚠️ Attempting retry for malformed JSON");

            messages.add(new Message("assistant", response));
            messages.add(new Message("user", promptLoader.getJsonCorrectionPrompt()));

            try {
                String retryResponse = callOpenRouter(messages, temperature);
                String cleanedRetry = cleanJsonResponse(retryResponse);

                if (cleanedRetry != null && !cleanedRetry.trim().startsWith("{")) {
                    log.info("📝 Retry response is not JSON, treating as final text answer");
                    return ToolResponse.builder()
                            .step(STEP_FINAL)
                            .answer(retryResponse)
                            .toolCalls(List.of())
                            .build();
                }

                return objectMapper.readValue(cleanedRetry, ToolResponse.class);
            } catch (Exception retryException) {
                log.error("❌ Retry also failed: {}", retryException.getMessage());
                return null;
            }
        }
    }

    /**
     * Bereinigt die Antwort von Markdown-Blöcken.
     */
    private String cleanJsonResponse(String response) {
        if (response == null) return null;

        String cleaned = response.trim();

        // Entfernt ```json ... ```
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
     * Führt ein MCP Tool aus.
     */
    private String executeMcpTool(ToolResponse.ToolCall toolCall) {
        log.info("🔧 Executing MCP tool: {} with args: {}", toolCall.getName(), toolCall.getArguments());

        try {
            MCPToolResult result = mcpFactory.route(
                    toolCall.getName(),
                    toolCall.getArguments() != null ? toolCall.getArguments() : Map.of()
            );

            if (result.isSuccess()) {
                log.info("✅ MCP tool {} executed successfully", toolCall.getName());
                return objectMapper.writeValueAsString(result.getResult());
            } else {
                log.warn("⚠️ MCP tool {} returned error: {}", toolCall.getName(), result.getError());
                return String.format("ERROR: %s", result.getError());
            }

        } catch (Exception e) {
            log.error("❌ Error executing MCP tool {}: {}", toolCall.getName(), e.getMessage());
            return String.format("ERROR: %s", e.getMessage());
        }
    }

    /**
     * Speichert die Nachricht und die Antwort in der Konversationshistorie.
     *
     * @param conversationId die Konversations-ID
     * @param userMessage die Benutzernachricht
     * @param assistantReply die Antwort des Assistenten
     */
    private void saveToHistory(String conversationId, String userMessage, String assistantReply) {
        if (conversationId == null || conversationId.isBlank()) {
            log.debug("No conversationId provided, skipping history save");
            return;
        }

        historyService.addMessage(conversationId, "user", userMessage);
        historyService.addMessage(conversationId, "assistant", assistantReply);

        log.info("💾 Saved conversation to history for conversationId: {}", conversationId);
    }
}
