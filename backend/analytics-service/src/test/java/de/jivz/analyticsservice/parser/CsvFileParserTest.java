package de.jivz.analyticsservice.parser;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CsvFileParserTest {

    private final CsvFileParser parser = new CsvFileParser();

    @Test
    void testParseCsv() throws Exception {
        String csvContent = """
                timestamp,service,level,message,error_type
                2026-01-22 10:15:23,llm-chat-service,ERROR,chat() failed,NullPointerException
                2026-01-22 10:18:45,llm-chat-service,ERROR,Ollama timeout,TimeoutException
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                csvContent.getBytes()
        );

        List<Map<String, String>> records = parser.parse(file);

        assertEquals(2, records.size());
        assertEquals("llm-chat-service", records.get(0).get("service"));
        assertEquals("NullPointerException", records.get(0).get("error_type"));
        assertEquals("TimeoutException", records.get(1).get("error_type"));
    }

    @Test
    void testSupports() {
        assertTrue(parser.supports("test.csv"));
        assertTrue(parser.supports("test.CSV"));
        assertFalse(parser.supports("test.json"));
        assertFalse(parser.supports("test.txt"));
    }
}


