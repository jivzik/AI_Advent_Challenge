package de.jivz.mcp.service;

import de.jivz.mcp.model.McpTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Perplexity Tool Provider
 * Integration mit dem Perplexity AI Service für erweiterte Suchanfragen
 */
@Component
@Slf4j
public class PerplexityToolProvider implements ToolProvider {

    private final List<McpTool> availableTools;
    // TODO: Perplexity Client wird später injiziert
    // private final PerplexityToolClient perplexityClient;

    public PerplexityToolProvider() {
        this.availableTools = initializeTools();
        log.info("Perplexity Tool Provider initialized with {} tools", availableTools.size());
    }

    @Override
    public String getProviderName() {
        return "perplexity";
    }

    @Override
    public List<McpTool> getTools() {
        return new ArrayList<>(availableTools);
    }

    @Override
    public Object executeTool(String toolName, Map<String, Object> arguments) {
        log.info("Executing Perplexity tool: {} with arguments: {}", toolName, arguments);

        return switch (toolName) {
            case "perplexity_search" -> perplexitySearch(arguments);
            case "perplexity_chat" -> perplexityChat(arguments);
            default -> throw new IllegalArgumentException("Unknown Perplexity tool: " + toolName);
        };
    }

    @Override
    public boolean supportsTool(String toolName) {
        return availableTools.stream()
                .anyMatch(tool -> tool.getName().equals(toolName));
    }

    /**
     * Initialize Perplexity-specific tools
     */
    private List<McpTool> initializeTools() {
        List<McpTool> tools = new ArrayList<>();

        // Tool 1: Perplexity Search
        tools.add(McpTool.builder()
                .name("perplexity_search")
                .description("Search the web using Perplexity AI with citations and up-to-date information")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "query", Map.of(
                                        "type", "string",
                                        "description", "The search query"
                                ),
                                "model", Map.of(
                                        "type", "string",
                                        "description", "Perplexity model to use (optional)",
                                        "enum", List.of(
                                                "llama-3.1-sonar-small-128k-online",
                                                "llama-3.1-sonar-large-128k-online",
                                                "llama-3.1-sonar-huge-128k-online"
                                        )
                                ),
                                "temperature", Map.of(
                                        "type", "number",
                                        "description", "Temperature for response generation (0.0-1.0)",
                                        "minimum", 0.0,
                                        "maximum", 1.0,
                                        "default", 0.2
                                )
                        ),
                        "required", List.of("query")
                ))
                .build());

        // Tool 2: Perplexity Chat
        tools.add(McpTool.builder()
                .name("perplexity_chat")
                .description("Chat with Perplexity AI for complex questions and multi-turn conversations")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "messages", Map.of(
                                        "type", "array",
                                        "description", "Array of chat messages",
                                        "items", Map.of(
                                                "type", "object",
                                                "properties", Map.of(
                                                        "role", Map.of(
                                                                "type", "string",
                                                                "enum", List.of("user", "assistant", "system")
                                                        ),
                                                        "content", Map.of(
                                                                "type", "string"
                                                        )
                                                ),
                                                "required", List.of("role", "content")
                                        )
                                ),
                                "model", Map.of(
                                        "type", "string",
                                        "description", "Perplexity model to use (optional)"
                                ),
                                "temperature", Map.of(
                                        "type", "number",
                                        "description", "Temperature for response generation",
                                        "default", 0.7
                                )
                        ),
                        "required", List.of("messages")
                ))
                .build());

        return tools;
    }

    // Tool Implementations

    /**
     * Perplexity Search Tool
     * TODO: Implementierung mit PerplexityToolClient
     */
    private Map<String, Object> perplexitySearch(Map<String, Object> args) {
        String query = (String) args.get("query");
        String model = (String) args.getOrDefault("model", "llama-3.1-sonar-small-128k-online");
        Double temperature = args.containsKey("temperature")
                ? ((Number) args.get("temperature")).doubleValue()
                : 0.2;

        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Parameter 'query' is required");
        }

        log.info("Perplexity search: query='{}', model='{}', temperature={}", query, model, temperature);

        // TODO: Integration mit PerplexityToolClient
        // Temporäre Mock-Antwort
        Map<String, Object> result = new HashMap<>();
        result.put("query", query);
        result.put("model", model);
        result.put("answer", "Mock answer - Perplexity integration pending");
        result.put("citations", List.of());
        result.put("status", "mock");

        return result;
    }

    /**
     * Perplexity Chat Tool
     * TODO: Implementierung mit PerplexityToolClient
     */
    private Map<String, Object> perplexityChat(Map<String, Object> args) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) args.get("messages");
        String model = (String) args.get("model");
        Double temperature = args.containsKey("temperature")
                ? ((Number) args.get("temperature")).doubleValue()
                : 0.7;

        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("Parameter 'messages' is required and cannot be empty");
        }

        log.info("Perplexity chat: {} messages, model='{}', temperature={}",
                messages.size(), model, temperature);

        // TODO: Integration mit PerplexityToolClient
        // Temporäre Mock-Antwort
        Map<String, Object> result = new HashMap<>();
        result.put("messages", messages);
        result.put("model", model);
        result.put("response", "Mock response - Perplexity integration pending");
        result.put("status", "mock");

        return result;
    }
}

