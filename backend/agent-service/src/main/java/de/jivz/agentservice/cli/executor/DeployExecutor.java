package de.jivz.agentservice.cli.executor;

import de.jivz.agentservice.cli.domain.Command;
import de.jivz.agentservice.cli.domain.CommandResult;
import de.jivz.agentservice.cli.domain.DeploymentInfo;
import de.jivz.agentservice.client.GitHubActionsClient;
import de.jivz.agentservice.dto.github.WorkflowRun;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Executor for deploy commands.
 * Single Responsibility: Handle deployment operations.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DeployExecutor implements CommandExecutor {

    private final GitHubActionsClient githubClient;

    private static final String WORKFLOW_FILE = "deploy.yml";
    private static final String DEFAULT_BRANCH = "main";

    @Override
    public boolean canExecute(Command command) {
        return command.getType() == Command.CommandType.DEPLOY;
    }

    @Override
    public Mono<CommandResult> execute(Command command) {
        String serviceName = command.getServiceName();

        if (serviceName == null) {
            return Mono.just(CommandResult.failure("Service name is required for deployment"));
        }

        log.info("Starting deployment for service: {}", serviceName);

        return deployService(serviceName)
            .map(deployInfo -> {
                if (deployInfo.getStatus() == DeploymentInfo.DeploymentStatus.SUCCESS) {
                    String message = String.format("Successfully deployed %s v%s in %s",
                        serviceName,
                        deployInfo.getVersion(),
                        formatDuration(deployInfo.getStartTime(), deployInfo.getEndTime()));

                    String details = String.format("🔗 Workflow: %s", deployInfo.getWorkflowRunUrl());

                    return CommandResult.success(message, details);
                } else {
                    return CommandResult.failure(
                        String.format("Deployment failed for %s", serviceName));
                }
            })
            .onErrorResume(error -> {
                log.error("Deployment error: {}", error.getMessage());
                return Mono.just(CommandResult.failure(
                    "Deployment failed: " + error.getMessage(),
                    (Exception) error));
            });
    }

    private Mono<DeploymentInfo> deployService(String serviceName) {
        LocalDateTime startTime = LocalDateTime.now();

        // Trigger GitHub Actions workflow
        return githubClient.triggerWorkflow(WORKFLOW_FILE, DEFAULT_BRANCH)
            .then(Mono.delay(Duration.ofSeconds(3))) // Wait for workflow to start
            .flatMap(ignored -> pollWorkflowStatus(serviceName, startTime))
            .timeout(Duration.ofMinutes(10))
            .doOnSuccess(info -> log.info("Deployment completed: {}", info.getStatus()));
    }

    private Mono<DeploymentInfo> pollWorkflowStatus(String serviceName, LocalDateTime startTime) {
        return githubClient.getWorkflowRuns(WORKFLOW_FILE, 1)
            .flatMap(runs -> {
                if (runs.isEmpty()) {
                    return Mono.error(new RuntimeException("No workflow runs found"));
                }

                WorkflowRun latestRun = runs.get(0);

                DeploymentInfo info = DeploymentInfo.builder()
                    .serviceName(serviceName)
                    .startTime(startTime)
                    .workflowRunUrl(latestRun.getHtmlUrl())
                    .build();

                if (latestRun.isCompleted()) {
                    info.setEndTime(LocalDateTime.now());

                    if (latestRun.isSuccess()) {
                        info.setStatus(DeploymentInfo.DeploymentStatus.SUCCESS);
                        info.setVersion("latest");
                    } else {
                        info.setStatus(DeploymentInfo.DeploymentStatus.FAILED);
                    }

                    return Mono.just(info);
                } else {
                    // Still running, poll again
                    info.setStatus(DeploymentInfo.DeploymentStatus.DEPLOYING);

                    return Mono.delay(Duration.ofSeconds(5))
                        .flatMap(ignored -> pollWorkflowStatus(serviceName, startTime));
                }
            });
    }

    private String formatDuration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return "N/A";
        }

        Duration duration = Duration.between(start, end);
        long minutes = duration.toMinutes();
        long seconds = duration.minusMinutes(minutes).getSeconds();

        return String.format("%dm %ds", minutes, seconds);
    }
}

