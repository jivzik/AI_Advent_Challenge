package de.jivz.ai_challenge.openrouterservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO für Chat-Responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response von einer Chat-Anfrage")
public class ChatResponse {

    @Schema(description = "Die Antwort des LLM", example = "Dies ist eine detaillierte Antwort auf deine Frage...")
    private String reply;

    @Schema(description = "Das verwendete Modell", example = "openrouter/auto")
    private String model;

    @Schema(description = "Anzahl der Tokens in der Eingabe", example = "10")
    private Integer inputTokens;

    @Schema(description = "Anzahl der Tokens in der Ausgabe", example = "25")
    private Integer outputTokens;

    @Schema(description = "Gesamte Anzahl der Tokens (Input + Output)", example = "35")
    private Integer totalTokens;

    @Schema(description = "Geschätzte Kosten in USD", example = "0.0001")
    private Double cost;

    @Schema(description = "Verarbeitungszeit in Millisekunden", example = "1250")
    private Long responseTimeMs;

    @Schema(description = """
            Grund für die Beendigung der Antwort.
            - stop: Normal beendet
            - length: Max Tokens erreicht
            - error: Fehler
            - content_filter: Content-Filter aktiviert""",
            example = "stop",
            allowableValues = {"stop", "length", "error", "content_filter"})
    private String finishReason;
}

