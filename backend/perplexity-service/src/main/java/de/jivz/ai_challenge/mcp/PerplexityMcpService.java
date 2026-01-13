/*
package de.jivz.ai_challenge.mcp;

import de.jivz.ai_challenge.exception.MCPExecutionException;
import de.jivz.ai_challenge.mcp.model.MCPExecuteRequest;
import de.jivz.ai_challenge.mcp.model.MCPToolResult;
import de.jivz.ai_challenge.mcp.model.PerplexityToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class PerplexityMcpService extends BaseMCPService{

    public PerplexityMcpService(@Qualifier("mcpPerplexityWebClient") WebClient webClient) {
        super(webClient, "perplexity");
    }

    @Override
    public MCPToolResult execute(String toolName, Map<String, Object> params) {
        log.info("Executing Perplexity tool: {} with params: {}", toolName, params);

        try {
            MCPExecuteRequest request = MCPExecuteRequest.builder()
                    .toolName(toolName)
                    .arguments(params)
                    .build();

            // Получаем нестандартный ответ
            PerplexityToolResult perplexityResult = webClient.post()
                    .uri("/api/execute")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(BodyInserters.fromValue(request))
                    .retrieve()
                    .bodyToMono(PerplexityToolResult.class)
                    .block();

            // Адаптируем к стандартному формату
            return adaptPerplexityResult(perplexityResult);

        } catch (Exception e) {
            log.error("Error executing Perplexity tool {}", toolName, e);
            throw new MCPExecutionException(
                    String.format("Failed to execute perplexity:%s", toolName),
                    e
            );
        }
    }

    */
/*

    private MCPToolResult adaptPerplexityResult(PerplexityToolResult perplexityResult) {
        Map<String, Object> metadata = new HashMap<>();

        if (perplexityResult.getResult() != null) {
            PerplexityToolResult.Result result = perplexityResult.getResult();

            // Добавляем дополнительные данные в metadata
            metadata.put("model", result.getModel());
            metadata.put("citations", result.getCitations());

            if (result.getUsage() != null) {
                metadata.put("usage", Map.of(
                        "promptTokens", result.getUsage().getPromptTokens(),
                        "completionTokens", result.getUsage().getCompletionTokens(),
                        "totalTokens", result.getUsage().getTotalTokens(),
                        "searchContextSize", result.getUsage().getSearchContextSize()
                ));

                if (result.getUsage().getCost() != null) {
                    metadata.put("cost", Map.of(
                            "inputTokensCost", result.getUsage().getCost().getInputTokensCost(),
                            "outputTokensCost", result.getUsage().getCost().getOutputTokensCost(),
                            "requestCost", result.getUsage().getCost().getRequestCost(),
                            "totalCost", result.getUsage().getCost().getTotalCost()
                    ));
                }
            }
        }

        return MCPToolResult.builder()
                .success(perplexityResult.isSuccess())
                .result(perplexityResult.getResult() != null ? perplexityResult.getResult().getAnswer() : null)
                .metadata(metadata)
                .build();
    }
}
