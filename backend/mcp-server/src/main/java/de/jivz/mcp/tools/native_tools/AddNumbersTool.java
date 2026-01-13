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
public class AddNumbersTool implements Tool {

    private static final String NAME = "add_numbers";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ToolDefinition getDefinition() {
        Map<String, PropertyDefinition> properties = new LinkedHashMap<>();

        properties.put("a", PropertyDefinition.builder()
                .type("integer")
                .description("Erste Zahl")
                .build());

        properties.put("b", PropertyDefinition.builder()
                .type("integer")
                .description("Zweite Zahl")
                .build());

        return ToolDefinition.builder()
                .name(NAME)
                .description("Addiert zwei Zahlen und gibt das Ergebnis zurück")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .required(List.of("a", "b"))
                        .build())
                .build();
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        ToolArguments args = ToolArguments.of(arguments);

        int a = args.getInt("a", 0);
        int b = args.getInt("b", 0);

        if (!args.has("a") || !args.has("b")) {
            throw new IllegalArgumentException("Parameter 'a' und 'b' sind erforderlich");
        }

        int result = a + b;
        log.debug("Addiere {} + {} = {}", a, b, result);

        return result;
    }
}

