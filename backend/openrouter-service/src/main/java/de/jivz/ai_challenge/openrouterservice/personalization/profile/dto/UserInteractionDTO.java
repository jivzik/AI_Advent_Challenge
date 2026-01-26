package de.jivz.ai_challenge.openrouterservice.personalization.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for UserInteraction responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInteractionDTO {
    private Long id;
    private String userId;
    private String query;
    private String queryType;
    private String response;
    private Integer feedback;
    private Map<String, Object> context;
    private Integer processingTimeMs;
    private Integer tokensUsed;
    private String model;
    private LocalDateTime createdAt;
}
