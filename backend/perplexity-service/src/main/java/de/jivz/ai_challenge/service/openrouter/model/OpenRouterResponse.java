package de.jivz.ai_challenge.service.openrouter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for OpenRouter API (Chat Completions format).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenRouterResponse {

    private String id;
    private String provider;
    private String object;
    private Long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    /**
     * Choice in the response.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Choice {
        private Integer index;
        private Message message;
        private Object logprobs;

        @JsonProperty("finish_reason")
        private String finishReason;

        @JsonProperty("native_finish_reason")
        private String nativeFinishReason;
    }

    /**
     * Message in the choice.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Message {
        private String role;
        private String content;
        private String refusal;
        private Object reasoning;
    }

    /**
     * Usage statistics.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        @JsonProperty("total_tokens")
        private Integer totalTokens;

        private Double cost;

        @JsonProperty("is_byok")
        private Boolean isByok;

        @JsonProperty("prompt_tokens_details")
        private PromptTokensDetails promptTokensDetails;

        @JsonProperty("cost_details")
        private CostDetails costDetails;

        @JsonProperty("completion_tokens_details")
        private CompletionTokensDetails completionTokensDetails;
    }

    /**
     * Prompt tokens details.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PromptTokensDetails {
        @JsonProperty("cached_tokens")
        private Integer cachedTokens;

        @JsonProperty("audio_tokens")
        private Integer audioTokens;

        @JsonProperty("video_tokens")
        private Integer videoTokens;
    }

    /**
     * Cost details.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CostDetails {
        @JsonProperty("upstream_inference_cost")
        private Double upstreamInferenceCost;

        @JsonProperty("upstream_inference_prompt_cost")
        private Double upstreamInferencePromptCost;

        @JsonProperty("upstream_inference_completions_cost")
        private Double upstreamInferenceCompletionsCost;
    }

    /**
     * Completion tokens details.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompletionTokensDetails {
        @JsonProperty("reasoning_tokens")
        private Integer reasoningTokens;

        @JsonProperty("image_tokens")
        private Integer imageTokens;
    }
}