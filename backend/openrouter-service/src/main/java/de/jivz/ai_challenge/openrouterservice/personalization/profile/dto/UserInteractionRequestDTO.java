package de.jivz.ai_challenge.openrouterservice.personalization.profile.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for recording a user interaction
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInteractionRequestDTO {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Query is required")
    private String query;

    private String queryType;
    private String response;

    @Min(value = -1, message = "Feedback must be -1, 0, or 1")
    @Max(value = 1, message = "Feedback must be -1, 0, or 1")
    private Integer feedback;

    private Map<String, Object> context;
    private Integer processingTimeMs;
    private Integer tokensUsed;
    private String model;
}
