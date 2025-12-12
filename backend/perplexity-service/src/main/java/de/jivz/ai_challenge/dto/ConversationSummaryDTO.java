package de.jivz.ai_challenge.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO для краткой информации о диалоге.
 * Используется в списке всех диалогов.
 *
 * Поля:
 * - conversationId: идентификатор диалога
 * - firstMessage: первые 50 символов сообщения (для заголовка)
 * - lastMessageTime: время последнего сообщения (для сортировки)
 * - messageCount: количество сообщений в диалоге
 * - hasCompression: есть ли сжатие (summary) в диалоге
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationSummaryDTO {

    /**
     * Идентификатор диалога
     */
    private String conversationId;

    /**
     * Первые 50 символов первого сообщения для заголовка
     */
    private String firstMessage;

    /**
     * Время последнего сообщения (для сортировки)
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastMessageTime;

    /**
     * Количество всех сообщений в диалоге
     */
    private long messageCount;

    /**
     * Есть ли в диалоге сжатые сообщения (summary)
     */
    private boolean hasCompression;

    /**
     * Пользователь (опционально, для фильтрации)
     */
    private String userId;

    /**
     * Количество сжатых сообщений
     */
    private long compressedMessageCount;
}

