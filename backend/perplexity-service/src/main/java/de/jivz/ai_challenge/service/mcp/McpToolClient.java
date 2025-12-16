package de.jivz.ai_challenge.service.mcp;

import de.jivz.ai_challenge.service.mcp.McpDto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * MCP Tool Client
 * Kommuniziert mit dem MCP Service um Tools zu nutzen
 *
 * Features:
 * - Liste aller verfügbaren Tools
 * - Tool-Ausführung (inkl. google-service Tools)
 * - Provider-Verwaltung
 * - Status-Abfragen
 */
@Service
@Slf4j
public class McpToolClient {

    final WebClient mcpWebClient;

    public McpToolClient( WebClient mcpWebClient ) {
        this.mcpWebClient = mcpWebClient;
    }

    /**
     * Holt alle verfügbaren MCP Tools
     * Inkludiert: native tools, perplexity tools, google-service tools, etc.
     */
    public List<McpTool> getAllTools() {
        try {
            List<McpTool> tools =  mcpWebClient.get()
                    .uri("/mcp/tools")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToFlux(McpTool.class)
                    .collectList()
                    .block();

            log.info("✅ Retrieved {} MCP tools", tools != null ? tools.size() : 0);
            return tools;

        } catch (Exception e) {
            log.error("❌ Error fetching MCP tools: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch MCP tools", e);
        }
    }



    /**
     * Führt ein MCP Tool aus
     *
     * @param toolName Name des Tools (z.B. "google_tasks_list", "add_numbers", etc.)
     * @param arguments Argumente für das Tool
     * @return Tool-Ausführungsergebnis
     */
    public ToolExecutionResponse executeTool(String toolName, Map<String, Object> arguments) {
        try {
            log.info("🔧 Executing MCP tool '{}' with args: {}", toolName, arguments);

            ToolExecutionRequest request = ToolExecutionRequest.builder()
                    .toolName(toolName)
                    .arguments(arguments)
                    .build();


            ToolExecutionResponse result =  mcpWebClient.post()
                    .uri("/mcp/execute")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(BodyInserters.fromValue(request))
                    .retrieve()
                    .bodyToMono(ToolExecutionResponse.class)
                    .block();


            if (result != null && result.isSuccess()) {
                log.info("✅ Tool '{}' executed successfully", toolName);
            } else {
                log.warn("⚠️ Tool '{}' execution failed: {}", toolName,
                        result != null ? result.getError() : "Unknown error");
            }

            return result;

        } catch (Exception e) {
            log.error("❌ Error executing tool '{}': {}", toolName, e.getMessage());
            return ToolExecutionResponse.builder()
                    .success(false)
                    .toolName(toolName)
                    .error("Execution failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Holt den Status des MCP Servers
     */
    public McpStatus getStatus() {
        try {
            log.debug("Fetching MCP server status from: /mcp/status");

            McpStatus status =  mcpWebClient.get()
                    .uri("/mcp/status")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToMono(McpStatus.class)
                    .block();
            log.info("✅ MCP Server status: {}", status != null ? status.getStatus() : "unknown");
            return status;

        } catch (Exception e) {
            log.error("❌ Error fetching MCP status: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch MCP status", e);
        }
    }

    /**
     * Holt alle registrierten Provider
     */
    public ProviderInfo getProviders() {
        try {
            log.debug("Fetching MCP providers from: /mcp/providers");

            ProviderInfo info =  mcpWebClient.get()
                    .uri("/mcp/providers")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToMono(ProviderInfo.class)
                    .block();

            log.info("✅ Retrieved {} providers",
                    info != null && info.getProviders() != null ? info.getProviders().size() : 0);

            return info;
        } catch (Exception e) {
            log.error("❌ Error fetching providers: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch providers", e);
        }
    }

    /**
     * Führt Google Tasks Tool aus - Convenience-Methode
     */
    public ToolExecutionResponse getGoogleTasks() {
        return executeTool("google_tasks_get", Map.of());
    }

    /**
     * Erstellt eine Google Task - Convenience-Methode
     */
    public ToolExecutionResponse createGoogleTask(String title, String notes) {
        return executeTool("google_tasks_create", Map.of(
                "title", title,
                "notes", notes != null ? notes : ""
        ));
    }

    /**
     * Führt mathematische Operation aus - Convenience-Methode
     */
    public ToolExecutionResponse addNumbers(double a, double b) {
        return executeTool("add_numbers", Map.of(
                "a", a,
                "b", b
        ));
    }
}

