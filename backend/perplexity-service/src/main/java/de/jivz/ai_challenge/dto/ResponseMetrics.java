package de.jivz.ai_challenge.dto;

import lombok.*;

/**
 * Response metrics containing token usage and cost information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ResponseMetrics {
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private Double cost;
    private Long responseTimeMs;
    private String model;
    private String provider;
}

