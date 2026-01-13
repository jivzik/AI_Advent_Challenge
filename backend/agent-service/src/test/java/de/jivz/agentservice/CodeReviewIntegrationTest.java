package de.jivz.agentservice;


import de.jivz.agentservice.agent.model.AgentResult;
import de.jivz.agentservice.agent.model.AgentTask;
import de.jivz.agentservice.persistence.PRReviewEntity;
import de.jivz.agentservice.persistence.PRReviewRepository;
import de.jivz.agentservice.service.AgentOrchestratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CodeReviewIntegrationTest {

    @Autowired
    private AgentOrchestratorService orchestrator;

    @Autowired
    private PRReviewRepository repository;

    @Test
    void fullCodeReviewFlow() {
        // Given: Create a review task
        AgentTask task = AgentTask.builder()
                .type(AgentTask.TaskType.CODE_REVIEW)
                .prNumber(999)
                .repository("test/repo")
                .build();

        // When: Execute task
        AgentResult result = orchestrator.executeTask(task);

        // Then: Check result
        assertNotNull(result);
        assertEquals("CodeReviewAgent", result.getAgentName());

        // Verify saved in DB
        List<PRReviewEntity> reviews = repository.findByPrNumberOrderByReviewedAtDesc(999);

        if (result.getStatus() == AgentResult.ExecutionStatus.SUCCESS) {
            assertFalse(reviews.isEmpty());
            PRReviewEntity saved = reviews.get(0);
            assertEquals(999, saved.getPrNumber());
            assertNotNull(saved.getReviewedAt());
        }
    }
}