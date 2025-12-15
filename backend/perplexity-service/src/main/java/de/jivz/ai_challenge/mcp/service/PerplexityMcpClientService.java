package de.jivz.ai_challenge.mcp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.ai_challenge.mcp.model.McpError;
import de.jivz.ai_challenge.mcp.model.McpRequest;
import de.jivz.ai_challenge.mcp.model.McpResponse;
import de.jivz.ai_challenge.mcp.model.McpTool;
import de.jivz.ai_challenge.mcp.model.McpToolsListResult;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MCP Client Service for Perplexity MCP Server
 * Communicates with Node.js MCP server via STDIO
 */
@Service
@Slf4j
public class PerplexityMcpClientService {

    private final ObjectMapper objectMapper;
    private final AtomicInteger requestIdCounter = new AtomicInteger(1);

    @Value("${mcp.perplexity.node.executable:node}")
    private String nodeExecutable;

    @Value("${mcp.perplexity.server.path}")
    private String serverScriptPath;

    private Process mcpServerProcess;
    private BufferedWriter processInput;
    private BufferedReader processOutput;
    private BufferedReader processError;
    private boolean initialized = false;

    public PerplexityMcpClientService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Initialize connection to Perplexity MCP server on startup
     */
    @PostConstruct
    public void initialize() {
        try {
            log.info("================================================================================");
            log.info("Initializing Perplexity MCP Client");
            log.info("================================================================================");
            log.info("Node executable: {}", nodeExecutable);
            log.info("MCP Server script: {}", serverScriptPath);

            // Start Node.js MCP server process
            ProcessBuilder processBuilder = new ProcessBuilder(nodeExecutable, serverScriptPath);
            processBuilder.redirectErrorStream(false);

            mcpServerProcess = processBuilder.start();
            processInput = new BufferedWriter(new OutputStreamWriter(mcpServerProcess.getOutputStream()));
            processOutput = new BufferedReader(new InputStreamReader(mcpServerProcess.getInputStream()));
            processError = new BufferedReader(new InputStreamReader(mcpServerProcess.getErrorStream()));

            // Start error stream reader thread
            startErrorStreamReader();

            // Wait a bit for server to start
            Thread.sleep(500);

            // Send initialize request
            Map<String, Object> params = new HashMap<>();
            params.put("protocolVersion", "2024-11-05");
            params.put("capabilities", Map.of());
            params.put("clientInfo", Map.of(
                "name", "Java Perplexity MCP Client",
                "version", "1.0.0"
            ));

            McpRequest initRequest = McpRequest.builder()
                    .jsonrpc("2.0")
                    .id(requestIdCounter.getAndIncrement())
                    .method("initialize")
                    .params(params)
                    .build();

            McpResponse<?> initResponse = sendRequest(initRequest);

            if (initResponse.getError() != null) {
                throw new IOException("Failed to initialize MCP connection: " + initResponse.getError().getMessage());
            }

            log.info("MCP connection initialized successfully");

            // Send initialized notification
            McpRequest initializedNotification = McpRequest.builder()
                    .jsonrpc("2.0")
                    .method("notifications/initialized")
                    .build();

            sendNotification(initializedNotification);
            initialized = true;

            log.info("Perplexity MCP Client ready");
            log.info("================================================================================");

        } catch (Exception e) {
            log.error("Failed to initialize Perplexity MCP Client", e);
            initialized = false;
        }
    }

    /**
     * Start thread to read error stream
     */
    private void startErrorStreamReader() {
        new Thread(() -> {
            try {
                String line;
                while ((line = processError.readLine()) != null) {
                    log.debug("MCP Server stderr: {}", line);
                }
            } catch (IOException e) {
                log.debug("Error stream reader stopped", e);
            }
        }, "mcp-error-reader").start();
    }

    /**
     * Get list of available tools from Perplexity MCP server
     */
    public List<McpTool> listTools() throws IOException {
        if (!initialized) {
            throw new IllegalStateException("MCP client not initialized");
        }

        log.info("Requesting tools list from Perplexity MCP server");

        McpRequest request = McpRequest.builder()
                .jsonrpc("2.0")
                .id(requestIdCounter.getAndIncrement())
                .method("tools/list")
                .params(new HashMap<>())
                .build();

        @SuppressWarnings("unchecked")
        McpResponse<Map<String, Object>> response = (McpResponse<Map<String, Object>>) sendRequest(request);

        if (response.getError() != null) {
            throw new IOException("Failed to list tools: " + response.getError().getMessage());
        }

        // Parse the result
        McpToolsListResult result = objectMapper.convertValue(response.getResult(), McpToolsListResult.class);

        log.info("Received {} tools from Perplexity MCP server", result.getTools().size());
        return result.getTools();
    }

    /**
     * Execute a tool on the Perplexity MCP server
     */
    public Object executeTool(String toolName, Map<String, Object> arguments) throws IOException {
        if (!initialized) {
            throw new IllegalStateException("MCP client not initialized");
        }

        log.info("Executing tool '{}' with arguments: {}", toolName, arguments);

        Map<String, Object> params = new HashMap<>();
        params.put("name", toolName);
        params.put("arguments", arguments);

        McpRequest request = McpRequest.builder()
                .jsonrpc("2.0")
                .id(requestIdCounter.getAndIncrement())
                .method("tools/call")
                .params(params)
                .build();

        @SuppressWarnings("unchecked")
        McpResponse<Map<String, Object>> response = (McpResponse<Map<String, Object>>) sendRequest(request);

        if (response.getError() != null) {
            throw new IOException("Failed to execute tool: " + response.getError().getMessage());
        }

        return response.getResult();
    }

    /**
     * Send a request and wait for response
     */
    private synchronized McpResponse<?> sendRequest(McpRequest request) throws IOException {
        String jsonRequest = objectMapper.writeValueAsString(request);
        log.debug("Sending request: {}", jsonRequest);

        processInput.write(jsonRequest);
        processInput.newLine();
        processInput.flush();

        String jsonResponse = processOutput.readLine();
        log.debug("Received response: {}", jsonResponse);

        if (jsonResponse == null) {
            throw new IOException("No response from MCP server (server may have crashed)");
        }

        return objectMapper.readValue(jsonResponse, new TypeReference<McpResponse<Map<String, Object>>>() {});
    }

    /**
     * Send a notification (no response expected)
     */
    private synchronized void sendNotification(McpRequest notification) throws IOException {
        String jsonNotification = objectMapper.writeValueAsString(notification);
        log.debug("Sending notification: {}", jsonNotification);

        processInput.write(jsonNotification);
        processInput.newLine();
        processInput.flush();
    }

    /**
     * Shutdown the MCP connection
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down Perplexity MCP client");

        try {
            if (processInput != null) {
                processInput.close();
            }
            if (processOutput != null) {
                processOutput.close();
            }
            if (processError != null) {
                processError.close();
            }
            if (mcpServerProcess != null) {
                mcpServerProcess.destroy();
                mcpServerProcess.waitFor();
            }
        } catch (Exception e) {
            log.error("Error during shutdown", e);
        }

        initialized = false;
    }

    public boolean isInitialized() {
        return initialized;
    }
}

