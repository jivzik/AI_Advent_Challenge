package de.jivz.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {
    private String answer;
    private Map<String, Object> rawData;
    private List<String> insights;
    private List<String> recommendations;
    private AnalyticsMetadata metadata;
}
