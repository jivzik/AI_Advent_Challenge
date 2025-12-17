package de.jivz.ai_challenge.repository;

import de.jivz.ai_challenge.entity.ReminderSummary;
import de.jivz.ai_challenge.entity.ReminderSummary.SummaryType;
import de.jivz.ai_challenge.entity.ReminderSummary.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository für ReminderSummary Entities.
 *
 * Bietet Methoden für:
 * - Abfragen von Zusammenfassungen nach Benutzer, Typ, Priorität
 * - Finden von nicht benachrichtigten Reminders
 * - Zeitbasierte Abfragen
 * - Aggregationen und Statistiken
 */
@Repository
public interface ReminderSummaryRepository extends JpaRepository<ReminderSummary, Long> {

    /**
     * Findet alle Zusammenfassungen für einen Benutzer, sortiert nach Erstellungsdatum.
     */
    List<ReminderSummary> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * Findet alle Zusammenfassungen eines bestimmten Typs für einen Benutzer.
     */
    List<ReminderSummary> findByUserIdAndSummaryTypeOrderByCreatedAtDesc(String userId, SummaryType summaryType);

    /**
     * Findet alle nicht benachrichtigten Summaries.
     * Wird vom Scheduler verwendet um Benachrichtigungen zu senden.
     */
    List<ReminderSummary> findByNotifiedFalseOrderByCreatedAtAsc();

    /**
     * Findet alle nicht benachrichtigten Summaries für einen bestimmten Benutzer.
     */
    List<ReminderSummary> findByUserIdAndNotifiedFalseOrderByCreatedAtAsc(String userId);

    /**
     * Findet Summaries mit hoher Priorität die noch nicht benachrichtigt wurden.
     */
    List<ReminderSummary> findByPriorityAndNotifiedFalseOrderByCreatedAtAsc(Priority priority);

    /**
     * Findet die neueste Zusammenfassung eines Typs für einen Benutzer.
     */
    Optional<ReminderSummary> findTopByUserIdAndSummaryTypeOrderByCreatedAtDesc(String userId, SummaryType summaryType);

    /**
     * Findet alle Zusammenfassungen die nach einem bestimmten Datum erstellt wurden.
     */
    List<ReminderSummary> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime after);

    /**
     * Findet alle Zusammenfassungen innerhalb eines Zeitraums.
     */
    List<ReminderSummary> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

    /**
     * Findet Summaries wo nextReminderAt vor dem angegebenen Zeitpunkt liegt.
     * Wird vom Scheduler verwendet um fällige Erinnerungen zu finden.
     */
    List<ReminderSummary> findByNextReminderAtBeforeAndNotifiedFalse(LocalDateTime before);

    /**
     * Zählt alle Zusammenfassungen für einen Benutzer.
     */
    long countByUserId(String userId);

    /**
     * Zählt alle nicht benachrichtigten Zusammenfassungen.
     */
    long countByNotifiedFalse();

    /**
     * Findet Zusammenfassungen mit Paginierung für einen Benutzer.
     */
    Page<ReminderSummary> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Findet die letzten N Zusammenfassungen (für Dashboard).
     */
    List<ReminderSummary> findTop10ByOrderByCreatedAtDesc();

    /**
     * Markiert alle Summaries eines Benutzers als benachrichtigt.
     */
    @Modifying
    @Query("UPDATE ReminderSummary r SET r.notified = true, r.notifiedAt = :notifiedAt WHERE r.userId = :userId AND r.notified = false")
    int markAllAsNotified(@Param("userId") String userId, @Param("notifiedAt") LocalDateTime notifiedAt);

    /**
     * Markiert eine einzelne Summary als benachrichtigt.
     */
    @Modifying
    @Query("UPDATE ReminderSummary r SET r.notified = true, r.notifiedAt = :notifiedAt WHERE r.id = :id")
    int markAsNotified(@Param("id") Long id, @Param("notifiedAt") LocalDateTime notifiedAt);

    /**
     * Löscht alle Summaries älter als das angegebene Datum.
     * Für Cleanup-Zwecke.
     */
    @Modifying
    @Query("DELETE FROM ReminderSummary r WHERE r.createdAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);

    /**
     * Findet die aktuellste Zusammenfassung (global).
     */
    Optional<ReminderSummary> findTopByOrderByCreatedAtDesc();

    /**
     * Findet alle Typen von Summaries für einen Benutzer.
     */
    @Query("SELECT DISTINCT r.summaryType FROM ReminderSummary r WHERE r.userId = :userId")
    List<SummaryType> findDistinctSummaryTypesByUserId(@Param("userId") String userId);
}

