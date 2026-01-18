package de.jivz.agentservice.cli.executor;

import de.jivz.agentservice.cli.domain.Command;
import de.jivz.agentservice.cli.domain.CommandResult;
import de.jivz.agentservice.client.GitHubActionsClient;
import de.jivz.agentservice.dto.Message;
import de.jivz.agentservice.dto.github.GitHubCommit;
import de.jivz.agentservice.service.client.OpenRouterApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Executor for generating release notes using AI.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ReleaseNotesExecutor implements CommandExecutor {

    private final GitHubActionsClient githubClient;
    private final OpenRouterApiClient openRouterApiClient;

    private static final int COMMIT_LIMIT = 30;

    @Override
    public boolean canExecute(Command command) {
        return command.getType() == Command.CommandType.RELEASE_NOTES;
    }

    @Override
    public Mono<CommandResult> execute(Command command) {
        log.info("Generating release notes");

        return githubClient.getCommits(COMMIT_LIMIT)
            .flatMap(this::generateReleaseNotes)
            .map(notes -> CommandResult.success("Release Notes Generated", notes))
            .onErrorResume(error -> {
                log.error("Failed to generate release notes: {}", error.getMessage());
                return Mono.just(CommandResult.failure(
                    "Failed to generate release notes: " + error.getMessage(),
                    (Exception) error));
            });
    }

    private Mono<String> generateReleaseNotes(List<GitHubCommit> commits) {
        if (commits.isEmpty()) {
            return Mono.just("No commits found for release notes.");
        }

        String commitSummary = commits.stream()
            .map(this::formatCommit)
            .collect(Collectors.joining("\n"));

        String prompt = buildPrompt(commitSummary);

        List<Message> messages = List.of(
            Message.builder()
                .role("system")
                .content("You are a technical writer specializing in release notes.")
                .build(),
            Message.builder()
                .role("user")
                .content(prompt)
                .build()
        );

        return Mono.fromCallable(() -> openRouterApiClient.sendChatRequest(messages, 0.3, 1000))
            .map(this::formatReleaseNotes);
    }

    private String buildPrompt(String commitSummary) {
        return "Analyze these Git commits and create professional release notes.\n\n" +
            "Format:\n" +
            "# Release Notes - [Date]\n\n" +
            "## [Service Name]\n" +
            "### Features\n" +
            "- ...\n\n" +
            "### Bug Fixes\n" +
            "- ...\n\n" +
            "Make it concise, professional, and user-friendly.\n" +
            "Group by service if possible (team-service, support-service, etc.).\n\n" +
            "Commits:\n" + commitSummary;
    }

    private String formatCommit(GitHubCommit commit) {
        String author = commit.getCommit().getAuthor().getName();
        String message = commit.getCommit().getMessage();
        String date = commit.getCommit().getAuthor().getDate();

        return String.format("- [%s] %s (by %s)",
            date.substring(0, 10),
            message.split("\n")[0],
            author);
    }

    private String formatReleaseNotes(String aiGeneratedNotes) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n📝 Generated Release Notes:\n");
        sb.append("═".repeat(70)).append("\n\n");
        sb.append(aiGeneratedNotes);
        sb.append("\n\n");
        sb.append("═".repeat(70)).append("\n");
        sb.append("\n💡 To create a GitHub release, use: create release\n");

        return sb.toString();
    }
}

