package de.jivz.ai_challenge.service.perplexity;

import de.jivz.ai_challenge.exception.ExternalServiceException;
import de.jivz.ai_challenge.service.perplexity.model.PerplexityRequest;
import de.jivz.ai_challenge.service.perplexity.model.PerplexityResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Perplexity API client implementation.
 * Uses configured WebClient and model from PerplexityClientConfig.
 */
@Slf4j
@Component
public class PerplexityToolClient {

    private final WebClient webClient;
    private final String model;

    public PerplexityToolClient(
            @Qualifier("perplexityWebClient") WebClient webClient,
            @Qualifier("perplexityModel") String model) {
        this.webClient = webClient;
        this.model = model;
        log.info("✅ PerplexityToolClient initialized with WebClient and model: {}", model);
    }

    /**
     * Requests a chat completion from Perplexity API.
     *
     * @param input the user's message
     * @return the AI's response
     * @throws ExternalServiceException if the API call fails
     */
    public String requestCompletion(String input) {
        try {
            // Typsicherer Request mit Builder-Pattern
            PerplexityRequest request = PerplexityRequest.builder()
                    .model(model)
                    .addMessage("user", input)
                    .build();

            log.info("🚀 Calling Perplexity API with model: {}", model);
            log.debug("📝 User message: {}", input);

            // WebClient mit typsicherer PerplexityResponse
            PerplexityResponse response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError(),
                            clientResponse -> {
                                log.error("❌ 4xx Error: {}", clientResponse.statusCode());
                                return Mono.error(new ExternalServiceException(
                                        "Perplexity API client error: " + clientResponse.statusCode()));
                            }
                    )
                    .onStatus(
                            status -> status.is5xxServerError(),
                            clientResponse -> {
                                log.error("❌ 5xx Error: {}", clientResponse.statusCode());
                                return Mono.error(new ExternalServiceException(
                                        "Perplexity API server error: " + clientResponse.statusCode()));
                            }
                    )
                    .bodyToMono(PerplexityResponse.class)
                    .block(); // Blockierend für synchrone Verwendung

            log.info("✅ Received response from Perplexity API");

            // Extrahiere Antwort aus choices[0].message.content (typsicher!)
            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                PerplexityResponse.Choice firstChoice = response.getChoices().get(0);
                if (firstChoice.getMessage() != null && firstChoice.getMessage().getContent() != null) {
                    String reply = firstChoice.getMessage().getContent();
                    log.info("💬 Reply preview: {}...", reply.substring(0, Math.min(100, reply.length())));

                    if (response.getCitations() != null && !response.getCitations().isEmpty()) {
                        log.info("📚 Citations: {} sources", response.getCitations().size());
                    }
                    if (response.getUsage() != null) {
                        log.info("💰 Usage: {} tokens", response.getUsage().getTotalTokens());
                    }

                    return reply;
                }
            }

            throw new ExternalServiceException("Unexpected response format from Perplexity API");
        } catch (ExternalServiceException e) {
            throw e; // Re-throw custom exception
        } catch (Exception e) {
            log.error("❌ Exception in requestCompletion: {}", e.getMessage(), e);
            throw new ExternalServiceException("Failed to call Perplexity API: " + e.getMessage(), e);
        }
    }
}
