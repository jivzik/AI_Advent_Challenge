package de.jivz.analyticsservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.analyticsservice.dto.AnalyticsOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromptBuilder {

    private final ObjectMapper objectMapper;

    public String buildPrompt(String userQuery, Map<String, Object> analysisResults, AnalyticsOptions options) {
        StringBuilder prompt = new StringBuilder();

        // Compact context
        prompt.append("Spring Boot Platform Analytics\n\n");

        // User question
        prompt.append("Question: ").append(userQuery).append("\n\n");

        // Analysis results (compact format)
        prompt.append("Data:\n");
        try {
            String jsonResults = objectMapper.writeValueAsString(analysisResults);
            prompt.append(jsonResults).append("\n\n");
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize analysis results", e);
            prompt.append(analysisResults.toString()).append("\n\n");
        }

        // Compact instructions
        prompt.append("Provide:\n");
        prompt.append("1. Direct answer with numbers\n");
        prompt.append("2. Key findings (2-3 points)\n");

        if (options != null && Boolean.TRUE.equals(options.getDetailedRecommendations())) {
            prompt.append("3. Detailed recommendations with examples\n");
        } else {
            prompt.append("3. Top 2 recommendations\n");
        }

        prompt.append("\nBe concise and specific.");

        return prompt.toString();
    }
}
