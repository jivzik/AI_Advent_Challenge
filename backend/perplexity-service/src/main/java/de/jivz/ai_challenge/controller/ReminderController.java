package de.jivz.ai_challenge.controller;

import de.jivz.ai_challenge.entity.ReminderSummary;
import de.jivz.ai_challenge.batch.OpenRouterReminderSchedulerService;
import de.jivz.ai_challenge.batch.ReminderSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller für Reminder-Zusammenfassungen.
 *
 * Endpoints:
 * - POST /api/reminder/trigger - Manueller Trigger des Perplexity-Reminder-Workflows
 * - POST /api/reminder/openrouter/trigger - Manueller Trigger des OpenRouter-Reminder-Workflows
 * - POST /api/reminder/task/create - Task-Erstellung mit Event-Recherche
 * - GET /api/reminder/summaries - Alle Zusammenfassungen eines Benutzers
 * - GET /api/reminder/latest - Neueste Zusammenfassung
 * - GET /api/reminder/pending - Nicht benachrichtigte Zusammenfassungen
 * - GET /api/reminder/status - Status-Informationen
 */
@RestController
@RequestMapping("/api/reminder")
@RequiredArgsConstructor
@Slf4j
public class ReminderController {

    private final ReminderSchedulerService reminderService;
    private final OpenRouterReminderSchedulerService openRouterReminderService;

    /**
     * Manueller Trigger des Perplexity-Reminder-Workflows.
     * Nützlich für Tests und Ad-hoc-Zusammenfassungen.
     *
     * @param userId Optional: Benutzer-ID (default: "manual-trigger")
     * @return Die erstellte Zusammenfassung
     */
    @PostMapping("/trigger")
    public ResponseEntity<ReminderSummary> triggerReminder(
            @RequestParam(defaultValue = "manual-trigger") String userId) {

        log.info("🔔 Manual Perplexity reminder trigger for user: {}", userId);

        try {
            ReminderSummary summary = reminderService.executeReminderWorkflow(userId);

            if (summary != null) {
                log.info("✅ Perplexity reminder triggered successfully. ID: {}", summary.getId());
                return ResponseEntity.ok(summary);
            } else {
                return ResponseEntity.noContent().build();
            }

        } catch (Exception e) {
            log.error("❌ Failed to trigger Perplexity reminder: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Manueller Trigger des OpenRouter-Reminder-Workflows.
     * Nutzt native Tool-Unterstützung von OpenRouter/OpenAI.
     *
     * @param userId Optional: Benutzer-ID (default: "manual-trigger-openrouter")
     * @return Die erstellte Zusammenfassung
     */
    @PostMapping("/openrouter/trigger")
    public ResponseEntity<ReminderSummary> triggerOpenRouterReminder(
            @RequestParam(defaultValue = "manual-trigger-openrouter") String userId) {

        log.info("🔔 Manual OpenRouter reminder trigger for user: {}", userId);

        try {
            ReminderSummary summary = openRouterReminderService.executeReminderWorkflow(userId);

            if (summary != null) {
                log.info("✅ OpenRouter reminder triggered successfully. ID: {}", summary.getId());
                return ResponseEntity.ok(summary);
            } else {
                return ResponseEntity.noContent().build();
            }

        } catch (Exception e) {
            log.error("❌ Failed to trigger OpenRouter reminder: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Task-Erstellung mit Event-Recherche über perplexity_search
     *
     * Workflow:
     * 1. Benutzer gibt Task-Anfrage ein (z.B. "Erstelle einen Task für Spring Conference 2025")
     * 2. perplexity_search Tool sammelt aktuelle Event-Informationen
     * 3. Sonar LLM erstellt strukturierten Task mit allen Details
     * 4. Task wird gespeichert in der Datenbank
     *
     * @param userId Benutzer-ID
     * @param request Map mit "taskRequest" Key
     * @return Die erstellte Task-Zusammenfassung
     */
/*    @PostMapping("/task/create")
    public ResponseEntity<ReminderSummary> createTaskWithEventResearch(
            @RequestParam String userId,
            @RequestBody Map<String, String> request) {

        String taskRequest = request.get("taskRequest");
        if (taskRequest == null || taskRequest.isBlank()) {
            log.warn("❌ Task request is empty for user: {}", userId);
            return ResponseEntity.badRequest().build();
        }

        log.info("🎯 Task creation workflow for user: {} with request: {}", userId, taskRequest);

        try {
            ReminderSummary taskSummary = taskCreationService.createTaskWithEventResearch(userId, taskRequest);

            if (taskSummary != null) {
                log.info("✅ Task created successfully with event research. ID: {}", taskSummary.getId());
                return ResponseEntity.ok(taskSummary);
            } else {
                return ResponseEntity.noContent().build();
            }

        } catch (Exception e) {
            log.error("❌ Failed to create task with event research: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(null);
        }
    }*/

    /**
     * Holt alle Zusammenfassungen für einen Benutzer.
     *
     * @param userId Benutzer-ID
     * @return Liste der Zusammenfassungen
     */
    @GetMapping("/summaries")
    public ResponseEntity<List<ReminderSummary>> getSummaries(
            @RequestParam String userId) {

        log.info("📋 Fetching summaries for user: {}", userId);
        List<ReminderSummary> summaries = reminderService.getSummariesForUser(userId);
        return ResponseEntity.ok(summaries);
    }

    /**
     * Holt die neueste Zusammenfassung (global).
     *
     * @return Die neueste Zusammenfassung
     */
    @GetMapping("/latest")
    public ResponseEntity<ReminderSummary> getLatestSummary() {
        log.info("📋 Fetching latest summary");
        ReminderSummary summary = reminderService.getLatestSummary();

        if (summary != null) {
            return ResponseEntity.ok(summary);
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    /**
     * Holt alle nicht benachrichtigten Zusammenfassungen.
     *
     * @return Liste der ausstehenden Benachrichtigungen
     */
    @GetMapping("/pending")
    public ResponseEntity<List<ReminderSummary>> getPendingNotifications() {
        log.info("📋 Fetching pending notifications");
        List<ReminderSummary> pending = reminderService.getPendingNotifications();
        return ResponseEntity.ok(pending);
    }

    /**
     * Status-Endpoint für den Scheduler.
     *
     * @return Status-Informationen
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        log.info("📊 Fetching reminder status");

        List<ReminderSummary> pending = reminderService.getPendingNotifications();
        ReminderSummary latest = reminderService.getLatestSummary();

        return ResponseEntity.ok(Map.of(
            "schedulerEnabled", true,
            "pendingNotifications", pending.size(),
            "latestSummaryId", latest != null ? latest.getId() : "none",
            "latestSummaryTime", latest != null ? latest.getCreatedAt().toString() : "never"
        ));
    }
}

