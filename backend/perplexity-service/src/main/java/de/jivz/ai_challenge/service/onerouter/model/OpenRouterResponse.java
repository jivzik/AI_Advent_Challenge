package de.jivz.ai_challenge.service.onerouter.model;

import java.util.List;

/**
 * OpenRouter API response record.
 */
public record OpenRouterResponse(
        String id,
        String model,
        List<OpenRouterChoice> choices
) {}

