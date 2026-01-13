package de.jivz.agentservice.agent;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.jivz.agentservice.agent.model.AgentResult;
import de.jivz.agentservice.agent.model.AgentTask;
import de.jivz.agentservice.dto.Message;
import de.jivz.agentservice.dto.PRInfo;
import de.jivz.agentservice.dto.ReviewDecision;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            String userPrompt = buildReviewPrompt(prInfo);
            log.debug("📝 Review prompt built");

            // Step 3: Get MCP tools
            List<ToolDefinition> tools = mcpFactory.getAllToolDefinitions();
            log.info("🔧 Got {} MCP tools", tools.size());

            // Step 4: Build messages with code-reviewer prompt
            List<Message> messages = buildReviewMessages(userPrompt, tools);

            // Step 5: Execute tool loop (LLM will call git/rag tools as needed)
            String reviewResult = toolOrchestrator.executeToolLoop(messages, 0.2);

            // Step 5: Validate review quality
            if (isReviewIncomplete(reviewResult)) {
                log.warn("⚠️ First review attempt incomplete, retrying with stricter prompt...");

                // Add follow-up message
                messages.add(new Message("assistant", reviewResult));
                messages.add(new Message("user",
                        "Your review is incomplete! You MUST:\n" +
                                "1. Call git:get_pr_diff to get actual code\n" +
                                "2. Analyze the code thoroughly\n" +
                                "3. Include the DECISION BLOCK with structured format\n" +
                                "4. Provide detailed findings\n\n" +
                                "Do NOT explain what you will do - JUST DO IT NOW!"));

                // Retry
                reviewResult = toolOrchestrator.executeToolLoop(messages, 0.2);

                // If still bad, fail
                if (isReviewIncomplete(reviewResult)) {
                    log.error("❌ Review still incomplete after retry");
                    return AgentResult.builder()
                            .status(AgentResult.ExecutionStatus.FAILED)
                            .agentName(getName())
                            .errorMessage("LLM failed to provide proper review after retry")
                            .executionTimeMs(System.currentTimeMillis() - startTime)
                            .build();
                }
            }



            // Step 6: Parse result into structured review
            ReviewResult review = parseReviewResult(reviewResult, prInfo);

            long executionTime = System.currentTimeMillis() - startTime;
            review.setReviewTimeMs(executionTime);

            // 7. Save to DB + file
            log.info("💾 Saving review...");
            storageService.saveReview(review, getName());

            log.info("🎉 {} completed in {}ms with {} issues (DECISION: {})",
                    getName(), executionTime, review.getTotalIssues(), review.getDecision());

            return AgentResult.success(getName(), review, executionTime);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("❌ {} failed: {}", getName(), e.getMessage(), e);

            return AgentResult.failed(getName(), e.getMessage(), executionTime);
        }
    }

    /**
     * Проверяет что review не пустой и содержит анализ
     */
    private boolean isReviewIncomplete(String reviewText) {
        if (reviewText == null || reviewText.isBlank()) {
            return true;
        }

        String lower = reviewText.toLowerCase();

        // Плохие признаки: LLM объясняет вместо действия
        boolean hasExplanations = lower.contains("let me") ||
                lower.contains("first, i will") ||
                lower.contains("i need to") ||
                lower.contains("i'll get");

        // Слишком короткий review
        boolean tooShort = reviewText.length() < 150;

        // Нет анализа кода
        boolean noAnalysis = !lower.contains("code") &&
                !lower.contains("file") &&
                !lower.contains("line");

        // Нет структурированного блока решения
        boolean noDecisionBlock = !reviewText.contains("--- DECISION BLOCK ---") &&
                !reviewText.contains("DECISION:");

        return hasExplanations || tooShort || noAnalysis || noDecisionBlock;
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

            // Extract branch information (nested in "branches" object)
            Map<String, Object> branches = (Map<String, Object>) data.get("branches");
            String baseBranch = branches != null ? (String) branches.get("base") : null;
            String headBranch = branches != null ? (String) branches.get("head") : null;
            String baseSha = branches != null ? (String) branches.get("baseSha") : null;
            String headSha = branches != null ? (String) branches.get("headSha") : null;

            return PRInfo.builder()
                    .number(task.getPrNumber())
                    .repository(task.getRepository())
                    .title((String) data.get("title"))
                    .description((String) data.get("description"))
                    .author((String) data.get("author"))
                    .baseBranch(baseBranch)
                    .headBranch(headBranch)
                    .baseSha(baseSha)
                    .headSha(headSha)
                    .filesCount((Integer) data.get("filesCount"))
                    .additions((Integer) data.get("additions"))
                    .deletions((Integer) data.get("deletions"))
                    .build();

        } catch (Exception e) {
            log.error("❌ Error getting PR info: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Строит промпт для review - ЧЕТКИЕ ИНСТРУКЦИИ
     */
    private String buildReviewPrompt(PRInfo prInfo) {
        StringBuilder prompt = new StringBuilder();

        // Header
        prompt.append("# CODE REVIEW TASK - EXECUTE IMMEDIATELY!\n\n");

        // PR Details
        prompt.append("Review this Pull Request:\n\n");
        prompt.append("**PR Details:**\n");
        prompt.append("- Number: #").append(prInfo.getNumber()).append("\n");
        prompt.append("- Repository: ").append(prInfo.getRepository()).append("\n");
        prompt.append("- Title: ").append(prInfo.getTitle()).append("\n");
        prompt.append("- Author: ").append(prInfo.getAuthor()).append("\n");
        prompt.append("- Branch: ").append(prInfo.getHeadBranch())
                .append(" → ").append(prInfo.getBaseBranch()).append("\n");

        if (prInfo.getHeadSha() != null) {
            prompt.append("- Commit: ")
                    .append(prInfo.getHeadSha().substring(0, Math.min(7, prInfo.getHeadSha().length())))
                    .append("\n");
        }

        prompt.append("- Files: ").append(prInfo.getFilesCount())
                .append(" (+").append(prInfo.getAdditions())
                .append(" -").append(prInfo.getDeletions()).append(")\n\n");

        // Mandatory Steps
        prompt.append("## MANDATORY STEPS:\n\n");

        prompt.append("1. **IMMEDIATELY** call git:get_pr_diff with:\n");
        prompt.append("   - prNumber: ").append(prInfo.getNumber()).append("\n");
        prompt.append("   - repository: ").append(prInfo.getRepository()).append("\n");

        if (prInfo.getBaseSha() != null) {
            prompt.append("   - baseSha: ").append(prInfo.getBaseSha()).append("\n");
        }
        if (prInfo.getHeadSha() != null) {
            prompt.append("   - headSha: ").append(prInfo.getHeadSha()).append("\n");
        }
        prompt.append("\n");

        prompt.append("2. **ANALYZE** the actual code diff you receive\n\n");

        prompt.append("3. **PROVIDE** structured review with DECISION BLOCK:\n");
        prompt.append("```\n");
        prompt.append("--- DECISION BLOCK ---\n");
        prompt.append("DECISION: [APPROVE|REQUEST_CHANGES|COMMENT]\n");
        prompt.append("TOTAL_ISSUES: [number]\n");
        prompt.append("CRITICAL_ISSUES: [number]\n");
        prompt.append("MAJOR_ISSUES: [number]\n");
        prompt.append("MINOR_ISSUES: [number]\n");
        prompt.append("--- END DECISION ---\n");
        prompt.append("```\n\n");

        // Critical Rules
        prompt.append("## CRITICAL RULES:\n\n");
        prompt.append("- ❌ DO NOT say \"let me get...\" or \"first I will...\"\n");
        prompt.append("- ❌ DO NOT explain your process\n");
        prompt.append("- ✅ JUST CALL git:get_pr_diff IMMEDIATELY\n");
        prompt.append("- ✅ THEN analyze the code\n");
        prompt.append("- ✅ THEN provide structured review with DECISION BLOCK\n\n");

        prompt.append("**START NOW!**\n");

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
     * НОВАЯ ВЕРСИЯ: ищет структурированный DECISION BLOCK
     */
    private ReviewResult parseReviewResult(String reviewText, PRInfo prInfo) {
        log.debug("🔍 Parsing review result...");

        // Try to extract structured decision block first
        ReviewDecisionBlock decisionBlock = extractDecisionBlock(reviewText);

        ReviewDecision decision;
        int totalIssues;
        int criticalIssues;
        int majorIssues;
        int minorIssues;

        if (decisionBlock != null) {
            // Use structured data
            log.info("✅ Found structured DECISION BLOCK: {} with {} issues",
                    decisionBlock.decision, decisionBlock.totalIssues);

            decision = decisionBlock.decision;
            totalIssues = decisionBlock.totalIssues;
            criticalIssues = decisionBlock.criticalIssues;
            majorIssues = decisionBlock.majorIssues;
            minorIssues = decisionBlock.minorIssues;
        } else {
            // Fallback to heuristic parsing
            log.warn("⚠️ No structured DECISION BLOCK found, using fallback heuristics");

            totalIssues = countIssuesInText(reviewText);
            criticalIssues = countIssuesBySeverity(reviewText, "critical");
            majorIssues = countIssuesBySeverity(reviewText, "major");
            minorIssues = countIssuesBySeverity(reviewText, "minor");

            decision = determineDecisionHeuristic(reviewText, totalIssues, criticalIssues);
        }

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
                .criticalIssues(criticalIssues)
                .majorIssues(majorIssues)
                .minorIssues(minorIssues)
                .reviewText(reviewText)
                .reviewTimeMs(0L)
                .build();
    }

    /**
     * Извлекает структурированный блок решения из review текста
     */
    private ReviewDecisionBlock extractDecisionBlock(String text) {
        // Pattern to match DECISION BLOCK
        // Example:
        // --- DECISION BLOCK ---
        // DECISION: REQUEST_CHANGES
        // TOTAL_ISSUES: 7
        // CRITICAL_ISSUES: 2
        // MAJOR_ISSUES: 3
        // MINOR_ISSUES: 2
        // --- END DECISION ---

        Pattern pattern = Pattern.compile(
                "---\\s*DECISION BLOCK\\s*---\\s*" +
                        "DECISION:\\s*(\\w+)\\s*" +
                        "TOTAL_ISSUES:\\s*(\\d+)\\s*" +
                        "(?:CRITICAL_ISSUES:\\s*(\\d+)\\s*)?" +
                        "(?:MAJOR_ISSUES:\\s*(\\d+)\\s*)?" +
                        "(?:MINOR_ISSUES:\\s*(\\d+)\\s*)?" +
                        "---\\s*END DECISION\\s*---",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            try {
                String decisionStr = matcher.group(1).toUpperCase();
                int total = Integer.parseInt(matcher.group(2));
                int critical = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
                int major = matcher.group(4) != null ? Integer.parseInt(matcher.group(4)) : 0;
                int minor = matcher.group(5) != null ? Integer.parseInt(matcher.group(5)) : 0;

                ReviewDecision decision = ReviewDecision.valueOf(decisionStr);

                log.debug("✅ Extracted decision block: decision={}, total={}, critical={}, major={}, minor={}",
                        decision, total, critical, major, minor);

                return new ReviewDecisionBlock(decision, total, critical, major, minor);

            } catch (Exception e) {
                log.warn("⚠️ Failed to parse decision block: {}", e.getMessage());
                return null;
            }
        }

        log.debug("❌ No decision block found in review text");
        return null;
    }

    /**
     * Helper class для хранения извлеченного блока решения
     */
    private static class ReviewDecisionBlock {
        final ReviewDecision decision;
        final int totalIssues;
        final int criticalIssues;
        final int majorIssues;
        final int minorIssues;

        ReviewDecisionBlock(ReviewDecision decision, int total, int critical, int major, int minor) {
            this.decision = decision;
            this.totalIssues = total;
            this.criticalIssues = critical;
            this.majorIssues = major;
            this.minorIssues = minor;
        }
    }

    /**
     * Fallback: подсчет issues в тексте (старая логика)
     */
    private int countIssuesInText(String text) {
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

    /**
     * Подсчет issues по severity
     */
    private int countIssuesBySeverity(String text, String severity) {
        int count = 0;
        String lowerText = text.toLowerCase();
        String lowerSeverity = severity.toLowerCase();

        String[] lines = lowerText.split("\n");
        for (String line : lines) {
            if (line.contains(lowerSeverity) &&
                    (line.contains("issue") || line.contains("🔴") || line.contains("⚠️"))) {
                count++;
            }
        }
        return count;
    }

    /**
     * Fallback: определение решения эвристически (старая логика с ИСПРАВЛЕНИЕМ)
     */
    private ReviewDecision determineDecisionHeuristic(String text, int issueCount, int criticalCount) {
        String lower = text.toLowerCase();

        // FIX: Ищем ТОЧНОЕ совпадение для decision, а не просто наличие слова
        if (lower.contains("decision: approve") || lower.contains("decision:approve")) {
            return ReviewDecision.APPROVE;
        }
        if (lower.contains("decision: request_changes") ||
                lower.contains("decision:request_changes") ||
                lower.contains("decision: request changes")) {
            return ReviewDecision.REQUEST_CHANGES;
        }
        if (lower.contains("decision: comment") || lower.contains("decision:comment")) {
            return ReviewDecision.COMMENT;
        }

        // Fallback to issue-based heuristics
        if (criticalCount > 0 || issueCount > 5) {
            return ReviewDecision.REQUEST_CHANGES;
        } else if (issueCount == 0 || lower.contains("looks good") || lower.contains("lgtm")) {
            return ReviewDecision.APPROVE;
        } else {
            return ReviewDecision.COMMENT;
        }
    }

    private String extractSummary(String text) {
        // Skip decision block if present
        String cleanText = text;
        int endDecisionIndex = text.indexOf("--- END DECISION ---");
        if (endDecisionIndex > 0) {
            cleanText = text.substring(endDecisionIndex + 20).trim();
        }

        // Extract first paragraph as summary
        String[] paragraphs = cleanText.split("\n\n");
        if (paragraphs.length > 0) {
            String firstPara = paragraphs[0].trim();
            // Skip markdown headers
            if (firstPara.startsWith("#")) {
                if (paragraphs.length > 1) {
                    firstPara = paragraphs[1].trim();
                }
            }
            return firstPara.substring(0, Math.min(300, firstPara.length()));
        }
        return "Review completed";
    }
}