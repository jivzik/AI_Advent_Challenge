package de.jivz.mcp.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.mcp.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MCP Client Service that communicates with MCP Server via STDIO
 */
@Service
@Slf4j
public class McpClientService {

    private final ObjectMapper objectMapper;
    private final AtomicInteger requestIdCounter = new AtomicInteger(1);

    private Process mcpServerProcess;
    private BufferedWriter processInput;
    private BufferedReader processOutput;
    private boolean initialized = false;

    public McpClientService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Initialize connection to MCP server
     */
    public void initialize(String pythonExecutable, String serverScriptPath) throws IOException {
        log.info("Starting MCP server process: {} {}", pythonExecutable, serverScriptPath);

        ProcessBuilder processBuilder = new ProcessBuilder(pythonExecutable, serverScriptPath);
        processBuilder.redirectErrorStream(false);

        mcpServerProcess = processBuilder.start();
        processInput = new BufferedWriter(new OutputStreamWriter(mcpServerProcess.getOutputStream()));
        processOutput = new BufferedReader(new InputStreamReader(mcpServerProcess.getInputStream()));

        // Send initialize request
        Map<String, Object> params = new HashMap<>();
        params.put("protocolVersion", "2024-11-05");
        params.put("capabilities", Map.of());
        params.put("clientInfo", Map.of(
            "name", "Java MCP Client",
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
    }

    /**
     * Get list of available tools from MCP server
     */
    public List<McpTool> listTools() throws IOException {
        if (!initialized) {
            throw new IllegalStateException("MCP client not initialized. Call initialize() first.");
        }

        log.info("Requesting tools list from MCP server");

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
        ToolsListResult result = objectMapper.convertValue(response.getResult(), ToolsListResult.class);

        log.info("Received {} tools from MCP server", result.getTools().size());
        return result.getTools();
    }

    /**
     * Send a request and wait for response
     */
    private McpResponse<?> sendRequest(McpRequest request) throws IOException {
        String jsonRequest = objectMapper.writeValueAsString(request);
        log.debug("Sending request: {}", jsonRequest);

        processInput.write(jsonRequest);
        processInput.newLine();
        processInput.flush();

        String jsonResponse = processOutput.readLine();
        log.debug("Received response: {}", jsonResponse);

        if (jsonResponse == null) {
            throw new IOException("No response from MCP server");
        }

        return objectMapper.readValue(jsonResponse, new TypeReference<McpResponse<Map<String, Object>>>() {});
    }

    /**
     * Send a notification (no response expected)
     */
    private void sendNotification(McpRequest notification) throws IOException {
        String jsonNotification = objectMapper.writeValueAsString(notification);
        log.debug("Sending notification: {}", jsonNotification);

        processInput.write(jsonNotification);
        processInput.newLine();
        processInput.flush();
    }

    /**
     * Shutdown the MCP connection
     */
    public void shutdown() {
        log.info("Shutting down MCP client");

        try {
            if (processInput != null) {
                processInput.close();
            }
            if (processOutput != null) {
                processOutput.close();
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

