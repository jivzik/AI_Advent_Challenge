package de.jivz.ai_challenge.service.openrouter;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.ai_challenge.dto.Message;
import de.jivz.ai_challenge.exception.ExternalServiceException;
import de.jivz.ai_challenge.service.CostCalculationService;
import de.jivz.ai_challenge.service.openrouter.model.OpenRouterRequest;
import de.jivz.ai_challenge.service.openrouter.model.OpenRouterResponse;
import de.jivz.ai_challenge.service.openrouter.model.OpenRouterResponseWithMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * OpenRouter API client implementation.
 * Uses configured WebClient and model from OpenRouterClientConfig.
 */
@Slf4j
@Component
public class OpenRouterToolClient {

    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final int DEFAULT_MAX_TOKENS = 1000;
    private static final double DEFAULT_TOP_P = 0.9;
    private static final int LOG_PREVIEW_LENGTH = 100;

    private final WebClient webClient;
    private final String defaultModel;
    private final CostCalculationService costCalculationService;
    private final ObjectMapper objectMapper;

    public OpenRouterToolClient(
            @Qualifier("openRouterWebClient") WebClient webClient,
            @Qualifier("openRouterModel") String defaultModel,
            CostCalculationService costCalculationService,
            ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.defaultModel = defaultModel;
        this.costCalculationService = costCalculationService;
        this.objectMapper = objectMapper;
        log.info("OpenRouterToolClient initialized with model: {}", defaultModel);
    }

    /**
     * Requests a chat completion with full metrics.
     */
    public OpenRouterResponseWithMetrics requestCompletionWithMetrics(
            List<Message> messages, Double temperature, String model) {
        return requestCompletionWithMetrics(messages, temperature, model, null);
    }

    /**
     * Requests a chat completion with full metrics and optional tools.
     */
    public OpenRouterResponseWithMetrics requestCompletionWithMetrics(
            List<Message> messages, Double temperature, String model, List<OpenRouterRequest.Tool> tools) {
        validateMessages(messages);

        OpenRouterRequest request = buildRequest(messages, temperature, model, tools);
        log.info("Calling OpenRouter API with metrics - model: {}, temperature: {}, messages: {}, tools: {}",
                resolveModel(model), temperature, messages.size(), tools != null ? tools.size() : 0);

        return executeRequest(request);
    }

