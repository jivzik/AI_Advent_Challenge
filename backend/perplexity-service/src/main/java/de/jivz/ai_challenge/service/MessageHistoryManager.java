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
     * Prepares the message history with the user's message and optional JSON instruction.
     *
     * @param history The existing conversation history
     * @param request The chat request containing the user's message
     */
    public void prepareHistory(List<Message> history, ChatRequest request) {
        if (request.isJsonMode()) {
            addJsonModeMessage(history, request);
        } else {
            addNormalMessage(history, request.getMessage());
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

