package de.jivz.ai_challenge.configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for model pricing.
 * Prices are per 1 million tokens (1M) for both input and output.
 *
 * Formula:
 * costInput = inputTokens * priceInputPerMillion / 1_000_000
 * costOutput = outputTokens * priceOutputPerMillion / 1_000_000
 * totalCost = costInput + costOutput
 */
public class ModelPricingConfig {

    /**
     * Model pricing map: modelName -> ModelPricing
     */
    private static final Map<String, ModelPricing> PRICING_MAP = new HashMap<>();

    static {
        // OpenRouter Models
        // Anthropic Claude models
        PRICING_MAP.put("anthropic/claude-opus", new ModelPricing(15.00, 75.00));
        PRICING_MAP.put("anthropic/claude-opus-4", new ModelPricing(15.00, 75.00));
        PRICING_MAP.put("anthropic/claude-3.5-sonnet", new ModelPricing(3.00, 15.00));
        PRICING_MAP.put("anthropic/claude-3-sonnet", new ModelPricing(3.00, 15.00));
        PRICING_MAP.put("anthropic/claude-3-haiku", new ModelPricing(0.80, 4.00));

        // OpenAI Models
        PRICING_MAP.put("openai/gpt-4-turbo", new ModelPricing(10.00, 30.00));
        PRICING_MAP.put("openai/gpt-4", new ModelPricing(30.00, 60.00));
        PRICING_MAP.put("openai/gpt-4o", new ModelPricing(5.00, 15.00));
        PRICING_MAP.put("openai/gpt-3.5-turbo", new ModelPricing(0.50, 1.50));

        // Google Gemini Models
        PRICING_MAP.put("google/gemma-3n-e4b-it", new ModelPricing(0.20, 0.20));
        PRICING_MAP.put("google/gemini-pro", new ModelPricing(0.50, 1.50));
        PRICING_MAP.put("google/gemini-1.5-pro", new ModelPricing(3.50, 10.50));

        // Mistral Models
        PRICING_MAP.put("mistralai/mistral-large", new ModelPricing(8.00, 24.00));
        PRICING_MAP.put("mistralai/mistral-medium", new ModelPricing(2.70, 8.10));
        PRICING_MAP.put("mistralai/mistral-small-24b-instruct-2501", new ModelPricing(0.14, 0.42));

        // Meta Llama Models
        PRICING_MAP.put("meta-llama/llama-3-70b-instruct", new ModelPricing(0.59, 0.79));
        PRICING_MAP.put("meta-llama/llama-2-70b-chat", new ModelPricing(0.70, 0.90));

        // Perplexity Models
        PRICING_MAP.put("perplexity/pplx-7b-online", new ModelPricing(0.07, 0.07));
        PRICING_MAP.put("perplexity/pplx-70b-online", new ModelPricing(0.75, 0.90));
        PRICING_MAP.put("perplexity/pplx-70b-chat", new ModelPricing(0.75, 1.00));

        PRICING_MAP.put("sonar", new ModelPricing(3, 15));
        PRICING_MAP.put("sonar-pro", new ModelPricing(3, 15));
    }

    /**
     * Get pricing information for a model.
     *
     * @param modelName the model name (e.g., "anthropic/claude-3.5-sonnet")
     * @return ModelPricing with input and output prices per million tokens,
     *         or null if model is not found
     */
    public static ModelPricing getPricing(String modelName) {
        if (modelName == null) {
            return null;
        }
        return PRICING_MAP.get(modelName);
    }

    /**
     * Check if pricing is available for a model.
     *
     * @param modelName the model name
     * @return true if pricing is configured, false otherwise
     */
    public static boolean hasPricing(String modelName) {
        return modelName != null && PRICING_MAP.containsKey(modelName);
    }

    /**
     * Get all configured models.
     *
     * @return a copy of the pricing map
     */
    public static Map<String, ModelPricing> getAllPricing() {
        return new HashMap<>(PRICING_MAP);
    }

    /**
     * Pricing information for a model.
     */
    public static class ModelPricing {
        private final double inputPricePerMillion;
        private final double outputPricePerMillion;

        public ModelPricing(double inputPricePerMillion, double outputPricePerMillion) {
            this.inputPricePerMillion = inputPricePerMillion;
            this.outputPricePerMillion = outputPricePerMillion;
        }

        public double getInputPricePerMillion() {
            return inputPricePerMillion;
        }

        public double getOutputPricePerMillion() {
            return outputPricePerMillion;
        }

        @Override
        public String toString() {
            return String.format("Input: $%.2f/1M, Output: $%.2f/1M",
                    inputPricePerMillion, outputPricePerMillion);
        }
    }
}

