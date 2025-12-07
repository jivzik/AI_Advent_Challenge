package de.jivz.ai_challenge.dto;

import jakarta.validation.constraints.NotBlank;

public class ChatRequest {

    @NotBlank(message = "Message cannot be empty")
    private String message;
    private String userId;
    private String conversationId;
    private boolean jsonMode = false;
    private String jsonSchema; // Optional: Custom JSON schema for structured responses
    private boolean autoSchema = false; // Auto-generate JSON schema based on question
    private String systemPrompt; // Optional: System prompt to define agent personality

    public ChatRequest() {
    }

    public ChatRequest(String message, String userId) {
        this.message = message;
        this.userId = userId;
    }

    public ChatRequest(String message, String userId, String conversationId) {
        this.message = message;
        this.userId = userId;
        this.conversationId = conversationId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public boolean isJsonMode() {
        return jsonMode;
    }

    public void setJsonMode(boolean jsonMode) {
        this.jsonMode = jsonMode;
    }

    public String getJsonSchema() {
        return jsonSchema;
    }

    public void setJsonSchema(String jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    public boolean isAutoSchema() {
        return autoSchema;
    }

    public void setAutoSchema(boolean autoSchema) {
        this.autoSchema = autoSchema;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }
}

