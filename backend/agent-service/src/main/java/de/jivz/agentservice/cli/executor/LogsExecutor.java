package de.jivz.agentservice.cli.executor;

import de.jivz.agentservice.cli.domain.Command;
import de.jivz.agentservice.cli.domain.CommandResult;
import de.jivz.agentservice.client.DockerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Executor for log viewing commands.
 * Single Responsibility: Retrieve and format container logs.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LogsExecutor implements CommandExecutor {

    private final DockerClient dockerClient;

    private static final int DEFAULT_LOG_LINES = 20;

    @Override
    public boolean canExecute(Command command) {
        return command.getType() == Command.CommandType.LOGS;
    }

    @Override
    public Mono<CommandResult> execute(Command command) {
        String serviceName = command.getServiceName();

        if (serviceName == null) {
            return Mono.just(CommandResult.failure("Service name is required for logs"));
        }

        log.info("Fetching logs for service: {}", serviceName);

        return dockerClient.getContainerLogs(serviceName, DEFAULT_LOG_LINES)
            .map(logs -> {
                String formattedLogs = formatLogs(serviceName, logs);
                return CommandResult.success(
                    String.format("Logs for %s (last %d lines)", serviceName, DEFAULT_LOG_LINES),
                    formattedLogs);
            })
            .onErrorResume(error -> {
                log.error("Failed to fetch logs: {}", error.getMessage());
                return Mono.just(CommandResult.failure(
                    "Failed to fetch logs: " + error.getMessage(),
                    (Exception) error));
            });
    }

    private String formatLogs(String serviceName, String logs) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("\n📜 Logs for %s:\n", serviceName));
        sb.append("─".repeat(70)).append("\n");
        sb.append(logs);
        if (!logs.endsWith("\n")) {
            sb.append("\n");
        }
        sb.append("─".repeat(70));

        return sb.toString();
    }
}

