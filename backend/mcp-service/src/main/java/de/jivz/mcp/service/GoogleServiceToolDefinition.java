package de.jivz.mcp.service;

import de.jivz.mcp.model.McpTool;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service for defining Google Service Tools
 * Provides unified tool definitions for Google Tasks API
 */
@Service
public class GoogleServiceToolDefinition {

    /**
     * Returns all available tool definitions for Google Service
     */
    public List<McpTool> getToolDefinitions() {
        List<McpTool> tools = new ArrayList<>();

        // Tool 1: Get Task Lists - NO parameters
        tools.add(createTool(
            "google_tasks_list",
            "Get all Google Tasks lists for the authenticated user",
            Map.of(
                "properties", Map.of(),
                "required", List.of()
            )
        ));

        // Tool 2: Get Tasks from a list
        tools.add(createTool(
            "google_tasks_get",
            "Get all tasks from a specific Google Tasks list",
            Map.of(
                "properties", Map.of(
                    "taskListId", Map.of(
                        "type", "string",
                        "description", "The ID of the task list (optional, uses default if not provided)"
                    )
                ),
                "required", List.of()
            )
        ));

        // Tool 3: Create Task
        tools.add(createTool(
            "google_tasks_create",
            "Create a new task in a Google Tasks list",
            Map.of(
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
            )
        ));

        // Tool 4: Update Task
        tools.add(createTool(
            "google_tasks_update",
            "Update an existing task in Google Tasks",
            Map.of(
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
            )
        ));

        // Tool 5: Complete Task (convenience method)
        tools.add(createTool(
            "google_tasks_complete",
            "Mark a task as completed in Google Tasks",
            Map.of(
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
            )
        ));

        // Tool 6: Delete Task
        tools.add(createTool(
            "google_tasks_delete",
            "Delete a task from Google Tasks",
            Map.of(
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
            )
        ));

        return tools;
    }

    /**
     * Helper method to create an McpTool
     */
    private McpTool createTool(String name, String description, Map<String, Object> inputSchema) {
        return McpTool.builder()
                .name(name)
                .description(description)
                .inputSchema(inputSchema)
                .build();
    }
}

