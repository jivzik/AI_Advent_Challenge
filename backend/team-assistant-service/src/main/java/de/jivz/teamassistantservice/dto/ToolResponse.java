package de.jivz.teamassistantservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * DTO für die strukturierte Antwort des LLM bei Tool-Verwendung.
 *
 * Das LLM antwortet im Format:
 * - step="tool" mit tool_calls wenn Tools aufgerufen werden sollen
 * - step="final" mit answer wenn eine finale Antwort gegeben wird
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ToolResponse {

    /**
     * Aktueller Schritt: "tool" oder "final"
     */
    private String step;

    /**
     * Liste der Tool-Aufrufe (nur bei step="tool")
     */
    @JsonProperty("tool_calls")
    private List<ToolCall> toolCalls;

    /**
     * Antwort für den Benutzer (bei step="final" die finale Antwort)
     */
    private String answer;

    /**
     * Optionale Zusammenfassung (bei komplexen Aufgaben)
     */
    private Summary summary;

    private List<String> toolsUsed;

    private List<String> sources;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCall {
        private String name;
        private Map<String, Object> arguments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private String title;

        @JsonProperty("total_items")
        private Integer totalItems;

        private String priority;
        private List<String> highlights;

        @JsonProperty("due_soon")
        private List<TaskItem> dueSoon;

        private List<TaskItem> overdue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskItem {
        private String task;
        private String due;
    }
}

