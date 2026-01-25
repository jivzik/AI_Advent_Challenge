package de.jivz.analyticsservice.parser;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
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

@Slf4j
@Component
public class CsvFileParser implements FileParser {

    @Override
    public List<Map<String, String>> parse(MultipartFile file) throws IOException, CsvException {
        log.info("Parsing CSV file: {}", file.getOriginalFilename());

        List<Map<String, String>> records = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new BufferedReader(
                new InputStreamReader(file.getInputStream())))) {

            List<String[]> allRows = reader.readAll();

            if (allRows.isEmpty()) {
                log.warn("CSV file is empty");
                return records;
            }

            // First row is header
            String[] headers = allRows.get(0);

            // Parse remaining rows
            for (int i = 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);
                Map<String, String> record = new HashMap<>();

                for (int j = 0; j < headers.length && j < row.length; j++) {
                    record.put(headers[j].trim(), row[j].trim());
                }

                records.add(record);
            }
        }

        log.info("Parsed {} records from CSV", records.size());
        return records;
    }

    @Override
    public boolean supports(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".csv");
    }
}
