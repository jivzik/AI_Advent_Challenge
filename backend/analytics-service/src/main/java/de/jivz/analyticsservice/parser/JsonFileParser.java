package de.jivz.analyticsservice.parser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonFileParser implements FileParser {

    private final ObjectMapper objectMapper;

    @Override
    public List<Map<String, String>> parse(MultipartFile file) throws IOException {
        log.info("Parsing JSON file: {}", file.getOriginalFilename());

        List<Map<String, String>> records = new ArrayList<>();

        // Try to parse as array of objects
        try {
            List<Map<String, Object>> rawRecords = objectMapper.readValue(
                    file.getInputStream(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            for (Map<String, Object> rawRecord : rawRecords) {
                Map<String, String> record = new HashMap<>();
                for (Map.Entry<String, Object> entry : rawRecord.entrySet()) {
                    record.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
                records.add(record);
            }
        } catch (Exception e) {
            // Try to parse as single object
            log.debug("Failed to parse as array, trying as single object", e);
            Map<String, Object> rawRecord = objectMapper.readValue(
                    file.getInputStream(),
                    new TypeReference<Map<String, Object>>() {}
            );

            Map<String, String> record = new HashMap<>();
            for (Map.Entry<String, Object> entry : rawRecord.entrySet()) {
                record.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
            records.add(record);
        }

        log.info("Parsed {} records from JSON", records.size());
        return records;
    }

    @Override
    public boolean supports(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".json");
    }
}
