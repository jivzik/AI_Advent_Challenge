package de.jivz.ai_challenge.mcp.model;


import org.springframework.stereotype.Component;

@Component
public class McpMapper {


    public McpDto.ToolExecutionResponse perplexityToolResultToToolExecutionResponse(McpDto.PerplexityToolResult perplexityToolResult) {
        return McpDto.ToolExecutionResponse.builder()
                .success(perplexityToolResult.isSuccess())
                .toolName(perplexityToolResult.getTool())
                .result(perplexityToolResult.getResult().getAnswer())
                .build();
    }
}