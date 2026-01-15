package de.jivz.supportservice.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for customer support tickets
 */
@Entity
@Table(
        name = "support_tickets",
        indexes = {
                @Index(name = "idx_support_tickets_user_id", columnList = "user_id"),
                @Index(name = "idx_support_tickets_status", columnList = "status"),
                @Index(name = "idx_support_tickets_priority", columnList = "priority"),
                @Index(name = "idx_support_tickets_category", columnList = "category"),
                @Index(name = "idx_support_tickets_created_at", columnList = "created_at"),
                @Index(name = "idx_support_tickets_assigned_to", columnList = "assigned_to"),
                @Index(name = "idx_support_tickets_order_id", columnList = "order_id")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ticket_number", unique = true, nullable = false, length = 20)
    private String ticketNumber;

    // User reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private SupportUser user;

    // Ticket details
    @Column(nullable = false, length = 500)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String category; // auth, catalog, order, payment, delivery, billing, api, other

    @Column(length = 20)
    @Builder.Default
    private String priority = "medium"; // low, medium, high, critical

    @Column(length = 50)
    @Builder.Default
    private String status = "open"; // open, in_progress, waiting_customer, resolved, closed

    // Assignment
    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    // Context (для RAG)
    @Column(name = "order_id", length = 50)
    private String orderId;

    @Column(name = "product_id", length = 50)
    private String productId;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    // Resolution
    @Column(columnDefinition = "TEXT")
    private String resolution;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    // Satisfaction
    @Column(name = "satisfaction_rating")
    private Integer satisfactionRating; // 1-5

    @Column(name = "satisfaction_comment", columnDefinition = "TEXT")
    private String satisfactionComment;

    // SLA tracking
    @Column(name = "first_response_at")
    private LocalDateTime firstResponseAt;

    @Column(name = "first_response_time_minutes")
    private Integer firstResponseTimeMinutes;

    @Column(name = "resolution_time_minutes")
    private Integer resolutionTimeMinutes;

    @Column(name = "sla_breached")
    @Builder.Default
    private Boolean slaBreached = false;

    // Timestamps
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Enums
    public enum Category {
        AUTH("auth"),
        CATALOG("catalog"),
        ORDER("order"),
        PAYMENT("payment"),
        DELIVERY("delivery"),
        BILLING("billing"),
        API("api"),
        RETURN("return"),
        OTHER("other");

        private final String value;

        Category(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum Status {
        OPEN("open"),
        IN_PROGRESS("in_progress"),
        WAITING_CUSTOMER("waiting_customer"),
        RESOLVED("resolved"),
        CLOSED("closed");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum Priority {
        LOW("low"),
        MEDIUM("medium"),
        HIGH("high"),
        CRITICAL("critical");

        private final String value;

        Priority(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}