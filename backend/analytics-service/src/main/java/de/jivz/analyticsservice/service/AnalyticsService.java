package de.jivz.analyticsservice.service;

import de.jivz.analyticsservice.analyzer.DataAnalyzer;
import de.jivz.analyticsservice.dto.AnalyticsMetadata;
import de.jivz.analyticsservice.dto.AnalyticsOptions;
import de.jivz.analyticsservice.dto.AnalyticsResponse;
import de.jivz.analyticsservice.parser.FileParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final List<FileParser> fileParsers;
    private final DataAnalyzer dataAnalyzer;
    private final LlmChatClient llmChatClient;
    private final PromptBuilder promptBuilder;

    public AnalyticsResponse analyze(String query, MultipartFile file, AnalyticsOptions options) {
        log.info("Starting analysis for query: {} with file: {}", query, file.getOriginalFilename());

        long startTime = System.currentTimeMillis();

        try {
            // Validate inputs
            if (query == null || query.trim().isEmpty()) {
                throw new IllegalArgumentException("Please enter a question");
            }

            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Please select a file");
            }

            if (file.getSize() > 50 * 1024 * 1024) {
                throw new IllegalArgumentException("File must be < 50MB");
            }

            // Parse file
            FileParser parser = findParser(file.getOriginalFilename());
            if (parser == null) {
                throw new IllegalArgumentException("Only CSV, JSON, TXT supported");
            }

            List<Map<String, String>> records = parser.parse(file);

            if (records.isEmpty()) {
                throw new IllegalArgumentException("File contains no data");
            }

            // Analyze data
            Map<String, Object> analysisResults = dataAnalyzer.analyzeData(records, query);

            // Build prompt for LLM
            String prompt = promptBuilder.buildPrompt(query, analysisResults, options);

            // Get insights from LLM
            String llmResponse = llmChatClient.getInsights(prompt);

            // Extract insights and recommendations
            List<String> insights = extractInsights(analysisResults);
            List<String> recommendations = extractRecommendations(llmResponse);

            // Build response
            long processingTime = System.currentTimeMillis() - startTime;

            return AnalyticsResponse.builder()
                    .answer(llmResponse)
                    .rawData(analysisResults)
                    .insights(insights)
                    .recommendations(recommendations)
                    .metadata(AnalyticsMetadata.builder()
                            .fileFormat(getFileFormat(file.getOriginalFilename()))
                            .rowsAnalyzed(records.size())
                            .processingTimeMs(processingTime)
                            .timestamp(LocalDateTime.now())
                            .build())
                    .build();

        } catch (Exception e) {
            log.error("Analysis failed", e);
            throw new RuntimeException("Analysis failed: " + e.getMessage(), e);
        }
    }

    private FileParser findParser(String filename) {
        return fileParsers.stream()
                .filter(p -> p.supports(filename))
                .findFirst()
                .orElse(null);
    }

    private String getFileFormat(String filename) {
        if (filename == null) return "unknown";

        String lower = filename.toLowerCase();
        if (lower.endsWith(".csv")) return "csv";
        if (lower.endsWith(".json")) return "json";
        if (lower.endsWith(".log") || lower.endsWith(".txt")) return "log";

        return "unknown";
    }

    private List<String> extractInsights(Map<String, Object> analysisResults) {
        List<String> insights = new ArrayList<>();

        // Extract key insights from analysis
        if (analysisResults.containsKey("peak_hour")) {
            insights.add("Peak activity at hour " + analysisResults.get("peak_hour"));
        }

        if (analysisResults.containsKey("most_common_error")) {
            insights.add("Most common error: " + analysisResults.get("most_common_error") +
                    " (" + analysisResults.get("most_common_error_percentage") + "%)");
        }

        if (analysisResults.containsKey("slowest_endpoint")) {
            insights.add("Slowest endpoint: " + analysisResults.get("slowest_endpoint") +
                    " (avg " + analysisResults.get("slowest_endpoint_avg_ms") + "ms)");
        }

        if (analysisResults.containsKey("total_records")) {
            insights.add("Total records analyzed: " + analysisResults.get("total_records"));
        }

        return insights;
    }

    private List<String> extractRecommendations(String llmResponse) {
        List<String> recommendations = new ArrayList<>();

        // Extract recommendations from LLM response
        // Look for lines starting with "Recommendation:" or numbered recommendations
        String[] lines = llmResponse.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.toLowerCase().startsWith("recommendation:") ||
                trimmed.matches("^\\d+\\.\\s+.*") ||
                trimmed.startsWith("- ") && trimmed.toLowerCase().contains("should")) {
                recommendations.add(trimmed);
            }
        }

        // If no recommendations found, add a generic one
        if (recommendations.isEmpty()) {
            recommendations.add("Review the analysis results and take appropriate action");
        }

        return recommendations;
    }
}
