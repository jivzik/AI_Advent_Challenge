package de.jivz.ai_challenge.service.openrouter.model;


import lombok.Getter;

/**
 * Enum representing available OpenRouter AI models.
 */
@Getter
public enum OpenRouterModelEnum {

    GEMMA_3N_E4B_IT(
            "google/gemma-3n-e4b-it",
            "Google Gemma 3N E4B IT",
            "Google",
            4096
    ),

    MISTRAL_SMALL_24B(
            "mistralai/mistral-small-24b-instruct-2501",
            "Mistral Small 24B Instruct",
            "Mistral AI",
            32768
    ),

    GPT_5(
            "openai/gpt-5-2025-08-07",
            "GPT-5",
            "OpenAI",
            128000
    ),

    CLAUDE_SONNET_4(
            "anthropic/claude-sonnet-4",
            "Claude Sonnet 4",
            "Anthropic",
            200000
    ),

    GPT_5_1(
            "openai/gpt-5.1",
            "GPT-5.1",
            "OpenAI",
            128000
    );

    private final String modelId;
    private final String displayName;
    private final String provider;
    private final int maxContextTokens;

    OpenRouterModelEnum(String modelId, String displayName, String provider, int maxContextTokens) {
        this.modelId = modelId;
        this.displayName = displayName;
        this.provider = provider;
        this.maxContextTokens = maxContextTokens;
    }

    /**
     * Get model by ID.
     *
     * @param modelId the model identifier
     * @return the corresponding OpenRouterModel
     * @throws IllegalArgumentException if model not found
     */
    public static OpenRouterModelEnum fromModelId(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            throw new IllegalArgumentException("Model ID cannot be null or empty");
        }

        for (OpenRouterModelEnum model : values()) {
            if (model.modelId.equalsIgnoreCase(modelId)) {
                return model;
            }
        }

        throw new IllegalArgumentException("Unknown model ID: " + modelId);
    }

    /**
     * Check if a model ID is valid.
     *
     * @param modelId the model identifier to check
     * @return true if valid, false otherwise
     */
    public static boolean isValidModelId(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return false;
        }

        for (OpenRouterModelEnum model : values()) {
            if (model.modelId.equalsIgnoreCase(modelId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get all model IDs as an array.
     *
     * @return array of model IDs
     */
    public static String[] getAllModelIds() {
        OpenRouterModelEnum[] models = values();
        String[] ids = new String[models.length];
        for (int i = 0; i < models.length; i++) {
            ids[i] = models[i].modelId;
        }
        return ids;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - %s", displayName, provider, modelId);
    }
}