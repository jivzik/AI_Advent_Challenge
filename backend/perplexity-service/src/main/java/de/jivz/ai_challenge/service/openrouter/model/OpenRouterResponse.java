package de.jivz.ai_challenge.service.openrouter.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response DTO for OpenRouter API (Chat Completions format).
 */
public class OpenRouterResponse {

    private String id;
    private String provider;
    private String object;
    private Long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    /**
     * Choice in the response.
     */
    public static class Choice {
        private Integer index;
        private Message message;
        private Object logprobs;

        @JsonProperty("finish_reason")
        private String finishReason;

        @JsonProperty("native_finish_reason")
        private String nativeFinishReason;

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }

        public Object getLogprobs() {
            return logprobs;
        }

        public void setLogprobs(Object logprobs) {
            this.logprobs = logprobs;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }

        public String getNativeFinishReason() {
            return nativeFinishReason;
        }

        public void setNativeFinishReason(String nativeFinishReason) {
            this.nativeFinishReason = nativeFinishReason;
        }
    }

    /**
     * Message in the choice.
     */
    public static class Message {
        private String role;
        private String content;
        private String refusal;
        private Object reasoning;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getRefusal() {
            return refusal;
        }

        public void setRefusal(String refusal) {
            this.refusal = refusal;
        }

        public Object getReasoning() {
            return reasoning;
        }

        public void setReasoning(Object reasoning) {
            this.reasoning = reasoning;
        }
    }

    /**
     * Usage statistics.
     */
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

        public Integer getPromptTokens() {
            return promptTokens;
        }

        public void setPromptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
        }

        public Integer getCompletionTokens() {
            return completionTokens;
        }

        public void setCompletionTokens(Integer completionTokens) {
            this.completionTokens = completionTokens;
        }

        public Integer getTotalTokens() {
            return totalTokens;
        }

        public void setTotalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
        }

        public Double getCost() {
            return cost;
        }

        public void setCost(Double cost) {
            this.cost = cost;
        }

        public Boolean getIsByok() {
            return isByok;
        }

        public void setIsByok(Boolean isByok) {
            this.isByok = isByok;
        }

        public PromptTokensDetails getPromptTokensDetails() {
            return promptTokensDetails;
        }

        public void setPromptTokensDetails(PromptTokensDetails promptTokensDetails) {
            this.promptTokensDetails = promptTokensDetails;
        }

        public CostDetails getCostDetails() {
            return costDetails;
        }

        public void setCostDetails(CostDetails costDetails) {
            this.costDetails = costDetails;
        }

        public CompletionTokensDetails getCompletionTokensDetails() {
            return completionTokensDetails;
        }

        public void setCompletionTokensDetails(CompletionTokensDetails completionTokensDetails) {
            this.completionTokensDetails = completionTokensDetails;
        }
    }

    /**
     * Prompt tokens details.
     */
    public static class PromptTokensDetails {
        @JsonProperty("cached_tokens")
        private Integer cachedTokens;

        @JsonProperty("audio_tokens")
        private Integer audioTokens;

        @JsonProperty("video_tokens")
        private Integer videoTokens;

        public Integer getCachedTokens() {
            return cachedTokens;
        }

        public void setCachedTokens(Integer cachedTokens) {
            this.cachedTokens = cachedTokens;
        }

        public Integer getAudioTokens() {
            return audioTokens;
        }

        public void setAudioTokens(Integer audioTokens) {
            this.audioTokens = audioTokens;
        }

        public Integer getVideoTokens() {
            return videoTokens;
        }

        public void setVideoTokens(Integer videoTokens) {
            this.videoTokens = videoTokens;
        }
    }

    /**
     * Cost details.
     */
    public static class CostDetails {
        @JsonProperty("upstream_inference_cost")
        private Double upstreamInferenceCost;

        @JsonProperty("upstream_inference_prompt_cost")
        private Double upstreamInferencePromptCost;

        @JsonProperty("upstream_inference_completions_cost")
        private Double upstreamInferenceCompletionsCost;

        public Double getUpstreamInferenceCost() {
            return upstreamInferenceCost;
        }

        public void setUpstreamInferenceCost(Double upstreamInferenceCost) {
            this.upstreamInferenceCost = upstreamInferenceCost;
        }

        public Double getUpstreamInferencePromptCost() {
            return upstreamInferencePromptCost;
        }

        public void setUpstreamInferencePromptCost(Double upstreamInferencePromptCost) {
            this.upstreamInferencePromptCost = upstreamInferencePromptCost;
        }

        public Double getUpstreamInferenceCompletionsCost() {
            return upstreamInferenceCompletionsCost;
        }

        public void setUpstreamInferenceCompletionsCost(Double upstreamInferenceCompletionsCost) {
            this.upstreamInferenceCompletionsCost = upstreamInferenceCompletionsCost;
        }
    }

    /**
     * Completion tokens details.
     */
    public static class CompletionTokensDetails {
        @JsonProperty("reasoning_tokens")
        private Integer reasoningTokens;

        @JsonProperty("image_tokens")
        private Integer imageTokens;

        public Integer getReasoningTokens() {
            return reasoningTokens;
        }

        public void setReasoningTokens(Integer reasoningTokens) {
            this.reasoningTokens = reasoningTokens;
        }

        public Integer getImageTokens() {
            return imageTokens;
        }

        public void setImageTokens(Integer imageTokens) {
            this.imageTokens = imageTokens;
        }
    }
}