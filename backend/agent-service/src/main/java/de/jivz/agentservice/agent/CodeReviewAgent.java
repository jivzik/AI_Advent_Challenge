package de.jivz.agentservice.agent;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.jivz.agentservice.agent.model.AgentResult;
import de.jivz.agentservice.agent.model.AgentTask;
import de.jivz.agentservice.dto.Message;
import de.jivz.agentservice.dto.PRInfo;
import de.jivz.agentservice.dto.ReviewResult;
import de.jivz.agentservice.mcp.MCPFactory;
import de.jivz.agentservice.mcp.model.MCPToolResult;
import de.jivz.agentservice.mcp.model.ToolDefinition;
import de.jivz.agentservice.service.PromptLoaderService;
import de.jivz.agentservice.service.ReviewStorageService;
import de.jivz.agentservice.service.orchestrator.ToolExecutionOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Agent для автоматического code review
 * Использует существующую инфраструктуру: MCPFactory, ToolExecutionOrchestrator
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CodeReviewAgent implements Agent {

    private final MCPFactory mcpFactory;
    private final ToolExecutionOrchestrator toolOrchestrator;
    private final PromptLoaderService promptLoader;
    private final ReviewStorageService storageService;

    @Override
    public boolean canHandle(AgentTask task) {
        return task.getType() == AgentTask.TaskType.CODE_REVIEW;
    }

    @Override
    public int priority() {
        return 90; // High priority
    }

    @Override
    public String getName() {
        return "CodeReviewAgent";
    }

    @Override
    public AgentResult execute(AgentTask task) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("🔍 {} starting review for PR #{}",
                    getName(), task.getPrNumber());

            // Step 1: Get PR info via MCP
            PRInfo prInfo = getPRInfo(task);

            if (prInfo == null) {
                return AgentResult.builder()
                        .status(AgentResult.ExecutionStatus.FAILED)
                        .agentName(getName())
                        .errorMessage("Failed to get PR info")
                        .executionTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            log.info("✅ Got PR info: {} files changed", prInfo.getFilesCount());

            // Step 2: Build review prompt
            String userPrompt = buildReviewPrompt(prInfo, task);
            log.debug("📝 Review prompt built");

            // Step 3: Get MCP tools
            List<ToolDefinition> tools = mcpFactory.getAllToolDefinitions();
            log.info("🔧 Got {} MCP tools", tools.size());

            // Step 4: Build messages with code-reviewer prompt
            List<Message> messages = buildReviewMessages(userPrompt, tools);

            // Step 5: Execute tool loop (LLM will call git/rag tools as needed)
            String reviewResult = toolOrchestrator.executeToolLoop(messages, 0.2);

            // Step 6: Parse result into structured review
            ReviewResult review = parseReviewResult(reviewResult, prInfo);

            long executionTime = System.currentTimeMillis() - startTime;
            review.setReviewTimeMs(executionTime);

            // 7. Save to DB + file
            log.info("💾 Saving review...");
            storageService.saveReview(review, getName());

            log.info("🎉 {} completed in {}ms with {} issues",
                    getName(), executionTime, review.getTotalIssues());

            return AgentResult.success(getName(), review, executionTime);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("❌ {} failed: {}", getName(), e.getMessage(), e);

            return AgentResult.failed(getName(), e.getMessage(), executionTime);
        }
    }

    /**
     * Получает информацию о PR через MCP git tools
     */
    private PRInfo getPRInfo(AgentTask task) {
        try {
            // Call git:get_pr_info
            Map<String, Object> params = new HashMap<>();
            params.put("prNumber", task.getPrNumber());
            if (task.getRepository() != null) {
                params.put("repository", task.getRepository());
            }

            MCPToolResult result = mcpFactory.route("git:get_pr_info", params);

            if (!result.isSuccess()) {
                log.error("❌ git:get_pr_info failed: {}", result.getError());
                return null;
            }

            // Parse result to PRInfo
            Map<String, Object> data = (Map<String, Object>) result.getResult();

            return PRInfo.builder()
                    .number(task.getPrNumber())
                    .title((String) data.get("title"))
                    .description((String) data.get("description"))
                    .author((String) data.get("author"))
                    .baseBranch((String) data.get("baseBranch"))
                    .headBranch((String) data.get("headBranch"))
                    .baseSha((String) data.get("baseSha"))
                    .headSha((String) data.get("headSha"))
                    .filesCount((Integer) data.getOrDefault("filesCount", 0))
                    .additions((Integer) data.getOrDefault("additions", 0))
                    .deletions((Integer) data.getOrDefault("deletions", 0))
                    .build();

        } catch (Exception e) {
            log.error("❌ Error getting PR info: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Строит промпт для review
     */
    private String buildReviewPrompt(PRInfo prInfo, AgentTask task) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Please review this Pull Request:\n\n");
        prompt.append("**PR #").append(prInfo.getNumber()).append("**\n");
        prompt.append("**Title:** ").append(prInfo.getTitle()).append("\n");
        if (prInfo.getDescription() != null && !prInfo.getDescription().isBlank()) {
            prompt.append("**Description:** ").append(prInfo.getDescription()).append("\n");
        }
        prompt.append("**Author:** ").append(prInfo.getAuthor()).append("\n");
        prompt.append("**Branch:** ").append(prInfo.getHeadBranch())
                .append(" → ").append(prInfo.getBaseBranch()).append("\n\n");

        prompt.append("**Changes:**\n");
        prompt.append("- Files: ").append(prInfo.getFilesCount()).append("\n");
        prompt.append("- Additions: +").append(prInfo.getAdditions()).append("\n");
        prompt.append("- Deletions: -").append(prInfo.getDeletions()).append("\n\n");

        prompt.append("**Instructions:**\n");
        prompt.append("1. Use `git:get_pr_diff` to get the actual code changes\n");
        prompt.append("2. Use `rag:search_documents` to find relevant coding standards\n");
        prompt.append("3. Analyze the code for:\n");
        prompt.append("   - Code quality issues\n");
        prompt.append("   - Security vulnerabilities\n");
        prompt.append("   - Performance problems\n");
        prompt.append("   - Best practices violations\n");
        prompt.append("4. Provide structured review with issues and suggestions\n");

        return prompt.toString();
    }

    /**
     * Собирает сообщения для LLM с code-reviewer prompt
     */
    private List<Message> buildReviewMessages(String userPrompt, List<ToolDefinition> tools) {
        List<Message> messages = new ArrayList<>();

        // System prompt: code-reviewer
        String codeReviewerPrompt = promptLoader.loadPrompt("code-reviewer");

        if (codeReviewerPrompt == null) {
            log.warn("⚠️ code-reviewer prompt not found, using default");
            codeReviewerPrompt = "You are a senior code reviewer. " +
                    "Review code thoroughly for quality, security, and best practices.";
        }

        // Add tools to system prompt
        String systemPrompt = promptLoader.buildSystemPromptWithTools(tools);
        String fullSystemPrompt = systemPrompt + "\n\n" + codeReviewerPrompt;

        messages.add(new Message("system", fullSystemPrompt));
        messages.add(new Message("user", userPrompt));

        return messages;
    }

    /**
     * Парсит текстовый результат в структурированный ReviewResult
     */
    private ReviewResult parseReviewResult(String reviewText, PRInfo prInfo) {
        // Count issues
        int totalIssues = countIssuesInText(reviewText);

        // Determine decision
        ReviewResult.ReviewDecision decision = determineDecision(reviewText, totalIssues);

        // Extract summary (first paragraph or first 200 chars)
        String summary = extractSummary(reviewText);

        return ReviewResult.builder()
                .prNumber(prInfo.getNumber())
                .repository(prInfo.getRepository())
                .baseSha(prInfo.getBaseSha())
                .headSha(prInfo.getHeadSha())
                .prTitle(prInfo.getTitle())
                .prAuthor(prInfo.getAuthor())
                .baseBranch(prInfo.getBaseBranch())
                .headBranch(prInfo.getHeadBranch())
                .decision(decision)
                .summary(summary)
                .totalIssues(totalIssues)
                .reviewText(reviewText)  // Полный текст от LLM
                .reviewTimeMs(0L)  // Будет установлено позже
                .build();
    }

    private int countIssuesInText(String text) {
        // Count "Issue:", "Problem:", "Warning:" etc
        int count = 0;
        String[] lines = text.toLowerCase().split("\n");
        for (String line : lines) {
            if (line.contains("issue:") ||
                    line.contains("problem:") ||
                    line.contains("warning:") ||
                    line.contains("⚠️") ||
                    line.contains("🔴")) {
                count++;
            }
        }
        return count;
    }

    private ReviewResult.ReviewDecision determineDecision(String text, int issueCount) {
        String lower = text.toLowerCase();

        if (issueCount == 0 || lower.contains("looks good") || lower.contains("approve")) {
            return ReviewResult.ReviewDecision.APPROVE;
        } else if (issueCount > 5 || lower.contains("critical") || lower.contains("must fix")) {
            return ReviewResult.ReviewDecision.REQUEST_CHANGES;
        } else {
            return ReviewResult.ReviewDecision.COMMENT;
        }
    }

    private String extractSummary(String text) {
        // Extract first paragraph as summary
        String[] paragraphs = text.split("\n\n");
        if (paragraphs.length > 0) {
            return paragraphs[0].substring(0, Math.min(200, paragraphs[0].length()));
        }
        return "Review completed";
    }
}