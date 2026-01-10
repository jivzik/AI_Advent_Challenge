package de.jivz.ai_challenge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Einheitliches JSON-Format für Reminder-Zusammenfassungen
 * Beide Models (Sonar + OpenRouter) werden angewiesen, genau DIESES Format auszuspucken!
 *
 * Dieses DTO wird DIREKT vom LLM als JSON erzeugt, nicht umgewandelt.
 * Kein Parsing, Normalisierung oder Konvertierung nötig!
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderSummaryJsonDto {

    @JsonProperty("title")
    private String title;                    // "Aufgaben-Zusammenfassung"

    @JsonProperty("summary")
    private String summary;                  // Zusammenfassungstext

    @JsonProperty("total_items")
    private Integer totalItems;              // 5

    @JsonProperty("priority")
    private String priority;                 // HIGH|MEDIUM|LOW

    @JsonProperty("highlights")
    private List<String> highlights;         // ["Highlight 1", "Highlight 2"]

    @JsonProperty("due_soon")
    private List<TaskDto> dueSoon;          // Bald fällige Aufgaben

    @JsonProperty("overdue")
    private List<TaskDto> overdue;          // Überfällige Aufgaben

    @JsonProperty("active_tasks")
    private List<TaskDto> activeTasks;      // Aktive Aufgaben

    /**
     * Task-Item im Reminder
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskDto {
        @JsonProperty("name")
        private String name;                 // "Футбол завтра в 15:00"

        @JsonProperty("due_date")
        private String dueDate;              // "2025-12-18 15:00"

        @JsonProperty("description")
        private String description;          // Optional

        @JsonProperty("category")
        private String category;             // "SPORTS", "WORK", etc.

        @JsonProperty("urgency")
        private String urgency;              // HIGH|MEDIUM|LOW
    }

    /**
     * Konvertiert zu schönem Markdown für Speicherung/Anzeige
     */
    public String toMarkdownString() {
        StringBuilder sb = new StringBuilder();

        sb.append("# ").append(title).append("\n\n");

        if (summary != null && !summary.isEmpty()) {
            sb.append(summary).append("\n\n");
        }

        // Meta
        sb.append("**Items:** ").append(totalItems);
        sb.append(" | **Priorität:** ").append(priority).append("\n\n");

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
            for (TaskDto task : activeTasks) {
                sb.append("- **").append(task.name).append("**");
                if (task.category != null) {
                    sb.append(" (").append(task.category).append(")");
                }
                sb.append("\n");
                if (task.dueDate != null) {
                    sb.append("  - Fällig: ").append(task.dueDate).append("\n");
                }
                if (task.description != null) {
                    sb.append("  - ").append(task.description).append("\n");
                }
            }
            sb.append("\n");
        }

        // Due Soon
        if (dueSoon != null && !dueSoon.isEmpty()) {
            sb.append("## ⏱️ Bald fällig\n");
            for (TaskDto task : dueSoon) {
                sb.append("- **").append(task.name).append("** ");
                sb.append("[").append(task.urgency).append("]\n");
                if (task.dueDate != null) {
                    sb.append("  - Fällig: ").append(task.dueDate).append("\n");
                }
            }
            sb.append("\n");
        }

        // Overdue
        if (overdue != null && !overdue.isEmpty()) {
            sb.append("## ⚠️ Überfällig\n");
            for (TaskDto task : overdue) {
                sb.append("- **").append(task.name).append("** ").append("⚠️\n");
                if (task.dueDate != null) {
                    sb.append("  - War fällig: ").append(task.dueDate).append("\n");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}

