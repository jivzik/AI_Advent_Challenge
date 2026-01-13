package de.jivz.agentservice.agent.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Result of agent execution
 * Immutable value object
 */
@Data
@Builder
public class AgentResult {

    /**
     * Execution status
     */
    private final ExecutionStatus status;

    /**
     * Agent that produced this result
     */
    private final String agentName;

    /**
     * Execution time in milliseconds
     */
    private final long executionTimeMs;

    /**
     * Result data (flexible for different agents)
     */
    private final Object data;

    /**
     * Error message (if failed)
     */
    private final String errorMessage;

    /**
     * Timestamp when executed
     */
    @Builder.Default
    private final LocalDateTime executedAt = LocalDateTime.now();

    /**
     * Additional metadata
     */
    @Builder.Default
    private final Map<String, Object> metadata = Map.of();

    /**
     * Execution status enum
     */
    public enum ExecutionStatus {
        SUCCESS,
        FAILED,
        SKIPPED,
        NOT_IMPLEMENTED
    }

    // Factory methods for convenience

    public static AgentResult success(String agentName, Object data, long executionTimeMs) {
        return AgentResult.builder()
                .status(ExecutionStatus.SUCCESS)
                .agentName(agentName)
                .data(data)
                .executionTimeMs(executionTimeMs)
                .build();
    }

    public static AgentResult failed(String agentName, String error, long executionTimeMs) {
        return AgentResult.builder()
                .status(ExecutionStatus.FAILED)
                .agentName(agentName)
                .errorMessage(error)
                .executionTimeMs(executionTimeMs)
                .build();
    }

    public static AgentResult notImplemented(String agentName) {
        return AgentResult.builder()
                .status(ExecutionStatus.NOT_IMPLEMENTED)
                .agentName(agentName)
                .executionTimeMs(0)
                .build();
    }
}