package de.jivz.supportservice.persistence;

import de.jivz.supportservice.persistence.entity.SupportTicket;
import de.jivz.supportservice.persistence.entity.SupportUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {

    Optional<SupportTicket> findByTicketNumber(String ticketNumber);

    List<SupportTicket> findByUserOrderByCreatedAtDesc(SupportUser user);

    List<SupportTicket> findByUserAndStatusInOrderByCreatedAtDesc(
            SupportUser user, List<String> statuses);

    List<SupportTicket> findByStatusOrderByCreatedAtDesc(String status);

    List<SupportTicket> findByCategoryOrderByCreatedAtDesc(String category);

    List<SupportTicket> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime start, LocalDateTime end);

    long countByStatus(String status);

    long countByCategory(String category);

    // Get open tickets for user
    @Query("SELECT t FROM SupportTicket t " +
            "WHERE t.user = :user " +
            "AND t.status IN ('open', 'in_progress', 'waiting_customer') " +
            "ORDER BY t.createdAt DESC")
    List<SupportTicket> findOpenTicketsByUser(@Param("user") SupportUser user);

    // Get tickets with SLA breach
    @Query("SELECT t FROM SupportTicket t " +
            "WHERE t.slaBreached = true " +
            "AND t.status NOT IN ('resolved', 'closed') " +
            "ORDER BY t.createdAt ASC")
    List<SupportTicket> findSlaBreachedTickets();

    // Search tickets by text
    @Query(value = "SELECT * FROM support_ticket t " +
            "WHERE t.search_vector @@ to_tsquery('simple', :query) " +
            "ORDER BY ts_rank(t.search_vector, to_tsquery('simple', :query)) DESC",
            nativeQuery = true)
    List<SupportTicket> searchByText(@Param("query") String query);
}