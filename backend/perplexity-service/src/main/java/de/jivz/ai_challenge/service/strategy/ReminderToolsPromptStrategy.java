package de.jivz.ai_challenge.service.strategy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.ai_challenge.service.mcp.McpDto.McpTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Dynamischer MCP Tools Prompt Builder.
 *
 * Erstellt System-Prompts für den Reminder-Scheduler mit:
 * - Dynamisch geladenen MCP Tools (name, description, inputSchema)
 * - Strukturiertem JSON-Output Format
 * - Spezifischen Anweisungen für Reminder/Summary-Generierung
 *
 * Der Prompt wird vor jedem LLM-Aufruf aktualisiert mit den aktuellsten Tools.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ReminderToolsPromptStrategy {

    private final ObjectMapper objectMapper;

    /**
     * Создает системный промпт с динамически внедренными MCP Tools.
     *
     * @param tools Список доступных MCP Tools
     * @return Полный системный промпт
     */
    public String buildDynamicSystemPrompt(List<McpTool> tools) {
        StringBuilder toolsDescription = new StringBuilder();

        if (tools != null && !tools.isEmpty()) {
            toolsDescription.append("## Доступные MCP Tools:\n\n");

            for (int i = 0; i < tools.size(); i++) {
                McpTool tool = tools.get(i);
                toolsDescription.append(String.format("%d. **%s**\n", i + 1, tool.getName()));
                toolsDescription.append(String.format("   - Описание: %s\n",
                    tool.getDescription() != null ? tool.getDescription() : "Описание недоступно"));

                if (tool.getInputSchema() != null && !tool.getInputSchema().isEmpty()) {
                    try {
                        String schemaJson = objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(tool.getInputSchema());
                        toolsDescription.append(String.format("   - Схема: ```json\n%s\n```\n", schemaJson));
                    } catch (JsonProcessingException e) {
                        toolsDescription.append(String.format("   - Схема: %s\n", tool.getInputSchema()));
                    }
                }
                toolsDescription.append("\n");
            }
        } else {
            toolsDescription.append("## MCP Tools не доступны\n\n");
            toolsDescription.append("В настоящее время нет зарегистрированных внешних инструментов.\n\n");
        }

        return buildPromptTemplate(toolsDescription.toString());
    }

    /**
     * Erstellt das Prompt-Template mit eingebetteten Tools.
     */
    private String buildPromptTemplate(String toolsSection) {
        return """
            Ты умный помощник для напоминаний, который создает периодические сводки.
            
            ## Твоя задача:
            1. Используй доступные инструменты MCP для получения актуальных данных
            2. Проанализируй данные и создай полезную сводку
            3. Определи важные задачи, сроки и приоритеты
            4. Предоставь структурированные рекомендации
           
            %s
            
            ## Рабочий процесс:
            1. **Получение данных**: Используй предоставленные инструменты для получения всех списков задач и их содержимого
            2. **Анализ**: Определи открытые, просроченные и важные задачи
            3. **Резюме**: Создай понятную сводку
            
            ## АБСОЛЮТНЫЙ ФОРМАТ ВЫВОДА - ТОЛЬКО ЧИСТЫЙ JSON, БЕЗ MARKDOWN:
            
            Когда тебе нужно вызвать инструменты, ответь чистым JSON (БЕЗ блоков ```json ... ```):
            {"step":"tool","tool_calls":[{"name":"<tool_name>","arguments":{}}],"answer":""}
            
            Когда ты даешь финальную сводку, ответь чистым JSON (БЕЗ блоков ```json ... ```):
            {"step":"final","tool_calls":[],"answer":"<твоя структурированная сводка>","summary":{"title":"<Заголовок сводки>","total_items":<Количество>,"priority":"HIGH|MEDIUM|LOW","highlights":["<Важный пункт 1>","<Важный пункт 2>"],"due_soon":[{"task":"<Задача>","due":"<Дата>"}],"overdue":[{"task":"<Задача>","due":"<Дата>"}]}}
            
            ## КРИТИЧЕСКИЕ ПРАВИЛА - ОБРАТИ НА НИХ ВНИМАНИЕ:
            - Отвечай ТОЛЬКО чистым JSON-объектом
            - НИКОГДА не используй блоки Markdown-кода (``` или ```json)
            - НИКОГДА не добавляй дополнительный текст перед или после JSON
            - JSON должен быть на ОДНОЙ СТРОКЕ (без разрывов строк/отступов)
            - JSON-объект должен начинаться с { и заканчиваться }
            - Если нужен описательный текст, помести его в поле "answer" как строковое значение
            - Приоритизируй просроченные задачи как HIGH
            - Группируй похожие задачи
            - Будь лаконичен, но информативен
            - Если нет задач, все равно предоставь структурированный результат
            """.formatted(toolsSection);
    }

    /**
     * Erstellt einen einfachen Prompt für die Summary-Generierung aus Rohdaten.
     * Wird verwendet wenn die Daten bereits vorhanden sind und nur noch
     * zusammengefasst werden müssen.
     *
     * @param rawData Die Rohdaten die zusammengefasst werden sollen
     * @return Der Summary-Prompt
     */
    public String buildSummaryOnlyPrompt(String rawData) {
        return """
            Ты помощник, который анализирует и резюмирует данные о задачах.
            
            ## Твоя задача:
            Проанализируй следующие данные и создай полезную сводку.
            
            ## Исходные данные:
            %s
            
            ## ФОРМАТ ВЫВОДА (только JSON):
            {
              "step": "final",
              "tool_calls": [],
              "answer": "<твое читаемое резюме для пользователя>",
              "summary": {
                "title": "<Заголовок>",
                "total_items": <Количество>,
                "priority": "HIGH|MEDIUM|LOW",
                "highlights": ["<Пункт 1>", "<Пункт 2>"],
                "due_soon": [{"task": "<Задача>", "due": "<Дата>"}],
                "overdue": [{"task": "<Задача>", "due": "<Дата>"}]
              }
            }
            
            Отвечай ТОЛЬКО валидным JSON.
            """.formatted(rawData);
    }

    /**
     * Erstellt einen Mini-Prompt für schnelle Task-Abfragen.
     */
    public String buildQuickTasksPrompt() {
        return """
            Ты помощник. Вызови google_tasks_get и верни задачи.
            
            ВЫВОД (только JSON):
            {
              "step": "tool",
              "tool_calls": [{"name": "google_tasks_get", "arguments": {}}],
              "answer": ""
            }
            """;
    }
}

