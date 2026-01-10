package de.jivz.ai_challenge.service.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Kontextabhängige Prompt-Strategie für OpenRouter.
 *
 * Erkennt automatisch den Anfrage-Typ und wählt den passenden
 * spezialisierten System-Prompt aus:
 * - Docker → DevOps-Experte für Container-Analyse
 * - Tasks → Task-Manager mit strukturiertem JSON-Output
 * - Default → Allgemeiner Assistent
 */
@Component
@Slf4j
public class ContextualPromptStrategy {

    // Keyword-Patterns für verschiedene Kontexte
    private static final List<Pattern> DOCKER_PATTERNS = List.of(
        Pattern.compile("(?i)docker"),
        Pattern.compile("(?i)container"),
        Pattern.compile("(?i)контейнер"),
        Pattern.compile("(?i)compose"),
        Pattern.compile("(?i)image"),
        Pattern.compile("(?i)volume"),
        Pattern.compile("(?i)kubernetes"),
        Pattern.compile("(?i)k8s"),
        Pattern.compile("(?i)pod")
    );

    private static final List<Pattern> TASK_PATTERNS = List.of(
        Pattern.compile("(?i)task"),
        Pattern.compile("(?i)задач"),
        Pattern.compile("(?i)aufgabe"),
        Pattern.compile("(?i)todo"),
        Pattern.compile("(?i)reminder"),
        Pattern.compile("(?i)напоминани"),
        Pattern.compile("(?i)erinnerung"),
        Pattern.compile("(?i)deadline"),
        Pattern.compile("(?i)срок"),
        Pattern.compile("(?i)frist")
    );

    private static final List<Pattern> SUMMARIZE_PATTERNS = List.of(
        Pattern.compile("(?i)summarize"),
        Pattern.compile("(?i)summary"),
        Pattern.compile("(?i)сводк"),
        Pattern.compile("(?i)zusammenfass"),
        Pattern.compile("(?i)übersicht"),
        Pattern.compile("(?i)обзор")
    );

    /**
     * Ermittelt den Kontext-Typ basierend auf der Benutzeranfrage.
     */
    public PromptContext detectContext(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return PromptContext.DEFAULT;
        }

        // Docker-Kontext prüfen
        for (Pattern p : DOCKER_PATTERNS) {
            if (p.matcher(userMessage).find()) {
                log.debug("Detected DOCKER context for message: {}", userMessage);
                return PromptContext.DOCKER;
            }
        }

        // Task-Kontext prüfen
        for (Pattern p : TASK_PATTERNS) {
            if (p.matcher(userMessage).find()) {
                log.debug("Detected TASKS context for message: {}", userMessage);
                return PromptContext.TASKS;
            }
        }

        // Summarize-Kontext prüfen
        for (Pattern p : SUMMARIZE_PATTERNS) {
            if (p.matcher(userMessage).find()) {
                log.debug("Detected SUMMARIZE context for message: {}", userMessage);
                return PromptContext.SUMMARIZE;
            }
        }

