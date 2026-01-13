package de.jivz.ai_challenge.openrouterservice.controller;

import de.jivz.ai_challenge.openrouterservice.dto.ChatRequest;
import de.jivz.ai_challenge.openrouterservice.dto.ChatResponse;
import de.jivz.ai_challenge.openrouterservice.dto.DevHelpRequest;
import de.jivz.ai_challenge.openrouterservice.service.ChatWithToolsService;
import de.jivz.ai_challenge.openrouterservice.service.PromptLoaderService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Developer Assistant Controller
 *
 * Endpoint для помощи разработчикам с использованием RAG (документация)
 * и Git MCP tools (контекст проекта).
 *
 * Features:
 * - Автоматический поиск в документации проекта через RAG
 * - Получение Git контекста (ветка, измененные файлы, коммиты)
 * - Генерация примеров кода в стиле проекта
 * - Ссылки на релевантные файлы и документацию
 *
 * Example usage:
 * POST /api/dev/help
 * {
 *   "query": "How to create a new MCP Provider?",
 *   "userId": "dev-123"
 * }
 */
@RestController
@RequestMapping("/api/dev")
@Slf4j
public class DevAssistantController {

    private final ChatWithToolsService chatWithToolsService;
    private final PromptLoaderService promptLoader;

    public DevAssistantController(
            ChatWithToolsService chatWithToolsService,
            PromptLoaderService promptLoader) {
        this.chatWithToolsService = chatWithToolsService;
        this.promptLoader = promptLoader;
        log.info("✅ DevAssistantController initialized");
    }

    /**
     * Главный endpoint для Developer Assistant.
     *
     * LLM автоматически вызывает нужные MCP tools:
     * - rag:search_documents - поиск в документации проекта
     * - git:get_current_branch - текущая Git ветка
     * - git:get_git_status - измененные файлы
     * - git:get_git_log - последние коммиты
     * - git:read_project_file - чтение конкретных файлов
     *
     * @param request запрос с вопросом разработчика
     * @return ответ с кодом, документацией и Git контекстом
     */
    @PostMapping("/help")
    public ChatResponse help(@Valid @RequestBody DevHelpRequest request) {
        log.info("🧑‍💻 Developer assistant request from user: {}", request.getUserId());
        log.info("📝 Query: {}", request.getQuery().substring(0, Math.min(100, request.getQuery().length())));

        // Загрузить developer-specific промпты
        String developerPrompt = promptLoader.loadPrompt("context-developer");
        String codeStylePrompt = promptLoader.loadPrompt("developer-code-style");

        // Объединить промпты для системного сообщения
        String systemPrompt = developerPrompt + "\n\n" + codeStylePrompt;

        // Создать ChatRequest с developer system prompt
        ChatRequest chatRequest = ChatRequest.builder()
                .message(request.getQuery())
                .conversationId(request.getConversationId())
                .temperature(0.3)  // Низкая температура для точных технических ответов
                .build();

        log.info("🚀 Routing to ChatWithToolsService with developer context");

        // ChatWithToolsService автоматически:
        // 1. Загрузит доступные MCP tools (RAG + Git)
        // 2. LLM сама решит какие tools вызвать
        // 3. Выполнит tool-calling loop
        // 4. Вернет финальный ответ с источниками
        ChatResponse response = chatWithToolsService.chatWithTools(chatRequest);

        log.info("✅ Developer assistant response generated");
        return response;
    }

    /**
     * Endpoint для быстрого вопроса без conversationId.
     * Для разовых запросов без контекста истории.
     */
    @GetMapping("/quick-help")
    public ChatResponse quickHelp(@RequestParam String query) {
        log.info("⚡ Quick help request: {}", query.substring(0, Math.min(50, query.length())));

        DevHelpRequest request = DevHelpRequest.builder()
                .query(query)
                .userId("anonymous")
                .build();

        return help(request);
    }

    /**
     * Health check endpoint для developer assistant.
     */
    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "Developer Assistant");
        status.put("status", "operational");
        status.put("features", List.of(
                "RAG documentation search",
                "Git context integration",
                "Code style enforcement",
                "Multi-language support"
        ));

        // Проверить доступность промптов
        boolean developerPromptLoaded = promptLoader.loadPrompt("context-developer") != null;
        boolean codeStylePromptLoaded = promptLoader.loadPrompt("developer-code-style") != null;

        status.put("prompts_loaded", developerPromptLoaded && codeStylePromptLoaded);

        return status;
    }
}