package de.jivz.ai_challenge.openrouterservice.personalization.config;

import de.jivz.ai_challenge.openrouterservice.personalization.profile.dto.UserProfileRequestDTO;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.repository.UserProfileRepository;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Initializes default user profile on application startup if no profiles exist
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ProfileInitializer implements ApplicationRunner {

    private final UserProfileService profileService;
    private final UserProfileRepository profileRepository;
    private final PersonalizationProperties properties;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("ProfileInitializer: Starting profile initialization check");

        // Check if any profiles exist in DB
        long profileCount = profileRepository.count();

        if (profileCount == 0) {
            log.info("No profiles found, creating default profile");
            createDefaultProfile();
        } else {
            log.info("Found {} existing profiles", profileCount);
        }

        // Check for profile-config.yaml in project root
        checkAndImportProfileConfig();

        log.info("ProfileInitializer: Completed profile initialization check");
    }

    /**
     * Create default profile from properties
     */
    private void createDefaultProfile() {
        try {
            String userId = "default-user";

            // Get default profile data from properties
            PersonalizationProperties.DefaultProfile defaultProfile = properties.getDefaultProfile();

            // Build tech stack map
            Map<String, Object> techStack = Map.of(
                    "backend", defaultProfile.getTechStack().getBackend(),
                    "frontend", defaultProfile.getTechStack().getFrontend(),
                    "ai", defaultProfile.getTechStack().getAi(),
                    "devops", defaultProfile.getTechStack().getDevops()
            );

            // Build coding style map
            Map<String, Object> codingStyle = Map.of(
                    "naming", defaultProfile.getCodingStyle().getNaming(),
                    "annotations", defaultProfile.getCodingStyle().getAnnotations(),
                    "errorHandling", defaultProfile.getCodingStyle().getErrorHandling(),
                    "responseType", defaultProfile.getCodingStyle().getResponseType(),
                    "validation", defaultProfile.getCodingStyle().getValidation(),
                    "documentation", defaultProfile.getCodingStyle().getDocumentation()
            );

            // Build communication preferences map
            Map<String, Object> communicationPreferences = Map.of(
                    "tone", defaultProfile.getCommunicationPreferences().getTone(),
                    "emojiUsage", defaultProfile.getCommunicationPreferences().getEmojiUsage(),
                    "responseFormat", defaultProfile.getCommunicationPreferences().getResponseFormat(),
                    "detailLevel", defaultProfile.getCommunicationPreferences().getDetailLevel(),
                    "language", defaultProfile.getCommunicationPreferences().getLanguage()
            );

            // Convert workStyle List to Map
            List<String> workStyleList = defaultProfile.getWorkStyle();
            Map<String, Object> workStyleMap = Map.of(
                    "items", workStyleList
            );

            // Create profile request DTO
            UserProfileRequestDTO request = UserProfileRequestDTO.builder()
                    .userId(userId)
                    .name(defaultProfile.getName())
                    .expertiseLevel(defaultProfile.getExpertiseLevel())
                    .preferredLanguage(defaultProfile.getPreferredLanguage())
                    .timezone(defaultProfile.getTimezone())
                    .techStack(techStack)
                    .codingStyle(codingStyle)
                    .communicationPreferences(communicationPreferences)
                    .workStyle(workStyleMap)
                    .build();

            // Create profile through service
            profileService.createProfile(request);

            log.info("Default profile created for user: {}", userId);

        } catch (Exception e) {
            log.error("Failed to create default profile: {}", e.getMessage(), e);
        }
    }

    /**
     * Check for profile-config.yaml and import if exists
     */
    private void checkAndImportProfileConfig() {
        try {
            // Check in project root
            Path configPath = Paths.get("profile-config.yaml");

            if (Files.exists(configPath)) {
                log.info("Found profile-config.yaml, attempting to import");

                String yamlContent = Files.readString(configPath);

                // Import profile from YAML
                profileService.importProfileFromYaml("imported-user", yamlContent);

                log.info("Imported profile from profile-config.yaml");
            } else {
                log.debug("No profile-config.yaml found in project root");
            }

        } catch (Exception e) {
            log.error("Failed to import profile from profile-config.yaml: {}", e.getMessage(), e);
        }
    }
}
