package de.jivz.ai_challenge.openrouterservice.personalization.profile.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity representing a user profile with personalization settings.
 * Stores user preferences, expertise level, tech stack, and communication preferences.
 */
@Entity
@Table(
    name = "user_profiles",
    indexes = {
        @Index(name = "idx_user_profile_user_id", columnList = "user_id", unique = true)
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique identifier for the user
     */
    @Column(name = "user_id", nullable = false, unique = true, length = 100)
    private String userId;

    /**
     * User's display name
     */
    @Column(name = "name", length = 255)
    private String name;

    /**
     * User's expertise level: "junior", "middle", "senior"
     */
    @Column(name = "expertise_level", length = 50)
    @Builder.Default
    private String expertiseLevel = "middle";

    /**
     * Preferred language: "ru", "de", "en"
     */
    @Column(name = "preferred_language", length = 10)
    @Builder.Default
    private String preferredLanguage = "ru";

    /**
     * User's timezone (e.g., "Europe/Berlin")
     */
    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "Europe/Berlin";

    /**
     * User's tech stack as JSONB
     * Example: {"backend": ["Java 21", "Spring Boot 3.x"], "frontend": ["Vue 3", "TypeScript"]}
     */
    @Type(JsonBinaryType.class)
    @Column(name = "tech_stack", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> techStack = Map.of(
        "backend", java.util.List.of("Java 21", "Spring Boot 3.x", "PostgreSQL", "Lombok"),
        "frontend", java.util.List.of("Vue 3", "TypeScript", "Vite"),
        "ai", java.util.List.of("OpenRouter", "Claude 3.5", "RAG", "pgvector"),
        "devops", java.util.List.of("Docker", "GitHub Actions")
    );

    /**
     * User's coding style preferences as JSONB
     * Example: {"naming": "camelCase", "annotations": ["@Slf4j", "@RequiredArgsConstructor"]}
     */
    @Type(JsonBinaryType.class)
    @Column(name = "coding_style", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> codingStyle = Map.of(
        "naming", "camelCase",
        "annotations", java.util.List.of("@Slf4j", "@RequiredArgsConstructor", "@Data"),
        "errorHandling", "@ControllerAdvice",
        "responseType", "ResponseEntity",
        "validation", "javax.validation"
    );

    /**
     * User's communication preferences as JSONB
     * Example: {"tone": "friendly-professional", "emojiUsage": "moderate"}
     */
    @Type(JsonBinaryType.class)
    @Column(name = "communication_preferences", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> communicationPreferences = Map.of(
        "tone", "friendly-professional",
        "emojiUsage", "moderate",
        "responseFormat", "structured",
        "detailLevel", "detailed-with-examples",
        "language", "ru"
    );

    /**
     * User's work style as JSONB
     * Example: {"workHours": "flexible", "preferredMeetingTime": "morning"}
     */
    @Type(JsonBinaryType.class)
    @Column(name = "work_style", columnDefinition = "jsonb")
    private Map<String, Object> workStyle;

    /**
     * User's recent projects as JSONB
     * Example: [{"name": "AI Advent Challenge", "tech": ["Spring Boot", "Vue 3"]}]
     */
    @Type(JsonBinaryType.class)
    @Column(name = "recent_projects", columnDefinition = "jsonb")
    private Map<String, Object> recentProjects;

    /**
     * Timestamp when the profile was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the profile was last updated
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
