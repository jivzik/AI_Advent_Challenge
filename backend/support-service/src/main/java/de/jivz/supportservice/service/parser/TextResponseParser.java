package de.jivz.supportservice.service.parser;

import de.jivz.supportservice.dto.ToolResponse;
import de.jivz.supportservice.service.parser.ResponseParserStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Parser für reine Text-Antworten (nicht-JSON).
 * Behandelt Antworten als finale Text-Antworten ohne Tool-Calls.
 */
@Component
@Order(10) // Niedrigste Priorität - wird zuletzt geprüft (Fallback)
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

        String cleaned = cleanJsonResponse(response);

        log.info("📝 Parsing response as plain text (not JSON)");

        return ToolResponse.builder()
                .step(STEP_FINAL)
                .answer(cleaned)
                .toolCalls(List.of())
                .build();
    }

    /**
     * Bereinigt die Antwort von Markdown-Blöcken und ungültigen JSON-Zeichen.
     *
     * @param response Die zu bereinigende Response
     * @return Die bereinigte Response
     */
    private String cleanJsonResponse(String response) {
        if (response == null) {
            return null;
        }

        String cleaned = response.trim();

        // Entfernt ```json ... ```
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        cleaned = cleaned.trim();

        // Bereinige ungültige Zeichen in JSON-Strings
        // Ersetze unescaped Newlines, Tabs und andere Kontrollzeichen
        cleaned = fixUnescapedControlChars(cleaned);

        return cleaned;
    }

    /**
     * Behebt unescaped Kontrollzeichen in JSON-Strings.
     * Dies ist notwendig, da LLMs manchmal ungültiges JSON mit Newlines in Strings generieren.
     */
    private String fixUnescapedControlChars(String json) {
        if (json == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                result.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\') {
                result.append(c);
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                result.append(c);
                continue;
            }

            // Wenn wir in einem String sind, escape Kontrollzeichen
            if (inString) {
                if (c == '\n') {
                    result.append("\\n");
                } else if (c == '\r') {
                    result.append("\\r");
                } else if (c == '\t') {
                    result.append("\\t");
                } else if (c == '\b') {
                    result.append("\\b");
                } else if (c == '\f') {
                    result.append("\\f");
                } else if (Character.isISOControl(c)) {
                    // Andere Kontrollzeichen als Unicode escape
                    result.append(String.format("\\u%04x", (int) c));
                } else {
                    result.append(c);
                }
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }
}

