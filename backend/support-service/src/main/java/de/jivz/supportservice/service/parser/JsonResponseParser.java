package de.jivz.supportservice.service.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.supportservice.dto.ToolResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Parser für JSON-formatierte Responses.
 * Bereinigt Markdown-Blöcke und parst das JSON zu ToolResponse.
 */
@Component
@Order(1) // Höchste Priorität - wird zuerst geprüft
@Slf4j
@RequiredArgsConstructor
public class JsonResponseParser implements ResponseParserStrategy {

    private final ObjectMapper objectMapper;

    @Override
    public boolean canParse(String response) {
        if (response == null || response.isBlank()) {
            return false;
        }

        String trimmed = response.trim();

        // Prüfe ob es ein Markdown-Code-Block mit JSON ist
        if (trimmed.startsWith("```json") || trimmed.startsWith("```JSON")) {
            return true;
        }

        // Prüfe ob es ein generischer Code-Block ist, der JSON enthält
        if (trimmed.startsWith("```") && trimmed.contains("{")) {
            return true;
        }

        String cleaned = cleanJsonResponse(response);
        return cleaned.trim().startsWith("{") || cleaned.trim().startsWith("[");
    }

    @Override
    public ToolResponse parse(String response) throws ResponseParsingException {
        String cleaned = cleanJsonResponse(response);

        try {
            ToolResponse toolResponse = objectMapper.readValue(cleaned, ToolResponse.class);
            log.debug("✅ Successfully parsed JSON response");
            return toolResponse;
        } catch (JsonProcessingException e) {
            log.warn("⚠️ Failed to parse JSON: {}", e.getMessage());
            log.debug("📝 Problematic JSON: {}", cleaned);

            // Detailliertes Logging für Debugging
            if (log.isDebugEnabled()) {
                log.debug("JSON length: {} chars", cleaned.length());
                log.debug("Contains Cyrillic: {}", containsCyrillic(cleaned));
                log.debug("First 200 chars: {}", cleaned.length() > 200 ? cleaned.substring(0, 200) : cleaned);
            }

            // Versuche JSON zu reparieren
            String repaired = repairCommonJsonIssues(cleaned);
            if (!repaired.equals(cleaned)) {
                try {
                    ToolResponse toolResponse = objectMapper.readValue(repaired, ToolResponse.class);
                    log.info("✅ Successfully parsed JSON after repair");
                    return toolResponse;
                } catch (JsonProcessingException e2) {
                    log.warn("⚠️ Repair attempt also failed: {}", e2.getMessage());
                    log.debug("📝 Repaired JSON that failed: {}", repaired);
                }
            }

            throw new ResponseParsingException("Failed to parse JSON response: " + e.getMessage(), e);
        }
    }

