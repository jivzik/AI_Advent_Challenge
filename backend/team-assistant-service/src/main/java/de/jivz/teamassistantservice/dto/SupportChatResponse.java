package de.jivz.teamassistantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for support chat
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportChatResponse {

    private String ticketNumber; // Номер тикета
    private String status; // Статус тикета
    private String answer; // Ответ AI ассистента
    private Boolean isAiGenerated; // Был ли ответ сгенерирован AI
    private BigDecimal confidenceScore; // Уверенность AI (0.00-1.00)
    private List<String> sources; // Источники из RAG
    private Boolean needsHumanAgent; // Нужна ли эскалация к человеку
    private String escalationReason; // Причина эскалации
    private LocalDateTime timestamp;

    // Metadata
    private Integer messageCount; // Количество сообщений в тикете
    private LocalDateTime firstResponseAt; // Время первого ответа
    private Boolean slaBreached; // Нарушен ли SLA
}