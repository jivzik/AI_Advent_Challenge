package de.jivz.ai_challenge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.ai_challenge.dto.ChatRequest;
import de.jivz.ai_challenge.dto.ChatResponse;
import de.jivz.ai_challenge.dto.Message;
import de.jivz.ai_challenge.dto.SonarToolDto.*;
import de.jivz.ai_challenge.service.mcp.McpToolClient;
import de.jivz.ai_challenge.service.mcp.McpDto.*;
import de.jivz.ai_challenge.service.perplexity.PerplexityToolClient;
import de.jivz.ai_challenge.service.perplexity.model.PerplexityResponseWithMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ChatWithToolsService - Реализует цикл Sonar + MCP Tools.
 *
 * Workflow:
 * 1. Собираем messages (system + user)
 * 2. Отправляем на Perplexity Sonar
 * 3. Парсим ответ как JSON (step, tool_calls, answer)
 * 4. Если step == "final" → возвращаем answer
 * 5. Если step == "tool" → вызываем MCP tools, добавляем результаты, повторяем
 *
 * Features:
 * - Автоматическое определение необходимости вызова MCP-инструментов
 * - Цикличная обработка до получения финального ответа
 * - Обработка ошибок с повторными попытками
 * - Поддержка всех Google Tasks MCP инструментов
 */
@Service
@Slf4j
public class ChatWithToolsService {

    private static final int MAX_TOOL_ITERATIONS = 10; // Защита от бесконечного цикла
    private static final String STEP_TOOL = "tool";
    private static final String STEP_FINAL = "final";

    private final PerplexityToolClient perplexityToolClient;
    private final McpToolClient mcpToolClient;
    private final ConversationHistoryService historyService;
    private final ObjectMapper objectMapper;

    /**
     * System prompt на русском языке.
     * Объясняет модели формат ответа и доступные MCP-инструменты.
     */
    private static final String SYSTEM_PROMPT_WITH_TOOLS = """
        Ты — умный ассистент, который может использовать внешние инструменты MCP для работы с Google Tasks.
        
        ## Доступные инструменты Google Tasks:
        
        1. **google_tasks_list** - Получить список всех списков задач
           - Аргументы: нет
        
        2. **google_tasks_get** - Получить задачи из списка
           - Аргументы: { "taskListId": "<id списка, опционально>" }
        
        3. **google_tasks_create** - Создать новую задачу
           - Аргументы: { "title": "<название>", "notes": "<описание, опционально>", "taskListId": "<id списка, опционально>" }
        
        4. **google_tasks_update** - Обновить задачу
           - Аргументы: { "taskId": "<id задачи>", "title": "<новое название>", "notes": "<новое описание>", "status": "needsAction|completed" }
        
        5. **google_tasks_complete** - Отметить задачу как выполненную
           - Аргументы: { "taskId": "<id задачи>", "taskListId": "<id списка, опционально>" }
        
        6. **google_tasks_delete** - Удалить задачу
           - Аргументы: { "taskId": "<id задачи>", "taskListId": "<id списка>" }
        
        ## Когда использовать инструменты:
        - Используй инструменты ТОЛЬКО когда пользователь просит выполнить реальные действия с задачами
        - Примеры: "покажи мои задачи", "создай задачу", "удали задачу", "отметь как выполненную"
        - Для обычных вопросов и разговоров НЕ используй инструменты
        
        ## ABSOLUTES FORMAT - NUR REINES JSON, KEIN MARKDOWN:
        
        Wenn du Inструменты aufrufen musst, antworte nur mit JSON (OHNE ```json ... ``` Blöcke):
        {"step":"tool","tool_calls":[{"name":"<tool_name>","arguments":{}}],"answer":""}
        
        Wenn du die finale Antwort gibst, antworte nur mit JSON (OHNE ```json ... ``` Blöcke):
        {"step":"final","tool_calls":[],"answer":"<твой ответ пользователю>"}
        
        ## KRITISCHE REGELN:
        - Antworte NUR mit reinem JSON-Objekt
        - NIEMALS Markdown-Code-Blöcke (``` oder ```json) verwenden
        - NIEMALS zusätzlicher Text vor oder nach dem JSON
        - Das JSON-Objekt muss mit { beginnen und mit } enden
        - Nach Erhalt von Tool-Ergebnissen einen verständlichen Antwort formulieren
        - Bei Tool-Fehler dem Benutzer erklären was falsch gelaufen ist
        - Mehrere Tools gleichzeitig aufrufen wenn nötig
        """;

    public ChatWithToolsService(
            PerplexityToolClient perplexityToolClient,
            McpToolClient mcpToolClient,
            ConversationHistoryService historyService,
            ObjectMapper objectMapper) {
        this.perplexityToolClient = perplexityToolClient;
        this.mcpToolClient = mcpToolClient;
        this.historyService = historyService;
        this.objectMapper = objectMapper;
    }

