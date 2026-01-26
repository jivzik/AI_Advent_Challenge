package de.jivz.ai_challenge.openrouterservice.personalization.profile.repository;

import de.jivz.ai_challenge.openrouterservice.personalization.profile.entity.AgentMemory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for AgentMemory entity.
 * Provides database access methods for agent memory entries.
 */
@Repository
public interface AgentMemoryRepository extends JpaRepository<AgentMemory, Long> {

    /**
     * Find a specific memory entry by user ID and key
     * @param userId The user ID
     * @param key The memory key
     * @return Optional containing the memory entry if found
     */
    Optional<AgentMemory> findByUserIdAndKey(String userId, String key);

    /**
     * Find all memory entries for a user
     * @param userId The user ID
     * @return List of memory entries
     */
    List<AgentMemory> findByUserId(String userId);

    /**
     * Find all memory entries for a user by type
     * @param userId The user ID
     * @param memoryType The memory type
     * @return List of memory entries
     */
    List<AgentMemory> findByUserIdAndMemoryType(String userId, String memoryType);

    /**
     * Find high-confidence memory entries for a user
     * @param userId The user ID
     * @param minConfidence Minimum confidence level
     * @return List of high-confidence memory entries
     */
    @Query("SELECT m FROM AgentMemory m WHERE m.userId = :userId AND m.confidence >= :minConfidence ORDER BY m.confidence DESC")
    List<AgentMemory> findHighConfidenceMemories(@Param("userId") String userId, @Param("minConfidence") Double minConfidence);

    /**
     * Find most used memory entries for a user
     * @param userId The user ID
     * @param limit Maximum number of results
     * @return List of most used memory entries
     */
    @Query("SELECT m FROM AgentMemory m WHERE m.userId = :userId ORDER BY m.usageCount DESC LIMIT :limit")
    List<AgentMemory> findMostUsedMemories(@Param("userId") String userId, @Param("limit") int limit);

    /**
     * Find memory entries by type and minimum confidence
     * @param userId The user ID
     * @param memoryType The memory type
     * @param minConfidence Minimum confidence level
     * @return List of memory entries
     */
    @Query("SELECT m FROM AgentMemory m WHERE m.userId = :userId AND m.memoryType = :memoryType AND m.confidence >= :minConfidence")
    List<AgentMemory> findByTypeAndConfidence(
        @Param("userId") String userId,
        @Param("memoryType") String memoryType,
        @Param("minConfidence") Double minConfidence
    );

    /**
     * Check if a memory entry exists for a user and key
     * @param userId The user ID
     * @param key The memory key
     * @return true if exists, false otherwise
     */
    boolean existsByUserIdAndKey(String userId, String key);

    /**
     * Delete all memory entries for a user
     * @param userId The user ID
     */
    void deleteByUserId(String userId);

    /**
     * Count memory entries by type for a user
     * @param userId The user ID
     * @param memoryType The memory type
     * @return Count of memory entries
     */
    long countByUserIdAndMemoryType(String userId, String memoryType);

    /**
     * Find memory entries for a user ordered by confidence descending
     * @param userId The user ID
     * @param pageable Pagination parameters
     * @return List of memory entries ordered by confidence
     */
    List<AgentMemory> findByUserIdOrderByConfidenceDesc(String userId, Pageable pageable);

    /**
     * Find memory entries with confidence greater than specified threshold
     * @param userId The user ID
     * @param minConfidence Minimum confidence threshold
     * @return List of memory entries above threshold
     */
    List<AgentMemory> findByUserIdAndConfidenceGreaterThan(String userId, Double minConfidence);

    /**
     * Delete memory entries with confidence less than specified threshold
     * @param userId The user ID
     * @param threshold Maximum confidence threshold for deletion
     */
    @Modifying
    void deleteByUserIdAndConfidenceLessThan(String userId, Double threshold);

    /**
     * Find top N memories with high confidence and usage
     * @param userId The user ID
     * @param minConfidence Minimum confidence threshold
     * @param pageable Pagination parameters (use PageRequest.of(0, N) for top N)
     * @return List of top memory entries
     */
    @Query("SELECT m FROM AgentMemory m WHERE m.userId = :userId " +
           "AND m.confidence > :minConfidence " +
           "ORDER BY m.confidence DESC, m.usageCount DESC")
    List<AgentMemory> findTopMemories(@Param("userId") String userId,
                                       @Param("minConfidence") Double minConfidence,
                                       Pageable pageable);

    /**
     * Increment usage count for a memory entry
     * @param id The memory entry ID
     * @param now Current timestamp
     */
    @Modifying
    @Query("UPDATE AgentMemory m SET m.usageCount = m.usageCount + 1, " +
           "m.lastUsed = :now WHERE m.id = :id")
    void incrementUsageCount(@Param("id") Long id, @Param("now") LocalDateTime now);
}
