package de.jivz.agentservice.cli;

import de.jivz.agentservice.cli.domain.Command;
import de.jivz.agentservice.cli.domain.CommandResult;
import de.jivz.agentservice.cli.formatter.CLIOutputFormatter;
import de.jivz.agentservice.cli.parser.CommandParser;
import de.jivz.agentservice.cli.service.CommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Main CLI Application.
 * Runs when spring.profiles.active=cli
 *
 * Single Responsibility: Handle CLI input/output loop.
 * Dependency Injection: All dependencies injected via constructor.
 */
@Component
@ConditionalOnProperty(name = "cli.enabled", havingValue = "true")
@Slf4j
@RequiredArgsConstructor
public class CLIApplication implements CommandLineRunner {

    private final CommandParser commandParser;
    private final CommandService commandService;
    private final CLIOutputFormatter formatter;

    @Override
    public void run(String... args) {
        log.info("Starting AI DevOps Agent CLI");

        try (Terminal terminal = TerminalBuilder.builder()
                .system(true)
                .build()) {

            LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(new DefaultParser())
                .build();

            runCLI(reader);

        } catch (IOException e) {
            log.error("Failed to initialize terminal: {}", e.getMessage());
            System.err.println("Error: Unable to start CLI - " + e.getMessage());
        } finally {
            formatter.cleanup();
        }
    }

    private void runCLI(LineReader reader) {
        formatter.printWelcome();

        boolean running = true;

        while (running) {
            try {
                formatter.printPrompt();

                String input = reader.readLine();

                if (input == null || input.isBlank()) {
                    continue;
                }

                // Parse command
                Command command = commandParser.parse(input);

                // Check for exit
                if (command.isExit()) {
                    formatter.printGoodbye();
                    running = false;
                    continue;
                }

                // Handle help
                if (command.getType() == Command.CommandType.HELP) {
                    formatter.printHelp();
                    continue;
                }

                // Validate command
                if (!commandService.validate(command)) {
                    formatter.printError("Invalid command. Missing required parameters.");
                    continue;
                }

                // Execute command
                executeCommand(command);

            } catch (Exception e) {
                log.error("Error in CLI loop: {}", e.getMessage(), e);
                formatter.printError("An error occurred: " + e.getMessage());
            }
        }
    }

    private void executeCommand(Command command) {
        try {
            formatter.printProgress("Processing command...");

            CommandResult result = commandService.execute(command)
                .block(); // Block for synchronous CLI experience

            if (result != null) {
                formatter.printResult(result);
            } else {
                formatter.printError("Command execution returned no result");
            }

        } catch (Exception e) {
            log.error("Command execution failed: {}", e.getMessage(), e);
            formatter.printError("Command execution failed: " + e.getMessage());
        }
    }
}

