package de.jivz.ai_challenge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Einheitliches DTO für strukturierte Reminder-Zusammenfassungen.
 *
 * Wird von beiden Services (Sonar/Perplexity und OpenRouter) verwendet,
 * um konsistente, schön formatierte Zusammenfassungen im Frontend anzuzeigen.
 *
 * Format:
 * {
 *   "title": "Aufgaben-Zusammenfassung",
 *   "summary": "Sie haben insgesamt 5 Aufgaben...",
 *   "total_items": 5,
 *   "priority": "MEDIUM",
 *   "highlights": ["Highlight 1", "Highlight 2"],
 *   "due_soon": [{"task": "Task name", "due": "morgen 15:00"}],
 *   "overdue": [{"task": "Task name", "due": "2 Tage überfällig"}]
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StructuredSummaryDto {

    /**
     * Titel der Zusammenfassung
     * Beispiel: "Aufgaben-Zusammenfassung", "Wöchentliche Übersicht", etc.
     */
    private String title;

    /**
     * Zusammenfassung als Text (Markdown oder einfacher Text)
     * Enthält die Hauptinformation über Aufgaben und Status
     */
    private String summary;

    /**
     * Gesamtanzahl der Aufgaben/Items
     * Beispiel: 5
     */
    @JsonProperty("total_items")
    private Integer totalItems;

    /**
     * Prioritätsstufe: HIGH, MEDIUM, LOW
     * Bestimmt die Dringlichkeit der Zusammenfassung
     */
    private String priority;

    /**
     * Wichtige Punkte/Highlights der Zusammenfassung
     * Beispiele:
     * - "4 offene Aufgaben benötigen Ihre Aufmerksamkeit"
     * - "Ein Arzttermin beim Therapeuten ist für den 28.12.2025 geplant"
     */
    private List<String> highlights;

    /**
     * Aufgaben, die bald fällig sind (nächste Tage/Woche)
     * Geordnet nach Fälligkeitsdatum
     */
    @JsonProperty("due_soon")
    private List<DueTaskDto> dueSoon;

    /**
     * Überfällige Aufgaben (hätte bereits erledigt sein sollen)
     * Priorisiert auf die ältesten zuerst
     */
    private List<DueTaskDto> overdue;

    /**
     * Eine einzelne Aufgabe mit Fälligkeitsinformation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DueTaskDto {
        /**
         * Name/Beschreibung der Aufgabe
         * Beispiel: "Футбол завтра в 15:00 за школой"
         */
        private String task;

        /**
         * Fälligkeitsinformation (relativ oder absolut)
         * Beispiele:
         * - "morgen 15:00"
         * - "28.12.2025 14:00"
         * - "2 Tage überfällig"
         * - "heute 20:00"
         */
        private String due;
    }

    /**
     * Generiert einen vollständigen Markdown-Content aus den strukturierten Daten.
     * Wird im `ReminderSummary.content`-Feld gespeichert.
     *
     * @return Formatierter Markdown-String
     */
    public String toMarkdownContent() {
        StringBuilder sb = new StringBuilder();

        // Titel
        sb.append("# ").append(title).append("\n\n");

        // Zusammenfassung
        if (summary != null && !summary.isEmpty()) {
            sb.append(summary).append("\n\n");
        }

        // Meta-Informationen
        sb.append("---\n\n");
        sb.append("**Metadaten:**\n");
        sb.append("- **Gesamtaufgaben:** ").append(totalItems != null ? totalItems : 0).append("\n");
        sb.append("- **Priorität:** ").append(priority != null ? priority : "MEDIUM").append("\n\n");

        // Highlights
        if (highlights != null && !highlights.isEmpty()) {
            sb.append("## 🌟 Highlights\n");
            for (String highlight : highlights) {
                sb.append("- ").append(highlight).append("\n");
            }
            sb.append("\n");
        }

        // Bald fällig
        if (dueSoon != null && !dueSoon.isEmpty()) {
            sb.append("## ⏱️ Bald fällig\n");
            for (DueTaskDto task : dueSoon) {
                sb.append("- **").append(task.getTask()).append("**\n");
                sb.append("  - Fällig: ").append(task.getDue()).append("\n");
            }
            sb.append("\n");
        }

        // Überfällig
        if (overdue != null && !overdue.isEmpty()) {
            sb.append("## ⚠️ Überfällig\n");
            for (DueTaskDto task : overdue) {
                sb.append("- **").append(task.getTask()).append("**\n");
                sb.append("  - ").append(task.getDue()).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Generiert einen kompakten JSON-Block für Datenausgabe.
     * Kann am Anfang des Contents platziert werden (in JSON-Code-Block).
     *
     * @return JSON-String
     */
    public String toJsonBlock() {
        return "```json\n" +
            "{\n" +
            "  \"title\": \"" + escapeJson(title) + "\",\n" +
            "  \"total_items\": " + (totalItems != null ? totalItems : 0) + ",\n" +
            "  \"priority\": \"" + (priority != null ? priority : "MEDIUM") + "\",\n" +
            "  \"summary\": \"" + escapeJson(summary) + "\"\n" +
            "}\n" +
            "```\n";
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }
}

