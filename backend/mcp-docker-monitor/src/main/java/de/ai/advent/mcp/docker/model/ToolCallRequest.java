package de.ai.advent.mcp.docker.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Tool Call Request from Perplexity
 */
@Setter
@Getter
public class ToolCallRequest {
    // Getters and Setters
    private String toolName;
    private Map<String, Object> arguments;

    public ToolCallRequest() {}

    public ToolCallRequest(String name, Map<String, Object> arguments) {
        this.toolName = name;
        this.arguments = arguments;
    }

    @Override
    public String toString() {
        return "ToolCallRequest{" +
                "name='" + toolName + '\'' +
                ", arguments=" + arguments +
                '}';
    }
}

