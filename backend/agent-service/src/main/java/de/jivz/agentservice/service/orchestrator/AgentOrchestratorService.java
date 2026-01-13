package de.jivz.agentservice.service;

import de.jivz.agentservice.agent.Agent;
import de.jivz.agentservice.agent.AgentRegistry;

import de.jivz.agentservice.agent.model.AgentResult;
import de.jivz.agentservice.agent.model.AgentTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Orchestrator for executing agent tasks
 * Routes tasks to appropriate agents
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgentOrchestratorService {

    private final AgentRegistry agentRegistry;

    /**
     * Execute task synchronously
     */
    public AgentResult executeTask(AgentTask task) {
        log.info("🎯 Orchestrator: Executing task {} for PR #{}",
                task.getType(), task.getPrNumber());

        // Get agents that can handle this task
        List<Agent> agents = agentRegistry.getAgentsFor(task);

        if (agents.isEmpty()) {
            log.warn("⚠️  No agents found for task type: {}", task.getType());
            return AgentResult.builder()
                    .status(AgentResult.ExecutionStatus.FAILED)
                    .agentName("Orchestrator")
                    .errorMessage("No agents available for task type: " + task.getType())
                    .executionTimeMs(0L)
                    .build();
        }

        // Execute first agent (highest priority)
        Agent agent = agents.get(0);
        log.info("🤖 Executing agent: {}", agent.getName());

        AgentResult result = agent.execute(task);

        log.info("✅ Agent {} completed with status: {}",
                agent.getName(), result.getStatus());

        return result;
    }

    /**
     * Execute task asynchronously
     */
    @Async("agentExecutor")
    public CompletableFuture<AgentResult> executeTaskAsync(AgentTask task) {
        return CompletableFuture.supplyAsync(() -> executeTask(task));
    }

    /**
     * Execute multiple tasks in parallel (for future use)
     */
    @Async("agentExecutor")
    public CompletableFuture<List<AgentResult>> executeTasksParallel(List<AgentTask> tasks) {
        List<CompletableFuture<AgentResult>> futures = tasks.stream()
                .map(task -> CompletableFuture.supplyAsync(() -> executeTask(task)))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }
}