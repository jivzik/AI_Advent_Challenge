package de.jivz.ai_challenge.openrouterservice.personalization.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO for personalized chat responses.
 * Contains the LLM response along with metadata about personalization usage.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalizedChatResponse {

    /**
     * The LLM's response to the user's message
     */
    private String response;

    /**
     * Whether the user's profile was used in generating the response
     */
    private Boolean usedProfile;

    /**
     * List of memory keys that were used in the response generation
     */
    private List<String> usedMemory;

    /**
     * Context from the user's profile that was considered
     */
    private Map<String, Object> profileContext;

    /**
     * Processing time in milliseconds
     */
    private Integer processingTimeMs;

    /**
     * Number of tokens used in the request and response
     */
    private Integer tokensUsed;

    /**
     * The LLM model used for generating the response
     */
    private String model;

    /**
     * ID of the saved interaction record
     */
    private Long interactionId;
}
