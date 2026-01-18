package de.jivz.agentservice.cli.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.agentservice.cli.domain.Command;
import de.jivz.agentservice.cli.domain.CommandResult;
import de.jivz.agentservice.mcp.GitHubMCPService;
import de.jivz.agentservice.mcp.model.MCPToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Executor for git status commands.
 * Single Responsibility: Show git repository status (modified, added, untracked files).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class GitStatusExecutor implements CommandExecutor {

    private final GitHubMCPService gitHubMCPService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean canExecute(Command command) {
        return command.getType() == Command.CommandType.GIT_STATUS;
    }

    @Override
    public Mono<CommandResult> execute(Command command) {
        log.info("Getting git status");

        return getGitStatus()
            .map(result -> {
                if (result != null) {
                    return formatGitStatus(result);
                } else {
                    return CommandResult.failure("Failed to get git status");
                }
            })
            .onErrorResume(error -> {
                log.error("Git status error: {}", error.getMessage());
                return Mono.just(CommandResult.failure(
                    "Failed to get git status: " + error.getMessage(),
                    (Exception) error));
            });
    }

    private Mono<Map<String, Object>> getGitStatus() {
        return Mono.fromCallable(() -> {
            MCPToolResult result = gitHubMCPService.execute("get_git_status", Map.of());

            if (!result.isSuccess()) {
                log.error("Git status failed: {}", result.getError());
                return null;
            }

            Object resultObj = result.getResult();
            if (resultObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> statusMap = (Map<String, Object>) resultObj;
                return statusMap;
            }

            return null;
        });
    }

    private CommandResult formatGitStatus(Map<String, Object> statusMap) {
        StringBuilder output = new StringBuilder();
        output.append("📊 Git Status:\n\n");

        List<?> modified = (List<?>) statusMap.get("modified");
        List<?> added = (List<?>) statusMap.get("added");
        List<?> untracked = (List<?>) statusMap.get("untracked");
        List<?> removed = (List<?>) statusMap.get("removed");
        List<?> missing = (List<?>) statusMap.get("missing");
        List<?> conflicting = (List<?>) statusMap.get("conflicting");

        boolean hasChanges = false;

        if (modified != null && !modified.isEmpty()) {
            hasChanges = true;
            output.append("📝 Modified files (").append(modified.size()).append("):\n");
            for (Object file : modified) {
                output.append("   M ").append(file).append("\n");
            }
            output.append("\n");
        }

        if (added != null && !added.isEmpty()) {
            hasChanges = true;
            output.append("✅ Staged files (").append(added.size()).append("):\n");
            for (Object file : added) {
                output.append("   A ").append(file).append("\n");
            }
            output.append("\n");
        }

        if (removed != null && !removed.isEmpty()) {
            hasChanges = true;
            output.append("🗑️  Removed files (").append(removed.size()).append("):\n");
            for (Object file : removed) {
                output.append("   D ").append(file).append("\n");
            }
            output.append("\n");
        }

        if (untracked != null && !untracked.isEmpty()) {
            hasChanges = true;
            output.append("❓ Untracked files (").append(untracked.size()).append("):\n");
            for (Object file : untracked) {
                output.append("   ? ").append(file).append("\n");
            }
            output.append("\n");
        }

        if (conflicting != null && !conflicting.isEmpty()) {
            hasChanges = true;
            output.append("⚠️  Conflicting files (").append(conflicting.size()).append("):\n");
            for (Object file : conflicting) {
                output.append("   C ").append(file).append("\n");
            }
            output.append("\n");
        }

        if (!hasChanges) {
            output.append("✨ Working tree clean - no changes\n");
        }

        return CommandResult.success(output.toString().trim());
    }
}

