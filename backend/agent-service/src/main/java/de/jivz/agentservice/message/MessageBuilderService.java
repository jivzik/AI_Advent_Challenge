package de.jivz.agentservice.message;


import de.jivz.agentservice.dto.Message;
import de.jivz.agentservice.mcp.model.ToolDefinition;
import de.jivz.agentservice.service.PromptLoaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service für das Zusammenstellen von Chat-Nachrichten.
 * Koordiniert System-Prompt, Kontext-Erkennung, Historie und User-Nachricht.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MessageBuilderService {

/*    private final ContextDetectionService contextDetectionService;
    private final ConversationHistoryService historyService;*/
    private final PromptLoaderService promptLoader;

    /**
     * Erstellt die vollständige Nachrichtenliste für eine Chat-Anfrage.
     *
     * @param conversationId Die Konversations-ID (optional)
     * @param userPrompt Die Benutzeranfrage
     * @param tools Die verfügbaren MCP-Tools
     * @return Die zusammengestellte Nachrichtenliste
     */
    public List<Message> buildMessages(String conversationId, String userPrompt, List<ToolDefinition> tools) {
        List<Message> messages = new ArrayList<>();

        // 1. Kontext erkennen
        /*String context = contextDetectionService.detectContext(userPrompt, tools);
        log.info("🎯 Detected context: {}", context);*/

        // 2. System-Prompt mit Tools und Kontext erstellen
        String systemPrompt = promptLoader.buildSystemPromptWithToolsAndContext(tools, null);
        messages.add(new Message("system", systemPrompt));

        // 3. Konversationshistorie laden (falls vorhanden)
        /*if (conversationId != null && !conversationId.isBlank()) {
            List<Message> history = historyService.getHistory(conversationId);
            if (history != null && !history.isEmpty()) {
                messages.addAll(history);
                log.info("📝 Loaded {} messages from history for conversationId: {}",
                        history.size(), conversationId);
            }
        }*/

        // 4. User-Nachricht hinzufügen
        messages.add(new Message("user", userPrompt));

        log.info("📝 Built {} messages for chat request", messages.size());
        return messages;
    }
}

