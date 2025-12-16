package de.jivz.mcp.service;

import de.jivz.mcp.model.McpTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Google Service Tool Provider
 * Stellt Google Tasks Service als MCP-Tool bereit
 *
 * Dieser Provider ruft den google-service Microservice auf
 * und macht dessen Funktionalität über MCP verfügbar
 */
@Component
@Slf4j
public class GoogleServiceToolProvider implements ToolProvider {

    private final List<McpTool> availableTools;
    private final RestTemplate restTemplate;
    private final String googleServiceUrl;

    public GoogleServiceToolProvider(
            @Value("${google.service.url:http://localhost:8082}") String googleServiceUrl) {
        this.googleServiceUrl = googleServiceUrl;
        this.restTemplate = new RestTemplate();
        this.availableTools = initializeTools();
        log.info("Google Service Tool Provider initialized with {} tools (Google Service URL: {})",
                availableTools.size(), googleServiceUrl);
    }

    @Override
    public String getProviderName() {
        return "google-service";
    }

    @Override
    public List<McpTool> getTools() {
        return new ArrayList<>(availableTools);
    }

    @Override
    public Object executeTool(String toolName, Map<String, Object> arguments) {
        log.info("Executing Google Service tool: {} with arguments: {}", toolName, arguments);

        return switch (toolName) {
            case "google_tasks_list" -> getTaskLists();
            case "google_tasks_get" -> getTasks(arguments);
            case "google_tasks_create" -> createTask(arguments);
            case "google_tasks_update" -> updateTask(arguments);
            case "google_tasks_delete" -> deleteTask(arguments);
            default -> throw new IllegalArgumentException("Unknown Google Service tool: " + toolName);
        };
    }

    @Override
    public boolean supportsTool(String toolName) {
        return availableTools.stream()
                .anyMatch(tool -> tool.getName().equals(toolName));
    }

