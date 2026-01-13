package de.jivz.agentservice.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.agentservice.agent.Agent;
import de.jivz.agentservice.agent.AgentRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Spring configuration for agent system
 * Auto-discovers and registers all agents
 */
@Configuration
@Slf4j
public class AgentConfig {

    /**
     * Create and populate agent registry
     * Spring automatically injects all Agent beans
     */
    @Bean
    public AgentRegistry agentRegistry(List<Agent> agents) {
        log.info("🚀 Initializing Agent Registry...");

        AgentRegistry registry = new AgentRegistry();

        // Register all discovered agents
        agents.forEach(agent -> {
            registry.register(agent);
            log.info("  ✓ Registered: {} v{} (priority: {})", agent.getName(), agent.getVersion(), agent.priority());
        });

        log.info("✅ Agent Registry initialized with {} agent(s)", agents.size());

        return registry;
    }

    /**
     * Provide ObjectMapper bean for JSON serialization/deserialization
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}