    /**
     * Главный метод: обрабатывает запрос с поддержкой MCP Tools.
     *
     * @param request ChatRequest от пользователя
     * @return ChatResponse с финальным ответом
     */
    public ChatResponse chatWithTools(ChatRequest request) {
        log.info("🚀 Starting chat with tools for user: {}", request.getUserId());

        String conversationId = request.getConversationId();
        String userPrompt = request.getMessage();

        // 1. Собираем messages (system + history + user)
        List<Message> messages = buildMessages(conversationId, userPrompt);

        // 2. Запускаем цикл обработки
        String finalAnswer = executeToolLoop(messages, request.getTemperature());

        // 3. Сохраняем в историю
        saveToHistory(conversationId, userPrompt, finalAnswer);

        // 4. Возвращаем ответ
        return ChatResponse.builder()
                .reply(finalAnswer)
                .toolName("ChatWithToolsService")
                .timestamp(new Date())
                .build();
    }

    /**
     * Основной цикл обработки: Sonar → MCP → Sonar → ... → final
     */
    private String executeToolLoop(List<Message> messages, Double temperature) {
        int iteration = 0;

        while (iteration < MAX_TOOL_ITERATIONS) {
            iteration++;
            log.info("🔄 Tool loop iteration: {}", iteration);

            // ====== ШАГ 1: Запрос к Sonar ======
            String sonarResponse = callSonar(messages, temperature);
            log.debug("📥 Sonar raw response: {}", sonarResponse);

            // ====== ШАГ 2: Парсинг JSON ответа ======
            SonarToolResponse parsed = parseSonarResponse(sonarResponse, messages, temperature);

            if (parsed == null) {
                log.error("❌ Failed to parse Sonar response after retries");
                return "Извините, произошла ошибка при обработке ответа. Попробуйте еще раз.";
            }

            // ====== ШАГ 3: Проверка step ======
            if (STEP_FINAL.equals(parsed.getStep())) {
                // Финальный ответ - возвращаем
                log.info("✅ Got final answer after {} iteration(s)", iteration);
                return parsed.getAnswer() != null ? parsed.getAnswer() : "";
            }

            if (STEP_TOOL.equals(parsed.getStep()) && parsed.getToolCalls() != null) {
                // ====== ШАГ 4: Вызов MCP Tools ======

                // Сначала добавляем ответ модели как assistant (её решение вызвать инструмент)
                messages.add(new Message("assistant", sonarResponse));

                // Собираем все результаты инструментов
                StringBuilder allToolResults = new StringBuilder();
                allToolResults.append("Результаты вызова инструментов:\n\n");

                for (ToolCall toolCall : parsed.getToolCalls()) {
                    String toolResult = executeMcpTool(toolCall);
                    allToolResults.append(String.format("TOOL_RESULT %s:\n%s\n\n", toolCall.getName(), toolResult));
                    log.info("📨 Executed tool: {}", toolCall.getName());
                }

                // Добавляем результаты как сообщение от user (Perplexity требует user/tool в конце)
                messages.add(new Message("user", allToolResults.toString().trim()));
                log.info("📨 Added tool results as user message");

                // Продолжаем цикл - снова запрос к Sonar
            } else {
                // Неизвестный step или пустые tool_calls
                log.warn("⚠️ Unknown step or empty tool_calls, treating as final");
                return parsed.getAnswer() != null ? parsed.getAnswer() : sonarResponse;
            }
        }

        log.error("❌ Max iterations ({}) reached in tool loop", MAX_TOOL_ITERATIONS);
        return "Извините, превышено максимальное количество итераций. Попробуйте сформулировать запрос иначе.";
    }

    /**
     * Формирует список messages для Sonar.
     */
    private List<Message> buildMessages(String conversationId, String userPrompt) {
        List<Message> messages = new ArrayList<>();

        // System prompt с описанием инструментов
        messages.add(new Message("system", SYSTEM_PROMPT_WITH_TOOLS));

        // История разговора (если есть)
        if (conversationId != null) {
            List<Message> history = historyService.getHistory(conversationId);
            if (history != null && !history.isEmpty()) {
                // Добавляем только user/assistant сообщения из истории
                for (Message msg : history) {
                    if ("user".equals(msg.getRole()) || "assistant".equals(msg.getRole())) {
                        messages.add(msg);
                    }
                }
            }
        }

        // Текущий запрос пользователя
        messages.add(new Message("user", userPrompt));

        log.info("📝 Built {} messages for Sonar", messages.size());
        return messages;
    }