    /**
     * Prüft ob der String Cyrillic-Zeichen enthält.
     */
    private boolean containsCyrillic(String text) {
        if (text == null) return false;
        return text.chars().anyMatch(c ->
            (c >= 0x0400 && c <= 0x04FF) || // Cyrillic
            (c >= 0x0500 && c <= 0x052F)    // Cyrillic Supplement
        );
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

    /**
     * Repariert häufige JSON-Fehler, die von LLMs generiert werden.
     * z.B. ungültige Schema-Definitionen in arguments
     */
    private String repairCommonJsonIssues(String json) {
        if (json == null) {
            return null;
        }

        String repaired = json;

        // 1. Entferne Text VOR dem JSON-Objekt
        int firstBrace = repaired.indexOf('{');
        if (firstBrace > 0) {
            log.debug("🔧 Removing {} chars before first '{'", firstBrace);
            repaired = repaired.substring(firstBrace);
        }

        // 2. Entferne Text NACH dem JSON-Objekt (finde letzte schließende Klammer)
        int lastBrace = repaired.lastIndexOf('}');
        if (lastBrace > 0 && lastBrace < repaired.length() - 1) {
            String afterJson = repaired.substring(lastBrace + 1).trim();
            if (!afterJson.isEmpty()) {
                log.debug("🔧 Removing text after last '}': {}", afterJson.substring(0, Math.min(50, afterJson.length())));
                repaired = repaired.substring(0, lastBrace + 1);
            }
        }

        // 3. Repariere unescaped Quotes in String-Werten (WICHTIG für schwache LLMs!)
        repaired = fixUnescapedQuotesInStrings(repaired);

        // 4. Problem: {"arguments":{"type":"object", "properties":{ "documentId": "value"}}}
        // Lösung: {"arguments":{"documentId": "value"}}
        // Entferne Schema-Wrapper um tatsächliche Argumente
        repaired = repaired.replaceAll(
            "\"arguments\"\\s*:\\s*\\{\\s*\"type\"\\s*:\\s*\"object\"\\s*,\\s*\"properties\"\\s*:\\s*\\{",
            "\"arguments\":{"
        );

        // 5. Repariere fehlende oder falsche Quotes bei Werten
        repaired = fixMissingQuotes(repaired);

        // 6. Wenn wir einen Schema-Wrapper entfernt haben, müssen wir auch die schließenden Klammern anpassen
        // Zähle öffnende und schließende Klammern in arguments-Objekten
        repaired = fixBracketBalance(repaired);

        return repaired;
    }

    /**
     * Repariert unescaped Quotes innerhalb von JSON-String-Werten.
     * Häufiger Fehler bei schwachen LLMs: "answer": "Text mit "unescaped" quotes"
     *
     * Verwendet einen SEHR konservativen Ansatz, um keine gültigen JSONs zu beschädigen.
     * Repariert nur offensichtliche Fälle.
     */
    private String fixUnescapedQuotesInStrings(String json) {
        if (json == null) {
            return null;
        }

        // Konservativer Ansatz: Suche nach dem spezifischen Pattern
        // "answer": "...text..."weiterer text"...text..."
        // wo nach einem Quote ein Buchstabe/Cyrillic folgt statt , } oder ]

        StringBuilder result = new StringBuilder();
        boolean inStringValue = false;
        boolean escaped = false;
        boolean afterColon = false;
        int nesting = 0; // Track {} Verschachtelung

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

            // Track nesting außerhalb von Strings
            if (!inStringValue) {
                if (c == '{') nesting++;
                if (c == '}') nesting--;
                if (c == ':') {
                    afterColon = true;
                    result.append(c);
                    continue;
                }
                if (c == ',') {
                    afterColon = false;
                    result.append(c);
                    continue;
                }
            }

            if (c == '"') {
                if (inStringValue) {
                    // Prüfe ob dies ein echtes String-Ende ist
                    // Schaue vorwärts: nach einem String-Ende sollte , } ] oder Whitespace folgen
                    int nextIdx = i + 1;
                    while (nextIdx < json.length() && Character.isWhitespace(json.charAt(nextIdx))) {
                        nextIdx++;
                    }

                    boolean isRealEnd = false;
                    if (nextIdx >= json.length()) {
                        isRealEnd = true; // Ende des JSON
                    } else {
                        char nextChar = json.charAt(nextIdx);
                        if (nextChar == ',' || nextChar == '}' || nextChar == ']') {
                            isRealEnd = true;
                        }
                    }

                    if (isRealEnd) {
                        // Echtes String-Ende
                        inStringValue = false;
                        afterColon = false;
                        result.append(c);
                    } else {
                        // Quote mittendrin - escape es NUR wenn wir sicher in einem Value sind
                        if (afterColon && nesting > 0) {
                            log.debug("🔧 Escaped unescaped quote at position {}", i);
                            result.append('\\').append(c);
                        } else {
                            // Unsicher - lass es wie es ist
                            result.append(c);
                        }
                    }
                } else {
                    // Potenzieller String-Start
                    if (afterColon) {
                        inStringValue = true;
                    }
                    result.append(c);
                }
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Findet das Ende eines normalen JSON-Strings (ohne unescaped Quotes).
     */
    private int findStringEnd(String json, int start) {
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    /**
     * Findet das Ende eines String-Werts, der möglicherweise unescaped Quotes enthält.
     * Nutzt Heuristik: Ende ist da, wo nach " ein , } ] oder Whitespace+diese Zeichen folgt.
     */
    private int findUnescapedStringEndWithQuotes(String json, int start) {
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                // Prüfe ob dies das echte Ende ist
                char next = getNextNonWhitespace(json, i + 1);
                if (next == ',' || next == '}' || next == ']' || next == '\0') {
                    return i; // Dies ist das String-Ende
                }
                // Sonst ist es ein Quote mittendrin, weitermachen
            }
        }
        return -1;
    }

    /**
     * Findet die nächste nicht-Whitespace Position.
     */
    private int findNextNonWhitespace(String json, int start) {
        for (int i = start; i < json.length(); i++) {
            if (!Character.isWhitespace(json.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Escaped alle unescaped Quotes in einem String-Wert.
     */
    private String escapeQuotesInValue(String value) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

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
                // Unescaped quote - escape es!
                result.append('\\').append(c);
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Findet das nächste nicht-Whitespace Zeichen ab Position start.
     */
    private char getNextNonWhitespace(String str, int start) {
        for (int i = start; i < str.length(); i++) {
            char c = str.charAt(i);
            if (!Character.isWhitespace(c)) {
                return c;
            }
        }
        return '\0';
    }

    /**
     * Findet das vorherige nicht-Whitespace Zeichen ab Position start rückwärts.
     */
    private char getPrevNonWhitespace(String str, int start) {
        for (int i = start; i >= 0; i--) {
            char c = str.charAt(i);
            if (!Character.isWhitespace(c)) {
                return c;
            }
        }
        return '\0';
    }

    /**
     * Versucht fehlende Quotes um String-Werte zu reparieren.
     * Dies ist eine defensive Strategie für häufige LLM-Fehler.
     */
    private String fixMissingQuotes(String json) {
        // Diese Methode ist komplex und könnte mehr Schaden anrichten
        // Vorerst geben wir den JSON unverändert zurück
        // TODO: Implementiere intelligente Quote-Reparatur wenn nötig
        return json;
    }

    /**
     * Behebt Ungleichgewicht von geschweiften Klammern.
     */
    private String fixBracketBalance(String json) {
        if (json == null) {
            return null;
        }

        // Einfache Heuristik: Zähle { und }
        int openCount = 0;
        int closeCount = 0;

        for (char c : json.toCharArray()) {
            if (c == '{') openCount++;
            if (c == '}') closeCount++;
        }

        // Wenn mehr öffnende als schließende Klammern
        if (openCount > closeCount) {
            int diff = openCount - closeCount;
            // Füge fehlende schließende Klammern am Ende hinzu
            StringBuilder sb = new StringBuilder(json);
            for (int i = 0; i < diff; i++) {
                sb.append('}');
            }
            log.debug("🔧 Added {} closing brace(s) to balance JSON", diff);
            return sb.toString();
        }

        // Wenn mehr schließende als öffnende Klammern (entferne überschüssige)
        if (closeCount > openCount) {
            int diff = closeCount - openCount;
            log.debug("⚠️ More closing braces than opening ({} extra)", diff);
            // Entferne überschüssige schließende Klammern vom Ende
            String result = json;
            for (int i = 0; i < diff; i++) {
                int lastIndex = result.lastIndexOf('}');
                if (lastIndex > 0) {
                    result = result.substring(0, lastIndex) + result.substring(lastIndex + 1);
                }
            }
            return result;
        }

        return json;
    }
}

