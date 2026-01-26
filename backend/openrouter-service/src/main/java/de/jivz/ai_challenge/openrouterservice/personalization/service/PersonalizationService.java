package de.jivz.ai_challenge.openrouterservice.personalization.service;

import de.jivz.ai_challenge.openrouterservice.personalization.learning.service.LearningService;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.dto.UserProfileDTO;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.entity.AgentMemory;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.entity.UserInteraction;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.entity.UserProfile;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.repository.AgentMemoryRepository;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.repository.UserInteractionRepository;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main personalization service that coordinates profile management,
 * learning, and response adaptation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalizationService {

    private final UserProfileService profileService;
    private final LearningService learningService;
    private final UserInteractionRepository interactionRepository;
    private final AgentMemoryRepository memoryRepository;

    /**
     * Enhance user prompt with profile and learned patterns
     * @param userId The user ID
     * @param userQuery The original user query
     * @return Enhanced prompt with personalization context
     */
    public String enhancePromptWithProfile(String userId, String userQuery) {
        log.debug("Enhancing prompt with profile for user: {}", userId);

        // Load user profile (or create default)
        UserProfile profile = profileService.loadOrCreateProfile(userId);

        // Load top 5 high-confidence memories
        Pageable top5 = PageRequest.of(0, 5);
        List<AgentMemory> topMemories = memoryRepository
                .findByUserIdOrderByConfidenceDesc(userId, top5);

        // Load last 3 interactions for context
        Pageable last3 = PageRequest.of(0, 3);
        List<UserInteraction> recentInteractions = interactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, last3);

        // Build enhanced prompt
        StringBuilder enhancedPrompt = new StringBuilder();

        enhancedPrompt.append("You are an AI assistant helping ");
        enhancedPrompt.append(profile.getName());
        enhancedPrompt.append(", a ");
        enhancedPrompt.append(profile.getExpertiseLevel());
        enhancedPrompt.append(" developer.\n\n");

        // === USER PROFILE ===
        enhancedPrompt.append("=== USER PROFILE ===\n");
        enhancedPrompt.append(buildProfileContextString(profile));
        enhancedPrompt.append("\n");

        // === LEARNED PATTERNS ===
        if (!topMemories.isEmpty()) {
            enhancedPrompt.append("=== LEARNED PATTERNS ===\n");
            enhancedPrompt.append(buildMemoryContextString(topMemories));
            enhancedPrompt.append("\n");
        }

        // === RECENT CONTEXT ===
        if (!recentInteractions.isEmpty()) {
            enhancedPrompt.append("=== RECENT CONTEXT ===\n");
            enhancedPrompt.append(buildRecentContextString(recentInteractions));
            enhancedPrompt.append("\n");
        }

        // === USER QUERY ===
        enhancedPrompt.append("=== USER QUERY ===\n");
        enhancedPrompt.append(userQuery);
        enhancedPrompt.append("\n\n");

        // === INSTRUCTIONS ===
        enhancedPrompt.append("=== INSTRUCTIONS ===\n");
        enhancedPrompt.append("- Respond in ");
        enhancedPrompt.append(getLanguageName(profile.getPreferredLanguage()));
        enhancedPrompt.append("\n");
        enhancedPrompt.append("- Use code examples in the user's coding style\n");
        enhancedPrompt.append("- Reference user's tech stack when suggesting solutions\n");
        enhancedPrompt.append("- Match the communication preferences (tone, format, detail level)\n");

        if (profile.getCodingStyle() != null && !profile.getCodingStyle().isEmpty()) {
            enhancedPrompt.append("- If generating code, use: ");
            enhancedPrompt.append(formatCodingStyle(profile.getCodingStyle()));
            enhancedPrompt.append("\n");
        }

        enhancedPrompt.append("- Include file paths in comments (user prefers this)\n\n");
        enhancedPrompt.append("Provide a helpful, personalized response.\n");

        String result = enhancedPrompt.toString();
        log.debug("Enhanced prompt created: {} characters", result.length());
        return result;
    }

    /**
     * Build profile context string for prompt
     * @param profile The user profile
     * @return Formatted profile string
     */
    public String buildProfileContextString(UserProfile profile) {
        StringBuilder sb = new StringBuilder();

        // Tech Stack
        if (profile.getTechStack() != null && !profile.getTechStack().isEmpty()) {
            sb.append("Tech Stack:\n");
            Map<String, Object> techStack = profile.getTechStack();
            for (Map.Entry<String, Object> entry : techStack.entrySet()) {
                sb.append("  - ").append(entry.getKey()).append(": ");
                sb.append(formatValue(entry.getValue())).append("\n");
            }
        }

        // Coding Style Preferences
        if (profile.getCodingStyle() != null && !profile.getCodingStyle().isEmpty()) {
            sb.append("\nCoding Style Preferences:\n");
            Map<String, Object> codingStyle = profile.getCodingStyle();
            if (codingStyle.containsKey("naming")) {
                sb.append("  - Naming: ").append(codingStyle.get("naming")).append("\n");
            }
            if (codingStyle.containsKey("annotations")) {
                sb.append("  - Annotations: ").append(formatValue(codingStyle.get("annotations"))).append("\n");
            }
            if (codingStyle.containsKey("errorHandling")) {
                sb.append("  - Error Handling: ").append(codingStyle.get("errorHandling")).append("\n");
            }
            if (codingStyle.containsKey("responseType")) {
                sb.append("  - Response Type: ").append(codingStyle.get("responseType")).append("\n");
            }
        }

        // Communication Preferences
        if (profile.getCommunicationPreferences() != null && !profile.getCommunicationPreferences().isEmpty()) {
            sb.append("\nCommunication Preferences:\n");
            Map<String, Object> commPrefs = profile.getCommunicationPreferences();
            if (commPrefs.containsKey("language")) {
                sb.append("  - Language: ").append(commPrefs.get("language")).append("\n");
            }
            if (commPrefs.containsKey("tone")) {
                sb.append("  - Tone: ").append(commPrefs.get("tone")).append("\n");
            }
            if (commPrefs.containsKey("responseFormat")) {
                sb.append("  - Format: ").append(commPrefs.get("responseFormat")).append("\n");
            }
            if (commPrefs.containsKey("detailLevel")) {
                sb.append("  - Detail Level: ").append(commPrefs.get("detailLevel")).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Build memory context string for prompt
     * @param memories List of agent memories
     * @return Formatted memory string
     */
    public String buildMemoryContextString(List<AgentMemory> memories) {
        StringBuilder sb = new StringBuilder();

        for (AgentMemory memory : memories) {
            sb.append("- ");
            sb.append(memory.getValue());
            sb.append(" (confidence: ");
            sb.append(String.format("%.2f", memory.getConfidence()));
            sb.append(")\n");
        }

        return sb.toString();
    }

    /**
     * Build recent context string from interactions
     * @param interactions Recent user interactions
     * @return Formatted context string
     */
    public String buildRecentContextString(List<UserInteraction> interactions) {
        StringBuilder sb = new StringBuilder();

        for (UserInteraction interaction : interactions) {
            sb.append("- Recently ");

            String query = interaction.getQuery();
            if (query.length() > 60) {
                query = query.substring(0, 57) + "...";
            }

            sb.append(interaction.getQueryType()).append(": ");
            sb.append(query).append("\n");
        }

        return sb.toString();
    }

    /**
     * Adapt LLM response to user's communication style
     * @param userId The user ID
     * @param llmResponse The raw LLM response
     * @return Adapted response
     */
    public String adaptResponseStyle(String userId, String llmResponse) {
        log.debug("Adapting response style for user: {}", userId);

        Optional<UserProfile> profileOpt = profileService.getProfile(userId);
        if (profileOpt.isEmpty()) {
            return llmResponse;
        }

        UserProfile profile = profileOpt.get();
        Map<String, Object> commPrefs = profile.getCommunicationPreferences();

        if (commPrefs == null || commPrefs.isEmpty()) {
            return llmResponse;
        }

        String adapted = llmResponse;

        // Check response format preference
        String responseFormat = (String) commPrefs.get("responseFormat");
        if ("structured".equals(responseFormat)) {
            // Ensure response has headers/sections
            if (!llmResponse.contains("##") && !llmResponse.contains("===")) {
                log.debug("Adding structure to response");
            }
        } else if ("conversational".equals(responseFormat)) {
            // Remove excessive formatting
            adapted = adapted.replaceAll("===.*?===\\n", "");
            adapted = adapted.replaceAll("##+ ", "");
        }

        // Check emoji usage preference
        String emojiUsage = (String) commPrefs.get("emojiUsage");
        if ("minimal".equals(emojiUsage)) {
            adapted = adapted.replaceAll("[\\p{So}\\p{Sk}]", "");
        } else if ("extensive".equals(emojiUsage)) {
            if (!adapted.contains("✅") && !adapted.contains("❌")) {
                adapted = adapted.replaceFirst("success", "success ✅");
                adapted = adapted.replaceFirst("error", "error ❌");
            }
        }

        log.debug("Response adapted: {} → {} characters", llmResponse.length(), adapted.length());
        return adapted;
    }

    /**
     * Get relevant examples from past interactions
     * @param userId The user ID
     * @param queryType The type of query
     * @param limit Maximum number of examples
     * @return List of relevant examples
     */
    public List<String> getRelevantExamples(String userId, String queryType, int limit) {
        log.debug("Getting relevant examples for user: {}, queryType: {}", userId, queryType);

        Pageable pageable = PageRequest.of(0, limit * 2);
        List<UserInteraction> interactions = interactionRepository
                .findByUserIdAndQueryType(userId, queryType, pageable)
                .getContent();

        List<String> examples = new ArrayList<>();

        for (UserInteraction interaction : interactions) {
            if (interaction.getFeedback() != null && interaction.getFeedback() == 1) {
                String response = interaction.getResponse();
                if (response != null) {
                    if (response.contains("```")) {
                        String[] parts = response.split("```");
                        for (int i = 1; i < parts.length; i += 2) {
                            if (i < parts.length) {
                                examples.add(parts[i].trim());
                                if (examples.size() >= limit) break;
                            }
                        }
                    } else if (response.length() > 100) {
                        examples.add(response.substring(0, Math.min(200, response.length())));
                    }
                }
            }
            if (examples.size() >= limit) break;
        }

        log.debug("Found {} relevant examples", examples.size());
        return examples;
    }

    /**
     * Check if personalization should be used for user
     * @param userId The user ID
     * @return true if personalization is recommended
     */
    public boolean shouldUseProfile(String userId) {
        if (!profileService.profileExists(userId)) {
            log.debug("No profile exists for user: {}", userId);
            return false;
        }

        long interactionCount = interactionRepository.countByUserId(userId);
        if (interactionCount < 3) {
            log.debug("Not enough interactions for user {}: {}", userId, interactionCount);
            return false;
        }

        log.debug("Personalization recommended for user: {} ({} interactions)",
                 userId, interactionCount);
        return true;
    }

    /**
     * Get personalization statistics for a user
     * @param userId The user ID
     * @return Map with personalization statistics
     */
    public Map<String, Object> getPersonalizationStats(String userId) {
        log.debug("Getting personalization stats for user: {}", userId);

        Map<String, Object> stats = new HashMap<>();

        long interactionCount = interactionRepository.countByUserId(userId);
        stats.put("totalInteractions", interactionCount);

        List<AgentMemory> memories = memoryRepository.findByUserId(userId);
        stats.put("totalMemories", memories.size());

        Double avgFeedback = interactionRepository.getAverageFeedback(userId);
        stats.put("averageFeedback", avgFeedback != null ? avgFeedback : 0.0);

        List<Object[]> queryTypeStats = interactionRepository.getQueryTypeStatistics(userId);
        Map<String, Long> topQueryTypes = new HashMap<>();
        for (Object[] stat : queryTypeStats) {
            topQueryTypes.put((String) stat[0], (Long) stat[1]);
        }
        stats.put("topQueryTypes", topQueryTypes);

        double avgConfidence = memories.stream()
                .mapToDouble(AgentMemory::getConfidence)
                .average()
                .orElse(0.0);
        stats.put("averageConfidence", avgConfidence);

        Optional<UserProfile> profileOpt = profileService.getProfile(userId);
        if (profileOpt.isPresent()) {
            UserProfile profile = profileOpt.get();
            int completeness = 0;
            if (profile.getName() != null) completeness += 10;
            if (profile.getExpertiseLevel() != null) completeness += 10;
            if (profile.getTechStack() != null && !profile.getTechStack().isEmpty()) completeness += 30;
            if (profile.getCodingStyle() != null && !profile.getCodingStyle().isEmpty()) completeness += 25;
            if (profile.getCommunicationPreferences() != null && !profile.getCommunicationPreferences().isEmpty()) completeness += 25;
            stats.put("profileCompleteness", completeness);
        } else {
            stats.put("profileCompleteness", 0);
        }

        log.debug("Stats generated: {} interactions, {} memories, {}% profile completeness",
                 interactionCount, memories.size(), stats.get("profileCompleteness"));

        return stats;
    }

    // ========== Helper Methods ==========

    private String getLanguageName(String langCode) {
        return switch (langCode != null ? langCode : "en") {
            case "ru" -> "Russian";
            case "de" -> "German";
            case "en" -> "English";
            default -> langCode;
        };
    }

    private String formatCodingStyle(Map<String, Object> codingStyle) {
        List<String> parts = new ArrayList<>();
        if (codingStyle.containsKey("naming")) {
            parts.add(String.valueOf(codingStyle.get("naming")));
        }
        if (codingStyle.containsKey("annotations")) {
            parts.add("Lombok annotations");
        }
        return String.join(", ", parts);
    }

    private String formatValue(Object value) {
        if (value instanceof List) {
            @SuppressWarnings("unchecked")
            List<?> list = (List<?>) value;
            return String.join(", ", list.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList()));
        }
        return String.valueOf(value);
    }

    /**
     * Get complete personalization context for a user
     * Combines user profile with learned patterns and preferences
     *
     * @param userId The user ID
     * @return Complete personalization context
     */
    public Map<String, Object> getPersonalizationContext(String userId) {
        log.debug("Building complete personalization context for user: {}", userId);

        Map<String, Object> context = new HashMap<>();

        // Get user profile
        Optional<UserProfileDTO> profileOpt = profileService.getProfileByUserId(userId);
        if (profileOpt.isPresent()) {
            UserProfileDTO profile = profileOpt.get();
            context.put("profile", convertProfileToMap(profile));
        } else {
            log.warn("No profile found for user: {}, using defaults", userId);
            context.put("profile", getDefaultProfile());
        }

        // Get learned patterns and preferences
        Map<String, Object> learnedContext = learningService.getPersonalizationContext(userId);
        context.put("learned", learnedContext);

        return context;
    }

    /**
     * Build a personalized system prompt based on user context
     * This prompt can be prepended to LLM requests
     *
     * @param userId The user ID
     * @return Personalized system prompt
     */
    public String buildPersonalizedPrompt(String userId) {
        log.debug("Building personalized prompt for user: {}", userId);

        Map<String, Object> context = getPersonalizationContext(userId);

        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) context.get("profile");
        @SuppressWarnings("unchecked")
        Map<String, Object> learned = (Map<String, Object>) context.get("learned");

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI assistant helping a developer. Here's what you know about them:\n\n");

        // Add profile information
        if (profile.containsKey("expertiseLevel")) {
            prompt.append("Expertise Level: ").append(profile.get("expertiseLevel")).append("\n");
        }
        if (profile.containsKey("preferredLanguage")) {
            String lang = (String) profile.get("preferredLanguage");
            String langName = switch (lang) {
                case "ru" -> "Russian";
                case "de" -> "German";
                case "en" -> "English";
                default -> lang;
            };
            prompt.append("Preferred Language: ").append(langName).append("\n");
        }

        // Add tech stack
        if (profile.containsKey("techStack")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> techStack = (Map<String, Object>) profile.get("techStack");
            prompt.append("Tech Stack: ").append(techStack).append("\n");
        }

        // Add communication preferences
        if (profile.containsKey("communicationPreferences")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> commPrefs = (Map<String, Object>) profile.get("communicationPreferences");
            if (commPrefs.containsKey("tone")) {
                prompt.append("Preferred Tone: ").append(commPrefs.get("tone")).append("\n");
            }
            if (commPrefs.containsKey("detailLevel")) {
                prompt.append("Detail Level: ").append(commPrefs.get("detailLevel")).append("\n");
            }
        }

        // Add learned patterns
        @SuppressWarnings("unchecked")
        Map<String, String> patterns = (Map<String, String>) learned.get("patterns");
        if (patterns != null && !patterns.isEmpty()) {
            prompt.append("\nObserved Patterns:\n");
            patterns.forEach((key, value) ->
                prompt.append("- ").append(key).append(": ").append(value).append("\n")
            );
        }

        // Add current focus areas
        @SuppressWarnings("unchecked")
        Map<String, String> focusAreas = (Map<String, String>) learned.get("focusAreas");
        if (focusAreas != null && !focusAreas.isEmpty()) {
            prompt.append("\nCurrent Focus Areas: ").append(String.join(", ", focusAreas.values())).append("\n");
        }

        prompt.append("\nPlease adapt your responses according to these preferences and patterns.\n");

        String result = prompt.toString();
        log.debug("Generated personalized prompt: {} characters", result.length());

        return result;
    }

    /**
     * Trigger learning process for a user
     * Analyzes recent interactions and updates memory
     *
     * @param userId The user ID
     */
    public void triggerLearning(String userId) {
        log.info("Triggering learning process for user: {}", userId);
        learningService.learnFromInteractions(userId);
    }

    /**
     * Check if a user has a personalization profile
     *
     * @param userId The user ID
     * @return true if profile exists
     */
    public boolean hasProfile(String userId) {
        return profileService.profileExists(userId);
    }

    /**
     * Convert UserProfileDTO to Map for easier handling
     */
    private Map<String, Object> convertProfileToMap(UserProfileDTO profile) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", profile.getUserId());
        map.put("name", profile.getName());
        map.put("expertiseLevel", profile.getExpertiseLevel());
        map.put("preferredLanguage", profile.getPreferredLanguage());
        map.put("timezone", profile.getTimezone());
        map.put("techStack", profile.getTechStack());
        map.put("codingStyle", profile.getCodingStyle());
        map.put("communicationPreferences", profile.getCommunicationPreferences());
        map.put("workStyle", profile.getWorkStyle());
        map.put("recentProjects", profile.getRecentProjects());
        return map;
    }

    /**
     * Get default profile for users without a profile
     */
    private Map<String, Object> getDefaultProfile() {
        Map<String, Object> map = new HashMap<>();
        map.put("expertiseLevel", "middle");
        map.put("preferredLanguage", "en");
        map.put("timezone", "UTC");
        return map;
    }
}
