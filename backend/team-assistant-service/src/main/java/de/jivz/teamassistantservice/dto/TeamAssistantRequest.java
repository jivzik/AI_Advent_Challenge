package de.jivz.teamassistantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to Team Assistant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamAssistantRequest {

    private String userEmail;
    private String query;
    private String sessionId; // optional - для группировки запросов
}