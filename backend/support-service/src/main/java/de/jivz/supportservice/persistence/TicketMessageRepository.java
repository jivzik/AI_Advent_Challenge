package de.jivz.supportservice.persistence;

import de.jivz.supportservice.persistence.entity.SupportTicket;
import de.jivz.supportservice.persistence.entity.TicketMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketMessageRepository extends JpaRepository<TicketMessage, UUID> {

    List<TicketMessage> findByTicketOrderByCreatedAtAsc(SupportTicket ticket);

    List<TicketMessage> findByTicketAndIsInternalFalseOrderByCreatedAtAsc(SupportTicket ticket);

    long countByTicket(SupportTicket ticket);

    long countByTicketAndSenderType(SupportTicket ticket, String senderType);
}
