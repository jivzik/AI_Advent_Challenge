package de.jivz.ai_challenge.openrouterservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OpenRouter API Request DTO
 * Unterstützt sowohl einfache Text-Nachrichten als auch multimodale Inhalte (Text + Audio/Bild)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenRouterApiRequest {

    private String model;
    private List<ChatMessage> messages;
    private Double temperature;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    @JsonProperty("top_p")
    private Double topP;

    /**
     * Chat-Nachricht mit optionalem multimodalem Content
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChatMessage {
        private String role;

        // Kann entweder String (für einfache Text-Nachrichten)
        // oder List<ContentPart> (für multimodale Inhalte) sein
        private Object content;

        // Convenience constructor für Text-only
        public ChatMessage(String role, String textContent) {
            this.role = role;
            this.content = textContent;
        }
    }

    /**
     * Content-Teil für multimodale Nachrichten
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ContentPart {
        private String type; // "text", "image_url", oder "input_audio"
        private String text; // Für type="text"

        @JsonProperty("image_url")
        private ImageUrl imageUrl; // Für type="image_url"

        @JsonProperty("input_audio")
        private InputAudio inputAudio; // Für type="input_audio"
    }

    /**
     * Image URL für multimodale Inhalte
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ImageUrl {
        private String url; // data:image/jpeg;base64,...
    }

    /**
     * Input Audio für Audio-Transkription (OpenRouter Format)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class InputAudio {
        private String data; // Base64-encoded audio data
        private String format; // "wav", "mp3", "aiff", "aac", "ogg", "flac", etc.
    }
}

