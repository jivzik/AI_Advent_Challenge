package de.jivz.ai_challenge.exception;

public class MCPServerNotFoundException extends RuntimeException {
    public MCPServerNotFoundException(String serverName) {
        super("MCP Server not found: " + serverName);
    }
}