package de.jivz.supportservice.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for ticket conversation messages
 */
@Entity
@Table(
        name = "ticket_messages",
        indexes = {
                @Index(name = "idx_ticket_messages_ticket_id", columnList = "ticket_id"),
                @Index(name = "idx_ticket_messages_created_at", columnList = "created_at")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    // Message details
    @Column(name = "sender_type", nullable = false, length = 20)
    private String senderType; // customer, agent, system, ai

    @Column(name = "sender_id", nullable = false, length = 100)
    private String senderId;

    @Column(name = "sender_name", nullable = false)
    private String senderName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    // AI context
    @Column(name = "is_ai_generated")
    @Builder.Default
    private Boolean isAiGenerated = false;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "rag_sources", columnDefinition = "text[]")
    private String[] ragSources;

    @Column(name = "confidence_score", precision = 3, scale = 2)
    private BigDecimal confidenceScore; // 0.00-1.00

    // Attachments (JSONB)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String attachments;

    // Metadata
    @Column(name = "is_internal")
    @Builder.Default
    private Boolean isInternal = false;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // Enums
    public enum SenderType {
        CUSTOMER("customer"),
        AGENT("agent"),
        SYSTEM("system"),
        AI("ai");

        private final String value;

        SenderType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}