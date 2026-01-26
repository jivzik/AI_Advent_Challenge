package de.jivz.ai_challenge.openrouterservice.personalization.profile.repository;

import de.jivz.ai_challenge.openrouterservice.personalization.profile.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for UserProfile entity.
 * Provides database access methods for user profiles.
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    /**
     * Find a user profile by user ID
     * @param userId The user ID
     * @return Optional containing the user profile if found
     */
    Optional<UserProfile> findByUserId(String userId);

    /**
     * Check if a user profile exists by user ID
     * @param userId The user ID
     * @return true if profile exists, false otherwise
     */
    boolean existsByUserId(String userId);

    /**
     * Delete a user profile by user ID
     * @param userId The user ID
     */
    void deleteByUserId(String userId);
}
