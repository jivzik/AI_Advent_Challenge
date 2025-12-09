package de.jivz.ai_challenge.service.openrouter;

import de.jivz.ai_challenge.dto.Message;
import de.jivz.ai_challenge.exception.ExternalServiceException;
import de.jivz.ai_challenge.service.CostCalculationService;
import de.jivz.ai_challenge.service.openrouter.model.OpenRouterRequest;
import de.jivz.ai_challenge.service.openrouter.model.OpenRouterResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.stream.Collectors;


/**
 * OpenRouter API client implementation.
 * Uses configured WebClient and model from OpenRouterClientConfig.
 */
@Slf4j
@Component
public class OpenRouterToolClient {

    private final WebClient webClient;
    private final String model;
    private final CostCalculationService costCalculationService;
    ObjectMapper objectMapper;

    public OpenRouterToolClient(
            @Qualifier("openRouterWebClient") WebClient webClient,
            @Qualifier("openRouterModel") String model,
            ObjectMapper objectMapper,
            CostCalculationService costCalculationService) {
        this.webClient = webClient;
        this.model = model;
        this.objectMapper = objectMapper ;
        this.costCalculationService = costCalculationService;
        log.info("✅ OpenRouterToolClient initialized with WebClient and model: {}", model);
    }

    /**
     * Requests a chat completion from OpenRouter API with conversation history.
     *
     * @param messages the conversation history (list of user and assistant messages)
     * @param temperature the temperature parameter for response generation (0.0 - 2.0)
     * @param model the specific model to use (if null, uses default)
     * @return the AI's response
     * @throws ExternalServiceException if the API call fails
     */
    public String requestCompletion(List<Message> messages, Double temperature, String model) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("Messages list cannot be empty");
        }

        try {
            // Convert our Message DTOs to OpenRouter ChatMessage
            List<OpenRouterRequest.ChatMessage> chatMessages = messages.stream()
                    .map(msg -> new OpenRouterRequest.ChatMessage(msg.getRole(), msg.getContent()))
                    .collect(Collectors.toList());

            // Use provided model or default
            String modelToUse = (model != null && !model.isBlank()) ? model : this.model;

            // Build request with builder pattern
            OpenRouterRequest request = OpenRouterRequest.builder()
                    .model(modelToUse)
                    .messages(chatMessages)
                    .temperature(temperature)
                    .maxTokens(4000)
                    .topP(0.9)
                    .build();

            log.info("🚀 Calling OpenRouter API with model: {}, temperature: {} and {} messages",
                    modelToUse, temperature, messages.size());
            log.debug("📝 Conversation history: {} messages", messages.size());

            return executeRequest(request);
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Exception in requestCompletion: {}", e.getMessage(), e);
            throw new ExternalServiceException("Failed to call OpenRouter API: " + e.getMessage(), e);
        }
    }

    /**
     * Requests a chat completion from OpenRouter API with conversation history.
     *
     * @param messages the conversation history (list of user and assistant messages)
     * @param temperature the temperature parameter for response generation (0.0 - 2.0)
     * @return the AI's response
     * @throws ExternalServiceException if the API call fails
     */
    public String requestCompletion(List<Message> messages, Double temperature) {
        return requestCompletion(messages, temperature, null);
    }

    /**
     * Requests a chat completion from OpenRouter API with conversation history and default temperature.
     *
     * @param messages the conversation history (list of user and assistant messages)
     * @return the AI's response
     * @throws ExternalServiceException if the API call fails
     */
    public String requestCompletion(List<Message> messages) {
        return requestCompletion(messages, 0.7); // Default temperature
    }

    /**
     * Requests a chat completion from OpenRouter API.
     *
     * @param input the user's message
     * @return the AI's response
     * @throws ExternalServiceException if the API call fails
     */
    public String requestCompletion(String input) {
        try {
            // Build request with builder pattern
            OpenRouterRequest request = OpenRouterRequest.builder()
                    .model(model)
                    .addMessage("user", input)
                    .temperature(0.7)
                    .maxTokens(4000)
                    .topP(0.9)
                    .build();

            log.info("🚀 Calling OpenRouter API with model: {}", model);
            log.debug("📝 User message: {}", input);

            return executeRequest(request);
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Exception in requestCompletion: {}", e.getMessage(), e);
            throw new ExternalServiceException("Failed to call OpenRouter API: " + e.getMessage(), e);
        }
    }

    /**
     * Executes the API request to OpenRouter.
     *
     * @param request the OpenRouter request
     * @return the AI's response
     */
    private String executeRequest(OpenRouterRequest request) {
        try {
            log.info ("📨 Sending request to OpenRouter API...");
            log.info(objectMapper.writeValueAsString(request));

            // Measure response time
            long start = System.nanoTime();

            // WebClient with type-safe OpenRouterResponse
            OpenRouterResponse response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError(),
                            clientResponse -> {
                                log.error("❌ 4xx Error: {}", clientResponse.statusCode());
                                return Mono.error(new ExternalServiceException(
                                        "OpenRouter API client error: " + clientResponse.statusCode()));
                            }
                    )
                    .onStatus(
                            status -> status.is5xxServerError(),
                            clientResponse -> {
                                log.error("❌ 5xx Error: {}", clientResponse.statusCode());
                                return Mono.error(new ExternalServiceException(
                                        "OpenRouter API server error: " + clientResponse.statusCode()));
                            }
                    )
                    .bodyToMono(OpenRouterResponse.class)
                    .block(); // Blocking for synchronous use

            long end = System.nanoTime();
            long responseTimeMs = (end - start) / 1_000_000;

            log.info("✅ Received response from OpenRouter API {}", response);
            log.info("⏱️ Response time: {} ms", responseTimeMs);

            // Extract answer from choices[0].message.content (Chat Completions format)
           if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                OpenRouterResponse.Choice firstChoice = response.getChoices().get(0);
                if (firstChoice.getMessage() != null && firstChoice.getMessage().getContent() != null) {
                    String reply = firstChoice.getMessage().getContent();
                    log.info("💬 Reply preview: {}...", reply.substring(0, Math.min(100, reply.length())));

                    if (response.getUsage() != null) {
                        Integer promptTokens = response.getUsage().getPromptTokens();
                        Integer completionTokens = response.getUsage().getCompletionTokens();
                        Integer totalTokens = (promptTokens != null ? promptTokens : 0) +
                                            (completionTokens != null ? completionTokens : 0);

                        log.info("💰 Tokens - Input: {}, Output: {}, Total: {}",
                                promptTokens, completionTokens, totalTokens);
                        log.info("💵 Cost from API: {}", response.getUsage().getCost());

                        // Calculate cost using configured pricing
                        String modelUsed = response.getModel();
                        if (promptTokens != null && completionTokens != null) {
                            CostCalculationService.CostBreakdown costBreakdown =
                                    costCalculationService.calculateCost(modelUsed, promptTokens, completionTokens);

                            if (costBreakdown != null) {
                                log.info("💵 Calculated cost: {}", costBreakdown.getFormattedString());
                            } else {
                                log.warn("⚠️ Unable to calculate cost - pricing not configured for model: {}", modelUsed);
                            }
                        }
                    }
                    return reply;
                }
            }

            throw new ExternalServiceException("Unexpected response format from OpenRouter API");
        } catch (ExternalServiceException e) {
            throw e; // Re-throw custom exception
        } catch (Exception e) {
            log.error("❌ Exception in executeRequest: {}", e.getMessage(), e);
            throw new ExternalServiceException("Failed to execute OpenRouter API request: " + e.getMessage(), e);
        }
    }

    /**
     * Requests a chat completion from OpenRouter API with metrics.
     *
     * @param messages the conversation history
     * @param temperature the temperature parameter
     * @param model the specific model to use (if null, uses default)
     * @return response with metrics
     */
    public OpenRouterResponseWithMetrics requestCompletionWithMetrics(List<Message> messages, Double temperature, String model) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("Messages list cannot be empty");
        }

        try {
            List<OpenRouterRequest.ChatMessage> chatMessages = messages.stream()
                    .map(msg -> new OpenRouterRequest.ChatMessage(msg.getRole(), msg.getContent()))
                    .collect(Collectors.toList());

            String modelToUse = (model != null && !model.isBlank()) ? model : this.model;

            OpenRouterRequest request = OpenRouterRequest.builder()
                    .model(modelToUse)
                    .messages(chatMessages)
                    .temperature(temperature)
                    .maxTokens(4000)
                    .topP(0.9)
                    .build();

            return executeRequestWithMetrics(request);
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Exception in requestCompletionWithMetrics: {}", e.getMessage(), e);
            throw new ExternalServiceException("Failed to call OpenRouter API: " + e.getMessage(), e);
        }
    }

    /**
     * Executes the API request and returns response with metrics.
     *
     * @param request the OpenRouter request
     * @return response with metrics
     */
    private OpenRouterResponseWithMetrics executeRequestWithMetrics(OpenRouterRequest request) {
        try {
            log.info("📨 Sending request to OpenRouter API...");

            long start = System.nanoTime();

            OpenRouterResponse response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError(),
                            clientResponse -> {
                                log.error("❌ 4xx Error: {}", clientResponse.statusCode());
                                return Mono.error(new ExternalServiceException(
                                        "OpenRouter API client error: " + clientResponse.statusCode()));
                            }
                    )
                    .onStatus(
                            status -> status.is5xxServerError(),
                            clientResponse -> {
                                log.error("❌ 5xx Error: {}", clientResponse.statusCode());
                                return Mono.error(new ExternalServiceException(
                                        "OpenRouter API server error: " + clientResponse.statusCode()));
                            }
                    )
                    .bodyToMono(OpenRouterResponse.class)
                    .block();

            long end = System.nanoTime();
            long responseTimeMs = (end - start) / 1_000_000;

            log.info("✅ Received response from OpenRouter API");
            log.info("⏱️ Response time: {} ms", responseTimeMs);

           if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                OpenRouterResponse.Choice firstChoice = response.getChoices().get(0);
                if (firstChoice.getMessage() != null && firstChoice.getMessage().getContent() != null) {
                    String reply = firstChoice.getMessage().getContent();
                    log.info("💬 Reply preview: {}...", reply.substring(0, Math.min(100, reply.length())));

                    Integer inputTokens = null;
                    Integer outputTokens = null;
                    Integer totalTokens = null;
                    Double cost = null;

                    if (response.getUsage() != null) {
                        inputTokens = response.getUsage().getPromptTokens();
                        outputTokens = response.getUsage().getCompletionTokens();
                        totalTokens = response.getUsage().getTotalTokens();
                        cost = response.getUsage().getCost();

                        log.info("💰 Tokens - Input: {}, Output: {}, Total: {}",
                                inputTokens, outputTokens, totalTokens);
                        log.info("💵 Cost: {}", cost);
                    }

                    return new OpenRouterResponseWithMetrics(
                            reply,
                            inputTokens,
                            outputTokens,
                            totalTokens,
                            cost,
                            responseTimeMs,
                            response.getModel()
                    );
                }
            }

            throw new ExternalServiceException("Unexpected response format from OpenRouter API");
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Exception in executeRequestWithMetrics: {}", e.getMessage(), e);
            throw new ExternalServiceException("Failed to execute OpenRouter API request: " + e.getMessage(), e);
        }
    }
}

