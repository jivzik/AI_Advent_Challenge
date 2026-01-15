package de.jivz.teamassistantservice.controller;

import de.jivz.teamassistantservice.dto.TeamAssistantRequest;
import de.jivz.teamassistantservice.dto.TeamAssistantResponse;
import de.jivz.teamassistantservice.service.TeamAssistantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Team Assistant Controller - REST API для командного ассистента
 */
@RestController
@RequestMapping("/api/team-assistant")
@RequiredArgsConstructor
@Slf4j
public class TeamAssistantController {

    private final TeamAssistantService teamAssistantService;

    /**
     * Отправить запрос к Team Assistant
     */
    @PostMapping("/query")
    public ResponseEntity<TeamAssistantResponse> query(@RequestBody TeamAssistantRequest request) {
        try {
            log.info("📩 Received query from: {}", request.getUserEmail());

            TeamAssistantResponse response = teamAssistantService.handleQuery(request);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error handling query", e);

            // Return user-friendly error
            TeamAssistantResponse errorResponse = TeamAssistantResponse.builder()
                    .answer("Sorry, I encountered an error processing your request. Please try again.")
                    .build();

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Team Assistant is running");
    }
}