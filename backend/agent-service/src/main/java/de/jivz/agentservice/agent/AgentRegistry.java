package de.jivz.agentservice.agent;

import de.jivz.agentservice.agent.model.AgentTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Registry for all agents
 * Thread-safe singleton
 */
@Component
@Slf4j
public class AgentRegistry {

    private final List<Agent> agents = new CopyOnWriteArrayList<>();

    /**
     * Register an agent
     * Automatically sorts by priority
     */
    public void register(Agent agent) {
        log.info("📝 Registering agent: {} (priority: {})",
                agent.getName(), agent.priority());

        agents.add(agent);

        // Sort by priority (descending)
        agents.sort(Comparator.comparingInt(Agent::priority).reversed());

        log.info("✅ Total agents registered: {}", agents.size());
    }

    /**
     * Get all agents that can handle this task
     * Returns sorted by priority
     */
    public List<Agent> getAgentsFor(AgentTask task) {
        return agents.stream()
                .filter(agent -> agent.canHandle(task))
                .collect(Collectors.toList());
    }

    /**
     * Get all registered agents
     */
    public List<Agent> getAllAgents() {
        return List.copyOf(agents);
    }

    /**
     * Get agent by name
     */
    public Agent getAgent(String name) {
        return agents.stream()
                .filter(agent -> agent.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Unregister agent (for testing)
     */
    public void unregister(Agent agent) {
        agents.remove(agent);
        log.info("❌ Unregistered agent: {}", agent.getName());
    }

    /**
     * Clear all agents (for testing)
     */
    public void clear() {
        agents.clear();
        log.info("🗑️  Cleared all agents");
    }
}