package de.jivz.agentservice.service;

import de.jivz.agentservice.agent.model.AgentTask;
import de.jivz.agentservice.dto.PRInfo;
import de.jivz.agentservice.mcp.MCPFactory;
import de.jivz.agentservice.mcp.model.MCPToolResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import de.jivz.agentservice.service.AgentOrchestratorService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for detecting new Pull Requests that need review
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PRDetectorService {

    private final MCPFactory mcpFactory;
    private final ReviewStorageService storageService;
    private final AgentOrchestratorService orchestrator;

    @Value("${code-review.repository:}")
    private String defaultRepository;

    /**
     * Main method: Detect new PRs and trigger reviews
     *
     * @return Number of PRs processed
     */
    public int detectAndProcessNewPRs() {
        log.info("🔎 Detecting new PRs...");

        // 1. Get list of open PRs via MCP
        List<PRInfo> openPRs = getOpenPRs();

        if (openPRs.isEmpty()) {
            log.debug("No open PRs found");
            return 0;
        }

        log.info("📋 Found {} open PR(s)", openPRs.size());

        // 2. Filter: only new PRs (not already reviewed)
        List<PRInfo> newPRs = filterNewPRs(openPRs);

        if (newPRs.isEmpty()) {
            log.info("✅ All PRs already reviewed");
            return 0;
        }

        log.info("🆕 Found {} new PR(s) to review", newPRs.size());

        // 3. Trigger review for each new PR
        int processedCount = 0;
        for (PRInfo pr : newPRs) {
            try {
                processPR(pr);
                processedCount++;
            } catch (Exception e) {
                log.error("❌ Failed to process PR #{}: {}",
                        pr.getNumber(), e.getMessage(), e);
            }
        }

        return processedCount;
    }

    /**
     * Get list of open PRs via MCP git tool
     */
    private List<PRInfo> getOpenPRs() {
        try {
            Map<String, Object> params = Map.of();

            if (defaultRepository != null && !defaultRepository.isBlank()) {
                params = Map.of("repository", defaultRepository);
            }

            MCPToolResult result = mcpFactory.route("git:list_open_prs", params);

            if (!result.isSuccess()) {
                log.warn("⚠️  git:list_open_prs failed: {}", result.getError());
                return List.of();
            }

            // Parse result
            return parsePRList(result.getResult());

        } catch (Exception e) {
            log.error("❌ Error getting open PRs: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Parse MCP result into PRInfo list
     */
    @SuppressWarnings("unchecked")
    private List<PRInfo> parsePRList(Object result) {
        if (!(result instanceof List)) {
            log.warn("Unexpected result type from git:list_open_prs");
            return List.of();
        }

        List<PRInfo> prs = new ArrayList<>();
        List<Map<String, Object>> list = (List<Map<String, Object>>) result;

        for (Map<String, Object> item : list) {
            try {
                PRInfo pr = PRInfo.builder()
                        .number((Integer) item.get("number"))
                        .title((String) item.get("title"))
                        .description((String) item.get("description"))
                        .author((String) item.get("author"))
                        .baseBranch((String) item.get("baseBranch"))
                        .headBranch((String) item.get("headBranch"))
                        .baseSha((String) item.get("baseSha"))
                        .headSha((String) item.get("headSha"))
                        .repository((String) item.get("repository"))
                        .build();

                prs.add(pr);

            } catch (Exception e) {
                log.warn("Failed to parse PR item: {}", e.getMessage());
            }
        }

        return prs;
    }

    /**
     * Filter PRs: return only those not yet reviewed
     */
    private List<PRInfo> filterNewPRs(List<PRInfo> allPRs) {
        List<PRInfo> newPRs = new ArrayList<>();

        for (PRInfo pr : allPRs) {
            // Check if already reviewed by checking (prNumber, headSha)
            boolean alreadyReviewed = storageService.isAlreadyReviewed(
                    pr.getNumber(),
                    pr.getHeadSha(),
                    "CodeReviewAgent"
            );

            if (!alreadyReviewed) {
                newPRs.add(pr);
                log.debug("🆕 New PR detected: #{} - {}", pr.getNumber(), pr.getTitle());
            } else {
                log.debug("⏭️  Already reviewed: PR #{} (sha: {})",
                        pr.getNumber(), pr.getHeadSha());
            }
        }

        return newPRs;
    }

    /**
     * Process a single PR: trigger agent review
     */
    private void processPR(PRInfo pr) {
        log.info("🚀 Processing PR #{}: {}", pr.getNumber(), pr.getTitle());

        // Create agent task
        AgentTask task = AgentTask.builder()
                .type(AgentTask.TaskType.CODE_REVIEW)
                .prNumber(pr.getNumber())
                .repository(pr.getRepository())
                .context(Map.of(
                        "prInfo", pr,
                        "baseSha", pr.getBaseSha(),
                        "headSha", pr.getHeadSha()
                ))
                .build();

        // Execute via orchestrator
        orchestrator.executeTask(task);
    }
}