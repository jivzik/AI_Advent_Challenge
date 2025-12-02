package de.jivz.ai_challenge.service.strategy;

/**
 * Strategy interface for building JSON instructions based on request configuration.
 * Follows Strategy Pattern and Open/Closed Principle.
 */
public interface JsonInstructionStrategy {
    
    /**
     * Builds the JSON instruction to be prepended to the user message.
     *
     * @return The instruction string
     */
    String buildInstruction();
    
    /**
     * Checks if this strategy should handle the given schema.
     *
     * @param customSchema The custom schema from the request
     * @param autoSchema Whether auto-schema is enabled
     * @return true if this strategy should be used
     */
    boolean canHandle(String customSchema, boolean autoSchema);
}