    /**
     * Initialize Google Service Tools
     */
    private List<McpTool> initializeTools() {
        List<McpTool> tools = new ArrayList<>();

        // Tool 1: Get Task Lists
        tools.add(McpTool.builder()
                .name("google_tasks_list")
                .description("Get all Google Tasks lists for the authenticated user")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(),
                        "required", List.of()
                ))
                .build());

        // Tool 2: Get Tasks from a list
        tools.add(McpTool.builder()
                .name("google_tasks_get")
                .description("Get all tasks from a specific Google Tasks list")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "taskListId", Map.of(
                                        "type", "string",
                                        "description", "The ID of the task list (optional, uses default if not provided)"
                                )
                        ),
                        "required", List.of()
                ))
                .build());

        // Tool 3: Create Task
        tools.add(McpTool.builder()
                .name("google_tasks_create")
                .description("Create a new task in a Google Tasks list")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "taskListId", Map.of(
                                        "type", "string",
                                        "description", "The ID of the task list (optional, uses default if not provided)"
                                ),
                                "title", Map.of(
                                        "type", "string",
                                        "description", "The title of the task"
                                ),
                                "notes", Map.of(
                                        "type", "string",
                                        "description", "Notes or description for the task"
                                ),
                                "due", Map.of(
                                        "type", "string",
                                        "description", "Due date in ISO 8601 format (e.g., 2024-12-31T23:59:59Z)"
                                )
                        ),
                        "required", List.of("title")
                ))
                .build());

        // Tool 4: Update Task
        tools.add(McpTool.builder()
                .name("google_tasks_update")
                .description("Update an existing task in Google Tasks")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "taskListId", Map.of(
                                        "type", "string",
                                        "description", "The ID of the task list"
                                ),
                                "taskId", Map.of(
                                        "type", "string",
                                        "description", "The ID of the task to update"
                                ),
                                "title", Map.of(
                                        "type", "string",
                                        "description", "The new title of the task"
                                ),
                                "notes", Map.of(
                                        "type", "string",
                                        "description", "New notes or description"
                                ),
                                "status", Map.of(
                                        "type", "string",
                                        "description", "Task status (needsAction or completed)",
                                        "enum", List.of("needsAction", "completed")
                                )
                        ),
                        "required", List.of("taskId")
                ))
                .build());

        // Tool 5: Delete Task
        tools.add(McpTool.builder()
                .name("google_tasks_delete")
                .description("Delete a task from Google Tasks")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "taskListId", Map.of(
                                        "type", "string",
                                        "description", "The ID of the task list"
                                ),
                                "taskId", Map.of(
                                        "type", "string",
                                        "description", "The ID of the task to delete"
                                )
                        ),
                        "required", List.of("taskId")
                ))
                .build());

        return tools;
    }

    /**
     * Get all task lists
     */
    private Object getTaskLists() {
        try {
            String url = googleServiceUrl + "/api/tasks/lists";
            log.debug("Calling Google Service: GET {}", url);

            ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);

            return Map.of(
                    "success", true,
                    "data", response.getBody()
            );
        } catch (Exception e) {
            log.error("Error calling Google Service: {}", e.getMessage());
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }

    /**
     * Get tasks from a specific list
     */
    private Object getTasks(Map<String, Object> arguments) {
        try {
            String taskListId = (String) arguments.get("taskListId");
            // Handle null or empty taskListId
            String url = (taskListId != null && !taskListId.isBlank())
                    ? googleServiceUrl + "/api/tasks/lists/" + taskListId
                    : googleServiceUrl + "/api/tasks";

            log.debug("Calling Google Service: GET {}", url);

            ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);

            return Map.of(
                    "success", true,
                    "data", response.getBody()
            );
        } catch (Exception e) {
            log.error("Error calling Google Service: {}", e.getMessage());
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }

    /**
     * Create a new task
     */
    private Object createTask(Map<String, Object> arguments) {
        try {
            String taskListId = (String) arguments.get("taskListId");
            // Handle null or empty taskListId
            String url = (taskListId != null && !taskListId.isBlank())
                    ? googleServiceUrl + "/api/tasks/lists/" + taskListId
                    : googleServiceUrl + "/api/tasks";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> taskRequest = new HashMap<>();
            taskRequest.put("title", arguments.get("title"));
            if (arguments.containsKey("notes")) {
                taskRequest.put("notes", arguments.get("notes"));
            }
            if (arguments.containsKey("due")) {
                taskRequest.put("due", arguments.get("due"));
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(taskRequest, headers);

            log.debug("Calling Google Service: POST {} with body: {}", url, taskRequest);

            ResponseEntity<Object> response = restTemplate.postForEntity(url, request, Object.class);

            return Map.of(
                    "success", true,
                    "data", response.getBody()
            );
        } catch (Exception e) {
            log.error("Error calling Google Service: {}", e.getMessage());
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }

    /**
     * Update an existing task
     */
    private Object updateTask(Map<String, Object> arguments) {
        try {
            String taskListId = (String) arguments.get("taskListId");
            String taskId = (String) arguments.get("taskId");

            if (taskId == null || taskId.isBlank()) {
                throw new IllegalArgumentException("taskId is required");
            }

            // Handle null or empty taskListId
            // Correct path: /api/tasks/lists/{taskListId}/tasks/{taskId}
            String url = (taskListId != null && !taskListId.isBlank())
                    ? String.format("%s/api/tasks/lists/%s/tasks/%s", googleServiceUrl, taskListId, taskId)
                    : String.format("%s/api/tasks/%s", googleServiceUrl, taskId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> updateRequest = new HashMap<>();
            if (arguments.containsKey("title")) {
                updateRequest.put("title", arguments.get("title"));
            }
            if (arguments.containsKey("notes")) {
                updateRequest.put("notes", arguments.get("notes"));
            }
            if (arguments.containsKey("status")) {
                updateRequest.put("status", arguments.get("status"));
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(updateRequest, headers);

            log.debug("Calling Google Service: PUT {} with body: {}", url, updateRequest);

            ResponseEntity<Object> response = restTemplate.exchange(
                    url, HttpMethod.PUT, request, Object.class);

            return Map.of(
                    "success", true,
                    "data", response.getBody()
            );
        } catch (Exception e) {
            log.error("Error calling Google Service: {}", e.getMessage());
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }

    /**
     * Delete a task
     */
    private Object deleteTask(Map<String, Object> arguments) {
        try {
            String taskListId = (String) arguments.get("taskListId");
            String taskId = (String) arguments.get("taskId");

            if (taskId == null || taskId.isBlank()) {
                throw new IllegalArgumentException("taskId is required");
            }

            // Handle null or empty taskListId
            // Correct path: /api/tasks/lists/{taskListId}/tasks/{taskId}
            String url = (taskListId != null && !taskListId.isBlank())
                    ? String.format("%s/api/tasks/lists/%s/tasks/%s", googleServiceUrl, taskListId, taskId)
                    : String.format("%s/api/tasks/%s", googleServiceUrl, taskId);

            log.debug("Calling Google Service: DELETE {}", url);

            restTemplate.delete(url);

            return Map.of(
                    "success", true,
                    "message", "Task deleted successfully"
            );
        } catch (Exception e) {
            log.error("Error calling Google Service: {}", e.getMessage());
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }
}

