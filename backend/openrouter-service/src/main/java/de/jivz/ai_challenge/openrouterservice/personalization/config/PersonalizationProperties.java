package de.jivz.ai_challenge.openrouterservice.personalization.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Configuration properties for personalization features
 */
@Component
@ConfigurationProperties(prefix = "personalization")
@Data
public class PersonalizationProperties {

    private boolean enabled = true;
    private DefaultProfile defaultProfile = new DefaultProfile();
    private Learning learning = new Learning();
    private Prompt prompt = new Prompt();

    @Data
    public static class DefaultProfile {
        private String name = "User";
        private String expertiseLevel = "senior";
        private String preferredLanguage = "ru";
        private String timezone = "Europe/Berlin";
        private TechStack techStack = new TechStack();
        private CodingStyle codingStyle = new CodingStyle();
        private CommunicationPreferences communicationPreferences = new CommunicationPreferences();
        private List<String> workStyle = List.of();
    }

    @Data
    public static class TechStack {
        private List<String> backend = List.of("Java 21", "Spring Boot 3.x", "PostgreSQL");
        private List<String> frontend = List.of("Vue 3", "TypeScript", "Vite");
        private List<String> ai = List.of("OpenRouter", "Claude 3.5 Sonnet");
        private List<String> devops = List.of("Docker", "GitHub Actions");
    }

    @Data
    public static class CodingStyle {
        private String naming = "camelCase";
        private List<String> annotations = List.of("@Slf4j", "@RequiredArgsConstructor", "@Data", "@Builder");
        private String errorHandling = "@ControllerAdvice";
        private String responseType = "ResponseEntity";
        private String validation = "javax.validation";
        private String documentation = "OpenAPI 3.0";
    }

    @Data
    public static class CommunicationPreferences {
        private String tone = "friendly-professional";
        private String emojiUsage = "moderate";
        private String responseFormat = "structured";
        private String detailLevel = "detailed-with-examples";
        private String language = "ru";
    }

    @Data
    public static class Learning {
        private boolean enabled = true;
        private int patternDetectionThreshold = 5;
        private double confidenceUpdateRate = 0.1;
        private int maxMemoryEntries = 1000;
        private String cleanupSchedule = "0 0 2 * * SUN";
        private double minConfidenceThreshold = 0.3;
    }

    @Data
    public static class Prompt {
        private int maxContextInteractions = 5;
        private int maxMemoryEntries = 5;
        private boolean includeProfile = true;
        private boolean includeMemory = true;
        private boolean includeContext = true;
    }

    /**
     * Get default profile as a Map for easy conversion
     */
    public Map<String, Object> getDefaultProfileAsMap() {
        return Map.of(
                "name", defaultProfile.getName(),
                "expertiseLevel", defaultProfile.getExpertiseLevel(),
                "preferredLanguage", defaultProfile.getPreferredLanguage(),
                "timezone", defaultProfile.getTimezone(),
                "techStack", Map.of(
                        "backend", defaultProfile.getTechStack().getBackend(),
                        "frontend", defaultProfile.getTechStack().getFrontend(),
                        "ai", defaultProfile.getTechStack().getAi(),
                        "devops", defaultProfile.getTechStack().getDevops()
                ),
                "codingStyle", Map.of(
                        "naming", defaultProfile.getCodingStyle().getNaming(),
                        "annotations", defaultProfile.getCodingStyle().getAnnotations(),
                        "errorHandling", defaultProfile.getCodingStyle().getErrorHandling(),
                        "responseType", defaultProfile.getCodingStyle().getResponseType(),
                        "validation", defaultProfile.getCodingStyle().getValidation(),
                        "documentation", defaultProfile.getCodingStyle().getDocumentation()
                ),
                "communicationPreferences", Map.of(
                        "tone", defaultProfile.getCommunicationPreferences().getTone(),
                        "emojiUsage", defaultProfile.getCommunicationPreferences().getEmojiUsage(),
                        "responseFormat", defaultProfile.getCommunicationPreferences().getResponseFormat(),
                        "detailLevel", defaultProfile.getCommunicationPreferences().getDetailLevel(),
                        "language", defaultProfile.getCommunicationPreferences().getLanguage()
                ),
                "workStyle", defaultProfile.getWorkStyle()
        );
    }
}
