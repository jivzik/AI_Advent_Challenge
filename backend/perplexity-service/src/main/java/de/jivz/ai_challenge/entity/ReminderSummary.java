package de.jivz.ai_challenge.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity für die Speicherung von Reminder-Zusammenfassungen in PostgreSQL.
 *
 * Wird vom ReminderSchedulerService verwendet um:
 * - Regelmäßige Summaries aus MCP Tools zu speichern
 * - Benutzer über wichtige Aufgaben zu benachrichtigen
 * - Historische Zusammenfassungen zu verfolgen
 */
@Entity
@Table(name = "reminder_summaries", indexes = {
    @Index(name = "idx_reminder_user_id", columnList = "user_id"),
    @Index(name = "idx_reminder_created_at", columnList = "created_at"),
    @Index(name = "idx_reminder_type", columnList = "summary_type"),
    @Index(name = "idx_reminder_notified", columnList = "notified")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User identifier - wem gehört diese Zusammenfassung.
     * Kann null sein für system-weite Summaries.
     */
    @Column(name = "user_id", length = 255)
    private String userId;

    /**
     * Typ der Zusammenfassung (TASKS, CALENDAR, GENERAL, etc.)
     */
    @Column(name = "summary_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private SummaryType summaryType;

    /**
     * Titel der Zusammenfassung
     */
    @Column(name = "title", nullable = false, length = 500)
    private String title;

    /**
     * Der generierte Summary-Text von der KI
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Rohe Daten von MCP Tools (JSON)
     */
    @Column(name = "raw_data", columnDefinition = "TEXT")
    private String rawData;

    /**
     * Wann wurde die Zusammenfassung erstellt
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Wann wurde der Benutzer benachrichtigt
     */
    @Column(name = "notified_at")
    private LocalDateTime notifiedAt;

    /**
     * Wurde der Benutzer bereits benachrichtigt?
     */
    @Column(name = "notified", nullable = false)
    @Builder.Default
    private Boolean notified = false;

    /**
     * Anzahl der Aufgaben/Items in dieser Zusammenfassung
     */
    @Column(name = "items_count")
    private Integer itemsCount;

    /**
     * Priorität: HIGH, MEDIUM, LOW
     */
    @Column(name = "priority", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    /**
     * Nächstes geplantes Reminder-Datum
     */
    @Column(name = "next_reminder_at")
    private LocalDateTime nextReminderAt;

    /**
     * Typen von Zusammenfassungen
     */
    public enum SummaryType {
        TASKS,          // Google Tasks Zusammenfassung
        CALENDAR,       // Kalender-Events
        GENERAL,        // Allgemeine Zusammenfassung
        DAILY_DIGEST,   // Tägliche Übersicht
        WEEKLY_DIGEST   // Wöchentliche Übersicht
    }

    /**
     * Prioritätsstufen
     */
    public enum Priority {
        HIGH,
        MEDIUM,
        LOW
    }

    /**
     * Pre-Persist Hook: setzt createdAt
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

