package de.jivz.supportservice.controller;

import de.jivz.supportservice.dto.SupportChatRequest;
import de.jivz.supportservice.dto.SupportChatResponse;
import de.jivz.supportservice.persistence.entity.SupportTicket;
import de.jivz.supportservice.persistence.entity.TicketMessage;
import de.jivz.supportservice.persistence.SupportTicketRepository;
import de.jivz.supportservice.persistence.TicketMessageRepository;
import de.jivz.supportservice.service.SupportChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API для Support Chat
 */
@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@Slf4j
public class SupportChatController {

    private final SupportChatService supportChatService;
    private final SupportTicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;

    /**
     * Отправить сообщение в Support Chat
     *
     * POST /api/support/chat
     */
    @PostMapping("/chat")
    public ResponseEntity<SupportChatResponse> chat(@RequestBody SupportChatRequest request) {
        log.info("📨 Received support chat request from: {}", request.getUserEmail());

        try {
            SupportChatResponse response = supportChatService.handleUserMessage(request);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error handling support request: {}", e.getMessage(), e);

            return ResponseEntity.internalServerError()
                    .body(SupportChatResponse.builder()
                            .answer("Извините, произошла ошибка при обработке вашего запроса. " +
                                    "Пожалуйста, попробуйте позже или свяжитесь с поддержкой: " +
                                    "support@webshop.example.com")
                            .isAiGenerated(false)
                            .needsHumanAgent(true)
                            .escalationReason("System error")
                            .build());
        }
    }

    /**
     * Получить историю тикета
     *
     * GET /api/support/tickets/{ticketNumber}/messages
     */
    @GetMapping("/tickets/{ticketNumber}/messages")
    public ResponseEntity<?> getTicketMessages(@PathVariable String ticketNumber) {
        log.info("📜 Fetching messages for ticket: {}", ticketNumber);

        try {
            SupportTicket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                    .orElseThrow(() -> new RuntimeException("Ticket not found"));

            List<TicketMessage> messages = messageRepository
                    .findByTicketAndIsInternalFalseOrderByCreatedAtAsc(ticket);

            List<MessageDTO> messageDTOs = messages.stream()
                    .map(m -> MessageDTO.builder()
                            .senderType(m.getSenderType())
                            .senderName(m.getSenderName())
                            .message(m.getMessage())
                            .isAiGenerated(m.getIsAiGenerated())
                            .confidenceScore(m.getConfidenceScore())
                            .sources(m.getRagSources() != null ? List.of(m.getRagSources()) : List.of())
                            .createdAt(m.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());

            return ResponseEntity.ok(messageDTOs);

        } catch (Exception e) {
            log.error("❌ Error fetching ticket messages: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Получить информацию о тикете
     *
     * GET /api/support/tickets/{ticketNumber}
     */
    @GetMapping("/tickets/{ticketNumber}")
    public ResponseEntity<?> getTicket(@PathVariable String ticketNumber) {
        log.info("🎫 Fetching ticket: {}", ticketNumber);

        try {
            SupportTicket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                    .orElseThrow(() -> new RuntimeException("Ticket not found"));

            TicketDTO ticketDTO = TicketDTO.builder()
                    .ticketNumber(ticket.getTicketNumber())
                    .subject(ticket.getSubject())
                    .category(ticket.getCategory())
                    .priority(ticket.getPriority())
                    .status(ticket.getStatus())
                    .orderId(ticket.getOrderId())
                    .productId(ticket.getProductId())
                    .createdAt(ticket.getCreatedAt())
                    .firstResponseAt(ticket.getFirstResponseAt())
                    .slaBreached(ticket.getSlaBreached())
                    .messageCount((int) messageRepository.countByTicket(ticket))
                    .build();

            return ResponseEntity.ok(ticketDTO);

        } catch (Exception e) {
            log.error("❌ Error fetching ticket: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Support Service is running");
    }

    // DTOs for responses
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class MessageDTO {
        private String senderType;
        private String senderName;
        private String message;
        private Boolean isAiGenerated;
        private java.math.BigDecimal confidenceScore;
        private List<String> sources;
        private java.time.LocalDateTime createdAt;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class TicketDTO {
        private String ticketNumber;
        private String subject;
        private String category;
        private String priority;
        private String status;
        private String orderId;
        private String productId;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime firstResponseAt;
        private Boolean slaBreached;
        private Integer messageCount;
    }
}