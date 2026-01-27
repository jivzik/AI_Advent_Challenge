package de.jivz.ai_challenge.openrouterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response-DTO für Whisper API
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhisperResponse {
    private String text; // Transkribierter Text
    private String language; // Erkannte Sprache
    private Double duration; // Audio-Dauer in Sekunden
    private String model; // Verwendetes Modell
}
