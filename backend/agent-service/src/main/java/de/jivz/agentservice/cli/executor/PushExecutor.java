package de.jivz.agentservice.cli.executor;

import de.jivz.agentservice.cli.domain.Command;
import de.jivz.agentservice.cli.domain.CommandResult;
import de.jivz.agentservice.mcp.GitHubMCPService;
import de.jivz.agentservice.mcp.model.MCPToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Executor for git push commands.
 * Single Responsibility: Handle git push operations via MCP.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PushExecutor implements CommandExecutor {

    private final GitHubMCPService gitHubMCPService;

    @Override
    public boolean canExecute(Command command) {
        return command.getType() == Command.CommandType.PUSH;
    }

    @Override
    public Mono<CommandResult> execute(Command command) {
        String branch = command.getServiceName(); // можно указать ветку

        log.info("Pushing to remote origin, branch: {}", branch != null ? branch : "current");

        return pushToRemote(branch)
            .map(success -> {
                if (success) {
                    return CommandResult.success(
                        "✅ Successfully pushed to origin",
                        branch != null ? "📤 Branch: " + branch : "📤 Current branch pushed");
                } else {
                    return CommandResult.failure("Failed to push to origin");
                }
            })
            .onErrorResume(error -> {
                log.error("Push error: {}", error.getMessage());
                return Mono.just(CommandResult.failure(
                    "Push failed: " + error.getMessage(),
                    (Exception) error));
            });
    }

    private Mono<Boolean> pushToRemote(String branch) {
        return Mono.fromCallable(() -> {
            MCPToolResult result = gitHubMCPService.execute("git_push", Map.of(
                "remote", "origin",
                "branch", branch != null ? branch : "",
                "force", false
            ));

            if (!result.isSuccess()) {
                log.error("Git push failed: {}", result.getError());
                throw new RuntimeException("Failed to push: " + result.getError());
            }

            log.info("Push completed successfully");
            return true;
        });
    }
}

