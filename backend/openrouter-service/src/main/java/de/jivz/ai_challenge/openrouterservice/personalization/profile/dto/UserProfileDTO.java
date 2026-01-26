package de.jivz.ai_challenge.openrouterservice.personalization.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for UserProfile responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDTO {
    private Long id;
    private String userId;
    private String name;
    private String expertiseLevel;
    private String preferredLanguage;
    private String timezone;
    private Map<String, Object> techStack;
    private Map<String, Object> codingStyle;
    private Map<String, Object> communicationPreferences;
    private Map<String, Object> workStyle;
    private Map<String, Object> recentProjects;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
