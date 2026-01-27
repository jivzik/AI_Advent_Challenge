package de.jivz.ai_challenge.openrouterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request-DTO für Voice Agent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceAgentRequest {
    private String userId; // User ID für Kontext
    private String language; // Optional: Sprache (de, en, etc.)
    private String model; // Optional: LLM-Modell
    private Double temperature; // Optional: LLM-Temperatur
    private String systemPrompt; // Optional: System-Prompt für LLM
}
