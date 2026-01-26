package de.jivz.analyticsservice.controller;

import de.jivz.analyticsservice.dto.AnalyticsOptions;
import de.jivz.analyticsservice.dto.AnalyticsResponse;
import de.jivz.analyticsservice.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyze(
            @RequestParam("query") String query,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "includeVisualization", required = false) Boolean includeVisualization,
            @RequestParam(value = "detailedRecommendations", required = false) Boolean detailedRecommendations
    ) {
        log.info("Received analysis request - query: {}, file: {}", query, file.getOriginalFilename());

        try {
            AnalyticsOptions options = AnalyticsOptions.builder()
                    .includeVisualization(includeVisualization)
                    .detailedRecommendations(detailedRecommendations)
                    .build();

            AnalyticsResponse response = analyticsService.analyze(query, file, options);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            log.error("Analysis failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Analysis failed: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "analytics-service",
                "timestamp", java.time.LocalDateTime.now()
        ));
    }

    private static class Map {
        static java.util.Map<String, Object> of(String key, Object value) {
            return java.util.Collections.singletonMap(key, value);
        }

        static java.util.Map<String, Object> of(String key1, Object value1, String key2, Object value2, String key3, Object value3) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put(key1, value1);
            map.put(key2, value2);
            map.put(key3, value3);
            return map;
        }
    }
}
