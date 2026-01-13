package de.jivz.agentservice.agent;

import de.jivz.agentservice.agent.model.AgentResult;
import de.jivz.agentservice.agent.model.AgentTask;
import de.jivz.agentservice.mcp.MCPFactory;
import de.jivz.agentservice.mcp.model.MCPToolResult;
import de.jivz.agentservice.service.PromptLoaderService;
import de.jivz.agentservice.service.ReviewStorageService;
import de.jivz.agentservice.service.orchestrator.ToolExecutionOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodeReviewAgentTest {

    @Mock
    private MCPFactory mcpFactory;

    @Mock
    private ToolExecutionOrchestrator toolOrchestrator;

    @Mock
    private PromptLoaderService promptLoader;

    @Mock
    private ReviewStorageService storageService;

    @InjectMocks
    private CodeReviewAgent agent;

    @Test
    void shouldHandleCodeReviewTasks() {
        // Given
        AgentTask task = AgentTask.builder()
                .type(AgentTask.TaskType.CODE_REVIEW)
                .prNumber(123)
                .build();

        // When
        boolean canHandle = agent.canHandle(task);

        // Then
        assertTrue(canHandle);
    }

    @Test
    void shouldNotHandleOtherTasks() {
        // Given
        AgentTask task = AgentTask.builder()
                .type(AgentTask.TaskType.CODE_GENERATION)
                .build();

        // When
        boolean canHandle = agent.canHandle(task);

        // Then
        assertFalse(canHandle);
    }

    @Test
    void shouldExecuteSuccessfully() {
        // Given
        AgentTask task = AgentTask.builder()
                .type(AgentTask.TaskType.CODE_REVIEW)
                .prNumber(123)
                .repository("owner/repo")
                .build();

        // Mock git:get_pr_info
        MCPToolResult prInfoResult = MCPToolResult.builder()
                .success(true)
                .result(Map.of(
                        "number", 123,
                        "title", "Test PR",
                        "author", "testuser",
                        "baseBranch", "main",
                        "headBranch", "feature",
                        "baseSha", "abc123",
                        "headSha", "def456",
                        "filesCount", 3,
                        "additions", 50,
                        "deletions", 10
                ))
                .build();

        when(mcpFactory.route(eq("git:get_pr_info"), any()))
                .thenReturn(prInfoResult);

        // Mock LLM response
        String llmReview = """
            # Code Review
            
            ## Summary
            Code looks good overall.
            
            ## Issues Found
            - Issue: Missing null check on line 45
            - Warning: Unused import
            
            ## Recommendation
            APPROVE with minor comments
            """;

        when(toolOrchestrator.executeToolLoop(any(), anyDouble()))
                .thenReturn(llmReview);

        // Mock storage
        doNothing().when(storageService).saveReview(any(), anyString());

        // When
        AgentResult result = agent.execute(task);

        // Then
        assertEquals(AgentResult.ExecutionStatus.SUCCESS, result.getStatus());
        assertEquals("CodeReviewAgent", result.getAgentName());
        assertNotNull(result.getData());
        assertTrue(result.getExecutionTimeMs() > 0);

        // Verify interactions
        verify(mcpFactory).route(eq("git:get_pr_info"), any());
        verify(toolOrchestrator).executeToolLoop(any(), eq(0.2));
        verify(storageService).saveReview(any(), eq("CodeReviewAgent"));
    }

    @Test
    void shouldHandleFailureWhenPRInfoNotFound() {
        // Given
        AgentTask task = AgentTask.builder()
                .type(AgentTask.TaskType.CODE_REVIEW)
                .prNumber(999)
                .build();

        // Mock failed git call
        MCPToolResult failedResult = MCPToolResult.builder()
                .success(false)
                .error("PR not found")
                .build();

        when(mcpFactory.route(eq("git:get_pr_info"), any()))
                .thenReturn(failedResult);

        // When
        AgentResult result = agent.execute(task);

        // Then
        assertEquals(AgentResult.ExecutionStatus.FAILED, result.getStatus());
        assertNotNull(result.getErrorMessage());

        // Should not call orchestrator or storage
        verify(toolOrchestrator, never()).executeToolLoop(any(), anyDouble());
        verify(storageService, never()).saveReview(any(), anyString());
    }

    @Test
    void shouldCountIssuesCorrectly() {
        // This would test the private countIssuesInText method
        // You can make it package-private for testing or use reflection

        // Example if method was accessible:
        String reviewText = """
            Issue: Missing null check
            Warning: Unused variable
            🔴 Critical: Security vulnerability
            Problem: Performance bottleneck
            """;

        // Expected: 4 issues
    }
}