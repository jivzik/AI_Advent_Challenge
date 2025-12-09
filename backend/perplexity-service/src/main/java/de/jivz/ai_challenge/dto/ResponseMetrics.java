package de.jivz.ai_challenge.dto;

/**
 * Response metrics containing token usage and cost information.
 */
public class ResponseMetrics {
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private Double cost;
    private Long responseTimeMs;
    private String model;
    private String provider;

    public ResponseMetrics() {
    }

    public ResponseMetrics(Integer inputTokens, Integer outputTokens, Integer totalTokens,
                          Double cost, Long responseTimeMs, String model, String provider) {
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalTokens = totalTokens;
        this.cost = cost;
        this.responseTimeMs = responseTimeMs;
        this.model = model;
        this.provider = provider;
    }

    public Integer getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(Integer inputTokens) {
        this.inputTokens = inputTokens;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(Integer outputTokens) {
        this.outputTokens = outputTokens;
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

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    @Override
    public String toString() {
        return "ResponseMetrics{" +
                "inputTokens=" + inputTokens +
                ", outputTokens=" + outputTokens +
                ", totalTokens=" + totalTokens +
                ", cost=" + cost +
                ", responseTimeMs=" + responseTimeMs +
                ", model='" + model + '\'' +
                ", provider='" + provider + '\'' +
                '}';
    }
}

