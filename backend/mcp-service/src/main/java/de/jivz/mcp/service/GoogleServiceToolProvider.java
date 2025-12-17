package de.jivz.mcp.service;

import de.jivz.mcp.model.McpTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

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
    private final WebClient gsWebClient;
    private final ThreadLocal<String> currentTaskListId = new ThreadLocal<>();

    public GoogleServiceToolProvider(WebClient gsWebClient) {
        this.gsWebClient = gsWebClient;
        this.availableTools = initializeTools();
        log.info("Google Service Tool Provider initialized with {} tools", availableTools.size());
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
            case "google_tasks_complete" -> completeTask(arguments);
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

        // Tool 4b: Complete Task (convenience method)
        tools.add(McpTool.builder()
                .name("google_tasks_complete")
                .description("Mark a task as completed in Google Tasks")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "taskListId", Map.of(
                                        "type", "string",
                                        "description", "The ID of the task list"
                                ),
                                "taskId", Map.of(
                                        "type", "string",
                                        "description", "The ID of the task to mark as completed"
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
            String url = "/api/tasks/lists";
            log.debug("Calling Google Service: GET {}", url);

            Object response = gsWebClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            return Map.of(
                    "success", true,
                    "data", response
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
                    ? "/api/tasks/lists/" + taskListId
                    : "/api/tasks";

            // Store taskListId in context for later use
            if (taskListId != null && !taskListId.isBlank()) {
                currentTaskListId.set(taskListId);
                log.debug("Stored taskListId in context: {}", taskListId);
            }

            log.debug("Calling Google Service: GET {}", url);

            Object response = gsWebClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            // Include taskListId in response so LLM can reference it
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("taskListId", taskListId != null ? taskListId : "default");
            result.put("data", response);

            return result;
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
                    ? "/api/tasks/lists/" + taskListId
                    : "/api/tasks";

            // Store taskListId in context for later use
            if (taskListId != null && !taskListId.isBlank()) {
                currentTaskListId.set(taskListId);
                log.debug("Stored taskListId in context: {}", taskListId);
            }

            Map<String, Object> taskRequest = new HashMap<>();
            taskRequest.put("title", arguments.get("title"));
            if (arguments.containsKey("notes")) {
                taskRequest.put("notes", arguments.get("notes"));
            }
            if (arguments.containsKey("due")) {
                taskRequest.put("due", arguments.get("due"));
            }

            log.debug("Calling Google Service: POST {} with body: {}", url, taskRequest);

            Object response = gsWebClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(taskRequest)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("taskListId", taskListId != null ? taskListId : "default");
            result.put("data", response);

            return result;
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

            // Use stored taskListId from context if current one is empty
            if ((taskListId == null || taskListId.isBlank()) && currentTaskListId.get() != null) {
                taskListId = currentTaskListId.get();
                log.debug("Using taskListId from context: {}", taskListId);
            }

            // Handle null or empty taskListId
            String url = (taskListId != null && !taskListId.isBlank())
                    ? String.format("/api/tasks/lists/%s/tasks/%s", taskListId, taskId)
                    : String.format("/api/tasks/%s", taskId);

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

            log.debug("Calling Google Service: PUT {} with body: {}", url, updateRequest);

            Object response = gsWebClient.put()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updateRequest)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("taskListId", taskListId != null ? taskListId : "default");
            result.put("data", response);

            return result;
        } catch (Exception e) {
            log.error("Error calling Google Service: {}", e.getMessage());
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }

    /**
     * Complete a task (mark as completed)
     */
    private Object completeTask(Map<String, Object> arguments) {
        try {
            String taskListId = (String) arguments.get("taskListId");
            String taskId = (String) arguments.get("taskId");

            if (taskId == null || taskId.isBlank()) {
                throw new IllegalArgumentException("taskId is required");
            }

            // Use stored taskListId from context if current one is empty
            if ((taskListId == null || taskListId.isBlank()) && currentTaskListId.get() != null) {
                taskListId = currentTaskListId.get();
                log.debug("Using taskListId from context: {}", taskListId);
            }

            // Handle null or empty taskListId
            String url = (taskListId != null && !taskListId.isBlank())
                    ? String.format("/api/tasks/lists/%s/tasks/%s/complete", taskListId, taskId)
                    : String.format("/api/tasks/%s/complete", taskId);

            log.debug("Calling Google Service: PATCH {}", url);

            Object response = gsWebClient.patch()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("taskListId", taskListId != null ? taskListId : "default");
            result.put("data", response);

            return result;
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

            // Use stored taskListId from context if current one is empty
            if ((taskListId == null || taskListId.isBlank()) && currentTaskListId.get() != null) {
                taskListId = currentTaskListId.get();
                log.debug("Using taskListId from context: {}", taskListId);
            }

            // Handle null or empty taskListId
            String url = (taskListId != null && !taskListId.isBlank())
                    ? String.format("/api/tasks/lists/%s/tasks/%s", taskListId, taskId)
                    : String.format("/api/tasks/%s", taskId);

            log.debug("Calling Google Service: DELETE {}", url);

            gsWebClient.delete()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("taskListId", taskListId != null ? taskListId : "default");
            result.put("message", "Task deleted successfully");

            return result;
        } catch (Exception e) {
            log.error("Error calling Google Service: {}", e.getMessage());
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }
}

