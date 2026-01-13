package de.jivz.mcp.tools.google;

import de.jivz.mcp.model.InputSchema;
import de.jivz.mcp.model.PropertyDefinition;
import de.jivz.mcp.model.ToolDefinition;
import de.jivz.mcp.tools.ToolArguments;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GoogleTasksCreateTool extends AbstractGoogleTool {

    private static final String NAME = "google_tasks_create";

    public GoogleTasksCreateTool(@Qualifier("gsWebClient") WebClient gsWebClient) {
        super(gsWebClient);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ToolDefinition getDefinition() {
        Map<String, PropertyDefinition> properties = new LinkedHashMap<>();

        properties.put("taskListId", PropertyDefinition.builder()
                .type("string")
                .description("Die ID der Task-Liste (optional, verwendet Standard wenn nicht angegeben)")
                .build());

        properties.put("title", PropertyDefinition.builder()
                .type("string")
                .description("Der Titel des Tasks")
                .build());

        properties.put("notes", PropertyDefinition.builder()
                .type("string")
                .description("Notizen oder Beschreibung für den Task")
                .build());

        properties.put("due", PropertyDefinition.builder()
                .type("string")
                .description("Fälligkeitsdatum im ISO 8601 Format (z.B. 2024-12-31T23:59:59Z)")
                .build());

        return ToolDefinition.builder()
                .name(NAME)
                .description("Einen neuen Task in einer Google Tasks Liste erstellen")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .required(List.of("title"))
                        .build())
                .build();
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        ToolArguments args = ToolArguments.of(arguments);

        String taskListId = args.getString("taskListId").orElse(null);
        String title = args.getRequiredString("title");

        String url = (taskListId != null && !taskListId.isBlank())
                ? "/api/tasks/lists/" + taskListId
                : "/api/tasks";

        if (taskListId != null && !taskListId.isBlank()) {
            contextTaskListId.set(taskListId);
            log.debug("TaskListId im Kontext gespeichert: {}", taskListId);
        }

        Map<String, Object> taskRequest = new HashMap<>();
        taskRequest.put("title", title);

        args.getString("notes").ifPresent(notes -> taskRequest.put("notes", notes));
        args.getString("due").ifPresent(due -> taskRequest.put("due", due));

        log.info("Erstelle Task: {} in Liste: {}", title, taskListId != null ? taskListId : "default");

        Object result = callGoogleServicePost(url, taskRequest);

        return buildSuccessResponse(result, taskListId != null ? taskListId : "default");
    }
}

