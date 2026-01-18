package de.jivz.agentservice.cli.executor;

import de.jivz.agentservice.cli.domain.Command;
import de.jivz.agentservice.cli.domain.CommandResult;
import de.jivz.agentservice.cli.domain.ContainerStatus;
import de.jivz.agentservice.client.DockerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Executor for status monitoring commands.
 * Single Responsibility: Container status monitoring.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StatusExecutor implements CommandExecutor {

    private final DockerClient dockerClient;

    @Override
    public boolean canExecute(Command command) {
        return command.getType() == Command.CommandType.STATUS;
    }

    @Override
    public Mono<CommandResult> execute(Command command) {
        log.info("Fetching container statuses");

        return dockerClient.listContainers()
            .map(this::formatContainerStatuses)
            .map(details -> CommandResult.success("Container Status", details))
            .onErrorResume(error -> {
                log.error("Failed to fetch container status: {}", error.getMessage());
                return Mono.just(CommandResult.failure(
                    "Failed to fetch container status: " + error.getMessage(),
                    (Exception) error));
            });
    }

    private String formatContainerStatuses(List<ContainerStatus> containers) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n📊 Services Status:\n");
        sb.append("─".repeat(70)).append("\n");

        for (ContainerStatus container : containers) {
            sb.append(String.format("%s %-25s │ %-12s │ uptime: %s\n",
                container.getStatusEmoji(),
                container.getName(),
                container.getStatus(),
                container.getUptime() != null ? container.getUptime() : "N/A"));

            if (container.getMemoryUsage() != null) {
                sb.append(String.format("   └─ Memory: %s\n", container.getMemoryUsage()));
            }
        }

        sb.append("─".repeat(70));

        return sb.toString();
    }
}

