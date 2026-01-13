package de.jivz.mcp.tools.native_tools;

import de.jivz.mcp.model.InputSchema;
import de.jivz.mcp.model.PropertyDefinition;
import de.jivz.mcp.model.ToolDefinition;
import de.jivz.mcp.tools.Tool;
import de.jivz.mcp.tools.ToolArguments;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ReverseStringTool implements Tool {

    private static final String NAME = "reverse_string";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ToolDefinition getDefinition() {
        Map<String, PropertyDefinition> properties = new LinkedHashMap<>();

        properties.put("text", PropertyDefinition.builder()
                .type("string")
                .description("Der umzukehrende String")
                .build());

        return ToolDefinition.builder()
                .name(NAME)
                .description("Kehrt einen String um")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .required(List.of("text"))
                        .build())
                .build();
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        ToolArguments args = ToolArguments.of(arguments);

        String text = args.getRequiredString("text");
        String reversed = new StringBuilder(text).reverse().toString();

        log.debug("String umgekehrt: '{}' -> '{}'", text, reversed);

        return reversed;
    }
}

