package de.jivz.analyticsservice.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class LogFileParser implements FileParser {

    // Pattern: timestamp level message
    private static final Pattern LOG_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}[T\\s]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?)\\s+(ERROR|WARN|INFO|DEBUG|TRACE)\\s+(.+)$"
    );

    @Override
    public List<Map<String, String>> parse(MultipartFile file) throws IOException {
        log.info("Parsing log file: {}", file.getOriginalFilename());

        List<Map<String, String>> records = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream()))) {

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.trim().isEmpty()) {
                    continue;
                }

                Map<String, String> record = parseLine(line, lineNumber);
                records.add(record);
            }
        }

        log.info("Parsed {} records from log file", records.size());
        return records;
    }

    private Map<String, String> parseLine(String line, int lineNumber) {
        Map<String, String> record = new HashMap<>();
        record.put("line_number", String.valueOf(lineNumber));
        record.put("raw_line", line);

        Matcher matcher = LOG_PATTERN.matcher(line);

        if (matcher.matches()) {
            record.put("timestamp", matcher.group(1));
            record.put("level", matcher.group(2));
            record.put("message", matcher.group(3));

            // Extract error type if present
            String message = matcher.group(3);
            if (message.contains("Exception")) {
                int exceptionIndex = message.indexOf("Exception");
                int startIndex = Math.max(0, message.lastIndexOf(" ", exceptionIndex) + 1);
                String errorType = message.substring(startIndex, exceptionIndex + "Exception".length());
                record.put("error_type", errorType);
            }
        } else {
            // Fallback: treat entire line as message
            record.put("timestamp", "");
            record.put("level", "UNKNOWN");
            record.put("message", line);
        }

        return record;
    }

    @Override
    public boolean supports(String filename) {
        if (filename == null) {
            return false;
        }
        String lower = filename.toLowerCase();
        return lower.endsWith(".log") || lower.endsWith(".txt");
    }
}