    /**
     * Вызывает Perplexity Sonar API.
     */
    private String callSonar(List<Message> messages, Double temperature) {
        log.info("📤 Calling Sonar with {} messages", messages.size());

        try {
            PerplexityResponseWithMetrics response = perplexityToolClient.requestCompletionWithMetrics(
                    messages,
                    temperature != null ? temperature : 0.7,
                    null
            );

            log.info("📥 Sonar response received (tokens: in={}, out={})",
                    response.getInputTokens(), response.getOutputTokens());

            return response.getReply();

        } catch (Exception e) {
            log.error("❌ Error calling Sonar: {}", e.getMessage());
            throw new RuntimeException("Failed to call Sonar API", e);
        }
    }

    /**
     * Парсит ответ Sonar как JSON.
     * При ошибке делает повторный запрос с инструкцией.
     * Если модель вернула обычный текст (не JSON) - это считается финальным ответом.
     */
    private SonarToolResponse parseSonarResponse(String response, List<Message> messages, Double temperature) {
        // Очищаем ответ от возможных markdown-блоков
        String cleanedResponse = cleanJsonResponse(response);

        try {
            return objectMapper.readValue(cleanedResponse, SonarToolResponse.class);
        } catch (JsonProcessingException e) {
            log.warn("⚠️ Failed to parse JSON: {}", e.getMessage());

            // Проверяем - если ответ не начинается с "{", это скорее всего финальный текстовый ответ
            // Модель "забыла" формат и просто ответила пользователю
            if (cleanedResponse != null && !cleanedResponse.trim().startsWith("{")) {
                log.info("📝 Response is not JSON, treating as final text answer");
                return SonarToolResponse.builder()
                        .step(STEP_FINAL)
                        .answer(response) // Используем оригинальный ответ
                        .toolCalls(List.of())
                        .build();
            }

            // Если это всё-таки попытка JSON но с ошибкой - делаем retry
            log.warn("⚠️ Attempting retry for malformed JSON");

            // Добавляем текущий (неудачный) ответ как assistant чтобы соблюдать чередование
            messages.add(new Message("assistant", response));

            // Добавляем сообщение об ошибке как user
            messages.add(new Message("user",
                "Твой предыдущий ответ был невалидным JSON. Верни ТОЛЬКО валидный JSON в указанном формате без markdown-блоков и текста вокруг."));

            try {
                String retryResponse = callSonar(messages, temperature);
                String cleanedRetry = cleanJsonResponse(retryResponse);

                // Если retry тоже не JSON - принимаем как финальный ответ
                if (cleanedRetry != null && !cleanedRetry.trim().startsWith("{")) {
                    log.info("📝 Retry response is not JSON, treating as final text answer");
                    return SonarToolResponse.builder()
                            .step(STEP_FINAL)
                            .answer(retryResponse)
                            .toolCalls(List.of())
                            .build();
                }

                return objectMapper.readValue(cleanedRetry, SonarToolResponse.class);
            } catch (Exception retryException) {
                log.error("❌ Retry also failed: {}", retryException.getMessage());
                return null;
            }
        }
    }

    /**
     * Очищает ответ от markdown-блоков.
     */
    private String cleanJsonResponse(String response) {
        if (response == null) return null;

        String cleaned = response.trim();

        // Удаляем ```json ... ```
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
     * Вызывает MCP Tool через /mcp/call.
     */
    private String executeMcpTool(ToolCall toolCall) {
        log.info("🔧 Executing MCP tool: {} with args: {}", toolCall.getName(), toolCall.getArguments());

        try {
            ToolExecutionResponse result = mcpToolClient.executeTool(
                    toolCall.getName(),
                    toolCall.getArguments() != null ? toolCall.getArguments() : Map.of()
            );

            if (result.isSuccess()) {
                log.info("✅ MCP tool {} executed successfully", toolCall.getName());
                return objectMapper.writeValueAsString(result.getResult());
            } else {
                log.warn("⚠️ MCP tool {} returned error: {}", toolCall.getName(), result.getError());
                return String.format("ERROR: %s", result.getError());
            }

        } catch (Exception e) {
            log.error("❌ Error executing MCP tool {}: {}", toolCall.getName(), e.getMessage());
            return String.format("ERROR: %s", e.getMessage());
        }
    }

    /**
     * Сохраняет в историю разговора.
     */
    private void saveToHistory(String conversationId, String userMessage, String assistantReply) {
        if (conversationId == null) return;

        List<Message> history = historyService.getHistory(conversationId);
        if (history == null) {
            history = new ArrayList<>();
        }

        history.add(new Message("user", userMessage));
        history.add(new Message("assistant", assistantReply));

        historyService.saveHistory(conversationId, history);
        log.info("💾 Saved conversation to history: {} messages", history.size());
    }

    /**
     * Возвращает список доступных MCP Tools.
     */
    public List<McpTool> getAvailableTools() {
        try {
            return mcpToolClient.getAllTools();
        } catch (Exception e) {
            log.error("❌ Error getting available tools: {}", e.getMessage());
            return List.of();
        }
    }
}

