package de.jivz.ai_challenge.dto;

import jakarta.validation.constraints.NotBlank;

public class ChatRequest {

    @NotBlank(message = "Message cannot be empty")
    private String message;
    private String userId;
    private String conversationId;

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
}

