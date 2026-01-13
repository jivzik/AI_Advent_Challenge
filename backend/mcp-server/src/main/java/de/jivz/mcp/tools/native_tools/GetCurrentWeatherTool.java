package de.jivz.mcp.tools.native_tools;

import de.jivz.mcp.model.InputSchema;
import de.jivz.mcp.model.PropertyDefinition;
import de.jivz.mcp.model.ToolDefinition;
import de.jivz.mcp.tools.Tool;
import de.jivz.mcp.tools.ToolArguments;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GetCurrentWeatherTool implements Tool {

    private static final String NAME = "get_current_weather";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ToolDefinition getDefinition() {
        Map<String, PropertyDefinition> properties = new LinkedHashMap<>();

        properties.put("location", PropertyDefinition.builder()
                .type("string")
                .description("Stadt- oder Ortsname")
                .build());

        properties.put("unit", PropertyDefinition.builder()
                .type("string")
                .description("Temperatureinheit (celsius oder fahrenheit)")
                .defaultValue("celsius")
                .build());

        return ToolDefinition.builder()
                .name(NAME)
                .description("Gibt das aktuelle Wetter für einen Ort zurück")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .required(List.of("location"))
                        .build())
                .build();
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        ToolArguments args = ToolArguments.of(arguments);

        String location = args.getRequiredString("location");
        String unit = args.getString("unit", "celsius");

        Map<String, Object> weather = new HashMap<>();
        weather.put("location", location);
        weather.put("temperature", 22);
        weather.put("unit", unit);
        weather.put("condition", "sunny");
        weather.put("humidity", 65);
        weather.put("windSpeed", 12);

        log.debug("Wetter für {} abgerufen: {}", location, weather);
        return weather;
    }
}

