package de.jivz.ai_challenge.openrouterservice.personalization.learning.service;

import de.jivz.ai_challenge.openrouterservice.personalization.profile.dto.AgentMemoryDTO;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.dto.AgentMemoryRequestDTO;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.entity.AgentMemory;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.entity.UserInteraction;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.entity.UserProfile;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.repository.AgentMemoryRepository;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.repository.UserInteractionRepository;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.repository.UserProfileRepository;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.service.AgentMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service responsible for learning from user interactions and updating agent memory.
 * This service analyzes patterns, preferences, and user behavior to personalize responses.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LearningService {

    private final UserInteractionRepository interactionRepository;
    private final AgentMemoryRepository memoryRepository;
    private final UserProfileRepository profileRepository;
    private final AgentMemoryService memoryService;

    private static final int RECENT_INTERACTIONS_COUNT = 50;
    private static final int MIN_PATTERN_OCCURRENCES = 3;
    private static final double HIGH_CONFIDENCE_THRESHOLD = 0.8;

    /**
     * Log a new user interaction
     * @param userId The user ID
     * @param query The user's query
     * @param response The system's response
     * @param queryType The type of query (can be null for auto-detection)
     * @param processingTimeMs Processing time in milliseconds
     * @param tokensUsed Number of tokens used
     * @param model The model used
     * @return The saved interaction
     */
    @Transactional
    public UserInteraction logInteraction(String userId, String query, String response,
                                          String queryType, int processingTimeMs,
                                          int tokensUsed, String model) {
        // Auto-detect query type if not provided
        if (queryType == null || queryType.isEmpty()) {
            queryType = detectQueryType(query);
        }

        UserInteraction interaction = UserInteraction.builder()
                .userId(userId)
                .query(query)
                .queryType(queryType)
                .response(response)
                .feedback(0) // Default, will be updated later
                .processingTimeMs(processingTimeMs)
                .tokensUsed(tokensUsed)
                .model(model)
                .context(new HashMap<>())
                .build();

        UserInteraction saved = interactionRepository.save(interaction);
        log.info("Logged interaction for user {}, type: {}", userId, queryType);

        // Asynchronously update patterns
        updatePatterns(userId);

        return saved;
    }

    /**
     * Update feedback for an interaction
     * @param interactionId The interaction ID
     * @param feedback The feedback value (1 for positive, -1 for negative)
     */
    @Transactional
    public void updateFeedback(Long interactionId, int feedback) {
        UserInteraction interaction = interactionRepository.findById(interactionId)
                .orElseThrow(() -> new IllegalArgumentException("Interaction not found: " + interactionId));

        interaction.setFeedback(feedback);
        interactionRepository.save(interaction);

        log.info("Updated feedback for interaction {}: {}", interactionId, feedback);

        // Learn from feedback
        learnFromFeedback(interaction.getUserId(), interactionId, feedback);
    }

    /**
     * Asynchronously update patterns based on user interactions
     * @param userId The user ID
     */
    @Async
    @Transactional
    public void updatePatterns(String userId) {
        log.debug("Starting pattern update for user: {}", userId);

        // Load recent interactions
        Pageable pageable = PageRequest.of(0, RECENT_INTERACTIONS_COUNT);
        List<UserInteraction> recentInteractions = interactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        if (recentInteractions.size() < 5) {
            log.debug("Not enough interactions for pattern analysis (need at least 5, have {})",
                     recentInteractions.size());
            return;
        }

        // Analyze query type frequency
        Map<String, Long> queryTypeCounts = recentInteractions.stream()
                .collect(Collectors.groupingBy(
                        UserInteraction::getQueryType,
                        Collectors.counting()
                ));

        // Find most frequent query type
        String mostFrequentType = null;
        long maxCount = 0;
        for (Map.Entry<String, Long> entry : queryTypeCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostFrequentType = entry.getKey();
            }
        }

        // If query type appears >= 40% of time, store it
        if (mostFrequentType != null) {
            double frequency = (double) maxCount / recentInteractions.size();
            if (frequency >= 0.4) {
                storeOrUpdateMemory(userId, "pattern", "frequent_query_type",
                                   mostFrequentType, 0.5 + (frequency * 0.5));
                log.info("Stored frequent query type for user {}: {} ({}%)",
                        userId, mostFrequentType, (int)(frequency * 100));
            }
        }

        // Analyze time-of-day patterns
        analyzeTimePatterns(userId, recentInteractions);

        log.info("Updated patterns for user {}", userId);
    }

    /**
     * Learn from user feedback
     * @param userId The user ID
     * @param interactionId The interaction ID
     * @param feedback The feedback value
     */
    @Transactional
    public void learnFromFeedback(String userId, Long interactionId, int feedback) {
        UserInteraction interaction = interactionRepository.findById(interactionId)
                .orElse(null);

        if (interaction == null) {
            return;
        }

        if (feedback == 1) { // Positive feedback
            // Increase confidence for patterns in this response
            if (interaction.getResponse().contains("```")) {
                storeOrUpdateMemory(userId, "preference", "likes_code_examples",
                                   "true", 0.7);
            }

            // Store preferred response length
            int responseLength = interaction.getResponse().length();
            String lengthCategory = responseLength > 1000 ? "detailed" : "concise";
            storeOrUpdateMemory(userId, "preference", "response_length",
                               lengthCategory, 0.65);

        } else if (feedback == -1) { // Negative feedback
            // Store what to avoid
            String avoidKey = "avoid_pattern_" + System.currentTimeMillis();
            storeOrUpdateMemory(userId, "learned", avoidKey,
                               interaction.getQueryType(), 0.5);
        }

        log.debug("Learned from feedback for user {}: interactionId={}, feedback={}",
                 userId, interactionId, feedback);
    }

    /**
     * Detect query type from query text
     * @param query The query text
     * @return The detected query type
     */
    private String detectQueryType(String query) {
        String lowerQuery = query.toLowerCase();

        if (lowerQuery.matches(".*(код|класс|метод|функци[яю]|code|class|method|function).*")) {
            return "code";
        } else if (lowerQuery.matches(".*(объясни|что такое|как работает|explain|what is|how does).*")) {
            return "explanation";
        } else if (lowerQuery.matches(".*(ошибка|баг|не работает|fix|error|bug|issue).*")) {
            return "debug";
        } else {
            return "general";
        }
    }

    /**
     * Analyze time-of-day patterns
     * @param userId The user ID
     * @param interactions List of interactions
     */
    private void analyzeTimePatterns(String userId, List<UserInteraction> interactions) {
        int eveningCount = 0;
        for (UserInteraction interaction : interactions) {
            LocalTime time = interaction.getCreatedAt().toLocalTime();
            if (time.isAfter(LocalTime.of(18, 0)) || time.isBefore(LocalTime.of(6, 0))) {
                eveningCount++;
            }
        }

        double eveningPercentage = (double) eveningCount / interactions.size();
        if (eveningPercentage > 0.7) {
            storeOrUpdateMemory(userId, "pattern", "evening_coder",
                               "true", 0.7 + (eveningPercentage * 0.2));
            log.info("User {} identified as evening coder ({}%)",
                    userId, (int)(eveningPercentage * 100));
        }
    }

    /**
     * Store or update memory entry
     * @param userId The user ID
     * @param memoryType The memory type
     * @param key The memory key
     * @param value The memory value
     * @param confidence The confidence level
     */
    private void storeOrUpdateMemory(String userId, String memoryType, String key,
                                    String value, double confidence) {
        Optional<AgentMemory> existing = memoryRepository.findByUserIdAndKey(userId, key);

        if (existing.isPresent()) {
            AgentMemory memory = existing.get();
            memory.setValue(value);
            memory.setConfidence(Math.min(0.95, memory.getConfidence() + 0.05));
            memory.setUsageCount(memory.getUsageCount() + 1);
            memoryRepository.save(memory);
        } else {
            AgentMemoryRequestDTO request = AgentMemoryRequestDTO.builder()
                    .userId(userId)
                    .memoryType(memoryType)
                    .key(key)
                    .value(value)
                    .confidence(confidence)
                    .build();
            memoryService.storeMemory(request);
        }
    }

    /**
     * Analyze recent interactions and update agent memory for a user
     * @param userId The user ID
     */
    public void learnFromInteractions(String userId) {
        log.info("Starting learning process for user: {}", userId);

        // Analyze query type preferences
        analyzeQueryTypePreferences(userId);

        // Analyze positive feedback patterns
        analyzePositiveFeedbackPatterns(userId);

        // Analyze negative feedback patterns
        analyzeNegativeFeedbackPatterns(userId);

        // Analyze recent query patterns
        analyzeRecentQueryPatterns(userId);

        log.info("Learning process completed for user: {}", userId);
    }

    /**
     * Analyze which query types the user prefers based on frequency
     * @param userId The user ID
     */
    private void analyzeQueryTypePreferences(String userId) {
        log.debug("Analyzing query type preferences for user: {}", userId);

        List<Object[]> stats = interactionRepository.getQueryTypeStatistics(userId);
        if (stats.isEmpty()) {
            return;
        }

        // Find most common query type
        String mostCommonType = null;
        long maxCount = 0;
        long totalCount = 0;

        for (Object[] stat : stats) {
            String queryType = (String) stat[0];
            Long count = (Long) stat[1];
            totalCount += count;

            if (count > maxCount) {
                maxCount = count;
                mostCommonType = queryType;
            }
        }

        if (mostCommonType != null && maxCount >= MIN_PATTERN_OCCURRENCES) {
            double frequency = (double) maxCount / totalCount;
            double confidence = Math.min(0.9, 0.5 + (frequency * 0.5));

            AgentMemoryRequestDTO memoryRequest = AgentMemoryRequestDTO.builder()
                    .userId(userId)
                    .memoryType("pattern")
                    .key("preferred_query_type")
                    .value(mostCommonType)
                    .confidence(confidence)
                    .build();

            memoryService.storeMemory(memoryRequest);
            log.info("Stored query type preference: userId={}, type={}, confidence={}",
                     userId, mostCommonType, confidence);
        }
    }

    /**
     * Analyze patterns from interactions with positive feedback
     * @param userId The user ID
     */
    private void analyzePositiveFeedbackPatterns(String userId) {
        log.debug("Analyzing positive feedback patterns for user: {}", userId);

        Pageable pageable = PageRequest.of(0, 20);
        List<UserInteraction> positiveInteractions = interactionRepository
                .findPositiveFeedbackByUserId(userId, pageable)
                .getContent();

        if (positiveInteractions.isEmpty()) {
            return;
        }

        // Analyze common words in queries with positive feedback
        Map<String, Integer> keywordFrequency = new HashMap<>();
        for (UserInteraction interaction : positiveInteractions) {
            String query = interaction.getQuery().toLowerCase();
            String[] words = query.split("\\s+");
            for (String word : words) {
                if (word.length() > 3) { // Only consider words longer than 3 characters
                    keywordFrequency.merge(word, 1, Integer::sum);
                }
            }
        }

        // Store top keywords as learned preferences
        keywordFrequency.entrySet().stream()
                .filter(entry -> entry.getValue() >= MIN_PATTERN_OCCURRENCES)
                .limit(5)
                .forEach(entry -> {
                    double confidence = Math.min(0.85, 0.6 + ((double) entry.getValue() / positiveInteractions.size()));

                    AgentMemoryRequestDTO memoryRequest = AgentMemoryRequestDTO.builder()
                            .userId(userId)
                            .memoryType("learned")
                            .key("positive_keyword_" + entry.getKey())
                            .value(entry.getKey())
                            .confidence(confidence)
                            .build();

                    memoryService.storeMemory(memoryRequest);
                    log.debug("Stored positive keyword: userId={}, keyword={}", userId, entry.getKey());
                });
    }

    /**
     * Analyze patterns from interactions with negative feedback
     * @param userId The user ID
     */
    private void analyzeNegativeFeedbackPatterns(String userId) {
        log.debug("Analyzing negative feedback patterns for user: {}", userId);

        Pageable pageable = PageRequest.of(0, 20);
        List<UserInteraction> negativeInteractions = interactionRepository
                .findNegativeFeedbackByUserId(userId, pageable)
                .getContent();

        if (negativeInteractions.isEmpty()) {
            return;
        }

        // Analyze response lengths in negative feedback
        double avgLength = negativeInteractions.stream()
                .filter(i -> i.getResponse() != null)
                .mapToInt(i -> i.getResponse().length())
                .average()
                .orElse(0);

        if (avgLength > 0) {
            String lengthPreference = avgLength > 1000 ? "concise" : "detailed";

            AgentMemoryRequestDTO memoryRequest = AgentMemoryRequestDTO.builder()
                    .userId(userId)
                    .memoryType("learned")
                    .key("response_length_preference")
                    .value(lengthPreference)
                    .confidence(0.7)
                    .build();

            memoryService.storeMemory(memoryRequest);
            log.info("Stored response length preference from negative feedback: userId={}, preference={}",
                     userId, lengthPreference);
        }
    }

    /**
     * Analyze recent query patterns to detect trends
     * @param userId The user ID
     */
    private void analyzeRecentQueryPatterns(String userId) {
        log.debug("Analyzing recent query patterns for user: {}", userId);

        LocalDateTime since = LocalDateTime.now().minusDays(7);
        Pageable pageable = PageRequest.of(0, RECENT_INTERACTIONS_COUNT);

        List<UserInteraction> recentInteractions = interactionRepository
                .findByUserIdAndCreatedAtAfter(userId, since, pageable)
                .getContent();

        if (recentInteractions.size() < MIN_PATTERN_OCCURRENCES) {
            return;
        }

        // Detect if user is working on a specific topic
        Map<String, Integer> topicFrequency = new HashMap<>();
        for (UserInteraction interaction : recentInteractions) {
            String query = interaction.getQuery().toLowerCase();

            // Check for specific topics
            if (query.contains("spring") || query.contains("спринг")) {
                topicFrequency.merge("spring", 1, Integer::sum);
            }
            if (query.contains("jpa") || query.contains("hibernate")) {
                topicFrequency.merge("jpa", 1, Integer::sum);
            }
            if (query.contains("vue") || query.contains("вью")) {
                topicFrequency.merge("vue", 1, Integer::sum);
            }
            if (query.contains("docker") || query.contains("докер")) {
                topicFrequency.merge("docker", 1, Integer::sum);
            }
            if (query.contains("postgres") || query.contains("sql")) {
                topicFrequency.merge("postgresql", 1, Integer::sum);
            }
        }

        // Store current focus topics
        topicFrequency.entrySet().stream()
                .filter(entry -> entry.getValue() >= MIN_PATTERN_OCCURRENCES)
                .forEach(entry -> {
                    double confidence = Math.min(0.9, 0.6 + ((double) entry.getValue() / recentInteractions.size()));

                    AgentMemoryRequestDTO memoryRequest = AgentMemoryRequestDTO.builder()
                            .userId(userId)
                            .memoryType("context")
                            .key("current_focus_" + entry.getKey())
                            .value(entry.getKey())
                            .confidence(confidence)
                            .build();

                    memoryService.storeMemory(memoryRequest);
                    log.info("Stored current focus topic: userId={}, topic={}, confidence={}",
                             userId, entry.getKey(), confidence);
                });
    }

    /**
     * Get personalization context for a user based on their profile and memory
     * This can be used to customize LLM prompts
     * @param userId The user ID
     * @return Map of personalization context
     */
    public Map<String, Object> getPersonalizationContext(String userId) {
        log.debug("Getting personalization context for user: {}", userId);

        Map<String, Object> context = new HashMap<>();

        // Get high-confidence memories
        var memories = memoryService.getHighConfidenceMemories(userId, HIGH_CONFIDENCE_THRESHOLD);

        Map<String, String> patterns = new HashMap<>();
        Map<String, String> preferences = new HashMap<>();
        Map<String, String> focusAreas = new HashMap<>();

        for (var memory : memories) {
            String key = memory.getKey();
            String value = memory.getValue();

            switch (memory.getMemoryType()) {
                case "pattern":
                    patterns.put(key, value);
                    break;
                case "preference":
                    preferences.put(key, value);
                    break;
                case "context":
                    if (key.startsWith("current_focus_")) {
                        focusAreas.put(key.replace("current_focus_", ""), value);
                    }
                    break;
                case "learned":
                    preferences.put(key, value);
                    break;
            }
        }

        context.put("patterns", patterns);
        context.put("preferences", preferences);
        context.put("focusAreas", focusAreas);

        log.debug("Personalization context created: patterns={}, preferences={}, focusAreas={}",
                  patterns.size(), preferences.size(), focusAreas.size());

        return context;
    }

    /**
     * Get top patterns for a user
     * @param userId The user ID
     * @param limit Maximum number of patterns to return
     * @return List of top patterns
     */
    @Transactional(readOnly = true)
    public List<AgentMemory> getTopPatterns(String userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return memoryRepository.findByUserIdAndMemoryType(userId, "pattern").stream()
                .sorted(Comparator.comparing(AgentMemory::getConfidence).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get top preferences for a user
     * @param userId The user ID
     * @param limit Maximum number of preferences to return
     * @return List of top preferences
     */
    @Transactional(readOnly = true)
    public List<AgentMemory> getTopPreferences(String userId, int limit) {
        return memoryRepository.findByUserIdAndMemoryType(userId, "preference").stream()
                .sorted(Comparator.comparing((AgentMemory m) ->
                        m.getConfidence() * m.getUsageCount()).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Clean up low confidence memories
     * @param userId The user ID
     * @return Number of deleted memory entries
     */
    @Transactional
    public int cleanupLowConfidenceMemories(String userId) {
        List<AgentMemory> allMemories = memoryRepository.findByUserId(userId);
        List<AgentMemory> toDelete = allMemories.stream()
                .filter(m -> m.getConfidence() < 0.3)
                .collect(Collectors.toList());

        int count = toDelete.size();
        memoryRepository.deleteAll(toDelete);

        log.info("Cleaned up {} low confidence memories for user {}", count, userId);
        return count;
    }

    /**
     * Detect coding style from user interactions
     * @param userId The user ID
     * @return Map with detected coding style patterns
     */
    @Transactional
    public Map<String, String> detectCodingStyleFromInteractions(String userId) {
        log.info("Detecting coding style for user: {}", userId);

        // Get code-related interactions
        Pageable pageable = PageRequest.of(0, 50);
        List<UserInteraction> codeInteractions = interactionRepository
                .findByUserIdAndQueryType(userId, "code", pageable)
                .getContent();

        if (codeInteractions.size() < 5) {
            log.debug("Not enough code interactions for style detection");
            return new HashMap<>();
        }

        Map<String, String> detectedStyle = new HashMap<>();
        Map<String, Integer> patternCounts = new HashMap<>();

        // Analyze all code snippets
        for (UserInteraction interaction : codeInteractions) {
            String response = interaction.getResponse();
            if (response == null) continue;

            // Detect naming convention
            if (response.matches(".*[a-z][A-Z].*")) {
                patternCounts.merge("camelCase", 1, Integer::sum);
            }
            if (response.matches(".*[a-z]_[a-z].*")) {
                patternCounts.merge("snake_case", 1, Integer::sum);
            }

            // Detect annotations
            if (response.contains("@Slf4j")) {
                patternCounts.merge("@Slf4j", 1, Integer::sum);
            }
            if (response.contains("@Data")) {
                patternCounts.merge("@Data", 1, Integer::sum);
            }
            if (response.contains("@Builder")) {
                patternCounts.merge("@Builder", 1, Integer::sum);
            }
            if (response.contains("@RequiredArgsConstructor")) {
                patternCounts.merge("@RequiredArgsConstructor", 1, Integer::sum);
            }

            // Detect error handling
            if (response.contains("try") && response.contains("catch")) {
                patternCounts.merge("try-catch", 1, Integer::sum);
            }
            if (response.contains("@ControllerAdvice")) {
                patternCounts.merge("@ControllerAdvice", 1, Integer::sum);
            }
        }

        // Store patterns with >= 5 occurrences
        List<String> annotations = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : patternCounts.entrySet()) {
            if (entry.getValue() >= 5) {
                String key = entry.getKey();
                String value = String.valueOf(entry.getValue());

                if (key.equals("camelCase") || key.equals("snake_case")) {
                    detectedStyle.put("naming", key);
                    storeOrUpdateMemory(userId, "learned", "naming_convention", key, 0.8);
                } else if (key.startsWith("@")) {
                    annotations.add(key);
                } else if (key.equals("try-catch") || key.equals("@ControllerAdvice")) {
                    detectedStyle.put("errorHandling", key);
                    storeOrUpdateMemory(userId, "learned", "error_handling", key, 0.75);
                }
            }
        }

        if (!annotations.isEmpty()) {
            detectedStyle.put("annotations", String.join(", ", annotations));
            storeOrUpdateMemory(userId, "learned", "preferred_annotations",
                               String.join(",", annotations), 0.8);
        }

        // Update profile coding style
        if (!detectedStyle.isEmpty()) {
            Optional<UserProfile> profileOpt = profileRepository.findByUserId(userId);
            if (profileOpt.isPresent()) {
                UserProfile profile = profileOpt.get();
                Map<String, Object> codingStyle = profile.getCodingStyle();
                if (codingStyle == null) {
                    codingStyle = new HashMap<>();
                }
                codingStyle.putAll(detectedStyle);
                profile.setCodingStyle(codingStyle);
                profileRepository.save(profile);
                log.info("Updated coding style in profile for user {}", userId);
            }
        }

        log.info("Detected coding style for user {}: {}", userId, detectedStyle);
        return detectedStyle;
    }

    /**
     * Suggest profile updates based on usage patterns
     * @param userId The user ID
     * @return List of suggestions
     */
    @Transactional(readOnly = true)
    public List<String> suggestProfileUpdates(String userId) {
        List<String> suggestions = new ArrayList<>();

        Optional<UserProfile> profileOpt = profileRepository.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            return suggestions;
        }

        UserProfile profile = profileOpt.get();

        // Analyze recent interactions for technology mentions
        LocalDateTime since = LocalDateTime.now().minusMonths(1);
        Pageable pageable = PageRequest.of(0, 100);
        List<UserInteraction> recentInteractions = interactionRepository
                .findByUserIdAndCreatedAtAfter(userId, since, pageable)
                .getContent();

        if (recentInteractions.isEmpty()) {
            return suggestions;
        }

        // Detect frequently mentioned technologies
        Map<String, Integer> techMentions = new HashMap<>();
        for (UserInteraction interaction : recentInteractions) {
            String query = interaction.getQuery().toLowerCase();

            checkAndCount(techMentions, query, "react");
            checkAndCount(techMentions, query, "vue");
            checkAndCount(techMentions, query, "angular");
            checkAndCount(techMentions, query, "docker");
            checkAndCount(techMentions, query, "kubernetes");
            checkAndCount(techMentions, query, "kafka");
            checkAndCount(techMentions, query, "redis");
            checkAndCount(techMentions, query, "mongodb");
            checkAndCount(techMentions, query, "elasticsearch");
        }

        // Suggest adding frequently mentioned technologies
        Map<String, Object> techStack = profile.getTechStack();
        if (techStack == null) {
            techStack = new HashMap<>();
        }

        String techStackStr = techStack.toString().toLowerCase();
        for (Map.Entry<String, Integer> entry : techMentions.entrySet()) {
            if (entry.getValue() >= 5 && !techStackStr.contains(entry.getKey())) {
                String tech = entry.getKey().substring(0, 1).toUpperCase() +
                             entry.getKey().substring(1);
                suggestions.add("Добавить '" + tech + "' в tech stack?");
            }
        }

        // Detect language preference from queries
        int ruCount = 0;
        int enCount = 0;
        int deCount = 0;

        for (UserInteraction interaction : recentInteractions) {
            String query = interaction.getQuery();
            if (query.matches(".*[а-яА-Я].*")) {
                ruCount++;
            } else if (query.matches(".*[äöüßÄÖÜ].*")) {
                deCount++;
            } else if (query.matches(".*[a-zA-Z].*")) {
                enCount++;
            }
        }

        int total = ruCount + enCount + deCount;
        if (total > 20) {
            String detectedLang = null;
            if (enCount > total * 0.8 && !"en".equals(profile.getPreferredLanguage())) {
                detectedLang = "en";
            } else if (deCount > total * 0.8 && !"de".equals(profile.getPreferredLanguage())) {
                detectedLang = "de";
            } else if (ruCount > total * 0.8 && !"ru".equals(profile.getPreferredLanguage())) {
                detectedLang = "ru";
            }

            if (detectedLang != null) {
                suggestions.add("Изменить preferred language на '" + detectedLang + "'?");
            }
        }

        log.info("Generated {} profile update suggestions for user {}", suggestions.size(), userId);
        return suggestions;
    }

    /**
     * Helper method to check and count technology mentions
     * @param map The map to update
     * @param text The text to search in
     * @param tech The technology name
     */
    private void checkAndCount(Map<String, Integer> map, String text, String tech) {
        if (text.contains(tech)) {
            map.merge(tech, 1, Integer::sum);
        }
    }
}
