package de.jivz.ai_challenge.openrouterservice.personalization.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for creating or updating a UserProfile
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileRequestDTO {

    @NotBlank(message = "User ID is required")
    private String userId;

    private String name;

    @Pattern(regexp = "junior|middle|senior", message = "Expertise level must be 'junior', 'middle', or 'senior'")
    private String expertiseLevel;

    @Pattern(regexp = "ru|de|en", message = "Preferred language must be 'ru', 'de', or 'en'")
    private String preferredLanguage;

    private String timezone;
    private Map<String, Object> techStack;
    private Map<String, Object> codingStyle;
    private Map<String, Object> communicationPreferences;
    private Map<String, Object> workStyle;
    private Map<String, Object> recentProjects;
}
