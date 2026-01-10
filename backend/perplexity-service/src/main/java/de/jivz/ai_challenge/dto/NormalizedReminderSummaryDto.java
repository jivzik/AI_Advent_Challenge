package de.jivz.ai_challenge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Normalisierte DTO für Reminder-Zusammenfassungen
 * Unabhängig vom Provider (Sonar/OpenRouter)
 *
 * Alle Provider-Responses werden zu diesem Format normalisiert
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NormalizedReminderSummaryDto {

    // Metadaten
    private String title;                          // "Aufgaben-Zusammenfassung"
    private String summaryText;                    // Hauptzusammenfassung
    private Integer totalItems;                    // 5
    private String priority;                       // HIGH|MEDIUM|LOW
    private String source;                         // SONAR|OPENROUTER|COMBINED

    // Strukturierte Inhalte
    private List<String> highlights;               // ["Highlight 1", "Highlight 2"]
    private List<TaskItemDto> dueSoon;            // Bald fällige Aufgaben
    private List<TaskItemDto> overdue;            // Überfällige Aufgaben
    private List<TaskItemDto> activeTasks;        // Aktive Aufgaben

    /**
     * Konvertiert zu Markdown für Speicherung in DB
     */
    public String toMarkdownFormat() {
        StringBuilder sb = new StringBuilder();

        // Titel
        sb.append("# ").append(title).append("\n\n");

        // Zusammenfassung
        if (summaryText != null && !summaryText.isEmpty()) {
            sb.append(summaryText).append("\n\n");
        }

        // Meta-Info
        sb.append("**Quelle:** ").append(source).append(" | ");
        sb.append("**Gesamt Items:** ").append(totalItems).append(" | ");
        sb.append("**Priorität:** ").append(priority).append("\n\n");

        // Highlights
        if (highlights != null && !highlights.isEmpty()) {
            sb.append("## 🌟 Highlights\n");
            for (String h : highlights) {
                sb.append("- ").append(h).append("\n");
            }
            sb.append("\n");
        }

        // Active Tasks
        if (activeTasks != null && !activeTasks.isEmpty()) {
            sb.append("## 📋 Aktive Aufgaben\n");
            for (TaskItemDto task : activeTasks) {
                sb.append("- **").append(task.getName()).append("**\n");
                if (task.getDueDate() != null) {
                    sb.append("  - Fällig: ").append(task.getDueDate()).append("\n");
                }
                if (task.getDescription() != null) {
                    sb.append("  - Beschreibung: ").append(task.getDescription()).append("\n");
                }
            }
            sb.append("\n");
        }

        // Due Soon
        if (dueSoon != null && !dueSoon.isEmpty()) {
            sb.append("## ⏱️ Bald fällig\n");
            for (TaskItemDto task : dueSoon) {
                sb.append("- **").append(task.getName()).append("**\n");
                sb.append("  - Fällig: ").append(task.getDueDate() != null ? task.getDueDate() : "Keine Info").append("\n");
                if (task.getUrgency() != null) {
                    sb.append("  - Dringlichkeit: ").append(task.getUrgency()).append("\n");
                }
            }
            sb.append("\n");
        }

        // Overdue
        if (overdue != null && !overdue.isEmpty()) {
            sb.append("## ⚠️ Überfällig\n");
            for (TaskItemDto task : overdue) {
                sb.append("- **").append(task.getName()).append("**\n");
                sb.append("  - Fällig: ").append(task.getDueDate() != null ? task.getDueDate() : "Keine Info").append("\n");
                if (task.getDescription() != null) {
                    sb.append("  - Beschreibung: ").append(task.getDescription()).append("\n");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Task-Item aus einer Zusammenfassung
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskItemDto {
        private String name;                       // "Футбол завтра в 15:00"
        private String dueDate;                    // "morgen 15:00"
        private String description;                // Optionale Beschreibung
        private String category;                   // z.B. "SPORTS", "HEALTH", "WORK"
        private boolean isOverdue;                 // Ist überfällig?
        private String urgency;                    // HIGH|MEDIUM|LOW
    }
}

