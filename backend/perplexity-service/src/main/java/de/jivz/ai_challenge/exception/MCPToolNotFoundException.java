package de.jivz.ai_challenge.exception;

public class MCPToolNotFoundException extends RuntimeException {
    public MCPToolNotFoundException(String toolName) {
        super("MCP Tool not found: " + toolName);
    }
}