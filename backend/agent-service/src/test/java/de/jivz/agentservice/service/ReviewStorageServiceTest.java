package de.jivz.agentservice.service;

import de.jivz.agentservice.dto.ReviewResult;
import de.jivz.agentservice.persistence.PRReviewEntity;
import de.jivz.agentservice.persistence.PRReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewStorageServiceTest {

    @Mock
    private PRReviewRepository repository;

    @InjectMocks
    private ReviewStorageService storageService;

    @TempDir
    Path tempDir;

    @Test
    void shouldDetectAlreadyReviewed() {
        // Given
        when(repository.existsByPrNumberAndHeadShaAndAgentName(123, "abc123", "TestAgent"))
                .thenReturn(true);

        // When
        boolean result = storageService.isAlreadyReviewed(123, "abc123", "TestAgent");

        // Then
        assertTrue(result);
    }

    @Test
    void shouldDetectNotReviewed() {
        // Given
        when(repository.existsByPrNumberAndHeadShaAndAgentName(456, "xyz789", "TestAgent"))
                .thenReturn(false);

        // When
        boolean result = storageService.isAlreadyReviewed(456, "xyz789", "TestAgent");

        // Then
        assertFalse(result);
    }

    @Test
    void shouldSaveReview() {
        // Given
        ReflectionTestUtils.setField(storageService, "reportsDir", tempDir.toString());

        ReviewResult review = ReviewResult.builder()
                .prNumber(123)
                .repository("owner/repo")
                .baseSha("abc123")
                .headSha("def456")
                .prTitle("Test PR")
                .prAuthor("testuser")
                .baseBranch("main")
                .headBranch("feature")
                .decision(ReviewResult.ReviewDecision.APPROVE)
                .summary("Looks good")
                .totalIssues(2)
                .reviewText("Full review text")
                .reviewTimeMs(5000L)
                .build();

        PRReviewEntity savedEntity = PRReviewEntity.builder()
                .id(1L)
                .prNumber(123)
                .headSha("def456")
                .build();

        when(repository.save(any(PRReviewEntity.class)))
                .thenReturn(savedEntity);

        // When
        PRReviewEntity result = storageService.saveReview(review, "TestAgent");

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());

        verify(repository).save(any(PRReviewEntity.class));
    }
}