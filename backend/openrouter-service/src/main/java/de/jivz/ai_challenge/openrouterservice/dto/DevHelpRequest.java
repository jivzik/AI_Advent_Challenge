package de.jivz.ai_challenge.openrouterservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Request DTO для Developer Assistant endpoint.
 *
 * Содержит вопрос разработчика и опциональные параметры
 * для управления поведением ассистента.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevHelpRequest {

    /**
     * Вопрос разработчика.
     * Примеры:
     * - "How to create a new MCP Provider?"
     * - "Wie funktioniert die RAG Integration?"
     * - "Покажи пример использования GitToolProvider"
     * - "Where is the ChatController.java file?"
     * - "Explain the Multi-Provider architecture"
     */
    @NotBlank(message = "Query is required")
    private String query;

    /**
     * ID пользователя для отслеживания запросов.
     * Используется для логирования и аналитики.
     */
    @NotBlank(message = "User ID is required")
    private String userId;

    /**
     * ID конверсации для follow-up вопросов.
     * Опционально. Если указан, будет использоваться история предыдущих вопросов.
     */
    private String conversationId;

    /**
     * Включать ли Git контекст в ответ.
     * По умолчанию: true
     *
     * Git контекст включает:
     * - Текущую ветку
     * - Измененные файлы
     * - Последние коммиты
     */
    @Builder.Default
    private Boolean includeGitContext = true;

    /**
     * Максимальное количество документов из RAG для поиска.
     * По умолчанию: 5
     *
     * LLM сама решает сколько документов нужно, но этот параметр
     * ограничивает максимум.
     */
    @Builder.Default
    private Integer maxDocuments = 5;

    /**
     * Читать ли содержимое файлов автоматически.
     * По умолчанию: false
     *
     * Если true, LLM может автоматически читать упомянутые файлы
     * через git:read_project_file tool.
     */
    @Builder.Default
    private Boolean autoReadFiles = false;
}