    private void validateMessages(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("Messages list cannot be null or empty");
        }
    }

    private OpenRouterRequest buildRequest(List<Message> messages, Double temperature, String model) {
        return buildRequest(messages, temperature, model, null);
    }

    private OpenRouterRequest buildRequest(List<Message> messages, Double temperature, String model, List<OpenRouterRequest.Tool> tools) {
        List<OpenRouterRequest.ChatMessage> chatMessages = messages.stream()
                .map(msg -> OpenRouterRequest.ChatMessage.builder()
                        .role(msg.getRole())
                        .content(msg.getContent())
                        .build())
                .collect(Collectors.toList());

        OpenRouterRequest.OpenRouterRequestBuilder builder = OpenRouterRequest.builder()
                .model(resolveModel(model))
                .messages(chatMessages)
                .temperature(temperature != null ? temperature : DEFAULT_TEMPERATURE)
                .maxTokens(DEFAULT_MAX_TOKENS)
                .topP(DEFAULT_TOP_P);

        if (tools != null && !tools.isEmpty()) {
            builder.tools(tools);
        }

        return builder.build();
    }

    private String resolveModel(String model) {
        return (model != null && !model.isBlank()) ? model : this.defaultModel;
    }

    private OpenRouterResponseWithMetrics executeRequest(OpenRouterRequest request) {
        try {
            logRequest(request);

            long startTime = System.nanoTime();
            OpenRouterResponse response = callApi(request);
            long responseTimeMs = (System.nanoTime() - startTime) / 1_000_000;

            log.info("Received response from OpenRouter API in {} ms", responseTimeMs);

            return extractResponseWithMetrics(response, responseTimeMs);

        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to execute OpenRouter API request: {}", e.getMessage(), e);
            throw new ExternalServiceException("Failed to execute OpenRouter API request: " + e.getMessage(), e);
        }
    }

    private void logRequest(OpenRouterRequest request) {
        try {
            log.debug("Sending request to OpenRouter API");
            log.trace("Request payload: {}", objectMapper.writeValueAsString(request));
        } catch (Exception e) {
            log.warn("Failed to serialize request for logging: {}", e.getMessage());
        }
    }

    private OpenRouterResponse callApi(OpenRouterRequest request) {
        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("OpenRouter API client error: {} - Body: {}",
                                            clientResponse.statusCode(), errorBody);
                                    return Mono.error(new ExternalServiceException(
                                            "OpenRouter API client error: " + clientResponse.statusCode() +
                                                    " - " + errorBody));
                                })
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("OpenRouter API server error: {} - Body: {}",
                                            clientResponse.statusCode(), errorBody);
                                    return Mono.error(new ExternalServiceException(
                                            "OpenRouter API server error: " + clientResponse.statusCode() +
                                                    " - " + errorBody));
                                })
                )
                .bodyToMono(OpenRouterResponse.class)
                .doOnError(error -> {
                    if (error.getMessage() != null && error.getMessage().contains("JSON decoding error")) {
                        log.error("JSON parsing failed. This may happen if the response was truncated due to token limit.");
                        log.error("Consider reducing maxTokens or handling streaming responses.");
                    }
                })
                .block();
    }

    private OpenRouterResponseWithMetrics extractResponseWithMetrics(
            OpenRouterResponse response, long responseTimeMs) {

        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new ExternalServiceException("Empty or invalid response from OpenRouter API");
        }

        OpenRouterResponse.Choice firstChoice = response.getChoices().getFirst();
        OpenRouterResponse.Message message = firstChoice.getMessage();

        if (message == null) {
            throw new ExternalServiceException("Response missing message");
        }

        // Content kann null sein wenn Tool-Calls vorhanden sind
        String reply = message.getContent() != null ? message.getContent() : "";
        String finishReason = firstChoice.getFinishReason();

        // Check if response was truncated due to token limit
        if ("length".equals(finishReason)) {
            log.warn("Response was truncated due to maxTokens limit. Consider increasing maxTokens or handling this case.");
            log.warn("Finish reason: {}", finishReason);
        }

        // Log tool calls if present
        if (message.getToolCalls() != null && !message.getToolCalls().isEmpty()) {
            log.info("🔧 Response contains {} tool calls", message.getToolCalls().size());
        }

        if (!reply.isEmpty()) {
            logReplyPreview(reply);
        }

        OpenRouterResponse.Usage usage = response.getUsage();
        Integer inputTokens = null;
        Integer outputTokens = null;
        Integer totalTokens = null;
        Double cost = null;

        if (usage != null) {
            inputTokens = usage.getPromptTokens();
            outputTokens = usage.getCompletionTokens();
            totalTokens = usage.getTotalTokens();
            cost = usage.getCost();

            logUsageMetrics(response.getModel(), inputTokens, outputTokens, totalTokens, cost);

            // Log if we hit the token limit
            if (outputTokens != null && outputTokens >= DEFAULT_MAX_TOKENS * 0.95) {
                log.warn("Output tokens ({}) are close to or at the maxTokens limit ({})",
                        outputTokens, DEFAULT_MAX_TOKENS);
            }
        }

        return OpenRouterResponseWithMetrics.builder()
                .reply(reply)
                .message(message)
                .finishReason(finishReason)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .totalTokens(totalTokens)
                .cost(cost)
                .responseTimeMs(responseTimeMs)
                .model(response.getModel())
                .build();
    }

    private void logReplyPreview(String reply) {
        if (reply.length() > LOG_PREVIEW_LENGTH) {
            log.debug("Reply preview: {}...", reply.substring(0, LOG_PREVIEW_LENGTH));
        } else {
            log.debug("Reply: {}", reply);
        }
    }

    private void logUsageMetrics(String model, Integer inputTokens,
                                 Integer outputTokens, Integer totalTokens, Double cost) {
        log.info("Tokens - Input: {}, Output: {}, Total: {}", inputTokens, outputTokens, totalTokens);
        log.info("Cost from API: ${}", cost);

        if (inputTokens != null && outputTokens != null) {
            calculateAndLogCost(model, inputTokens, outputTokens);
        }
    }

    private void calculateAndLogCost(String model, Integer inputTokens, Integer outputTokens) {
        try {
            CostCalculationService.CostBreakdown costBreakdown =
                    costCalculationService.calculateCost(model, inputTokens, outputTokens);

            if (costBreakdown != null) {
                log.info("Calculated cost: {}", costBreakdown.getFormattedString());
            } else {
                log.warn("Unable to calculate cost - pricing not configured for model: {}", model);
            }
        } catch (Exception e) {
            log.warn("Failed to calculate cost: {}", e.getMessage());
        }
    }
}