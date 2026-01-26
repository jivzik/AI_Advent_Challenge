package de.jivz.ai_challenge.openrouterservice.personalization.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for personalized chat requests.
 * Allows customization of personalization features and context.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalizedChatRequest {

    /**
     * The user's message/query
     */
    @NotBlank(message = "Message is required")
    @Size(min = 1, max = 5000, message = "Message must be between 1 and 5000 characters")
    private String message;

    /**
     * Whether to use the user's profile for personalization
     * Default: true
     */
    @Builder.Default
    private Boolean useProfile = true;

    /**
     * Whether to use conversation history
     * Default: true
     */
    @Builder.Default
    private Boolean useHistory = true;

    /**
     * Whether to use agent memory (learned preferences)
     * Default: true
     */
    @Builder.Default
    private Boolean useMemory = true;

    /**
     * Maximum number of previous interactions to include as context
     * Default: 5
     */
    @Builder.Default
    private Integer contextLimit = 5;
}
