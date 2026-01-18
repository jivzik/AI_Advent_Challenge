package de.jivz.agentservice.cli.executor;

import de.jivz.agentservice.cli.domain.Command;
import de.jivz.agentservice.cli.domain.CommandResult;
import de.jivz.agentservice.client.DockerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Executor for health check commands.
 * Single Responsibility: Check service health status.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class HealthExecutor implements CommandExecutor {

    private final DockerClient dockerClient;

    @Override
    public boolean canExecute(Command command) {
        return command.getType() == Command.CommandType.HEALTH;
    }

    @Override
    public Mono<CommandResult> execute(Command command) {
        String serviceName = command.getServiceName();

        if (serviceName == null) {
            return Mono.just(CommandResult.failure("Service name is required for health check"));
        }

        log.info("Checking health for service: {}", serviceName);

        return dockerClient.checkHealth(serviceName)
            .map(healthy -> {
                if (healthy) {
                    return CommandResult.success(
                        String.format("✅ %s is healthy", serviceName),
                        "All systems operational");
                } else {
                    return CommandResult.failure(
                        String.format("❌ %s is not healthy", serviceName));
                }
            })
            .onErrorResume(error -> {
                log.error("Health check error: {}", error.getMessage());
                return Mono.just(CommandResult.failure(
                    String.format("Health check failed for %s: %s", serviceName, error.getMessage()),
                    (Exception) error));
            });
    }
}

