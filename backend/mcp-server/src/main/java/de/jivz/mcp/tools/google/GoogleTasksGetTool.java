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
public class GoogleTasksGetTool extends AbstractGoogleTool {

    private static final String NAME = "google_tasks_get";

    public GoogleTasksGetTool(@Qualifier("gsWebClient") WebClient gsWebClient) {
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

        return ToolDefinition.builder()
                .name(NAME)
                .description("Alle Tasks aus einer spezifischen Google Tasks Liste abrufen")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .required(List.of())
                        .build())
                .build();
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        ToolArguments args = ToolArguments.of(arguments);
        String taskListId = args.getString("taskListId").orElse(null);

        String url = (taskListId != null && !taskListId.isBlank())
                ? "/api/tasks/lists/" + taskListId
                : "/api/tasks";

        if (taskListId != null && !taskListId.isBlank()) {
            contextTaskListId.set(taskListId);
            log.debug("TaskListId im Kontext gespeichert: {}", taskListId);
        }

        log.info("Rufe Tasks ab von Liste: {}", taskListId != null ? taskListId : "default");

        Object result = callGoogleService(url);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("result", result);
        response.put("taskListId", taskListId != null ? taskListId : "default");

        return response;
    }
}
