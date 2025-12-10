package de.jivz.ai_challenge.service.perplexity;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.ai_challenge.dto.Message;
import de.jivz.ai_challenge.exception.ExternalServiceException;
import de.jivz.ai_challenge.service.CostCalculationService;
import de.jivz.ai_challenge.service.perplexity.model.PerplexityRequest;
import de.jivz.ai_challenge.service.perplexity.model.PerplexityResponse;
import de.jivz.ai_challenge.service.perplexity.model.PerplexityResponseWithMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Perplexity API client implementation.
 * Uses configured WebClient and model from PerplexityClientConfig.
 */
@Slf4j
@Component
public class PerplexityToolClient {

    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final int DEFAULT_MAX_TOKENS = 500;
    private static final double DEFAULT_TOP_P = 0.9;
    private static final int LOG_PREVIEW_LENGTH = 100;

    private final WebClient webClient;
    private final String defaultModel;
    private final CostCalculationService costCalculationService;
    private final ObjectMapper objectMapper;

    public PerplexityToolClient(
            @Qualifier("perplexityWebClient") WebClient webClient,
            @Qualifier("perplexityModel") String defaultModel,
            CostCalculationService costCalculationService,
            ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.defaultModel = defaultModel;
        this.costCalculationService = costCalculationService;
        this.objectMapper = objectMapper;
        log.info("PerplexityToolClient initialized with model: {}", defaultModel);
    }

    /**
     * Requests a chat completion with full metrics.
     */
    public PerplexityResponseWithMetrics requestCompletionWithMetrics(
            List<Message> messages, Double temperature, String model) {
        validateMessages(messages);

        PerplexityRequest request = buildRequest(messages, temperature, model);
        log.info("Calling Perplexity API with metrics - model: {}, temperature: {}, messages: {}",
                resolveModel(model), temperature, messages.size());

        return executeRequest(request);
    }


    private void validateMessages(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("Messages list cannot be null or empty");
        }
    }

    private PerplexityRequest buildRequest(List<Message> messages, Double temperature, String model) {
        List<PerplexityRequest.Message> perplexityMessages = messages.stream()
                .map(msg -> new PerplexityRequest.Message(msg.getRole(), msg.getContent()))
                .collect(Collectors.toList());

        return PerplexityRequest.builder()
                .model(resolveModel(model))
                .messages(perplexityMessages)
                .temperature(temperature != null ? temperature : DEFAULT_TEMPERATURE)
                .maxTokens(DEFAULT_MAX_TOKENS)
                //.topP(DEFAULT_TOP_P)
                .build();
    }

    private String resolveModel(String model) {
        return (model != null && !model.isBlank()) ? model : this.defaultModel;
    }

    private PerplexityResponseWithMetrics executeRequest(PerplexityRequest request) {
        try {
            logRequest(request);

            long startTime = System.nanoTime();
            PerplexityResponse response = callApi(request);
            long responseTimeMs = (System.nanoTime() - startTime) / 1_000_000;

            log.info("Received response from Perplexity API in {} ms", responseTimeMs);

            return extractResponseWithMetrics(response, responseTimeMs);

        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to execute Perplexity API request: {}", e.getMessage(), e);
            throw new ExternalServiceException("Failed to execute Perplexity API request: " + e.getMessage(), e);
        }
    }

    private void logRequest(PerplexityRequest request) {
        try {
            log.debug("Sending request to Perplexity API");
            log.trace("Request payload: {}", objectMapper.writeValueAsString(request));
        } catch (Exception e) {
            log.warn("Failed to serialize request for logging: {}", e.getMessage());
        }
    }

    private PerplexityResponse callApi(PerplexityRequest request) {
        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Perplexity API client error: {} - Body: {}",
                                            clientResponse.statusCode(), errorBody);
                                    return Mono.error(new ExternalServiceException(
                                            "Perplexity API client error: " + clientResponse.statusCode() +
                                                    " - " + errorBody));
                                })
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("Perplexity API server error: {} - Body: {}",
                                            clientResponse.statusCode(), errorBody);
                                    return Mono.error(new ExternalServiceException(
                                            "Perplexity API server error: " + clientResponse.statusCode() +
                                                    " - " + errorBody));
                                })
                )
                .bodyToMono(PerplexityResponse.class)
                .doOnError(error -> {
                    if (error.getMessage() != null && error.getMessage().contains("JSON decoding error")) {
                        log.error("JSON parsing failed. This may happen if the response was truncated due to token limit.");
                        log.error("Consider reducing maxTokens or handling streaming responses.");
                    }
                })
                .block();
    }

    private PerplexityResponseWithMetrics extractResponseWithMetrics(
            PerplexityResponse response, long responseTimeMs) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            throw new ExternalServiceException("Empty or invalid response from Perplexity API");
        }

        PerplexityResponse.Choice firstChoice = response.getChoices().getFirst();
        if (firstChoice.getMessage() == null || firstChoice.getMessage().getContent() == null) {
            throw new ExternalServiceException("Response missing message content");
        }

        String reply = firstChoice.getMessage().getContent();
        String finishReason = firstChoice.getFinishReason();

        // Check if response was truncated due to token limit
        if ("length".equals(finishReason)) {
            log.warn("Response was truncated due to maxTokens limit. Consider increasing maxTokens or handling this case.");
            log.warn("Finish reason: {}", finishReason);
        }

        logReplyPreview(reply);

        // Log citations if available
        if (response.getCitations() != null && !response.getCitations().isEmpty()) {
            log.info("Citations: {} sources", response.getCitations().size());
        }

        PerplexityResponse.Usage usage = response.getUsage();
        Integer inputTokens = null;
        Integer outputTokens = null;
        Integer totalTokens = null;
        Double cost = null;

        if (usage != null) {
            inputTokens = usage.getPromptTokens();
            outputTokens = usage.getCompletionTokens();
            totalTokens = usage.getTotalTokens();
            if (inputTokens != null && outputTokens != null) {
                cost = Objects.requireNonNull(calculateAndLogCost(response.getModel(), inputTokens, outputTokens)).getTotalCost();
            }

            logUsageMetrics(response.getModel(), inputTokens, outputTokens, totalTokens, cost);

            // Log if we hit the token limit
            if (outputTokens != null && outputTokens >= DEFAULT_MAX_TOKENS * 0.95) {
                log.warn("Output tokens ({}) are close to or at the maxTokens limit ({})",
                        outputTokens, DEFAULT_MAX_TOKENS);
            }
        }

        return PerplexityResponseWithMetrics.builder()
                .reply(reply)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .totalTokens(totalTokens)
                .cost(cost)
                .responseTimeMs(responseTimeMs)
                .model(response.getModel())
                //.citations(response.getCitations())
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

    private CostCalculationService.CostBreakdown calculateAndLogCost(String model, Integer inputTokens, Integer outputTokens) {
        try {
            CostCalculationService.CostBreakdown costBreakdown =
                    costCalculationService.calculateCost(model, inputTokens, outputTokens);

            if (costBreakdown != null) {
                log.info("Calculated cost: {}", costBreakdown.getFormattedString());
            } else {
                log.warn("Unable to calculate cost - pricing not configured for model: {}", model);
            }
            return costBreakdown;
        } catch (Exception e) {
            log.warn("Failed to calculate cost: {}", e.getMessage());
        }
        return null;
    }
}