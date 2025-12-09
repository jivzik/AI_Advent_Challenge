package de.jivz.ai_challenge.service.onerouter;

import de.jivz.ai_challenge.service.onerouter.model.OpenRouterMessage;
import de.jivz.ai_challenge.service.onerouter.model.OpenRouterRequest;
import de.jivz.ai_challenge.service.onerouter.model.OpenRouterResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class OpenRouterService {

    private final WebClient openRouterWebClient;

    public OpenRouterService(WebClient openRouterWebClient) {
        this.openRouterWebClient = openRouterWebClient;
    }

    public Mono<String> chat(String userPrompt) {
        var systemMessage = new OpenRouterMessage(
                "system",
                "You are an AI assistant integrated through the OpenRouter API in a Java 21 Spring Boot backend. " +
                        "Keep answers concise, use Markdown, and provide production-ready Java/Spring examples when relevant."
        );

        var userMessage = new OpenRouterMessage("user", userPrompt);

        var request = new OpenRouterRequest(
                "openrouter/auto",   // or a specific model like "deepseek/deepseek-chat"
                List.of(systemMessage, userMessage),
                0.3,
                1024
        );

        return openRouterWebClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenRouterResponse.class)
                .map(response -> {
                    if (response.choices() == null || response.choices().isEmpty()) {
                        return "";
                    }
                    return response.choices().getFirst().message().content();
                });
    }
}
