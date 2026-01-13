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
public class CalculateFibonacciTool implements Tool {
    private static final String NAME = "calculate_fibonacci";
    @Override
    public String getName() {
        return NAME;
    }
    @Override
    public ToolDefinition getDefinition() {
        Map<String, PropertyDefinition> properties = new LinkedHashMap<>();
        properties.put("n", PropertyDefinition.builder()
                .type("integer")
                .description("Position in der Fibonacci-Folge (muss positiv sein)")
                .build());
        return ToolDefinition.builder()
                .name(NAME)
                .description("Berechnet die n-te Fibonacci-Zahl")
                .inputSchema(InputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .required(List.of("n"))
                        .build())
                .build();
    }
    @Override
    public Object execute(Map<String, Object> arguments) {
        ToolArguments args = ToolArguments.of(arguments);
        int n = args.getInt("n", -1);
        if (n < 0) {
            throw new IllegalArgumentException("Parameter 'n' muss nicht-negativ sein");
        }
        int result = fibonacci(n);
        log.debug("Fibonacci({}) = {}", n, result);
        return result;
    }
    private int fibonacci(int n) {
        if (n <= 1) {
            return n;
        }
        int a = 0, b = 1;
        for (int i = 2; i <= n; i++) {
            int temp = a + b;
            a = b;
            b = temp;
        }
        return b;
    }
}
