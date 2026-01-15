package de.jivz.teamassistantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response from Team Assistant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamAssistantResponse {

    private String answer;
    private String queryType; // SHOW_TASKS, CREATE_TASK, ANALYZE_PRIORITY, etc
    private List<String> toolsUsed;
    private List<String> sources; // RAG sources
    private List<String> actions; // Actions performed (task_created, etc)
    private BigDecimal confidenceScore;
    private Integer responseTimeMs;
    private LocalDateTime timestamp;
}
