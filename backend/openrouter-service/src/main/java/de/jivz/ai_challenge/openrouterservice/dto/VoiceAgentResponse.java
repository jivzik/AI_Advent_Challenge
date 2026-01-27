package de.jivz.ai_challenge.openrouterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response-DTO für Voice Agent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceAgentResponse {
    private String transcription; // Transkribierter Text
    private String response; // LLM-Antwort
    private LocalDateTime timestamp; // Zeitstempel
    private String language; // Erkannte Sprache
    private Long transcriptionTimeMs; // Zeit für Transkription
    private Long llmProcessingTimeMs; // Zeit für LLM-Verarbeitung
    private Long totalTimeMs; // Gesamtzeit
    private String model; // Verwendetes LLM-Modell
    private String userId; // User ID
}
