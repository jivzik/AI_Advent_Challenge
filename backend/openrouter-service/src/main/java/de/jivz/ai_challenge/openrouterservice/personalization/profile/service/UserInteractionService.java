package de.jivz.ai_challenge.openrouterservice.personalization.profile.service;

import de.jivz.ai_challenge.openrouterservice.personalization.profile.dto.UserInteractionDTO;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.dto.UserInteractionRequestDTO;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.entity.UserInteraction;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.repository.UserInteractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing user interactions with the chat service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserInteractionService {

    private final UserInteractionRepository userInteractionRepository;

    /**
     * Record a new user interaction
     * @param requestDTO The interaction request
     * @return The recorded interaction DTO
     */
    @Transactional
    public UserInteractionDTO recordInteraction(UserInteractionRequestDTO requestDTO) {
        log.debug("Recording interaction for user: {}", requestDTO.getUserId());

        UserInteraction interaction = UserInteraction.builder()
                .userId(requestDTO.getUserId())
                .query(requestDTO.getQuery())
                .queryType(requestDTO.getQueryType())
                .response(requestDTO.getResponse())
                .feedback(requestDTO.getFeedback() != null ? requestDTO.getFeedback() : 0)
                .context(requestDTO.getContext())
                .processingTimeMs(requestDTO.getProcessingTimeMs())
                .tokensUsed(requestDTO.getTokensUsed())
                .model(requestDTO.getModel())
                .build();

        UserInteraction saved = userInteractionRepository.save(interaction);
        log.info("Interaction recorded: id={}, userId={}, queryType={}",
                 saved.getId(), saved.getUserId(), saved.getQueryType());

        return toDTO(saved);
    }

    /**
     * Update feedback for an interaction
     * @param interactionId The interaction ID
     * @param feedback The feedback value (-1, 0, 1)
     * @return The updated interaction DTO
     */
    @Transactional
    public UserInteractionDTO updateFeedback(Long interactionId, Integer feedback) {
        log.info("Updating feedback for interaction: {}", interactionId);

        UserInteraction interaction = userInteractionRepository.findById(interactionId)
                .orElseThrow(() -> new IllegalArgumentException("Interaction not found: " + interactionId));

        interaction.setFeedback(feedback);
        UserInteraction updated = userInteractionRepository.save(interaction);

        log.info("Feedback updated: interactionId={}, feedback={}", interactionId, feedback);
        return toDTO(updated);
    }

    /**
     * Get interactions for a user
     * @param userId The user ID
     * @param pageable Pagination parameters
     * @return Page of interaction DTOs
     */
    @Transactional(readOnly = true)
    public Page<UserInteractionDTO> getUserInteractions(String userId, Pageable pageable) {
        log.debug("Getting interactions for user: {}", userId);
        return userInteractionRepository.findByUserId(userId, pageable)
                .map(this::toDTO);
    }

    /**
     * Get interactions by query type
     * @param userId The user ID
     * @param queryType The query type
     * @param pageable Pagination parameters
     * @return Page of interaction DTOs
     */
    @Transactional(readOnly = true)
    public Page<UserInteractionDTO> getInteractionsByType(String userId, String queryType, Pageable pageable) {
        log.debug("Getting {} interactions for user: {}", queryType, userId);
        return userInteractionRepository.findByUserIdAndQueryType(userId, queryType, pageable)
                .map(this::toDTO);
    }

    /**
     * Get recent interactions for a user
     * @param userId The user ID
     * @param daysAgo Number of days to look back
     * @param pageable Pagination parameters
     * @return Page of recent interaction DTOs
     */
    @Transactional(readOnly = true)
    public Page<UserInteractionDTO> getRecentInteractions(String userId, int daysAgo, Pageable pageable) {
        LocalDateTime since = LocalDateTime.now().minusDays(daysAgo);
        log.debug("Getting interactions for user {} since {}", userId, since);
        return userInteractionRepository.findByUserIdAndCreatedAtAfter(userId, since, pageable)
                .map(this::toDTO);
    }

    /**
     * Get interactions with positive feedback
     * @param userId The user ID
     * @param pageable Pagination parameters
     * @return Page of interactions with positive feedback
     */
    @Transactional(readOnly = true)
    public Page<UserInteractionDTO> getPositiveFeedback(String userId, Pageable pageable) {
        log.debug("Getting positive feedback for user: {}", userId);
        return userInteractionRepository.findPositiveFeedbackByUserId(userId, pageable)
                .map(this::toDTO);
    }

    /**
     * Get interactions with negative feedback
     * @param userId The user ID
     * @param pageable Pagination parameters
     * @return Page of interactions with negative feedback
     */
    @Transactional(readOnly = true)
    public Page<UserInteractionDTO> getNegativeFeedback(String userId, Pageable pageable) {
        log.debug("Getting negative feedback for user: {}", userId);
        return userInteractionRepository.findNegativeFeedbackByUserId(userId, pageable)
                .map(this::toDTO);
    }

    /**
     * Get query type statistics for a user
     * @param userId The user ID
     * @return Map of query types to counts
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getQueryTypeStatistics(String userId) {
        log.debug("Getting query type statistics for user: {}", userId);
        List<Object[]> results = userInteractionRepository.getQueryTypeStatistics(userId);

        Map<String, Long> statistics = new HashMap<>();
        for (Object[] result : results) {
            String queryType = (String) result[0];
            Long count = (Long) result[1];
            statistics.put(queryType, count);
        }

        return statistics;
    }

    /**
     * Get average processing time for a user's interactions
     * @param userId The user ID
     * @return Average processing time in milliseconds, or 0 if no data
     */
    @Transactional(readOnly = true)
    public Double getAverageProcessingTime(String userId) {
        log.debug("Getting average processing time for user: {}", userId);
        Double avg = userInteractionRepository.getAverageProcessingTime(userId);
        return avg != null ? avg : 0.0;
    }

    /**
     * Delete an interaction
     * @param interactionId The interaction ID to delete
     */
    @Transactional
    public void deleteInteraction(Long interactionId) {
        log.info("Deleting interaction: {}", interactionId);

        if (!userInteractionRepository.existsById(interactionId)) {
            throw new IllegalArgumentException("Interaction not found: " + interactionId);
        }

        userInteractionRepository.deleteById(interactionId);
        log.info("Interaction deleted: {}", interactionId);
    }

    /**
     * Convert UserInteraction entity to DTO
     * @param interaction The interaction entity
     * @return The interaction DTO
     */
    private UserInteractionDTO toDTO(UserInteraction interaction) {
        return UserInteractionDTO.builder()
                .id(interaction.getId())
                .userId(interaction.getUserId())
                .query(interaction.getQuery())
                .queryType(interaction.getQueryType())
                .response(interaction.getResponse())
                .feedback(interaction.getFeedback())
                .context(interaction.getContext())
                .processingTimeMs(interaction.getProcessingTimeMs())
                .tokensUsed(interaction.getTokensUsed())
                .model(interaction.getModel())
                .createdAt(interaction.getCreatedAt())
                .build();
    }
}
