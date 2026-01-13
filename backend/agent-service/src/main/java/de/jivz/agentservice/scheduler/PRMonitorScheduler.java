package de.jivz.agentservice.scheduler;

import de.jivz.agentservice.service.PRDetectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Scheduler for monitoring new Pull Requests
 * Runs every 2 minutes and triggers code review for new PRs
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "code-review.scheduler.enabled",
        havingValue = "true",
        matchIfMissing = true  // Enabled by default
)
public class PRMonitorScheduler {

    private final PRDetectorService detectorService;

    /**
     * Check for new PRs every 2 minutes
     * ShedLock prevents concurrent execution in distributed environment
     */
    @Scheduled(fixedDelayString = "${code-review.scheduler.interval:60000}") // 2 min default
    @SchedulerLock(
            name = "pr_monitor_scheduler",
            lockAtLeastFor = "1m",  // Hold lock for at least 1 minute
            lockAtMostFor = "10m"   // Release lock after 10 minutes max
    )
    public void monitorPullRequests() {
        log.info("🔍 PR Monitor: Starting scan at {}", LocalDateTime.now());

        try {
            int processedCount = detectorService.detectAndProcessNewPRs();

            if (processedCount > 0) {
                log.info("✅ PR Monitor: Processed {} new PR(s)", processedCount);
            } else {
                log.debug("ℹ️  PR Monitor: No new PRs found");
            }

        } catch (Exception e) {
            log.error("❌ PR Monitor: Error during scan - {}", e.getMessage(), e);
        }
    }

    /**
     * Health check - logs scheduler status every 10 minutes
     */
    @Scheduled(fixedDelay = 600000) // 10 minutes
    public void healthCheck() {
        log.debug("💚 PR Monitor: Scheduler is alive");
    }
}