package de.jivz.ai_challenge.service.strategy;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Strategy for custom JSON schema instructions.
 * Used when user provides a specific JSON schema (but NOT special modes like nutritionist).
 */
@Component
public class CustomSchemaInstructionStrategy implements JsonInstructionStrategy {

    private static final String NUTRITIONIST_MARKER = "nutritionist_mode";
    private static final String META_PROMPT_MARKER = "meta_prompt";
    private String customSchema;

    @Override
    public boolean canHandle(String customSchema, boolean autoSchema) {
        // Don't handle special modes (they have their own strategies)
        if (customSchema != null && (customSchema.contains(NUTRITIONIST_MARKER) || customSchema.contains(META_PROMPT_MARKER))) {
            return false;
        }

        // Handle only if custom schema is provided and not auto-schema mode
        if (StringUtils.hasText(customSchema) && !autoSchema) {
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

