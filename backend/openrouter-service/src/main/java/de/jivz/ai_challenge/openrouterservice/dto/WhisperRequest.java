package de.jivz.ai_challenge.openrouterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request-DTO für Audio-Transkription (Google Gemini Flash)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhisperRequest {
    @Builder.Default
    private String model = "google/gemini-flash-1.5-8b"; // Google Gemini Flash für Audio
    private String language; // Optional: ISO-639-1 Format (z.B. "de", "en")
    private String prompt; // Optional: Context für bessere Transkription
    private Double temperature; // Optional: 0-1, Sampling-Temperatur
}
