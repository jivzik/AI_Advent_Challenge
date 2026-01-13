package de.jivz.agentservice.agent.model;


import lombok.Builder;
import lombok.Data;
import java.util.Map;

/**
 * Task for agent to execute
 * Immutable value object
 */
@Data
@Builder
public class AgentTask {

    /**
     * Type of task
     */
    private final TaskType type;

    /**
     * PR number (if applicable)
     */
    private final Integer prNumber;

    /**
     * Repository name (e.g., "owner/repo")
     */
    private final String repository;

    /**
     * Additional context data
     * Flexible map for future extensibility
     */
    @Builder.Default
    private final Map<String, Object> context = Map.of();

    /**
     * Task priority override
     */
    private final Integer priorityOverride;

    /**
     * Task types supported by agents
     */
    public enum TaskType {
        CODE_REVIEW,        // CodeReviewAgent
        CODE_GENERATION,    // CoderAgent (future)
        BUG_FIXING,         // DebuggerAgent (future)
        REFACTORING,        // RefactorAgent (future)
        DOC_UPDATE,         // DocAgent (future)
        TEST_GENERATION     // TestAgent (future)
    }
}