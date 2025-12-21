package de.jivz.ai_challenge.openrouterservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO für Chat-Anfragen an Spring AI
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Chat-Anfrage mit erweiterten Parametern")
public class ChatRequest {

    @Schema(description = "Die Benutzernachricht", example = "Erkläre mir Quantenmechanik")
    private String message;

    @Schema(description = """
            Das zu verwendende Modell.
            Wenn nicht angegeben, wird das Standard-Modell verwendet.
            Beispiele: openrouter/auto, gpt-3.5-turbo, gpt-4, claude-3-opus""",
            example = "openrouter/auto",
            defaultValue = "openrouter/auto")
    private String model;

    @Schema(description = """
            Kontrolle der Ausgabe-Kreativität (0.0 = determinstisch, 1.0 = kreativ).
            Standard: 0.7""",
            example = "0.7",
            minimum = "0.0",
            maximum = "2.0")
    private Double temperature;

    @Schema(description = """
            Maximale Anzahl von Tokens in der Antwort.
            Standard: 1000""",
            example = "2000",
            minimum = "1",
            maximum = "32000")
    private Integer maxTokens;

    @Schema(description = """
            Gesprächsverlauf für Kontext in Multi-Turn-Konversationen.
            Wird für besseres Verständnis verwendet.""",
            example = "[\"Hallo\", \"Hallo! Wie kann ich dir helfen?\"]")
    private List<String> conversationHistory;
}

