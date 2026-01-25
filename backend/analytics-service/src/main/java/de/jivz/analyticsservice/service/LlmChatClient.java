package de.jivz.analyticsservice.service;

import de.jivz.analyticsservice.dto.LlmChatRequest;
import de.jivz.analyticsservice.dto.LlmChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmChatClient {

    private final WebClient webClient;

    @Value("${llm.chat.service.url:http://localhost:8084}")
    private String llmChatServiceUrl;

    @Value("${llm.chat.service.timeout:60}")
    private int timeoutSeconds;

    public String getInsights(String prompt) {
        log.info("Calling LLM Chat Service for insights");

        LlmChatRequest request = LlmChatRequest.builder()
                .message(prompt)
                //.model("llama3.1:8b")
                .temperature(0.7)
                .maxTokens(1000)
                .build();

        try {
            LlmChatResponse response = webClient.post()
                    .uri(llmChatServiceUrl + "/api/chat")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(LlmChatResponse.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .block();

            if (response != null && response.getResponse() != null) {
                log.info("Received insights from LLM");
                return response.getResponse();
            } else {
                log.warn("Empty response from LLM Chat Service");
                return "No insights available";
            }
        } catch (Exception e) {
            log.error("Failed to get insights from LLM Chat Service", e);
            return "Error getting insights: " + e.getMessage();
        }
    }
}
