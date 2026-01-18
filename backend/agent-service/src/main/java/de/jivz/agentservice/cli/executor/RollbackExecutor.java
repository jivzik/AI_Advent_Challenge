package de.jivz.agentservice.cli.executor;

import de.jivz.agentservice.cli.domain.Command;
import de.jivz.agentservice.cli.domain.CommandResult;
import de.jivz.agentservice.client.DockerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Executor for rollback commands.
 * Single Responsibility: Rollback deployments by restarting containers.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RollbackExecutor implements CommandExecutor {

    private final DockerClient dockerClient;

    @Override
    public boolean canExecute(Command command) {
        return command.getType() == Command.CommandType.ROLLBACK;
    }

    @Override
    public Mono<CommandResult> execute(Command command) {
        String serviceName = command.getServiceName();

        if (serviceName == null) {
            return Mono.just(CommandResult.failure("Service name is required for rollback"));
        }

        log.info("Rolling back service: {}", serviceName);

        return dockerClient.restartContainer(serviceName)
            .delayElement(Duration.ofSeconds(5))
            .flatMap(success -> {
                if (success) {
                    return dockerClient.checkHealth(serviceName)
                        .map(healthy -> {
                            if (healthy) {
                                return CommandResult.success(
                                    String.format("Successfully rolled back %s", serviceName),
                                    "✅ Container restarted\n✅ Health check: OK");
                            } else {
                                return CommandResult.failure(
                                    String.format("Rollback completed but health check failed for %s", serviceName));
                            }
                        });
                } else {
                    return Mono.just(CommandResult.failure(
                        String.format("Failed to rollback %s", serviceName)));
                }
            })
            .onErrorResume(error -> {
                log.error("Rollback error: {}", error.getMessage());
                return Mono.just(CommandResult.failure(
                    "Rollback failed: " + error.getMessage(),
                    (Exception) error));
            });
    }
}

