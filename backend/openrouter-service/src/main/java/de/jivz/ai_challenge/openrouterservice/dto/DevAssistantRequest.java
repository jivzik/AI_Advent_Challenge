package de.jivz.ai_challenge.openrouterservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for the Developer Assistant Endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevAssistantRequest {

    /**
     * The developer's question
     */
    @NotBlank(message = "Query must not be empty")
    private String query;

    /**
     * User ID for tracking and personalization
     */
//    @NotNull(message = "User ID is required")
    private String userId;

    /**
     * Should Git context be included
     */
    @Builder.Default
    private Boolean includeGitContext = true;

    /**
     * Maximum number of documents to search
     */
    @Builder.Default
    private Integer maxDocuments = 5;

    /**
     * Optional: Specific AI model
     */
    private String model;

    /**
     * Optional: Temperature for AI responses (0.0 - 1.0)
     */
    private Double temperature;
}

