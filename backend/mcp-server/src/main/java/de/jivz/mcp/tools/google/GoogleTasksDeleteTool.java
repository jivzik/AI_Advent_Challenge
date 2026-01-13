package de.jivz.mcp.tools.google;

import de.jivz.mcp.model.InputSchema;
import de.jivz.mcp.model.PropertyDefinition;
import de.jivz.mcp.model.ToolDefinition;
import de.jivz.mcp.tools.ToolArguments;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GoogleTasksDeleteTool extends AbstractGoogleTool {

    private static final String NAME = "google_tasks_delete";

    public GoogleTasksDeleteTool(@Qualifier("gsWebClient") WebClient gsWebClient) {
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
                .description("Die ID des zu löschenden Tasks")
                .build());

        return ToolDefinition.builder()
                .name(NAME)
                .description("Einen Task aus Google Tasks löschen")
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

        log.info("Lösche Task: {} aus Liste: {}", taskId, taskListId != null ? taskListId : "default");

        return callGoogleServiceDelete(url);
    }
}

