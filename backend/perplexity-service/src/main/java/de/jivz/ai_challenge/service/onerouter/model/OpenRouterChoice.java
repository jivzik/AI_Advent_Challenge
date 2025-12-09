package de.jivz.ai_challenge.service.onerouter.model;

public record OpenRouterChoice(
        OpenRouterChoiceMessage message,
        Integer index,
        String finish_reason
) {}