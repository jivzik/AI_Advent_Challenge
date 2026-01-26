package de.jivz.ai_challenge.openrouterservice.personalization.profile.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating or updating AgentMemory
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentMemoryRequestDTO {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Memory type is required")
    @Pattern(regexp = "preference|pattern|context|learned",
             message = "Memory type must be 'preference', 'pattern', 'context', or 'learned'")
    private String memoryType;

    @NotBlank(message = "Key is required")
    private String key;

    @NotBlank(message = "Value is required")
    private String value;

    @Min(value = 0, message = "Confidence must be between 0.0 and 1.0")
    @Max(value = 1, message = "Confidence must be between 0.0 and 1.0")
    private Double confidence;
}
