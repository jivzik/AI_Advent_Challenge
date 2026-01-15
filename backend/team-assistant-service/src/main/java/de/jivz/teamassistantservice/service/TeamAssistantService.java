package de.jivz.teamassistantservice.service;


import de.jivz.teamassistantservice.dto.Message;
import de.jivz.teamassistantservice.dto.TeamAssistantRequest;
import de.jivz.teamassistantservice.dto.TeamAssistantResponse;
import de.jivz.teamassistantservice.dto.ToolResponse;
import de.jivz.teamassistantservice.mcp.MCPFactory;
import de.jivz.teamassistantservice.mcp.model.ToolDefinition;
import de.jivz.teamassistantservice.persistence.entity.QueryLog;
import de.jivz.teamassistantservice.persistence.entity.TeamMember;
import de.jivz.teamassistantservice.persistence.QueryLogRepository;
import de.jivz.teamassistantservice.persistence.TeamMemberRepository;
import de.jivz.teamassistantservice.service.metadata.MetadataService;
import de.jivz.teamassistantservice.service.orchestrator.ToolExecutionOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Team Assistant Service - AI помощник для команды разработки
 *
 * Refactored to follow Clean Code and SOLID principles:
 * - Delegates metadata extraction to MetadataService
 * - Simplified source/action extraction logic
 * - Better separation of concerns
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TeamAssistantService {

    private final TeamMemberRepository teamMemberRepository;
    private final QueryLogRepository queryLogRepository;
    private final MCPFactory mcpFactory;
    private final ToolExecutionOrchestrator toolExecutionOrchestrator;
    private final PromptLoaderService promptLoader;
    private final MetadataService metadataService;

    @Value("${team-assistant.temperature:0.7}")
    private Double temperature;

    /**
     * Обработка запроса к Team Assistant
     */
    @Transactional
    public TeamAssistantResponse handleQuery(TeamAssistantRequest request) {
        log.info("🤖 Team Assistant query from: {}", request.getUserEmail());

        long startTime = System.currentTimeMillis();

        // 1. Найти или создать team member
        TeamMember member = findOrCreateTeamMember(request.getUserEmail());
        member.setLastActiveAt(LocalDateTime.now());
        teamMemberRepository.save(member);

        // 2. Определить тип запроса
        QueryType queryType = detectQueryType(request.getQuery());
        log.info("📊 Query type detected: {}", queryType);

        // 3. Построить сообщения для AI
        List<Message> messages = buildMessages(request.getQuery(), member);

        // 4. Выполнить tool loop - JETZT GIBT ES ToolResponse ZURÜCK
        ToolResponse toolResponse = toolExecutionOrchestrator.executeToolLoop(messages, temperature);

        // 5. Extrahiere Metadaten direkt aus ToolResponse (nicht aus String!)
        String answer = toolResponse.getAnswer();
        List<String> sources = toolResponse.getSources() != null ? toolResponse.getSources() : new ArrayList<>();
        List<String> toolsUsed = toolResponse.getToolsUsed() != null ? toolResponse.getToolsUsed() : new ArrayList<>();
        List<String> actions = extractActions(answer);
        BigDecimal confidence = calculateConfidence(sources, actions);

        // 6. Логировать запрос
        long responseTime = System.currentTimeMillis() - startTime;
        logQuery(member, request, answer, queryType, sources, actions, toolsUsed,
                confidence, (int) responseTime);

        // 7. Построить ответ
        return TeamAssistantResponse.builder()
                .answer(answer)
                .queryType(queryType.name())
                .sources(sources)
                .actions(actions)
                .toolsUsed(toolsUsed)
                .confidenceScore(confidence)
                .responseTimeMs((int) responseTime)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Находит или создает team member
     */
    private TeamMember findOrCreateTeamMember(String email) {
        return teamMemberRepository.findByEmail(email)
                .orElseGet(() -> {
                    log.info("👤 Creating new team member: {}", email);
                    TeamMember newMember = TeamMember.builder()
                            .email(email)
                            .fullName(extractNameFromEmail(email))
                            .role("developer")
                            .isActive(true)
                            .aiEnabled(true)
                            .build();
                    return teamMemberRepository.save(newMember);
                });
    }

    /**
     * Извлекает имя из email
     */
    private String extractNameFromEmail(String email) {
        String name = email.split("@")[0];
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    /**
     * Определяет тип запроса
     */
    private enum QueryType {
        SHOW_TASKS,         // "Show tasks", "List tasks"
        CREATE_TASK,        // "Create task"
        ANALYZE_PRIORITY,   // "Analyze priorities", "What's urgent"
        ANSWER_QUESTION,    // "How does X work?"
        RECOMMENDATION,     // "What should I do?", "Suggest"
        STATUS_UPDATE       // "What's the status?"
    }

    private QueryType detectQueryType(String query) {
        String lower = query.toLowerCase();

        if (lower.matches(".*(show|list|get|display).*task.*")) {
            return QueryType.SHOW_TASKS;
        }
        if (lower.matches(".*(create|add|make).*task.*")) {
            return QueryType.CREATE_TASK;
        }
        if (lower.matches(".*(priorit|urgent|critical|important|first).*")) {
            return QueryType.ANALYZE_PRIORITY;
        }
        if (lower.matches(".*(what should|recommend|suggest|advice).*")) {
            return QueryType.RECOMMENDATION;
        }
        if (lower.matches(".*(status|progress|sprint).*")) {
            return QueryType.STATUS_UPDATE;
        }

        return QueryType.ANSWER_QUESTION;
    }

    /**
     * Строит сообщения для AI
     */
    private List<Message> buildMessages(String query, TeamMember member) {
        // Получить все MCP tools
        List<ToolDefinition> tools = mcpFactory.getAllToolDefinitions();

        // Базовый system prompt
        String basePrompt = promptLoader.loadPrompt("team-assistant");
        String toolsPrompt = promptLoader.buildSystemPromptWithTools(tools);

        // Контекст team member
        String memberContext = String.format("""
            ## USER CONTEXT:
            - Name: %s
            - Role: %s
            - Team: %s
            - Preferred Language: %s
            """,
                member.getFullName(),
                member.getRole(),
                member.getTeam() != null ? member.getTeam() : "default",
                member.getPreferredLanguage()
        );

        String systemPrompt = basePrompt + "\n\n" + toolsPrompt + "\n\n" + memberContext;

        // Построить messages
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", systemPrompt));
        messages.add(new Message("user", query));

        return messages;
    }

    /**
     * Извлекает выполненные действия из ответа
     */
    private List<String> extractActions(String answer) {
        List<String> actions = new ArrayList<>();

        // Ищем упоминания о созданных/обновленных задачах
        if (answer.contains("Task created") || answer.contains("✅ Created task")) {
            actions.add("task_created");
        }
        if (answer.contains("Task updated") || answer.contains("✅ Updated task")) {
            actions.add("task_updated");
        }
        if (answer.contains("Task deleted") || answer.contains("✅ Deleted task")) {
            actions.add("task_deleted");
        }

        log.debug("⚡ Extracted {} actions", actions.size());
        return actions;
    }


    /**
     * Вычисляет confidence score
     */
    private BigDecimal calculateConfidence(List<String> sources, List<String> actions) {
        if (sources.size() >= 3) return BigDecimal.valueOf(0.95);
        if (!sources.isEmpty()) return BigDecimal.valueOf(0.85);
        if (!actions.isEmpty()) return BigDecimal.valueOf(0.90);
        return BigDecimal.valueOf(0.70);
    }

    /**
     * Логирует запрос
     */
    private void logQuery(TeamMember member, TeamAssistantRequest request, String answer,
                          QueryType queryType, List<String> sources, List<String> actions,
                          List<String> toolsUsed, BigDecimal confidence, int responseTime) {
        QueryLog queryLog = QueryLog.builder()
                .teamMember(member)
                .query(request.getQuery())
                .answer(answer)
                .queryType(queryType.name())
                .ragSources(sources.toArray(new String[0]))
                .actionsPerformed(actions.toArray(new String[0]))
                .toolsUsed(toolsUsed.toArray(new String[0]))
                .confidenceScore(confidence)
                .responseTimeMs(responseTime)
                .sessionId(request.getSessionId())
                .build();

        queryLogRepository.save(queryLog);

        log.info("📊 Query logged: type={}, sources={}, actions={}, confidence={}, time={}ms",
                queryType, sources.size(), actions.size(), confidence, responseTime);
    }
}