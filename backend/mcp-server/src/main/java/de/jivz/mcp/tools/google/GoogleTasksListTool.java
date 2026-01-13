package de.jivz.mcp.tools.google;

import de.jivz.mcp.model.InputSchema;
import de.jivz.mcp.model.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GoogleTasksListTool extends AbstractGoogleTool {

    private static final String NAME = "google_tasks_list";

    public GoogleTasksListTool(@Qualifier("gsWebClient") WebClient gsWebClient) {
        super(gsWebClient);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ToolDefinition getDefinition() {
        return ToolDefinition.builder()
                .name(NAME)
                .description("Alle Google Tasks Listen für den authentifizierten Benutzer abrufen")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(new LinkedHashMap<>())
                        .required(List.of())
                        .build())
                .build();
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        log.info("Rufe Google Tasks Listen ab");
        return callGoogleService("/api/tasks/lists");
    }
}

