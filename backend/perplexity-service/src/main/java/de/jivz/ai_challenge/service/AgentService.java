package de.jivz.ai_challenge.service;

import de.jivz.ai_challenge.dto.ChatRequest;
import de.jivz.ai_challenge.dto.ChatResponse;
import de.jivz.ai_challenge.dto.Message;
import de.jivz.ai_challenge.dto.ResponseMetrics;
import de.jivz.ai_challenge.service.mcp.McpDto.McpTool;
import de.jivz.ai_challenge.service.perplexity.PerplexityToolClient;
import de.jivz.ai_challenge.service.openrouter.OpenRouterToolClient;
import de.jivz.ai_challenge.service.openrouter.model.OpenRouterResponseWithMetrics;
import de.jivz.ai_challenge.service.perplexity.model.PerplexityResponseWithMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Agent service that orchestrates chat request handling.
 * Follows Single Responsibility Principle and delegates specific tasks to specialized services.
 * Responsibilities:
 * - Validate requests
 * - Orchestrate the conversation flow
 * - Coordinate between history, parsing, and LLM services
 * - Automatically compress history when threshold is reached
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final PerplexityToolClient perplexityToolClient;
    private final OpenRouterToolClient openRouterToolClient;
    private final ConversationHistoryService historyService;
    private final MessageHistoryManager historyManager;
    private final JsonResponseParser jsonResponseParser;
    private final DialogCompressionService compressionService;
    private final MemoryService memoryService;  // ⭐ NEW: PostgreSQL persistence
    private final ChatWithToolsService chatWithToolsService; // ⭐ NEW: MCP Tools integration

    /**
     * Handles a chat request and returns the response.
     *
     * @param request the chat request
     * @return the chat response
     * @throws IllegalArgumentException if the message is empty
     */
    public ChatResponse handle(ChatRequest request) {
        long requestStartTime = System.nanoTime();
        validateRequest(request);

        String conversationId = request.getConversationId();
        String userId = request.getUserId();
        logRequestInfo(request, conversationId);

        // 1. Load conversation history (with automatic compression if needed)
        List<Message> history = loadHistoryWithCompression(conversationId);

        // 2. Prepare history with user message (and JSON instruction if needed)
        historyManager.prepareHistory(history, request);

        // ⭐ NEW: Save user message to PostgreSQL
        memoryService.saveMessage(conversationId, userId, "user", request.getMessage(), null, null);
        log.debug("💾 Saved user message to database");

        // 3. Get response from LLM with metrics
        ChatResponse llmResponse = getLlmResponseWithMetrics(history, request.getTemperature(), request.getProvider(), request.getModel());
        String rawReply = llmResponse.getReply();
        ResponseMetrics metrics = llmResponse.getMetrics();

        // 4. Parse response (if JSON mode enabled)
        String parsedReply = parseResponse(rawReply, request);

        // 5. Save to history (RAM)
        saveToHistory(history, parsedReply, conversationId);

        // ⭐ NEW: Save assistant response to PostgreSQL with metrics
        String modelName = metrics != null ? metrics.getModel() : request.getModel();
        memoryService.saveMessage(conversationId, userId, "assistant", parsedReply, modelName, metrics);
        log.debug("💾 Saved assistant message to database with metrics");

        long requestEndTime = System.nanoTime();
        long totalRequestTimeMs = (requestEndTime - requestStartTime) / 1_000_000;
        log.info("⏱️ Total request processing time: {} ms", totalRequestTimeMs);

        return buildResponse(parsedReply, request.getProvider(), metrics);
    }

    /**
     * Validates the incoming request.
     *
     * @param request The chat request to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateRequest(ChatRequest request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            log.warn("Received empty message from user: {}", request.getUserId());
            throw new IllegalArgumentException("Message cannot be empty");
        }
    }

    /**
     * Logs request information for debugging.
     *
     * @param request        The chat request
     * @param conversationId The conversation ID
     */
    private void logRequestInfo(ChatRequest request, String conversationId) {
        log.debug("Processing request for user: {}, conversationId: {}, jsonMode: {}",
                request.getUserId(), conversationId, request.isJsonMode());
    }

    /**
     * ⭐ UPDATED: Loads conversation history with automatic compression and summary reuse.
     *
     * Key improvements:
     * 1. Checks if a saved summary exists in PostgreSQL
     * 2. If yes - uses summary + recent messages (saves tokens!)
     * 3. If no - loads full history
     * 4. Automatically checks if new compression is needed
     * 5. Uses compressed version if available
     *
     * Token savings:
     * - Summary created once, reused forever (0 tokens to create)
     * - Only recent messages after summary are sent
     * - LLM understands full context with fewer tokens
     *
     * Flow:
     * 1. Try to load optimized history with saved summary
     * 2. Check if compression threshold reached (5+ messages)
     * 3. If yes - compress and save summary
     * 4. Use compressed version if available, otherwise optimized history
     *
     * @param conversationId The conversation identifier
     * @return The conversation history (optimized with summary or full)
     */
    private List<Message> loadHistoryWithCompression(String conversationId) {
        // ⭐ FIRST: Try to load with saved summary from PostgreSQL
        List<Message> optimizedHistory = memoryService.loadHistoryForLLM(conversationId);
        log.info("📚 Loaded {} messages for conversation: {} (using saved summary if available)",
                optimizedHistory.size(), conversationId);

        // Check if compression is needed and perform it
        boolean wasCompressed = compressionService.checkAndCompress(conversationId);

        if (wasCompressed) {
            log.info("🗜️ History was compressed, new summary saved to PostgreSQL");
            // Reload with the newly created summary
            return memoryService.loadHistoryForLLM(conversationId);
        }

        // Try to use compressed version if available (for RAM cache efficiency)
        if (compressionService.hasCompressedVersion(conversationId)) {
            List<Message> compressedHistory = compressionService.getCompressedHistory(conversationId);
            log.info("✅ Using compressed history from RAM: {} messages (optimized: {})",
                    compressedHistory.size(), optimizedHistory.size());
            return compressedHistory;
        }

        // Otherwise use optimized history (with saved summary if available)
        log.debug("Using optimized history with saved summary (compression not needed yet)");
        return optimizedHistory;
    }

    /**
     * Gets response from the LLM with metrics.
     *
     * @param history The conversation history
     * @param temperature The temperature parameter for response generation
     * @param provider The AI provider to use (perplexity or openrouter)
     * @param model The specific model to use (optional)
     * @return The response with metrics
     */
    private ChatResponse getLlmResponseWithMetrics(List<Message> history, Double temperature, String provider, String model) {
        if ("openrouter".equalsIgnoreCase(provider)) {
            OpenRouterResponseWithMetrics response = openRouterToolClient.requestCompletionWithMetrics(history, temperature, model);
            String rawReply = response.getReply();
            log.info("🔍 Raw reply from OpenRouter with model {} (first 200 chars): {}",
                    model, rawReply.substring(0, Math.min(200, rawReply.length())));

            ResponseMetrics metrics = ResponseMetrics.builder()
                    .inputTokens(response.getInputTokens())
                    .outputTokens(response.getOutputTokens())
                    .totalTokens(response.getTotalTokens())
                    .cost(response.getCost())
                    .responseTimeMs(response.getResponseTimeMs())
                    .model(response.getModel())
                    .provider("openrouter")
                    .build();

            return ChatResponse.builder().reply(rawReply).metrics(metrics).build();
        } else {
            PerplexityResponseWithMetrics response = perplexityToolClient.requestCompletionWithMetrics(history, temperature, null);
            String rawReply = response.getReply();
            log.info("🔍 Raw reply from Perplexity (first 200 chars): {}",
                    rawReply.substring(0, Math.min(200, rawReply.length())));

            ResponseMetrics metrics = ResponseMetrics.builder()
                    .inputTokens(response.getInputTokens())
                    .outputTokens(response.getOutputTokens())
                    .totalTokens(response.getTotalTokens())
                    .cost(response.getCost())
                    .responseTimeMs(response.getResponseTimeMs())
                    .model(response.getModel())
                    .provider("perplexity")
                    .build();

            return ChatResponse.builder().reply(rawReply).metrics(metrics).build();
        }
    }

    /**
     * Parses the response based on request mode.
     *
     * @param rawReply The raw response from LLM
     * @param request  The chat request
     * @return Parsed response
     */
    private String parseResponse(String rawReply, ChatRequest request) {
        if (!request.isJsonMode()) {
            return rawReply;
        }

        String parsedReply = jsonResponseParser.parse(rawReply, request);
        log.info("📦 Parsed reply (first 100 chars): {}",
                parsedReply.substring(0, Math.min(100, parsedReply.length())));
        return parsedReply;
    }

    /**
     * ⭐ UPDATED: Saves conversation to BOTH full and compressed history.
     *
     * Always saves to full history (for compression source).
     * If compressed version exists, also updates it with the new message.
     *
     * @param history        The conversation history
     * @param reply          The assistant's reply
     * @param conversationId The conversation identifier
     */
    private void saveToHistory(List<Message> history, String reply, String conversationId) {
        historyManager.addAssistantResponse(history, reply);

        // Determine if we're working with compressed or full history
        boolean isCompressed = compressionService.hasCompressedVersion(conversationId) &&
                history.size() < historyService.getHistory(conversationId).size();

        if (isCompressed) {
            // Save back to compressed version
            String compressedId = conversationId + "_compressed";
            historyService.saveHistory(compressedId, history);
            log.info("💾 Saved to compressed history ({} messages)", history.size());

            // Also add to full history
            List<Message> fullHistory = historyService.getHistory(conversationId);
            fullHistory.add(new Message("assistant", reply));
            historyService.saveHistory(conversationId, fullHistory);
            log.info("💾 Also saved to full history ({} messages)", fullHistory.size());
        } else {
            // Save to full history only
            historyService.saveHistory(conversationId, history);
            log.info("💾 Saved conversation history ({} messages) for conversationId: {}",
                    history.size(), conversationId);
        }
    }

    /**
     * Builds the chat response DTO with metrics.
     *
     * @param reply The parsed reply
     * @param provider The AI provider used
     * @param metrics The response metrics
     * @return The chat response
     */
    private ChatResponse buildResponse(String reply, String provider, ResponseMetrics metrics) {
        String toolName = "openrouter".equalsIgnoreCase(provider) ? "OpenRouterToolClient" : "PerplexityToolClient";
        return ChatResponse.builder()
                .reply(reply)
                .toolName(toolName)
                .timestamp(new java.util.Date())
                .metrics(metrics)
                .build();
    }

    /**
     * ⭐ NEW: Handles chat with MCP Tool support.
     *
     * Delegates to ChatWithToolsService for the Sonar + MCP loop:
     * 1. Sonar decides if MCP tool is needed
     * 2. If yes, calls MCP, adds result to history, asks Sonar again
     * 3. Repeats until Sonar gives final answer
     *
     * @param request the chat request
     * @return the chat response with MCP tool integration
     */
    public ChatResponse handleWithMcpTools(ChatRequest request) {
        validateRequest(request);
        log.info("🔧 Handling request with MCP tools for user: {}", request.getUserId());

        return chatWithToolsService.chatWithTools(request);
    }

    /**
     * ⭐ NEW: Returns list of available MCP Tools.
     *
     * @return List of MCP tools from the MCP server
     */
    public List<McpTool> getAvailableMcpTools() {
        log.info("📋 Getting available MCP tools");
        return chatWithToolsService.getAvailableTools();
    }
}
