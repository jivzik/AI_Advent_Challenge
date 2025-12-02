package de.jivz.ai_challenge.service.strategy;

import org.springframework.stereotype.Component;

/**
 * Strategy for simple JSON format instructions.
 * Default fallback when no custom schema or auto-schema is specified.
 */
@Component
public class SimpleJsonInstructionStrategy implements JsonInstructionStrategy {

    @Override
    public boolean canHandle(String customSchema, boolean autoSchema) {
        // This is the fallback strategy, always can handle
        return true;
    }

    @Override
    public String buildInstruction() {
        return """
                CRITICAL INSTRUCTION: Respond with ONLY this JSON format:
                {"response": "your complete answer here"}
                
                STRICT RULES:
                - No markdown, no code blocks, no ```json notation
                - Just the raw JSON object
                - Do NOT wrap in code blocks
                """;
    }
}

