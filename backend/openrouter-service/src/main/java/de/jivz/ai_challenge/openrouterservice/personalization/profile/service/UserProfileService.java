package de.jivz.ai_challenge.openrouterservice.personalization.profile.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.dto.UserProfileDTO;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.dto.UserProfileRequestDTO;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.entity.UserProfile;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing user profiles and personalization settings
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final ObjectMapper objectMapper;
    private final Yaml yaml = new Yaml();

    /**
     * Load existing profile or create a default one if not found
     * @param userId The user ID
     * @return The user profile (existing or newly created)
     */
    @Transactional
    public UserProfile loadOrCreateProfile(String userId) {
        Optional<UserProfile> existingProfile = userProfileRepository.findByUserId(userId);

        if (existingProfile.isPresent()) {
            return existingProfile.get();
        }

        // Create default profile
        log.info("Created default profile for user: {}", userId);

        Map<String, Object> defaultTechStack = new HashMap<>();
        defaultTechStack.put("backend", Arrays.asList("Java", "Spring Boot", "PostgreSQL"));
        defaultTechStack.put("frontend", Arrays.asList("Vue.js", "TypeScript"));
        defaultTechStack.put("tools", Arrays.asList("Docker", "Git", "Maven"));

        Map<String, Object> defaultCodingStyle = new HashMap<>();
        defaultCodingStyle.put("naming", "camelCase");
        defaultCodingStyle.put("annotations", Arrays.asList("@Slf4j", "@Data", "@Builder", "@RequiredArgsConstructor"));
        defaultCodingStyle.put("errorHandling", "ControllerAdvice");
        defaultCodingStyle.put("testing", "JUnit5 + Mockito");

        Map<String, Object> defaultCommunicationPrefs = new HashMap<>();
        defaultCommunicationPrefs.put("responseStyle", "detailed");
        defaultCommunicationPrefs.put("codeComments", "inline");
        defaultCommunicationPrefs.put("explanationLevel", "intermediate");

        UserProfile defaultProfile = UserProfile.builder()
                .userId(userId)
                .name("User")
                .expertiseLevel("senior")
                .preferredLanguage("ru")
                .timezone("Europe/Berlin")
                .techStack(defaultTechStack)
                .codingStyle(defaultCodingStyle)
                .communicationPreferences(defaultCommunicationPrefs)
                .workStyle(new HashMap<>())
                .recentProjects(new HashMap<>())
                .build();

        return userProfileRepository.save(defaultProfile);
    }

    /**
     * Get a user profile entity by user ID
     * @param userId The user ID
     * @return Optional containing the profile entity if found
     */
    @Transactional(readOnly = true)
    public Optional<UserProfile> getProfile(String userId) {
        return userProfileRepository.findByUserId(userId);
    }

    /**
     * Get a user profile by user ID
     * @param userId The user ID
     * @return Optional containing the profile DTO if found
     */
    @Transactional(readOnly = true)
    public Optional<UserProfileDTO> getProfileByUserId(String userId) {
        log.debug("Getting profile for user: {}", userId);
        return userProfileRepository.findByUserId(userId)
                .map(this::toDTO);
    }

    /**
     * Create a new user profile
     * @param requestDTO The profile creation request
     * @return The created profile DTO
     */
    @Transactional
    public UserProfileDTO createProfile(UserProfileRequestDTO requestDTO) {
        log.info("Creating profile for user: {}", requestDTO.getUserId());

        if (userProfileRepository.existsByUserId(requestDTO.getUserId())) {
            throw new IllegalArgumentException("Profile already exists for user: " + requestDTO.getUserId());
        }

        UserProfile profile = UserProfile.builder()
                .userId(requestDTO.getUserId())
                .name(requestDTO.getName())
                .expertiseLevel(requestDTO.getExpertiseLevel() != null ? requestDTO.getExpertiseLevel() : "middle")
                .preferredLanguage(requestDTO.getPreferredLanguage() != null ? requestDTO.getPreferredLanguage() : "ru")
                .timezone(requestDTO.getTimezone() != null ? requestDTO.getTimezone() : "Europe/Berlin")
                .techStack(requestDTO.getTechStack())
                .codingStyle(requestDTO.getCodingStyle())
                .communicationPreferences(requestDTO.getCommunicationPreferences())
                .workStyle(requestDTO.getWorkStyle())
                .recentProjects(requestDTO.getRecentProjects())
                .build();

        UserProfile savedProfile = userProfileRepository.save(profile);
        log.info("Profile created successfully for user: {}", savedProfile.getUserId());

        return toDTO(savedProfile);
    }

    /**
     * Update an existing user profile
     * @param userId The user ID
     * @param request The profile update request
     * @return The updated profile entity
     */
    @Transactional
    public UserProfile updateProfile(String userId, UserProfileRequestDTO request) {
        log.info("Updated profile for user: {}", userId);

        // Load or create profile
        UserProfile profile = loadOrCreateProfile(userId);

        // Update only non-null fields from request
        if (request.getName() != null) {
            profile.setName(request.getName());
        }
        if (request.getExpertiseLevel() != null) {
            profile.setExpertiseLevel(request.getExpertiseLevel());
        }
        if (request.getPreferredLanguage() != null) {
            profile.setPreferredLanguage(request.getPreferredLanguage());
        }
        if (request.getTimezone() != null) {
            profile.setTimezone(request.getTimezone());
        }
        if (request.getTechStack() != null) {
            profile.setTechStack(request.getTechStack());
        }
        if (request.getCodingStyle() != null) {
            profile.setCodingStyle(request.getCodingStyle());
        }
        if (request.getCommunicationPreferences() != null) {
            profile.setCommunicationPreferences(request.getCommunicationPreferences());
        }
        if (request.getWorkStyle() != null) {
            profile.setWorkStyle(request.getWorkStyle());
        }
        if (request.getRecentProjects() != null) {
            profile.setRecentProjects(request.getRecentProjects());
        }

        return userProfileRepository.save(profile);
    }

    /**
     * Get profile context as a Map for use in prompts
     * @param userId The user ID
     * @return Map with profile context
     */
    @Transactional
    public Map<String, Object> getProfileContext(String userId) {
        UserProfile profile = loadOrCreateProfile(userId);

        Map<String, Object> context = new HashMap<>();
        context.put("name", profile.getName());
        context.put("expertiseLevel", profile.getExpertiseLevel());
        context.put("techStack", profile.getTechStack());
        context.put("codingStyle", profile.getCodingStyle());
        context.put("communicationPreferences", profile.getCommunicationPreferences());
        context.put("preferredLanguage", profile.getPreferredLanguage());

        return context;
    }

    /**
     * Convert UserProfile entity to DTO
     * @param profile The profile entity
     * @return The profile DTO
     */
    public UserProfileDTO convertToDto(UserProfile profile) {
        return toDTO(profile);
    }

    /**
     * Import profile from YAML content
     * @param userId The user ID
     * @param yamlContent The YAML content as string
     * @return The created/updated profile
     */
    @Transactional
    public UserProfile importProfileFromYaml(String userId, String yamlContent) {
        log.info("Importing profile from YAML for user: {}", userId);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> yamlData = yaml.load(yamlContent);

            UserProfile profile = loadOrCreateProfile(userId);

            // Update profile from YAML data
            if (yamlData.containsKey("name")) {
                profile.setName((String) yamlData.get("name"));
            }
            if (yamlData.containsKey("expertiseLevel")) {
                profile.setExpertiseLevel((String) yamlData.get("expertiseLevel"));
            }
            if (yamlData.containsKey("preferredLanguage")) {
                profile.setPreferredLanguage((String) yamlData.get("preferredLanguage"));
            }
            if (yamlData.containsKey("timezone")) {
                profile.setTimezone((String) yamlData.get("timezone"));
            }
            if (yamlData.containsKey("techStack")) {
                profile.setTechStack(convertToMap(yamlData.get("techStack")));
            }
            if (yamlData.containsKey("codingStyle")) {
                profile.setCodingStyle(convertToMap(yamlData.get("codingStyle")));
            }
            if (yamlData.containsKey("communicationPreferences")) {
                profile.setCommunicationPreferences(convertToMap(yamlData.get("communicationPreferences")));
            }
            if (yamlData.containsKey("workStyle")) {
                profile.setWorkStyle(convertToMap(yamlData.get("workStyle")));
            }
            if (yamlData.containsKey("recentProjects")) {
                profile.setRecentProjects(convertToMap(yamlData.get("recentProjects")));
            }

            UserProfile saved = userProfileRepository.save(profile);
            log.info("Profile imported from YAML successfully for user: {}", userId);
            return saved;

        } catch (Exception e) {
            log.error("Error importing profile from YAML for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to import profile from YAML", e);
        }
    }

    /**
     * Export profile to YAML format
     * @param userId The user ID
     * @return YAML string representation of the profile
     */
    @Transactional(readOnly = true)
    public String exportProfileToYaml(String userId) {
        log.info("Exporting profile to YAML for user: {}", userId);

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found for user: " + userId));

        try {
            Map<String, Object> exportData = new LinkedHashMap<>();
            exportData.put("userId", profile.getUserId());
            exportData.put("name", profile.getName());
            exportData.put("expertiseLevel", profile.getExpertiseLevel());
            exportData.put("preferredLanguage", profile.getPreferredLanguage());
            exportData.put("timezone", profile.getTimezone());
            exportData.put("techStack", profile.getTechStack());
            exportData.put("codingStyle", profile.getCodingStyle());
            exportData.put("communicationPreferences", profile.getCommunicationPreferences());
            exportData.put("workStyle", profile.getWorkStyle());
            exportData.put("recentProjects", profile.getRecentProjects());

            String yamlOutput = yaml.dump(exportData);
            log.info("Profile exported to YAML successfully for user: {}", userId);
            return yamlOutput;

        } catch (Exception e) {
            log.error("Error exporting profile to YAML for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to export profile to YAML", e);
        }
    }

    /**
     * Serialize object to JSON string
     * @param obj The object to serialize
     * @return JSON string
     */
    private String serializeToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Error serializing object to JSON: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * Deserialize JSON string to object
     * @param json The JSON string
     * @param clazz The target class
     * @return Deserialized object or null if error
     */
    private Object deserializeFromJson(String json, Class<?> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing JSON to {}: {}", clazz.getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * Convert object to Map
     * @param obj The object to convert
     * @return Map representation
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return new HashMap<>();
    }

    /**
     * Delete a user profile
     * @param userId The user ID
     */
    @Transactional
    public void deleteProfile(String userId) {
        log.info("Deleting profile for user: {}", userId);
        userProfileRepository.deleteByUserId(userId);
        log.info("Profile deleted successfully for user: {}", userId);
    }

    /**
     * Get all user profiles
     * @return List of all profile DTOs
     */
    @Transactional(readOnly = true)
    public List<UserProfileDTO> getAllProfiles() {
        log.debug("Getting all user profiles");
        return userProfileRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Check if a profile exists for a user
     * @param userId The user ID
     * @return true if exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean profileExists(String userId) {
        return userProfileRepository.existsByUserId(userId);
    }

    /**
     * Convert UserProfile entity to DTO
     * @param profile The profile entity
     * @return The profile DTO
     */
    private UserProfileDTO toDTO(UserProfile profile) {
        return UserProfileDTO.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .name(profile.getName())
                .expertiseLevel(profile.getExpertiseLevel())
                .preferredLanguage(profile.getPreferredLanguage())
                .timezone(profile.getTimezone())
                .techStack(profile.getTechStack())
                .codingStyle(profile.getCodingStyle())
                .communicationPreferences(profile.getCommunicationPreferences())
                .workStyle(profile.getWorkStyle())
                .recentProjects(profile.getRecentProjects())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
