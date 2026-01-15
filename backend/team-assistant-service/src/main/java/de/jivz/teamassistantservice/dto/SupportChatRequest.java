package de.jivz.teamassistantservice.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for support chat
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportChatRequest {

    private String userEmail; // Email пользователя (для идентификации)
    private String ticketNumber; // Номер тикета (если продолжение диалога)
    private String message; // Сообщение пользователя
    private String category; // Категория (auth, catalog, order, etc.)
    private String priority; // Приоритет (low, medium, high, critical)

    // Optional context
    private String orderId; // Номер заказа (если связан)
    private String productId; // Артикул товара (если связан)
    private String errorCode; // Код ошибки (если технический вопрос)
}