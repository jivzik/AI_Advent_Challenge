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
public class GoogleTasksUpdateTool extends AbstractGoogleTool {

    private static final String NAME = "google_tasks_update";

    public GoogleTasksUpdateTool(@Qualifier("gsWebClient") WebClient gsWebClient) {
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
                .description("Die ID der Task-Liste")
                .build());

        properties.put("taskId", PropertyDefinition.builder()
                .type("string")
                .description("Die ID des zu aktualisierenden Tasks")
                .build());

        properties.put("title", PropertyDefinition.builder()
                .type("string")
                .description("Der neue Titel des Tasks")
                .build());

        properties.put("notes", PropertyDefinition.builder()
                .type("string")
                .description("Neue Notizen oder Beschreibung")
                .build());

        properties.put("status", PropertyDefinition.builder()
                .type("string")
                .description("Task-Status (needsAction oder completed)")
                .build());

        return ToolDefinition.builder()
                .name(NAME)
                .description("Einen bestehenden Task in Google Tasks aktualisieren")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .required(List.of("taskId"))
                        .build())
                .build();
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        ToolArguments args = ToolArguments.of(arguments);

        String taskListId = args.getString("taskListId").orElse(null);
        String taskId = args.getRequiredString("taskId");

        String url = (taskListId != null && !taskListId.isBlank())
                ? "/api/tasks/lists/" + taskListId + "/" + taskId
                : "/api/tasks/" + taskId;

        Map<String, Object> updateRequest = new HashMap<>();

        args.getString("title").ifPresent(title -> updateRequest.put("title", title));
        args.getString("notes").ifPresent(notes -> updateRequest.put("notes", notes));
        args.getString("status").ifPresent(status -> updateRequest.put("status", status));

        log.info("Aktualisiere Task: {} in Liste: {}", taskId, taskListId != null ? taskListId : "default");

        Object result = callGoogleServicePatch(url, updateRequest);

        return buildSuccessResponse(result, taskListId);
    }
}

