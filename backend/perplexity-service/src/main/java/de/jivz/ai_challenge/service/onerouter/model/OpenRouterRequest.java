package de.jivz.ai_challenge.service.onerouter.model;

import java.util.List;

public record OpenRouterRequest(
        String model,
        List<OpenRouterMessage> messages,
        Double temperature,
        Integer max_tokens
) {}