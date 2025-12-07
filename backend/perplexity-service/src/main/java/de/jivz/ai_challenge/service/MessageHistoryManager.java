package de.jivz.ai_challenge.service;

import de.jivz.ai_challenge.dto.ChatRequest;
import de.jivz.ai_challenge.dto.Message;
import de.jivz.ai_challenge.service.strategy.JsonInstructionStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service responsible for managing conversation message history.
 * Handles adding messages and JSON instructions to the history.
 * Follows Single Responsibility Principle.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageHistoryManager {

    private final List<JsonInstructionStrategy> instructionStrategies;

    /**
     * Prepares the message history with the system prompt, user's message and optional JSON instruction.
     * The system prompt is always set as the first message, replacing any existing system message.
     *
     * @param history The existing conversation history
     * @param request The chat request containing the user's message
     */
    public void prepareHistory(List<Message> history, ChatRequest request) {
        // Handle system prompt - always update/set as first message
        updateSystemPrompt(history, request.getSystemPrompt());

        if (request.isJsonMode()) {
            addJsonModeMessage(history, request);
        } else {
            addNormalMessage(history, request.getMessage());
        }
    }

    /**
     * Updates or adds the system prompt as the first message in the history.
     * If a system prompt already exists, it will be replaced with the new one.
     *
     * @param history      The conversation history
     * @param systemPrompt The system prompt to set (can be null or empty)
     */
    private void updateSystemPrompt(List<Message> history, String systemPrompt) {
        // Remove existing system message if present
        if (!history.isEmpty() && "system".equals(history.getFirst().getRole())) {
            history.removeFirst();
            log.debug("Removed existing system prompt");
        }

        // Add new system prompt if provided
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            history.addFirst(new Message("system", systemPrompt));
            log.info("🎭 Set system prompt: {}...",
                    systemPrompt.substring(0, Math.min(50, systemPrompt.length())));
        }
    }

    /**
     * Adds the user message with JSON instruction prepended.
     *
     * @param history The conversation history
     * @param request The chat request
     */
    private void addJsonModeMessage(List<Message> history, ChatRequest request) {
        String instruction = buildJsonInstruction(request);
        String fullMessage = instruction + "\n\nQuestion: " + request.getMessage();

        removeLastUserMessageIfExists(history);
        history.add(new Message("user", fullMessage));

        log.info("✅ Added JSON mode instruction (auto-schema: {}, custom-schema: {})",
                request.isAutoSchema(), request.getJsonSchema() != null);
    }

    /**
     * Adds a normal user message without JSON instruction.
     *
     * @param history The conversation history
     * @param message The user's message
     */
    private void addNormalMessage(List<Message> history, String message) {
        history.add(new Message("user", message));
    }

    /**
     * Removes the last user message if it exists.
     * Used to replace it with an updated message including instructions.
     *
     * @param history The conversation history
     */
    private void removeLastUserMessageIfExists(List<Message> history) {
        if (!history.isEmpty() && "user".equals(history.getLast().getRole())) {
            history.removeLast();
        }
    }

    /**
     * Builds the JSON instruction using the appropriate strategy.
     *
     * @param request The chat request
     * @return The JSON instruction string
     */
    private String buildJsonInstruction(ChatRequest request) {
        return instructionStrategies.stream()
                .filter(strategy -> strategy.canHandle(request.getJsonSchema(), request.isAutoSchema()))
                .findFirst()
                .map(JsonInstructionStrategy::buildInstruction)
                .orElseThrow(() -> new IllegalStateException("No suitable JSON instruction strategy found"));
    }

    /**
     * Adds the assistant's response to the history.
     *
     * @param history  The conversation history
     * @param response The assistant's response
     */
    public void addAssistantResponse(List<Message> history, String response) {
        history.add(new Message("assistant", response));
    }
}

