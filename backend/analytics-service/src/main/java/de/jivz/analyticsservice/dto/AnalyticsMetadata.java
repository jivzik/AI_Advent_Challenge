package de.jivz.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsMetadata {
    private String fileFormat;
    private Integer rowsAnalyzed;
    private Long processingTimeMs;
    private LocalDateTime timestamp;
}
