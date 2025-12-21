package de.jivz.ai_challenge.openrouterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Nachricht für die Konversation mit dem LLM.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private String role;  // "system", "user", "assistant"
    private String content;
}

