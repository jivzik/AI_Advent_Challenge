package de.jivz.ai_challenge.openrouterservice.personalization.profile.repository;

import de.jivz.ai_challenge.openrouterservice.personalization.profile.entity.UserInteraction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for UserInteraction entity.
 * Provides database access methods for user interactions.
 */
@Repository
public interface UserInteractionRepository extends JpaRepository<UserInteraction, Long> {

    /**
     * Find all interactions for a specific user
     * @param userId The user ID
     * @param pageable Pagination parameters
     * @return Page of user interactions
     */
    Page<UserInteraction> findByUserId(String userId, Pageable pageable);

    /**
     * Find all interactions for a user with a specific query type
     * @param userId The user ID
     * @param queryType The query type
     * @param pageable Pagination parameters
     * @return Page of user interactions
     */
    Page<UserInteraction> findByUserIdAndQueryType(String userId, String queryType, Pageable pageable);

    /**
     * Find recent interactions for a user
     * @param userId The user ID
     * @param since The cutoff date
     * @param pageable Pagination parameters
     * @return Page of recent interactions
     */
    Page<UserInteraction> findByUserIdAndCreatedAtAfter(String userId, LocalDateTime since, Pageable pageable);

    /**
     * Find interactions with positive feedback
     * @param userId The user ID
     * @param pageable Pagination parameters
     * @return Page of interactions with positive feedback
     */
    @Query("SELECT i FROM UserInteraction i WHERE i.userId = :userId AND i.feedback > 0")
    Page<UserInteraction> findPositiveFeedbackByUserId(@Param("userId") String userId, Pageable pageable);

    /**
     * Find interactions with negative feedback
     * @param userId The user ID
     * @param pageable Pagination parameters
     * @return Page of interactions with negative feedback
     */
    @Query("SELECT i FROM UserInteraction i WHERE i.userId = :userId AND i.feedback < 0")
    Page<UserInteraction> findNegativeFeedbackByUserId(@Param("userId") String userId, Pageable pageable);

    /**
     * Count interactions by user and query type
     * @param userId The user ID
     * @param queryType The query type
     * @return Count of interactions
     */
    long countByUserIdAndQueryType(String userId, String queryType);

    /**
     * Count total interactions by user
     * @param userId The user ID
     * @return Total count of interactions
     */
    long countByUserId(String userId);

    /**
     * Find interactions by user and feedback value
     * @param userId The user ID
     * @param feedback The feedback value
     * @return List of interactions with specified feedback
     */
    List<UserInteraction> findByUserIdAndFeedback(String userId, Integer feedback);

    /**
     * Find interactions by user ordered by created date descending
     * @param userId The user ID
     * @param pageable Pagination parameters
     * @return List of interactions ordered by date
     */
    List<UserInteraction> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Get query type statistics for a user
     * @param userId The user ID
     * @return List of [queryType, count] pairs
     */
    @Query("SELECT i.queryType, COUNT(i) FROM UserInteraction i WHERE i.userId = :userId GROUP BY i.queryType")
    List<Object[]> getQueryTypeStatistics(@Param("userId") String userId);

    /**
     * Find top query types for a user since a given date
     * @param userId The user ID
     * @param since The cutoff date
     * @return List of [queryType, count] pairs ordered by count descending
     */
    @Query("SELECT i.queryType, COUNT(i) as count FROM UserInteraction i " +
           "WHERE i.userId = :userId AND i.createdAt > :since " +
           "GROUP BY i.queryType ORDER BY count DESC")
    List<Object[]> findTopQueryTypes(@Param("userId") String userId, @Param("since") LocalDateTime since);

    /**
     * Get average feedback for a user (excluding 0)
     * @param userId The user ID
     * @return Average feedback value
     */
    @Query("SELECT AVG(CAST(i.feedback AS double)) FROM UserInteraction i " +
           "WHERE i.userId = :userId AND i.feedback != 0")
    Double getAverageFeedback(@Param("userId") String userId);

    /**
     * Get processing time statistics (avg, min, max)
     * @param userId The user ID
     * @return Array with [avg, min, max] processing times
     */
    @Query("SELECT AVG(i.processingTimeMs), MIN(i.processingTimeMs), MAX(i.processingTimeMs) " +
           "FROM UserInteraction i WHERE i.userId = :userId AND i.processingTimeMs IS NOT NULL")
    Object[] getProcessingTimeStats(@Param("userId") String userId);

    /**
     * Get average processing time for a user's interactions
     * @param userId The user ID
     * @return Average processing time in milliseconds
     */
    @Query("SELECT AVG(i.processingTimeMs) FROM UserInteraction i WHERE i.userId = :userId AND i.processingTimeMs IS NOT NULL")
    Double getAverageProcessingTime(@Param("userId") String userId);
}
