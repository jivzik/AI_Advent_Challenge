package de.jivz.analyticsservice.analyzer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DataAnalyzer {

    public Map<String, Object> analyzeData(List<Map<String, String>> records, String query) {
        log.info("Analyzing {} records for query: {}", records.size(), query);

        Map<String, Object> analysis = new HashMap<>();

        if (records.isEmpty()) {
            return analysis;
        }

        // Determine analysis type based on query
        String lowerQuery = query.toLowerCase();

        if (containsKeywords(lowerQuery, "error", "exception", "fail")) {
            analysis.putAll(analyzeErrors(records));
        }

        if (containsKeywords(lowerQuery, "time", "when", "hour", "day")) {
            analysis.putAll(analyzeTimeDistribution(records));
        }

        if (containsKeywords(lowerQuery, "slow", "performance", "response time", "latency")) {
            analysis.putAll(analyzePerformance(records));
        }

        if (containsKeywords(lowerQuery, "most", "frequent", "common", "count")) {
            analysis.putAll(analyzeFrequency(records));
        }

        if (containsKeywords(lowerQuery, "average", "mean", "median", "statistics")) {
            analysis.putAll(analyzeStatistics(records));
        }

        // Always add basic info
        analysis.put("total_records", records.size());
        analysis.put("fields", new ArrayList<>(records.get(0).keySet()));

        return analysis;
    }

    private boolean containsKeywords(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> analyzeErrors(List<Map<String, String>> records) {
        Map<String, Object> result = new HashMap<>();

        // Count by error type
        Map<String, Long> errorCounts = records.stream()
                .filter(r -> r.containsKey("error_type") || r.containsKey("level"))
                .collect(Collectors.groupingBy(
                        r -> r.getOrDefault("error_type", r.getOrDefault("level", "UNKNOWN")),
                        Collectors.counting()
                ));

        if (!errorCounts.isEmpty()) {
            result.put("error_distribution", errorCounts);

            // Find most common error
            Map.Entry<String, Long> mostCommon = errorCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);

            if (mostCommon != null) {
                result.put("most_common_error", mostCommon.getKey());
                result.put("most_common_error_count", mostCommon.getValue());

                long total = errorCounts.values().stream().mapToLong(Long::longValue).sum();
                double percentage = (mostCommon.getValue() * 100.0) / total;
                result.put("most_common_error_percentage", Math.round(percentage * 10) / 10.0);
            }
        }

        return result;
    }

    private Map<String, Object> analyzeTimeDistribution(List<Map<String, String>> records) {
        Map<String, Object> result = new HashMap<>();

        // Extract hour from timestamp
        Map<String, Long> hourlyDistribution = records.stream()
                .filter(r -> r.containsKey("timestamp") && !r.get("timestamp").isEmpty())
                .collect(Collectors.groupingBy(
                        r -> extractHour(r.get("timestamp")),
                        Collectors.counting()
                ));

        if (!hourlyDistribution.isEmpty()) {
            result.put("hourly_distribution", hourlyDistribution);

            Map.Entry<String, Long> peakHour = hourlyDistribution.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);

            if (peakHour != null) {
                result.put("peak_hour", peakHour.getKey());
                result.put("peak_hour_count", peakHour.getValue());
            }
        }

        return result;
    }

    private String extractHour(String timestamp) {
        // Extract hour from timestamp like "2026-01-22 10:15:23" or "2026-01-22T10:15:23"
        if (timestamp.length() >= 13) {
            return timestamp.substring(11, 13);
        }
        return "00";
    }

    private Map<String, Object> analyzePerformance(List<Map<String, String>> records) {
        Map<String, Object> result = new HashMap<>();

        // Look for response_time, latency, or similar fields
        String timeField = findField(records.get(0), "response_time", "latency", "duration", "time_ms");

        if (timeField != null) {
            List<Double> times = records.stream()
                    .filter(r -> r.containsKey(timeField) && !r.get(timeField).isEmpty())
                    .map(r -> parseDouble(r.get(timeField)))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!times.isEmpty()) {
                result.put("performance_stats", calculateStats(times));

                // Group by endpoint if available
                if (records.get(0).containsKey("endpoint")) {
                    Map<String, Double> avgByEndpoint = records.stream()
                            .filter(r -> r.containsKey("endpoint") && r.containsKey(timeField))
                            .collect(Collectors.groupingBy(
                                    r -> r.get("endpoint"),
                                    Collectors.averagingDouble(r -> parseDouble(r.get(timeField)))
                            ));

                    result.put("average_by_endpoint", avgByEndpoint);

                    // Find slowest endpoint
                    Map.Entry<String, Double> slowest = avgByEndpoint.entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .orElse(null);

                    if (slowest != null) {
                        result.put("slowest_endpoint", slowest.getKey());
                        result.put("slowest_endpoint_avg_ms", Math.round(slowest.getValue() * 10) / 10.0);
                    }
                }
            }
        }

        return result;
    }

    private Map<String, Object> analyzeFrequency(List<Map<String, String>> records) {
        Map<String, Object> result = new HashMap<>();

        // Find most varied field
        Map<String, Set<String>> uniqueValues = new HashMap<>();

        for (String field : records.get(0).keySet()) {
            Set<String> values = records.stream()
                    .map(r -> r.get(field))
                    .filter(v -> v != null && !v.isEmpty())
                    .collect(Collectors.toSet());
            uniqueValues.put(field, values);
        }

        // Find field with reasonable cardinality (not too low, not too high)
        String bestField = uniqueValues.entrySet().stream()
                .filter(e -> e.getValue().size() > 1 && e.getValue().size() < records.size() / 2)
                .max(Comparator.comparingInt(e -> e.getValue().size()))
                .map(Map.Entry::getKey)
                .orElse(null);

        if (bestField != null) {
            Map<String, Long> distribution = records.stream()
                    .collect(Collectors.groupingBy(
                            r -> r.getOrDefault(bestField, "UNKNOWN"),
                            Collectors.counting()
                    ));

            result.put("frequency_field", bestField);
            result.put("frequency_distribution", distribution);
        }

        return result;
    }

    private Map<String, Object> analyzeStatistics(List<Map<String, String>> records) {
        Map<String, Object> result = new HashMap<>();

        // Find numeric fields
        for (String field : records.get(0).keySet()) {
            List<Double> values = records.stream()
                    .map(r -> parseDouble(r.get(field)))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!values.isEmpty() && values.size() > records.size() / 2) {
                result.put(field + "_stats", calculateStats(values));
            }
        }

        return result;
    }

    private Map<String, Object> calculateStats(List<Double> values) {
        Map<String, Object> stats = new HashMap<>();

        DoubleSummaryStatistics summary = values.stream()
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();

        stats.put("min", Math.round(summary.getMin() * 10) / 10.0);
        stats.put("max", Math.round(summary.getMax() * 10) / 10.0);
        stats.put("average", Math.round(summary.getAverage() * 10) / 10.0);
        stats.put("count", summary.getCount());

        return stats;
    }

    private String findField(Map<String, String> record, String... candidates) {
        for (String candidate : candidates) {
            for (String field : record.keySet()) {
                if (field.toLowerCase().contains(candidate.toLowerCase())) {
                    return field;
                }
            }
        }
        return null;
    }

    private Double parseDouble(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
