package de.jivz.ai_challenge.service.strategy;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Strategy for custom JSON schema instructions.
 * Used when user provides a specific JSON schema.
 */
@Component
public class CustomSchemaInstructionStrategy implements JsonInstructionStrategy {

    private String customSchema;

    @Override
    public boolean canHandle(String customSchema, boolean autoSchema) {
        if (StringUtils.hasText(customSchema)) {
            this.customSchema = customSchema;
            return true;
        }
        return false;
    }

    @Override
    public String buildInstruction() {
        return """
                CRITICAL INSTRUCTION: Respond with ONLY valid JSON matching this EXACT structure:
                %s
                
                STRICT RULES:
                - Follow the schema exactly
                - No markdown, no code blocks, no ```json notation
                - Just the raw JSON object
                - All field names must match the schema
                """.formatted(customSchema);
    }
}