        return PromptContext.DEFAULT;
    }

    /**
     * Gibt den passenden System-Prompt für den erkannten Kontext zurück.
     */
    public String getSystemPromptForContext(PromptContext context) {
        return switch (context) {
            case DOCKER -> buildDockerPrompt();
            case TASKS -> buildTasksPrompt();
            case SUMMARIZE -> buildSummarizePrompt();
            default -> buildDefaultPrompt();
        };
    }

    /**
     * Convenience-Methode: Erkennt Kontext und gibt passenden Prompt zurück.
     */
    public String getSystemPromptForMessage(String userMessage) {
        PromptContext context = detectContext(userMessage);
        return getSystemPromptForContext(context);
    }

    /**
     * Docker/DevOps-Experte Prompt
     */
    private String buildDockerPrompt() {
        return """
            Ты - эксперт DevOps инженер. Проанализируй состояние Docker инфраструктуры и дай краткий отчет.
            
            ДАННЫЕ: {данные будут предоставлены из MCP tools}
            
            ЗАДАЧИ:
            1. 🚦 Общий статус (1-2 предложения)
               - Количество контейнеров и их состояние
               - Критичные проблемы (если есть)
            
            2. ⚠️ Проблемы и риски
               - Контейнеры в статусе Exited/Dead/Restarting
               - Аномалии в логах (errors, warnings, crashes)
               - Высокое использование ресурсов (если видно)
            
            3. 💡 Рекомендации (топ 3)
               - Что нужно исправить срочно
               - Что проверить дополнительно
               - Оптимизация или улучшения
            
            4. 📊 Краткая статистика
               - Running: X
               - Stopped: X
               - Проблемных: X
            
            ФОРМАТ ОТВЕТА:
            - Максимум 150 слов
            - Bullet points
            - Эмодзи для наглядности
            - Без воды, только факты
            
            Если все хорошо - скажи "✅ Все контейнеры работают стабильно" и дай 1-2 совета для профилактики.
            
            ФИНАЛЬНЫЙ ВЫВОД (чистый JSON, БЕЗ ```json блоков):
            {"step":"final","tool_calls":[],"answer":"<твой отчет>","docker_status":{"running":0,"stopped":0,"problematic":0,"critical_issues":[],"recommendations":[]}}
            """;
    }

    /**
     * Task-Manager Prompt mit strukturiertem JSON-Output
     */
    private String buildTasksPrompt() {
        return """
            Ты умный помощник для управления задачами.
            
            ## Твоя задача:
            1. Получи данные о задачах через доступные MCP Tools
            2. Проанализируй сроки, приоритеты и статусы
            3. Создай структурированную сводку
            
            ## Рабочий процесс:
            1. Вызови инструменты для получения списков задач
            2. Определи просроченные и срочные задачи
            3. Сгруппируй по приоритету
            
            ## ФОРМАТ ВЫЗОВА ИНСТРУМЕНТОВ (чистый JSON, БЕЗ ```json блоков):
            {"step":"tool","tool_calls":[{"name":"<tool_name>","arguments":{}}],"answer":""}
            
            ## ФИНАЛЬНАЯ СВОДКА (чистый JSON, БЕЗ ```json блоков):
            {"step":"final","tool_calls":[],"answer":"<твоя структурированная сводка>","summary":{"title":"<Заголовок сводки>","total_items":0,"priority":"HIGH|MEDIUM|LOW","highlights":["<Важный пункт 1>","<Важный пункт 2>"],"due_soon":[{"task":"<Задача>","due":"<Дата>"}],"overdue":[{"task":"<Задача>","due":"<Дата>"}]}}
            
            ## КРИТИЧЕСКИЕ ПРАВИЛА:
            - Отвечай ТОЛЬКО чистым JSON-объектом
            - НИКОГДА не используй блоки Markdown (``` или ```json)
            - Приоритизируй просроченные задачи как HIGH
            - Будь лаконичен, но информативен
            """;
    }

    /**
     * Summarize/Zusammenfassung Prompt
     */
    private String buildSummarizePrompt() {
        return """
            Ты эксперт по анализу и суммаризации данных.
            
            ## Твоя задача:
            1. Собери все необходимые данные через MCP Tools
            2. Выдели ключевые моменты
            3. Создай краткую, но информативную сводку
            
            ## Принципы:
            - Фокус на главном (правило 80/20)
            - Структурированность
            - Actionable insights
            
            ## ФОРМАТ ОТВЕТА (чистый JSON):
            {"step":"final","tool_calls":[],"answer":"<сводка>","summary":{"key_points":["<пункт1>","<пункт2>"],"action_items":["<действие1>"],"stats":{}}}
            """;
    }

    /**
     * Default/Allgemeiner Assistent Prompt
     */
    private String buildDefaultPrompt() {
        return """
            Du bist ein intelligenter Assistent mit Zugriff auf MCP Tools.
            
            ## Deine Aufgabe:
            1. Verstehe die Anfrage des Benutzers
            2. Nutze bei Bedarf die verfügbaren Tools
            3. Gib eine hilfreiche, strukturierte Antwort
            
            ## Workflow:
            1. Analysiere die Anfrage
            2. Rufe bei Bedarf Tools auf
            3. Fasse die Ergebnisse zusammen
            
            ## TOOL-AUFRUF FORMAT (reines JSON, OHNE ```json Blöcke):
            {"step":"tool","tool_calls":[{"name":"<tool_name>","arguments":{}}],"answer":""}
            
            ## FINALE ANTWORT FORMAT (reines JSON, OHNE ```json Blöcke):
            {"step":"final","tool_calls":[],"answer":"<deine Antwort>"}
            
            ## Regeln:
            - Antworte NUR mit reinem JSON
            - Keine Markdown-Codeblöcke
            - Sei präzise und hilfreich
            """;
    }

    /**
     * Enum für die verschiedenen Prompt-Kontexte
     */
    public enum PromptContext {
        DOCKER,
        TASKS,
        SUMMARIZE,
        DEFAULT
    }
}

