package de.jivz.agentservice.agent;


import de.jivz.agentservice.agent.model.AgentResult;
import de.jivz.agentservice.agent.model.AgentTask;

/**
 * Base interface for all agents
 * Follows Strategy pattern for extensibility
 */
public interface Agent {

    /**
     * Check if this agent can handle the task
     *
     * @param task Task to check
     * @return true if agent can process this task
     */
    boolean canHandle(AgentTask task);

    /**
     * Execute the task
     *
     * @param task Task to execute
     * @return Result of execution
     * @throws AgentExecutionException if execution fails
     */
    AgentResult execute(AgentTask task);

    /**
     * Agent priority (0-100, higher = more important)
     * Used for ordering when multiple agents handle same task
     *
     * @return priority value
     */
    default int priority() {
        return 50; // Default medium priority
    }

    /**
     * Agent name for logging and tracking
     *
     * @return unique agent name
     */
    String getName();

    /**
     * Agent version
     *
     * @return version string (e.g., "1.0.0")
     */
    default String getVersion() {
        return "1.0.0";
    }
}