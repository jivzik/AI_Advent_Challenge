package de.jivz.analyticsservice.analyzer;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DataAnalyzerTest {

    private final DataAnalyzer analyzer = new DataAnalyzer();

    @Test
    void testAnalyzeErrors() {
        List<Map<String, String>> records = new ArrayList<>();

        Map<String, String> record1 = new HashMap<>();
        record1.put("error_type", "NullPointerException");
        record1.put("message", "test");
        records.add(record1);

        Map<String, String> record2 = new HashMap<>();
        record2.put("error_type", "NullPointerException");
        record2.put("message", "test");
        records.add(record2);

        Map<String, String> record3 = new HashMap<>();
        record3.put("error_type", "TimeoutException");
        record3.put("message", "test");
        records.add(record3);

        Map<String, Object> analysis = analyzer.analyzeData(records, "What errors are most common?");

        assertTrue(analysis.containsKey("error_distribution"));
        assertEquals("NullPointerException", analysis.get("most_common_error"));
        assertEquals(3, analysis.get("total_records"));
    }

    @Test
    void testAnalyzePerformance() {
        List<Map<String, String>> records = new ArrayList<>();

        Map<String, String> record1 = new HashMap<>();
        record1.put("endpoint", "/api/chat");
        record1.put("response_time_ms", "2340");
        records.add(record1);

        Map<String, String> record2 = new HashMap<>();
        record2.put("endpoint", "/api/chat");
        record2.put("response_time_ms", "3000");
        records.add(record2);

        Map<String, String> record3 = new HashMap<>();
        record3.put("endpoint", "/api/health");
        record3.put("response_time_ms", "100");
        records.add(record3);

        Map<String, Object> analysis = analyzer.analyzeData(records, "Which endpoint is slowest?");

        assertTrue(analysis.containsKey("average_by_endpoint"));
        assertEquals("/api/chat", analysis.get("slowest_endpoint"));
    }
}
