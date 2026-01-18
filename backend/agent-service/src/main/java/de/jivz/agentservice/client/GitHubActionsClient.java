package de.jivz.agentservice.client;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.agentservice.dto.github.GitHubCommit;
import de.jivz.agentservice.dto.github.WorkflowRun;
import de.jivz.agentservice.mcp.GitHubMCPService;
import de.jivz.agentservice.mcp.model.MCPToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Component
@Slf4j
@RequiredArgsConstructor
public class GitHubActionsClient {
    private final GitHubMCPService gitHubMCPService;
    private final ObjectMapper objectMapper;
    @Value("${github.repository}")
    private String repository;
    public Mono<Void> triggerWorkflow(String workflowFileName, String branch) {
        log.info("Triggering workflow via MCP: {} on branch: {}", workflowFileName, branch);
        return Mono.fromCallable(() -> {
            MCPToolResult result = gitHubMCPService.execute("trigger_workflow", Map.of(
                "workflow", workflowFileName,
                "ref", branch
            ));
            if (!result.isSuccess()) {
                throw new RuntimeException("Failed to trigger workflow: " + result.getError());
            }
            return result;
        }).then();
    }
    public Mono<List<WorkflowRun>> getWorkflowRuns(String workflowFileName, int perPage) {
        log.debug("Fetching workflow runs via MCP for: {}", workflowFileName);
        return Mono.fromCallable(() -> {
            MCPToolResult result = gitHubMCPService.execute("list_workflow_runs", Map.of(
                "workflow", workflowFileName,
                "limit", perPage
            ));
            if (!result.isSuccess()) {
                log.error("Failed to fetch workflow runs: {}", result.getError());
                return List.<WorkflowRun>of();
            }
            return parseWorkflowRuns(result);
        });
    }
    public Mono<WorkflowRun> getWorkflowRun(Long runId) {
        log.debug("Fetching workflow run via MCP: {}", runId);
        return Mono.fromCallable(() -> {
            MCPToolResult result = gitHubMCPService.execute("get_workflow_run", Map.of(
                "run_id", runId
            ));
            if (!result.isSuccess()) {
                log.error("Failed to fetch workflow run: {}", result.getError());
                return null;
            }
            return parseSingleWorkflowRun(result);
        });
    }
    public Mono<List<GitHubCommit>> getCommits(int perPage) {
        log.debug("Fetching commits via MCP");
        return Mono.fromCallable(() -> {
            MCPToolResult result = gitHubMCPService.execute("list_commits", Map.of(
                "limit", perPage
            ));
            if (!result.isSuccess()) {
                log.error("Failed to fetch commits: {}", result.getError());
                return List.<GitHubCommit>of();
            }
            return parseCommits(result);
        });
    }
    public Mono<List<GitHubCommit>> getCommitsSince(String since, int perPage) {
        log.debug("Fetching commits since: {} via MCP", since);
        return Mono.fromCallable(() -> {
            MCPToolResult result = gitHubMCPService.execute("list_commits", Map.of(
                "since", since,
                "limit", perPage
            ));
            if (!result.isSuccess()) {
                log.error("Failed to fetch commits: {}", result.getError());
                return List.<GitHubCommit>of();
            }
            return parseCommits(result);
        });
    }
    private List<WorkflowRun> parseWorkflowRuns(MCPToolResult result) {
        List<WorkflowRun> runs = new ArrayList<>();
        try {
            Object resultObj = result.getResult();
            if (resultObj == null) return runs;
            JsonNode dataNode = objectMapper.valueToTree(resultObj);
            if (dataNode.has("workflow_runs")) {
                JsonNode runsArray = dataNode.get("workflow_runs");
                for (JsonNode runNode : runsArray) {
                    WorkflowRun run = parseWorkflowRunNode(runNode);
                    if (run != null) {
                        runs.add(run);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse workflow runs: {}", e.getMessage(), e);
        }
        return runs;
    }
    private WorkflowRun parseSingleWorkflowRun(MCPToolResult result) {
        try {
            Object resultObj = result.getResult();
            if (resultObj == null) return null;
            JsonNode runNode = objectMapper.valueToTree(resultObj);
            return parseWorkflowRunNode(runNode);
        } catch (Exception e) {
            log.error("Failed to parse workflow run: {}", e.getMessage(), e);
        }
        return null;
    }
    private WorkflowRun parseWorkflowRunNode(JsonNode node) {
        try {
            return WorkflowRun.builder()
                .id(node.path("id").asLong())
                .name(node.path("name").asText())
                .status(node.path("status").asText())
                .conclusion(node.path("conclusion").asText())
                .htmlUrl(node.path("html_url").asText())
                .createdAt(node.path("created_at").asText())
                .updatedAt(node.path("updated_at").asText())
                .runNumber(node.path("run_number").asInt())
                .workflowId(node.path("workflow_id").asLong())
                .build();
        } catch (Exception e) {
            log.warn("Failed to parse workflow run node: {}", e.getMessage());
            return null;
        }
    }
    private List<GitHubCommit> parseCommits(MCPToolResult result) {
        List<GitHubCommit> commits = new ArrayList<>();
        try {
            Object resultObj = result.getResult();
            if (resultObj == null) return commits;
            JsonNode commitsArray = objectMapper.valueToTree(resultObj);
            if (commitsArray.isArray()) {
                for (JsonNode commitNode : commitsArray) {
                    GitHubCommit commit = parseCommitNode(commitNode);
                    if (commit != null) {
                        commits.add(commit);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse commits: {}", e.getMessage(), e);
        }
        return commits;
    }
    private GitHubCommit parseCommitNode(JsonNode node) {
        try {
            JsonNode commitDetail = node.path("commit");
            JsonNode author = commitDetail.path("author");
            GitHubCommit.Author commitAuthor = new GitHubCommit.Author(
                author.path("name").asText(),
                author.path("email").asText(),
                author.path("date").asText()
            );
            GitHubCommit.CommitDetail detail = new GitHubCommit.CommitDetail(
                commitDetail.path("message").asText(),
                commitAuthor
            );
            return GitHubCommit.builder()
                .sha(node.path("sha").asText())
                .commit(detail)
                .htmlUrl(node.path("html_url").asText())
                .build();
        } catch (Exception e) {
            log.warn("Failed to parse commit node: {}", e.getMessage());
            return null;
        }
    }
}
