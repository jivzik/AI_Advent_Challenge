package de.jivz.ai_challenge.openrouterservice.personalization.controller;

import de.jivz.ai_challenge.openrouterservice.personalization.profile.dto.UserProfileDTO;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.dto.UserProfileRequestDTO;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.entity.UserProfile;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.service.UserProfileService;
import de.jivz.ai_challenge.openrouterservice.personalization.learning.service.LearningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing user profiles
 */
@RestController
@RequestMapping("/api/personalization/profiles")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserProfileService profileService;
    private final LearningService learningService;

    /**
     * Get a user profile by user ID
     * GET /api/personalization/profiles/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileDTO> getProfile(@PathVariable String userId) {
        log.info("GET /api/personalization/profiles/{}", userId);

        return profileService.getProfileByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all user profiles
     * GET /api/personalization/profiles
     */
    @GetMapping
    public ResponseEntity<List<UserProfileDTO>> getAllProfiles() {
        log.info("GET /api/personalization/profiles");

        List<UserProfileDTO> profiles = profileService.getAllProfiles();
        return ResponseEntity.ok(profiles);
    }

    /**
     * Create a new user profile
     * POST /api/personalization/profiles
     */
    @PostMapping
    public ResponseEntity<UserProfileDTO> createProfile(
            @Valid @RequestBody UserProfileRequestDTO request) {
        log.info("POST /api/personalization/profiles - userId: {}", request.getUserId());

        try {
            UserProfileDTO created = profileService.createProfile(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Error creating profile: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update an existing user profile
     * PUT /api/personalization/profiles/{userId}
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserProfileDTO> updateProfile(
            @PathVariable String userId,
            @Valid @RequestBody UserProfileRequestDTO request) {
        log.info("PUT /api/personalization/profiles/{}", userId);

        try {
            UserProfile updated = profileService.updateProfile(userId, request);
            UserProfileDTO dto = profileService.convertToDto(updated);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            log.error("Error updating profile: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a user profile
     * DELETE /api/personalization/profiles/{userId}
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteProfile(@PathVariable String userId) {
        log.info("DELETE /api/personalization/profiles/{}", userId);

        profileService.deleteProfile(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if a profile exists
     * HEAD /api/personalization/profiles/{userId}
     */
    @RequestMapping(value = "/{userId}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> checkProfileExists(@PathVariable String userId) {
        log.debug("HEAD /api/personalization/profiles/{}", userId);

        if (profileService.profileExists(userId)) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Import user profile from YAML
     * POST /api/personalization/profiles/{userId}/import
     */
    @PostMapping("/{userId}/import")
    public ResponseEntity<UserProfileDTO> importProfile(
            @PathVariable String userId,
            @RequestBody String yamlContent) {
        log.info("POST /api/personalization/profiles/{}/import", userId);

        try {
            UserProfile imported = profileService.importProfileFromYaml(userId, yamlContent);
            UserProfileDTO dto = profileService.convertToDto(imported);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Error importing profile: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Export user profile to YAML
     * GET /api/personalization/profiles/{userId}/export
     */
    @GetMapping(value = "/{userId}/export", produces = "text/plain")
    public ResponseEntity<String> exportProfile(@PathVariable String userId) {
        log.info("GET /api/personalization/profiles/{}/export", userId);

        try {
            String yaml = profileService.exportProfileToYaml(userId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", "profile_" + userId + ".yaml");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(yaml);
        } catch (IllegalArgumentException e) {
            log.error("Profile not found for user: {}", userId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error exporting profile: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get AI-generated profile suggestions
     * GET /api/personalization/profiles/{userId}/suggestions
     */
    @GetMapping("/{userId}/suggestions")
    public ResponseEntity<List<String>> getProfileSuggestions(@PathVariable String userId) {
        log.info("GET /api/personalization/profiles/{}/suggestions", userId);

        try {
            List<String> suggestions = learningService.suggestProfileUpdates(userId);
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            log.error("Error getting suggestions for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
