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
 * Executor for git commit commands.
 * Single Responsibility: Handle git commit operations via MCP.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CommitExecutor implements CommandExecutor {

    private final GitHubMCPService gitHubMCPService;

    @Override
    public boolean canExecute(Command command) {
        return command.getType() == Command.CommandType.COMMIT;
    }

    @Override
    public Mono<CommandResult> execute(Command command) {
        String commitMessage = command.getServiceName(); // Используем serviceName для commit message

        if (commitMessage == null || commitMessage.isBlank()) {
            return Mono.just(CommandResult.failure("Commit message is required"));
        }

        log.info("Creating git commit with message: {}", commitMessage);

        return commitChanges(commitMessage)
            .map(success -> {
                if (success) {
                    return CommandResult.success(
                        "✅ Changes committed successfully",
                        "📝 Message: " + commitMessage);
                } else {
                    return CommandResult.failure("Failed to commit changes");
                }
            })
            .onErrorResume(error -> {
                log.error("Commit error: {}", error.getMessage());
                return Mono.just(CommandResult.failure(
                    "Commit failed: " + error.getMessage(),
                    (Exception) error));
            });
    }

    private Mono<Boolean> commitChanges(String message) {
        return Mono.fromCallable(() -> {
            // Сначала git add .
            MCPToolResult addResult = gitHubMCPService.execute("git_add", Map.of(
                "path", "."
            ));

            if (!addResult.isSuccess()) {
                log.error("Git add failed: {}", addResult.getError());
                throw new RuntimeException("Failed to stage changes: " + addResult.getError());
            }

            log.info("Files staged successfully");

            // Затем git commit
            MCPToolResult commitResult = gitHubMCPService.execute("git_commit", Map.of(
                "message", message
            ));

            if (!commitResult.isSuccess()) {
                log.error("Git commit failed: {}", commitResult.getError());
                throw new RuntimeException("Failed to commit: " + commitResult.getError());
            }

            log.info("Commit created successfully");
            return true;
        });
    }
}

