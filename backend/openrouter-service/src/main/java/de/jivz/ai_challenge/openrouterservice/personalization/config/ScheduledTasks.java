package de.jivz.ai_challenge.openrouterservice.personalization.config;

import de.jivz.ai_challenge.openrouterservice.personalization.learning.service.LearningService;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.entity.UserInteraction;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.entity.UserProfile;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.repository.UserInteractionRepository;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled tasks for personalization learning and maintenance
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ScheduledTasks {

    private final LearningService learningService;
    private final UserProfileRepository profileRepository;
    private final UserInteractionRepository interactionRepository;

    /**
     * Cleanup low confidence memories based on configured schedule
     * Default: Every Sunday at 2 AM
     */
    @Scheduled(cron = "${personalization.learning.cleanup-schedule:0 0 2 * * SUN}")
    public void cleanupLowConfidenceMemories() {
        log.info("Starting scheduled cleanup of low confidence memories");

        try {
            // Get all user IDs from profiles
            List<String> userIds = profileRepository.findAll().stream()
                    .map(UserProfile::getUserId)
                    .toList();

            int totalDeleted = 0;

            // Cleanup for each user
            for (String userId : userIds) {
                try {
                    int deleted = learningService.cleanupLowConfidenceMemories(userId);
                    totalDeleted += deleted;

                    if (deleted > 0) {
                        log.info("Cleaned up {} low confidence memories for user: {}", deleted, userId);
                    }

                } catch (Exception e) {
                    log.error("Error cleaning up memories for user {}: {}", userId, e.getMessage(), e);
                }
            }

            log.info("Completed scheduled cleanup. Total memories deleted: {} across {} users",
                     totalDeleted, userIds.size());

        } catch (Exception e) {
            log.error("Error during scheduled memory cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Update patterns for active users
     * Runs every hour (3600000 milliseconds)
     */
    @Scheduled(fixedDelay = 3600000)
    public void updatePatternsForActiveUsers() {
        log.info("Starting scheduled pattern update for active users");

        try {
            // Find users with interactions in the last 24 hours
            LocalDateTime since = LocalDateTime.now().minusHours(24);

            List<String> activeUserIds = interactionRepository.findAll().stream()
                    .filter(interaction -> interaction.getCreatedAt().isAfter(since))
                    .map(UserInteraction::getUserId)
                    .distinct()
                    .toList();

            if (activeUserIds.isEmpty()) {
                log.info("No active users found in the last 24 hours");
                return;
            }

            log.info("Found {} active users in the last 24 hours", activeUserIds.size());

            // Process each active user asynchronously
            int processedCount = 0;
            for (String userId : activeUserIds) {
                try {
                    learningService.updatePatterns(userId);
                    processedCount++;

                } catch (Exception e) {
                    log.error("Error updating patterns for user {}: {}", userId, e.getMessage(), e);
                }
            }

            log.info("Completed scheduled pattern update. Processed {} users", processedCount);

        } catch (Exception e) {
            log.error("Error during scheduled pattern update: {}", e.getMessage(), e);
        }
    }

    /**
     * Log scheduled tasks status
     * Runs every 6 hours
     */
    @Scheduled(fixedDelay = 21600000)
    public void logScheduledTasksStatus() {
        try {
            long totalProfiles = profileRepository.count();

            LocalDateTime last24h = LocalDateTime.now().minusHours(24);
            long activeInteractions = interactionRepository.findAll().stream()
                    .filter(interaction -> interaction.getCreatedAt().isAfter(last24h))
                    .count();

            log.info("Scheduled Tasks Status - Total Profiles: {}, Active Interactions (24h): {}",
                     totalProfiles, activeInteractions);

        } catch (Exception e) {
            log.error("Error logging scheduled tasks status: {}", e.getMessage(), e);
        }
    }
}
