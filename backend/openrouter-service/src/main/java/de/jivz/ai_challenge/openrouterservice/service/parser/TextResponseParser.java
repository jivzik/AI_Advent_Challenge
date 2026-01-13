package de.jivz.ai_challenge.openrouterservice.service.parser;

import de.jivz.ai_challenge.openrouterservice.dto.ToolResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Parser für reine Text-Antworten (nicht-JSON).
 * Behandelt Antworten als finale Text-Antworten ohne Tool-Calls.
 */
@Component
@Slf4j
public class TextResponseParser implements ResponseParserStrategy {

    private static final String STEP_FINAL = "final";

    @Override
    public boolean canParse(String response) {
        if (response == null || response.isBlank()) {
            return false;
        }

        String trimmed = response.trim();
        // Kann Text-Antworten parsen, die nicht mit { oder [ beginnen
        return !trimmed.startsWith("{") && !trimmed.startsWith("[");
    }

    @Override
    public ToolResponse parse(String response) throws ResponseParsingException {
        if (response == null || response.isBlank()) {
            throw new ResponseParsingException("Empty response");
        }

        log.info("📝 Parsing response as plain text (not JSON)");

        return ToolResponse.builder()
                .step(STEP_FINAL)
                .answer(response)
                .toolCalls(List.of())
                .build();
    }
}

