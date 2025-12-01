package de.jivz.ai_challenge.service.perplexity.model;

import java.util.List;

/**
 * Response DTOs for Perplexity API.
 */
public class PerplexityResponse {

    private String id;
    private String model;
    private Long created;
    private List<Choice> choices;
    private Usage usage;
    private List<String> citations;
    private List<SearchResult> searchResults;
    private String object;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
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

    public List<String> getCitations() {
        return citations;
    }

    public void setCitations(List<String> citations) {
        this.citations = citations;
    }

    public List<SearchResult> getSearchResults() {
        return searchResults;
    }

    public void setSearchResults(List<SearchResult> searchResults) {
        this.searchResults = searchResults;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    /**
     * Choice in the response.
     */
    public static class Choice {
        private Integer index;
        private Message message;
        private Delta delta;
        private String finishReason;

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

        public Delta getDelta() {
            return delta;
        }

        public void setDelta(Delta delta) {
            this.delta = delta;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }
    }

    /**
     * Message in the choice.
     */
    public static class Message {
        private String role;
        private String content;

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
    }

    /**
     * Delta for streaming responses.
     */
    public static class Delta {
        private String role;
        private String content;

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
    }

    /**
     * Usage statistics.
     */
    public static class Usage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
        private String searchContextSize;
        private Cost cost;

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

        public String getSearchContextSize() {
            return searchContextSize;
        }

        public void setSearchContextSize(String searchContextSize) {
            this.searchContextSize = searchContextSize;
        }

        public Cost getCost() {
            return cost;
        }

        public void setCost(Cost cost) {
            this.cost = cost;
        }
    }

    /**
     * Cost information.
     */
    public static class Cost {
        private Double inputTokensCost;
        private Double outputTokensCost;
        private Double requestCost;
        private Double totalCost;

        public Double getInputTokensCost() {
            return inputTokensCost;
        }

        public void setInputTokensCost(Double inputTokensCost) {
            this.inputTokensCost = inputTokensCost;
        }

        public Double getOutputTokensCost() {
            return outputTokensCost;
        }

        public void setOutputTokensCost(Double outputTokensCost) {
            this.outputTokensCost = outputTokensCost;
        }

        public Double getRequestCost() {
            return requestCost;
        }

        public void setRequestCost(Double requestCost) {
            this.requestCost = requestCost;
        }

        public Double getTotalCost() {
            return totalCost;
        }

        public void setTotalCost(Double totalCost) {
            this.totalCost = totalCost;
        }
    }

    /**
     * Search result from Perplexity.
     */
    public static class SearchResult {
        private String title;
        private String url;
        private String date;
        private String lastUpdated;
        private String snippet;
        private String source;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getLastUpdated() {
            return lastUpdated;
        }

        public void setLastUpdated(String lastUpdated) {
            this.lastUpdated = lastUpdated;
        }

        public String getSnippet() {
            return snippet;
        }

        public void setSnippet(String snippet) {
            this.snippet = snippet;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }
}

