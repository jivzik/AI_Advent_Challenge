package de.jivz.ai_challenge.openrouterservice.service;

import de.jivz.ai_challenge.openrouterservice.config.OpenRouterProperties;
import de.jivz.ai_challenge.openrouterservice.dto.*;
import de.jivz.ai_challenge.openrouterservice.mcp.MCPFactory;
import de.jivz.ai_challenge.openrouterservice.mcp.model.ToolDefinition;
import de.jivz.ai_challenge.openrouterservice.persistence.MemoryRepository;
import de.jivz.ai_challenge.openrouterservice.service.message.MessageBuilderService;
import de.jivz.ai_challenge.openrouterservice.service.orchestrator.ToolExecutionOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ChatWithToolsService - High-Level Orchestrator für Chat mit MCP-Tool-Unterstützung.
 *
 * Refactored nach SOLID-Prinzipien und Clean Code:
 * - Single Responsibility: Orchestriert nur den High-Level-Workflow
 * - Open/Closed: Erweiterbar durch Strategy Pattern in Sub-Services
 * - Liskov Substitution: Alle Services können durch Interfaces ersetzt werden
 * - Interface Segregation: Jeder Service hat eine klare, fokussierte Schnittstelle
 * - Dependency Inversion: Abhängigkeiten werden injiziert
 *
 * Workflow:
 * 1. Nachrichten zusammenstellen (delegiert an MessageBuilderService)
 * 2. Tool-Execution-Loop durchführen (delegiert an ToolExecutionOrchestrator)
 * 3. Konversationshistorie speichern
 * 4. Response zurückgeben
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatWithToolsService {

    private final MCPFactory mcpFactory;
    private final MessageBuilderService messageBuilderService;
    private final ToolExecutionOrchestrator toolExecutionOrchestrator;
    private final ConversationHistoryService historyService;
    private final MemoryRepository memoryRepository;
    private final OpenRouterProperties properties;

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

        // 1. MCP Tools abrufen
        List<ToolDefinition> tools = mcpFactory.getAllToolDefinitions();
        log.info("📋 Got {} MCP tools", tools.size());

        // 2. Nachrichten zusammenstellen (delegiert an MessageBuilderService)
        List<Message> messages = messageBuilderService.buildMessages(conversationId, userPrompt, tools);

        // 3. Tool-Loop ausführen (delegiert an ToolExecutionOrchestrator)
        String finalAnswer = toolExecutionOrchestrator.executeToolLoop(messages, request.getTemperature());

        // 4. In Historie speichern
        saveToHistory(conversationId, userPrompt, finalAnswer);

        // 5. Response erstellen
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

        // Speichere user-Nachricht
        historyService.addMessage(conversationId, "user", userMessage, null);

        // Speichere assistant-Antwort mit Model-Info
        historyService.addMessage(conversationId, "assistant", assistantReply, properties.getDefaultModel());

        log.info("💾 Saved conversation to history for conversationId: {}", conversationId);
    }

    /**
     * Gibt alle eindeutigen Konversations-IDs aus der Datenbank zurück.
     *
     * @return Liste aller conversation_id die in der memory_entries Tabelle vorhanden sind
     */
    public List<String> getAllConversationIds() {
        try {
            return memoryRepository.findAllConversationIds();
        } catch (Exception e) {
            log.error("Error retrieving all conversation IDs: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}

