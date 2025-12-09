package de.jivz.ai_challenge.dto;

import java.time.Instant;

public class ChatResponse {
    private String reply;
    private String toolName;
    private Instant timestamp;
    private ResponseMetrics metrics;

    public ChatResponse() {
    }

    public ChatResponse(String reply, String toolName, Instant timestamp) {
        this.reply = reply;
        this.toolName = toolName;
        this.timestamp = timestamp;
    }

    public ChatResponse(String reply, String toolName, Instant timestamp, ResponseMetrics metrics) {
        this.reply = reply;
        this.toolName = toolName;
        this.timestamp = timestamp;
        this.metrics = metrics;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public ResponseMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(ResponseMetrics metrics) {
        this.metrics = metrics;
    }
}
