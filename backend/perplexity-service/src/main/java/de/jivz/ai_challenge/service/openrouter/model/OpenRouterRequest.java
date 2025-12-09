package de.jivz.ai_challenge.service.openrouter.model;

import java.util.List;

/**
 * Request DTO for OpenRouter API.
 */
public class OpenRouterRequest {

    private List<ChatMessage> messages;
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private Double topP;
    private List<Tool> tools;

    public OpenRouterRequest() {
    }

    public OpenRouterRequest(String model, List<ChatMessage> messages, Double temperature) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }


    public List<Tool> getTools() {
        return tools;
    }

    public void setTools(List<Tool> tools) {
        this.tools = tools;
    }



    /**
     * Chat message in the request.
     */
    public static class ChatMessage {
        private String role;
        private String content;

        public ChatMessage() {
        }

        public ChatMessage(String role, String content) {
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
     * Tool definition (optional).
     */
    public static class Tool {
        private String type;
        private String name;
        private String description;
        private Object parameters;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Object getParameters() {
            return parameters;
        }

        public void setParameters(Object parameters) {
            this.parameters = parameters;
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
        private List<ChatMessage> messages;
        private Double temperature;
        private Integer maxTokens;
        private Double topP;
        private List<Tool> tools;

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder messages(List<ChatMessage> messages) {
            this.messages = messages;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public Builder tools(List<Tool> tools) {
            this.tools = tools;
            return this;
        }

        public Builder addMessage(String role, String content) {
            if (this.messages == null) {
                this.messages = new java.util.ArrayList<>();
            }
            this.messages.add(new ChatMessage(role, content));
            return this;
        }

        public OpenRouterRequest build() {
            OpenRouterRequest request = new OpenRouterRequest(model, messages, temperature);
            request.setMaxTokens(maxTokens);
            request.setTopP(topP);
            request.setTools(tools);
            return request;
        }
    }
}

