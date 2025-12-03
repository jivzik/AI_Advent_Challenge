package de.jivz.ai_challenge.service.strategy;

import org.springframework.stereotype.Component;

/**
 * Strategy for simple JSON format instructions.
 * Default fallback when no custom schema or auto-schema is specified.
 * This has the lowest priority - only used when no other strategy matches.
 */
@Component
public class SimpleJsonInstructionStrategy implements JsonInstructionStrategy {

    private static final String NUTRITIONIST_MARKER = "nutritionist_mode";
    private static final String META_PROMPT_MARKER = "meta_prompt";

    @Override
    public boolean canHandle(String customSchema, boolean autoSchema) {
        // This is the fallback strategy - handle only when no specific mode is set
        // Don't handle if it's nutritionist mode, meta_prompt mode, or auto-schema mode
        if (autoSchema) {
            return false;
        }
        if (customSchema != null && (customSchema.contains(NUTRITIONIST_MARKER) || customSchema.contains(META_PROMPT_MARKER))) {
            return false;
        }
        // Simple fallback for basic JSON mode
        return customSchema == null || customSchema.isBlank();
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

