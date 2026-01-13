package de.jivz.agentservice.service;

import de.jivz.agentservice.agent.model.AgentTask;
import de.jivz.agentservice.mcp.MCPFactory;
import de.jivz.agentservice.service.AgentOrchestratorService;
import de.jivz.agentservice.mcp.model.MCPToolResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PRDetectorServiceTest {

    @Mock
    private MCPFactory mcpFactory;

    @Mock
    private ReviewStorageService storageService;

    @Mock
    private AgentOrchestratorService orchestrator;

    @InjectMocks
    private PRDetectorService detectorService;

    @Test
    void shouldDetectNewPRs() {
        // Given: 2 open PRs, 1 already reviewed
        MCPToolResult listResult = MCPToolResult.builder()
                .success(true)
                .result(List.of(
                        Map.of(
                                "number", 123,
                                "title", "New Feature",
                                "headSha", "abc123",
                                "baseSha", "def456",
                                "author", "user1",
                                "baseBranch", "main",
                                "headBranch", "feature-1"
                        ),
                        Map.of(
                                "number", 124,
                                "title", "Bug Fix",
                                "headSha", "xyz789",
                                "baseSha", "def456",
                                "author", "user2",
                                "baseBranch", "main",
                                "headBranch", "bugfix"
                        )
                ))
                .build();

        when(mcpFactory.route(eq("git:list_open_prs"), any()))
                .thenReturn(listResult);

        // PR #123 already reviewed
        when(storageService.isAlreadyReviewed(123, "abc123", "CodeReviewAgent"))
                .thenReturn(true);

        // PR #124 not reviewed yet
        when(storageService.isAlreadyReviewed(124, "xyz789", "CodeReviewAgent"))
                .thenReturn(false);

        // When
        int processed = detectorService.detectAndProcessNewPRs();

        // Then
        assertEquals(1, processed);

        // Should only process PR #124
        verify(orchestrator, times(1)).executeTask(any(AgentTask.class));
    }

    @Test
    void shouldReturnZeroWhenNoPRs() {
        // Given
        MCPToolResult emptyResult = MCPToolResult.builder()
                .success(true)
                .result(List.of())
                .build();

        when(mcpFactory.route(eq("git:list_open_prs"), any()))
                .thenReturn(emptyResult);

        // When
        int processed = detectorService.detectAndProcessNewPRs();

        // Then
        assertEquals(0, processed);
        verify(orchestrator, never()).executeTask(any());
    }

    @Test
    void shouldHandleMCPFailure() {
        // Given
        MCPToolResult failedResult = MCPToolResult.builder()
                .success(false)
                .error("Connection timeout")
                .build();

        when(mcpFactory.route(eq("git:list_open_prs"), any()))
                .thenReturn(failedResult);

        // When
        int processed = detectorService.detectAndProcessNewPRs();

        // Then
        assertEquals(0, processed);
        verify(orchestrator, never()).executeTask(any());
    }
}