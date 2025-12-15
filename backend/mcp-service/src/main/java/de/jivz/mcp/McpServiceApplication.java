package de.jivz.mcp;

import de.jivz.mcp.model.McpTool;
import de.jivz.mcp.service.McpServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
@Slf4j
@RequiredArgsConstructor
public class McpServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServiceApplication.class, args);
    }

    /**
     * Display available MCP tools on startup
     */
    @Bean
    public CommandLineRunner displayMcpTools(McpServerService mcpServerService) {
        return args -> {
            log.info("\n");
            log.info("=".repeat(80));
            log.info("Java Native MCP Server Started Successfully");
            log.info("=".repeat(80));

            List<McpTool> tools = mcpServerService.listTools();

            log.info("\n");
            log.info("╔" + "═".repeat(78) + "╗");
            log.info("║" + centerText("AVAILABLE MCP TOOLS", 78) + "║");
            log.info("╠" + "═".repeat(78) + "╣");

            for (int i = 0; i < tools.size(); i++) {
                McpTool tool = tools.get(i);
                log.info("║ Tool #" + String.format("%-2d", i + 1) + " ".repeat(70) + "║");
                log.info("║   Name:        " + padRight(tool.getName(), 60) + "║");
                log.info("║   Description: " + padRight(truncate(tool.getDescription(), 60), 60) + "║");

                if (i < tools.size() - 1) {
                    log.info("╠" + "─".repeat(78) + "╣");
                }
            }

            log.info("╚" + "═".repeat(78) + "╝");
            log.info("\n");
            log.info("📊 Total tools available: {}", tools.size());
            log.info("\n");
            log.info("🌐 REST API Endpoints:");
            log.info("   • List all tools:    GET    http://localhost:8080/mcp/tools");
            log.info("   • Get specific tool: GET    http://localhost:8080/mcp/tools/{toolName}");
            log.info("   • Execute tool:      POST   http://localhost:8080/mcp/execute");
            log.info("   • Server status:     GET    http://localhost:8080/mcp/status");
            log.info("\n");
            log.info("📝 Example tool execution:");
            log.info("   curl -X POST http://localhost:8080/mcp/execute \\");
            log.info("        -H 'Content-Type: application/json' \\");
            log.info("        -d '{\"toolName\": \"add_numbers\", \"arguments\": {\"a\": 5, \"b\": 3}}'");
            log.info("\n");
            log.info("=".repeat(80));
            log.info("✓ Java MCP Server is ready to accept requests");
            log.info("=".repeat(80));
            log.info("\n");
        };
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    private String padRight(String text, int length) {
        if (text == null) text = "";
        if (text.length() >= length) return text.substring(0, length);
        return text + " ".repeat(length - text.length());
    }

    private String centerText(String text, int width) {
        if (text.length() >= width) return text.substring(0, width);
        int padding = (width - text.length()) / 2;
        int rightPadding = width - text.length() - padding;
        return " ".repeat(padding) + text + " ".repeat(rightPadding);
    }
}

