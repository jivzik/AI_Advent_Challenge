package de.jivz.ai_challenge.service;

import de.jivz.ai_challenge.config.ModelPricingConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for calculating API request costs based on token usage.
 *
 * Uses pricing information from ModelPricingConfig to calculate costs
 * based on the formula:
 * - costInput = inputTokens * priceInputPerMillion / 1_000_000
 * - costOutput = outputTokens * priceOutputPerMillion / 1_000_000
 * - totalCost = costInput + costOutput
 */
@Slf4j
@Service
public class CostCalculationService {

    /**
     * Calculate the cost of an API call.
     *
     * @param modelName the model used (e.g., "anthropic/claude-3.5-sonnet")
     * @param inputTokens number of input tokens
     * @param outputTokens number of output tokens
     * @return CostBreakdown with detailed cost information,
     *         or null if model pricing is not found
     */
    public CostBreakdown calculateCost(String modelName, int inputTokens, int outputTokens) {
        ModelPricingConfig.ModelPricing pricing = ModelPricingConfig.getPricing(modelName);

        if (pricing == null) {
            log.warn("⚠️ Pricing not found for model: {}", modelName);
            return null;
        }

        // Calculate costs
        double inputCost = (inputTokens * pricing.getInputPricePerMillion()) / 1_000_000;
        double outputCost = (outputTokens * pricing.getOutputPricePerMillion()) / 1_000_000;
        double totalCost = inputCost + outputCost;

        log.debug("💰 Cost calculation for model '{}': input_tokens={}, output_tokens={}",
                modelName, inputTokens, outputTokens);
        log.debug("💵 Costs: input=${:.6f}, output=${:.6f}, total=${:.6f}",
                inputCost, outputCost, totalCost);

        return new CostBreakdown(modelName, inputTokens, outputTokens,
                pricing.getInputPricePerMillion(), pricing.getOutputPricePerMillion(),
                inputCost, outputCost, totalCost);
    }

    /**
     * Calculate the cost using only the total cost (when detailed breakdown is unavailable).
     *
     * @param modelName the model used
     * @param totalCost the total cost from API response
     * @return CostBreakdown with only total cost information
     */
    public CostBreakdown calculateCostFromTotal(String modelName, double totalCost) {
        ModelPricingConfig.ModelPricing pricing = ModelPricingConfig.getPricing(modelName);

        if (pricing == null) {
            log.warn("⚠️ Pricing not found for model: {}", modelName);
            return null;
        }

        log.debug("💵 Using provided total cost for model '{}': ${}", modelName, String.format("%.6f", totalCost));

        return new CostBreakdown(modelName, 0, 0,
                pricing.getInputPricePerMillion(), pricing.getOutputPricePerMillion(),
                0.0, 0.0, totalCost);
    }

    /**
     * Detailed cost breakdown information.
     */
    public static class CostBreakdown {
        private final String modelName;
        private final int inputTokens;
        private final int outputTokens;
        private final double inputPricePerMillion;
        private final double outputPricePerMillion;
        private final double inputCost;
        private final double outputCost;
        private final double totalCost;

        public CostBreakdown(String modelName, int inputTokens, int outputTokens,
                           double inputPricePerMillion, double outputPricePerMillion,
                           double inputCost, double outputCost, double totalCost) {
            this.modelName = modelName;
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            this.inputPricePerMillion = inputPricePerMillion;
            this.outputPricePerMillion = outputPricePerMillion;
            this.inputCost = inputCost;
            this.outputCost = outputCost;
            this.totalCost = totalCost;
        }

        public String getModelName() {
            return modelName;
        }

        public int getInputTokens() {
            return inputTokens;
        }

        public int getOutputTokens() {
            return outputTokens;
        }

        public double getInputPricePerMillion() {
            return inputPricePerMillion;
        }

        public double getOutputPricePerMillion() {
            return outputPricePerMillion;
        }

        public double getInputCost() {
            return inputCost;
        }

        public double getOutputCost() {
            return outputCost;
        }

        public double getTotalCost() {
            return totalCost;
        }

        @Override
        public String toString() {
            return String.format(
                    "CostBreakdown{model='%s', inputTokens=%d, outputTokens=%d, " +
                    "inputCost=$%.6f, outputCost=$%.6f, totalCost=$%.6f}",
                    modelName, inputTokens, outputTokens, inputCost, outputCost, totalCost);
        }

        /**
         * Get a formatted string representation of the cost breakdown.
         *
         * @return formatted string with cost details
         */
        public String getFormattedString() {
            return String.format(
                    "💰 Tokens: Input=%d (priced at $%.2f/1M), Output=%d (priced at $%.2f/1M) " +
                    "| 💵 Costs: Input=$%.6f, Output=$%.6f, Total=$%.6f",
                    inputTokens, inputPricePerMillion,
                    outputTokens, outputPricePerMillion,
                    inputCost, outputCost, totalCost);
        }
    }
}

