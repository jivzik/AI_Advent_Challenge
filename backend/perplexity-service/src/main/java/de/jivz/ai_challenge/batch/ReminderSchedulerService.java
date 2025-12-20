package de.jivz.ai_challenge.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.ai_challenge.dto.Message;
import de.jivz.ai_challenge.dto.StructuredSummaryDto;
import de.jivz.ai_challenge.dto.SonarToolDto;
import de.jivz.ai_challenge.dto.SonarToolDto.SonarToolResponse;
import de.jivz.ai_challenge.dto.SonarToolDto.SummaryInfo;
import de.jivz.ai_challenge.dto.SonarToolDto.ToolCall;
import de.jivz.ai_challenge.dto.SonarToolDto.DueTask;
import de.jivz.ai_challenge.entity.ReminderSummary;
import de.jivz.ai_challenge.entity.ReminderSummary.Priority;
import de.jivz.ai_challenge.entity.ReminderSummary.SummaryType;
import de.jivz.ai_challenge.mcp.MCPFactory;
import de.jivz.ai_challenge.mcp.model.MCPToolResult;
import de.jivz.ai_challenge.mcp.model.ToolDefinition;
import de.jivz.ai_challenge.repository.ReminderSummaryRepository;
import de.jivz.ai_challenge.service.perplexity.PerplexityToolClient;
import de.jivz.ai_challenge.service.perplexity.model.PerplexityResponseWithMetrics;
import de.jivz.ai_challenge.service.strategy.ReminderToolsPromptStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ReminderSchedulerService - Cron Job für automatische Erinnerungs-Zusammenfassungen.
 *
 * Функциональность:
 * 1. Запускается периодически (конфигурируется через cron expression)
 * 2. Получает текущий список MCP Tools из Backend
 * 3. Создает динамический системный промпт с Tools
 * 4. Выполняет Tool-Loop (как ChatWithToolsService)
 * 5. Сохраняет сводку в PostgreSQL
 * 6. Может отправлять уведомления
 *
 * Конфигурация через application.properties:
 * - reminder.scheduler.enabled=true
 * - reminder.scheduler.cron=0 0 9 * * ?  (ежедневно в 9:00)
 * - reminder.scheduler.user-id=default-user
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReminderSchedulerService {

    private static final int MAX_TOOL_ITERATIONS = 10;
    private static final String STEP_TOOL = "tool";
    private static final String STEP_FINAL = "final";


    private final PerplexityToolClient perplexityToolClient;
    private final ReminderSummaryRepository reminderRepository;
    private final ReminderToolsPromptStrategy promptStrategy;
    private final ObjectMapper objectMapper;
    private final MCPFactory mcpFactory;

    @Value("${reminder.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    @Value("${reminder.scheduler.user-id:system}")
    private String defaultUserId;

    @Value("${reminder.scheduler.temperature:0.3}")
    private double temperature;

    /**
     * Основной Cron-Job: запускается согласно конфигурированному cron expression.
     *
     * По умолчанию: каждый день в 9:00
     * Может быть переопределено в application.properties:
     * reminder.scheduler.cron=0 0 9 * * ?
     */
    @Scheduled(cron = "${reminder.scheduler.cron:0 0 9 * * ?}")
    @Transactional
    public void scheduledReminderTask() {
        if (!schedulerEnabled) {
            log.debug("⏸️ Scheduler напоминаний отключен");
            return;
        }

        log.info("⏰ Запуск планового задания напоминания в {}", LocalDateTime.now());

        try {
            ReminderSummary summary = executeReminderWorkflow(defaultUserId);

            if (summary != null) {
                log.info("✅ Задание напоминания завершено. ID сводки: {}, Название: {}",
                    summary.getId(), summary.getTitle());

                // Опционально: отправить уведомление
                triggerNotification(summary);
            }

        } catch (Exception e) {
            log.error("❌ Задание напоминания не удалось: {}", e.getMessage(), e);
        }
    }

    /**
     * Ручной триггер для workflow напоминания.
     * Может быть вызван из контроллера.
     *
     * @param userId ID пользователя для сводки
     * @return Созданная сводка
     */
    @Transactional
    public ReminderSummary executeReminderWorkflow(String userId) {
        log.info("🚀 Выполнение workflow напоминания для пользователя: {}", userId);

        // 1. Получить текущие MCP Tools из Backend
        List<ToolDefinition> tools = mcpFactory.getAllToolDefinitions();

        log.info("📋 Получено {} MCP tools", tools.size());

        // 2. Создать динамический системный промпт
        String systemPrompt = promptStrategy.buildDynamicSystemPrompt(tools);
        log.debug("📝 Построен динамический системный промпт ({} символов)", systemPrompt.length());

        // 3. Собрать сообщения
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", systemPrompt));
        messages.add(new Message("user",
            "Создай сводку моих текущих задач. " +
            "Определи важные и просроченные задачи."));

        // 4. Выполнить Tool-Loop
        ToolLoopResult result = executeToolLoop(messages);

        // 5. Сохранить сводку в БД
        ReminderSummary summary = saveReminderSummary(userId, result);

        return summary;
    }


    /**
     * Tool-Loop - похож на ChatWithToolsService.
     * Итеративно выполняет инструменты до окончательного результата.
     */
    private ToolLoopResult executeToolLoop(List<Message> messages) {
        int iteration = 0;
        StringBuilder rawDataBuilder = new StringBuilder();

        while (iteration < MAX_TOOL_ITERATIONS) {
            iteration++;
            log.info("🔄 Итерация Tool loop: {}", iteration);

            // Вызов Sonar
            String sonarResponse = callSonar(messages);
            log.debug("📥 Ответ Sonar: {}", sonarResponse);

            // Парсинг JSON
            SonarToolResponse parsed = parseSonarResponse(sonarResponse);

            if (parsed == null) {
                log.error("❌ Ошибка парсинга ответа Sonar");
                return new ToolLoopResult(
                    "Ошибка при парсинге ответа",
                    rawDataBuilder.toString(),
                    null
                );
            }

            // Финальный результат?
            if (STEP_FINAL.equals(parsed.getStep())) {
                log.info("✅ Получен финальный ответ после {} итерации(-ий)", iteration);

                // Конвертируем SummaryInfo zu StructuredSummaryDto
                StructuredSummaryDto structuredSummary = convertToStructuredSummaryDto(parsed.getSummary());

                return new ToolLoopResult(
                    parsed.getAnswer() != null ? parsed.getAnswer() : "",
                    rawDataBuilder.toString(),
                    structuredSummary
                );
            }

            // Обработка вызовов инструментов
            if (STEP_TOOL.equals(parsed.getStep()) && parsed.getToolCalls() != null) {
                messages.add(new Message("assistant", sonarResponse));

                StringBuilder toolResults = new StringBuilder();
                toolResults.append("Результаты вызовов инструментов:\n\n");

                for (ToolCall toolCall : parsed.getToolCalls()) {
                    String toolResult = executeMcpTool(toolCall);
                    toolResults.append(String.format("TOOL_RESULT %s:\n%s\n\n",
                        toolCall.getName(), toolResult));
                    rawDataBuilder.append(toolResult).append("\n");
                    log.info("📨 Выполнен инструмент: {}", toolCall.getName());
                }

                messages.add(new Message("user", toolResults.toString().trim()));
            } else {
                log.warn("⚠️ Неизвестный шаг, рассматривается как финальный");
                return new ToolLoopResult(
                    parsed.getAnswer() != null ? parsed.getAnswer() : sonarResponse,
                    rawDataBuilder.toString(),
                    null
                );
            }
        }

        log.error("❌ Достигнут максимум итераций");
        return new ToolLoopResult(
            "Максимум итераций достигнут",
            rawDataBuilder.toString(),
            null
        );
    }

    /**
     * Вызывает Perplexity Sonar API.
     */
    private String callSonar(List<Message> messages) {
        try {
            PerplexityResponseWithMetrics response = perplexityToolClient.requestCompletionWithMetrics(
                messages, temperature, null
            );
            return response.getReply();
        } catch (Exception e) {
            log.error("❌ Ошибка при вызове Sonar: {}", e.getMessage());
            throw new RuntimeException("Failed to call Sonar API", e);
        }
    }

    /**
     * Парсит ответ Sonar как JSON.
     */
    private SonarToolResponse parseSonarResponse(String response) {
        log.info("Response: {}", response);
        String cleaned = cleanJsonResponse(response);
        log.info("Response Cleaned: {}", cleaned);
        try {
            return objectMapper.readValue(cleaned, SonarToolResponse.class);
        } catch (JsonProcessingException e) {
            log.warn("⚠️ Ошибка парсинга JSON: {}", e.getMessage());

            // Если нет JSON, рассматривать как финальный текст
            if (cleaned != null && !cleaned.trim().startsWith("{")) {
                return SonarToolResponse.builder()
                    .step(STEP_FINAL)
                    .answer(response)
                    .toolCalls(List.of())
                    .build();
            }
            return null;
        }
    }

    /**
     * Очищает JSON от Markdown-блоков.
     */
    private String cleanJsonResponse(String response) {
        if (response == null) return null;

        String cleaned = response.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }


        return cleaned.trim();
    }

    /**
     * Выполняет MCP Tool.
     */

    private String executeMcpTool(ToolCall toolCall) {
        try {
            MCPToolResult result = mcpFactory.route(
                toolCall.getName(),
                toolCall.getArguments() != null ? toolCall.getArguments() : Map.of()
            );

            if (result.isSuccess()) {
                return objectMapper.writeValueAsString(result.getResult());
            } else {
                return "ERROR: " + result.getError();
            }
        } catch (Exception e) {
            log.error("❌ Ошибка при выполнении инструмента {}: {}", toolCall.getName(), e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }


    /**
     * Сохраняет сводку в базе данных.
     */
    private ReminderSummary saveReminderSummary(String userId, ToolLoopResult result) {
        String title = "Сводка по задачам";
        int itemsCount = 0;
        Priority priority = Priority.MEDIUM;
        String content;

        // Попытка извлечь метаданные из объекта StructuredSummaryDto
        if (result.summaryInfo != null) {
            if (result.summaryInfo.getTitle() != null) {
                title = result.summaryInfo.getTitle();
            }

            if (result.summaryInfo.getTotalItems() != null) {
                itemsCount = result.summaryInfo.getTotalItems();
            }

            if (result.summaryInfo.getPriority() != null) {
                try {
                    priority = Priority.valueOf(result.summaryInfo.getPriority());
                } catch (Exception e) {
                    // Оставить значение по умолчанию
                }
            }

            // Setze das summary field mit dem LLM answer
            if (result.answer != null && !result.answer.isEmpty()) {
                result.summaryInfo.setSummary(result.answer);
            }

            // Используем toMarkdownContent() для красивого форматирования
            content = result.summaryInfo.toMarkdownContent();
        } else {
            // Fallback: используем ответ как содержание
            content = result.answer;
        }

        ReminderSummary summary = ReminderSummary.builder()
            .userId(userId)
            .summaryType(SummaryType.TASKS)
            .title(title)
            .content(content)
            .rawData(result.rawData)
            .itemsCount(itemsCount)
            .priority(priority)
            .notified(false)
            .nextReminderAt(LocalDateTime.now().plusDays(1))
            .build();

        return reminderRepository.save(summary);
    }

    /**
     * Активирует уведомление для сводки.
     * Может быть расширено для Email, Push, Webhook и т.д.
     */
    private void triggerNotification(ReminderSummary summary) {
        // TODO: Реализуй логику уведомления
        // - Отправить Email
        // - Push-уведомление
        // - Вызвать Webhook
        // - WebSocket-событие

        log.info("📧 Уведомление активировано для сводки: {} (Приоритет: {})",
            summary.getTitle(), summary.getPriority());

        // Отметить как уведомленное
        summary.setNotified(true);
        summary.setNotifiedAt(LocalDateTime.now());
        reminderRepository.save(summary);
    }

    /**
     * Получить все сводки для пользователя.
     */
    public List<ReminderSummary> getSummariesForUser(String userId) {
        return reminderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Получить последнюю сводку.
     */
    public ReminderSummary getLatestSummary() {
        return reminderRepository.findTopByOrderByCreatedAtDesc().orElse(null);
    }

    /**
     * Получить неотправленные уведомления.
     */
    public List<ReminderSummary> getPendingNotifications() {
        return reminderRepository.findByNotifiedFalseOrderByCreatedAtAsc();
    }

    /**
     * Построить структурированный контент с ответом и SummaryInfo в виде Markdown.
     */
    private StructuredSummaryDto convertToStructuredSummaryDto(SummaryInfo summaryInfo) {
        if (summaryInfo == null) {
            return null;
        }

        List<StructuredSummaryDto.DueTaskDto> dueSoon = new ArrayList<>();
        if (summaryInfo.getDueSoon() != null) {
            for (DueTask task : summaryInfo.getDueSoon()) {
                dueSoon.add(StructuredSummaryDto.DueTaskDto.builder()
                    .task(task.getTask())
                    .due(task.getDue())
                    .build());
            }
        }

        List<StructuredSummaryDto.DueTaskDto> overdue = new ArrayList<>();
        if (summaryInfo.getOverdue() != null) {
            for (DueTask task : summaryInfo.getOverdue()) {
                overdue.add(StructuredSummaryDto.DueTaskDto.builder()
                    .task(task.getTask())
                    .due(task.getDue())
                    .build());
            }
        }

        return StructuredSummaryDto.builder()
            .title(summaryInfo.getTitle())
            .summary(null) // Will be set from LLM answer
            .totalItems(summaryInfo.getTotalItems())
            .priority(summaryInfo.getPriority())
            .highlights(summaryInfo.getHighlights())
            .dueSoon(dueSoon.isEmpty() ? null : dueSoon)
            .overdue(overdue.isEmpty() ? null : overdue)
            .build();
    }

    /**
     * Внутренний объект результата для Tool-Loop.
     */
    private record ToolLoopResult(
        String answer,
        String rawData,
        StructuredSummaryDto summaryInfo
    ) {}
}

