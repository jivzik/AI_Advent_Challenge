package de.jivz.ai_challenge.service.perplexity.model;

import java.util.List;

/**
 * Request DTO for Perplexity API.
 */
public class PerplexityRequest {

    private String model;
    private List<Message> messages;
    private Double temperature;

    public PerplexityRequest() {
    }

    public PerplexityRequest(String model, List<Message> messages) {
        this.model = model;
        this.messages = messages;
    }

    public PerplexityRequest(String model, List<Message> messages, Double temperature) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    /**
     * Message in the request.
     */
    public static class Message {
        private String role;
        private String content;

        public Message() {
        }

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

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
     * Builder for easier construction.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String model;
        private List<Message> messages;
        private Double temperature;

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder messages(List<Message> messages) {
            this.messages = messages;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder addMessage(String role, String content) {
            if (this.messages == null) {
                this.messages = new java.util.ArrayList<>();
            }
            this.messages.add(new Message(role, content));
            return this;
        }

        public PerplexityRequest build() {
            return new PerplexityRequest(model, messages, temperature);
        }
    }
}

