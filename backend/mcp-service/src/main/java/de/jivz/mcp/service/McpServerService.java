package de.jivz.mcp.service;

import de.jivz.mcp.model.McpTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP Server Service - Orchestrator für alle Tool-Provider
 * Verwaltet mehrere Tool-Provider (Native, Perplexity, etc.)
 * und delegiert Tool-Aufrufe an den entsprechenden Provider
 */
@Service
@Slf4j
public class McpServerService {

    private final Map<String, ToolProvider> providers;
    private final List<McpTool> allTools;

    public McpServerService(List<ToolProvider> toolProviders) {
        this.providers = new HashMap<>();

        // Registriere alle verfügbaren Tool-Provider
        for (ToolProvider provider : toolProviders) {
            providers.put(provider.getProviderName(), provider);
            log.info("Registered tool provider: {} with {} tools",
                    provider.getProviderName(),
                    provider.getTools().size());
        }

        // Sammle alle Tools von allen Providern
        this.allTools = providers.values().stream()
                .flatMap(provider -> provider.getTools().stream())
                .collect(Collectors.toList());

        log.info("Initialized MCP Server with {} providers and {} total tools",
                providers.size(), allTools.size());
    }


    /**
     * Get list of all available tools from all providers
     */
    public List<McpTool> listTools() {
        log.debug("Listing {} available tools from {} providers",
                allTools.size(), providers.size());
        return new ArrayList<>(allTools);
    }

    /**
     * Get tools from a specific provider
     */
    public List<McpTool> listToolsByProvider(String providerName) {
        ToolProvider provider = providers.get(providerName);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown provider: " + providerName);
        }
        return provider.getTools();
    }

    /**
     * Get list of all registered providers
     */
    public List<String> listProviders() {
        return new ArrayList<>(providers.keySet());
    }

    /**
     * Execute a tool - automatically finds the correct provider
     */
    public Object executeTool(String toolName, Map<String, Object> arguments) {
        log.info("Executing tool: {} with arguments: {}", toolName, arguments);

        // Finde den Provider, der dieses Tool unterstützt
        for (ToolProvider provider : providers.values()) {
            if (provider.supportsTool(toolName)) {
                log.debug("Tool '{}' handled by provider '{}'", toolName, provider.getProviderName());
                return provider.executeTool(toolName, arguments);
            }
        }

        throw new IllegalArgumentException("Unknown tool: " + toolName +
                " (available tools: " + allTools.stream()
                        .map(McpTool::getName)
                        .collect(Collectors.joining(", ")) + ")");
    }

    /**
     * Get a specific tool by name from any provider
     */
    public Optional<McpTool> getTool(String toolName) {
        return allTools.stream()
                .filter(tool -> tool.getName().equals(toolName))
                .findFirst();
    }

    /**
     * Get execution statistics by provider
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_providers", providers.size());
        stats.put("total_tools", allTools.size());

        Map<String, Integer> providerStats = new HashMap<>();
        for (Map.Entry<String, ToolProvider> entry : providers.entrySet()) {
            providerStats.put(entry.getKey(), entry.getValue().getTools().size());
        }
        stats.put("tools_by_provider", providerStats);

        return stats;
    }
}

