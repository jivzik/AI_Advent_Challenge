package de.jivz.agentservice.persistence;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PRReviewRepository extends JpaRepository<PRReviewEntity, Long> {

    // Deduplication (key method!)
    boolean existsByPrNumberAndHeadShaAndAgentName(
            Integer prNumber,
            String headSha,
            String agentName
    );

    Optional<PRReviewEntity> findByPrNumberAndHeadShaAndAgentName(
            Integer prNumber,
            String headSha,
            String agentName
    );

    // Queries by PR
    List<PRReviewEntity> findByPrNumberOrderByReviewedAtDesc(Integer prNumber);

    Optional<PRReviewEntity> findFirstByPrNumberOrderByReviewedAtDesc(Integer prNumber);

    // Queries by repository
    List<PRReviewEntity> findByRepositoryOrderByReviewedAtDesc(String repository);

    // Queries by status
    List<PRReviewEntity> findByStatus(PRReviewEntity.ReviewStatus status);

    // Queries by time range
    List<PRReviewEntity> findByReviewedAtBetweenOrderByReviewedAtDesc(
            LocalDateTime start,
            LocalDateTime end
    );

    long countByReviewedAtBetween(LocalDateTime start, LocalDateTime end);

    // Statistics - FIXED VERSIONS

    /**
     * Count reviews today (using method query)
     */
    default long countReviewsToday() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return countByReviewedAtBetween(startOfDay, endOfDay);
    }

    /**
     * Get reviews with issues
     */
    @Query("SELECT r FROM PRReviewEntity r " +
            "WHERE r.totalIssues > 0 " +
            "ORDER BY r.totalIssues DESC")
    List<PRReviewEntity> findReviewsWithIssues();

    /**
     * Get average review time
     */
    @Query("SELECT AVG(CAST(r.reviewTimeMs AS double)) FROM PRReviewEntity r " +
            "WHERE r.reviewTimeMs IS NOT NULL")
    Double getAverageReviewTime();
}