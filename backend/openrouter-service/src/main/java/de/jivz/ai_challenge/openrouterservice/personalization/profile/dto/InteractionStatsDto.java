package de.jivz.ai_challenge.openrouterservice.personalization.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for interaction statistics.
 * Provides aggregated statistics about user interactions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteractionStatsDto {

    /**
     * Total number of interactions for the user
     */
    private Long totalInteractions;

    /**
     * Map of query types to their occurrence count
     * Key: query type, Value: count
     */
    private Map<String, Long> topQueryTypes;

    /**
     * Average feedback score (excluding 0)
     */
    private Double averageFeedback;

    /**
     * Percentage of positive feedback (feedback > 0)
     */
    private Double positivePercentage;

    /**
     * Average processing time in milliseconds
     */
    private Integer averageProcessingTime;

    /**
     * Total tokens used across all interactions
     */
    private Long totalTokensUsed;

    /**
     * Most frequently used model
     */
    private String mostUsedModel;
}
