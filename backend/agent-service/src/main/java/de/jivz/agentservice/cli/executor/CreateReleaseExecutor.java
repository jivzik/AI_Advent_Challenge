package de.jivz.agentservice.cli.executor;

import de.jivz.agentservice.cli.domain.Command;
import de.jivz.agentservice.cli.domain.CommandResult;
import de.jivz.agentservice.cli.service.CLIStateService;
import de.jivz.agentservice.client.GitHubActionsClient;
import de.jivz.agentservice.dto.github.GitHubCommit;
import de.jivz.agentservice.service.client.OpenRouterApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Executor for creating GitHub releases.
 * Single Responsibility: Create GitHub releases with AI-generated notes.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CreateReleaseExecutor implements CommandExecutor {

    private final GitHubActionsClient gitHubActionsClient;
    private final OpenRouterApiClient openRouterApiClient;
    private final WebClient.Builder webClientBuilder;
    private final CLIStateService cliStateService;

    @Value("${github.token}")
    private String githubToken;

    @Value("${github.repository}")
    private String repository;

    @Override
    public boolean canExecute(Command command) {
        return command.getType() == Command.CommandType.CREATE_RELEASE;
    }

    @Override
    public Mono<CommandResult> execute(Command command) {
        log.info("Creating GitHub release");

        // Используем сохраненные release notes если есть
        if (cliStateService.hasReleaseNotes()) {
            String releaseNotes = cliStateService.getAndClearReleaseNotes();
            return createRelease(releaseNotes, command.getServiceName());
        }

        // Иначе генерируем новые
        return generateAndCreateRelease(command.getServiceName());
    }

    private Mono<CommandResult> generateAndCreateRelease(String tagName) {
        return gitHubActionsClient.getCommits(30)
            .flatMap(commits -> {
                if (commits.isEmpty()) {
                    return Mono.just(CommandResult.failure("No commits found"));
                }

                String releaseNotes = generateReleaseNotes(commits);
                return createRelease(releaseNotes, tagName);
            })
            .onErrorResume(error -> {
                log.error("Failed to create release: {}", error.getMessage());
                return Mono.just(CommandResult.failure(
                    "Failed to create release: " + error.getMessage(),
                    (Exception) error));
            });
    }

    private Mono<CommandResult> createRelease(String releaseNotes, String customTag) {
        return Mono.fromCallable(() -> {
            String tag = customTag != null ? customTag : generateVersionTag();
            String name = "Release " + tag;

            log.info("Creating GitHub release: {}", tag);

            WebClient webClient = webClientBuilder
                .baseUrl("https://api.github.com")
                .defaultHeader("Authorization", "Bearer " + githubToken)
                .defaultHeader("Accept", "application/vnd.github.v3+json")
                .build();

            Map<String, Object> releaseRequest = Map.of(
                "tag_name", tag,
                "name", name,
                "body", releaseNotes,
                "draft", false,
                "prerelease", false
            );

            Map<String, Object> response = webClient.post()
                .uri("/repos/{owner}/{repo}/releases",
                    repository.split("/")[0],
                    repository.split("/")[1])
                .bodyValue(releaseRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            String htmlUrl = (String) response.get("html_url");

            String successMessage = String.format(
                "✅ GitHub Release created successfully!\n📦 Release: %s\n🔗 URL: %s\n🏷️  Tag: %s",
                name, htmlUrl, tag
            );

            return CommandResult.success(successMessage);
        });
    }

    private String generateReleaseNotes(List<GitHubCommit> commits) {
        StringBuilder commitsSummary = new StringBuilder();
        commitsSummary.append("Recent commits:\n\n");

        for (GitHubCommit commit : commits) {
            String message = commit.getCommit().getMessage();
            String author = commit.getCommit().getAuthor().getName();
            commitsSummary.append("- ").append(message)
                .append(" (").append(author).append(")\n");
        }

        String prompt = """
            Generate professional release notes from these commits.
            
            %s
            
            Format:
            # Release Notes - [Date]
            
            ## Features
            - ...
            
            ## Bug Fixes
            - ...
            
            ## Technical Improvements
            - ...
            
            Be concise and professional. Group similar changes together.
            """.formatted(commitsSummary);

        List<de.jivz.agentservice.dto.Message> messages = List.of(
            de.jivz.agentservice.dto.Message.builder().role("system").content("You are a technical writer creating release notes.").build(),
            de.jivz.agentservice.dto.Message.builder().role("user").content(prompt).build()
        );

        return openRouterApiClient.sendContextDetectionRequest(messages);
    }

    private String generateVersionTag() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd-HHmm"));
        return "v" + timestamp;
    }
}
