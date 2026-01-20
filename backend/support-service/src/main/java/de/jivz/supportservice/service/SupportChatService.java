package de.jivz.supportservice.service;

import de.jivz.supportservice.dto.Message;
import de.jivz.supportservice.dto.SupportChatRequest;
import de.jivz.supportservice.dto.SupportChatResponse;
import de.jivz.supportservice.mcp.MCPFactory;
import de.jivz.supportservice.mcp.model.ToolDefinition;
import de.jivz.supportservice.persistence.entity.SupportTicket;
import de.jivz.supportservice.persistence.entity.SupportUser;
import de.jivz.supportservice.persistence.entity.TicketMessage;
import de.jivz.supportservice.persistence.SupportTicketRepository;
import de.jivz.supportservice.persistence.SupportUserRepository;
import de.jivz.supportservice.persistence.TicketMessageRepository;
import de.jivz.supportservice.service.orchestrator.ToolExecutionOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Главный сервис для обработки Support Chat запросов.
 * Использует MCP для RAG поиска и ToolExecutionOrchestrator для AI ответов.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SupportChatService {

    private final SupportUserRepository userRepository;
    private final SupportTicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;
    private final MCPFactory mcpFactory;
    private final ToolExecutionOrchestrator toolExecutionOrchestrator;
    private final PromptLoaderService promptLoader;

    @Value("${support.ai.enabled:true}")
    private Boolean aiEnabled;

    @Value("${support.ai.temperature:0.7}")
    private Double aiTemperature;

    @Value("${support.ai.confidence-threshold:0.7}")
    private Double confidenceThreshold;


    /**
     * Определяет тип сообщения пользователя
     */
    private enum MessageIntent {
        GRATITUDE,      // Благодарность
        QUESTION,       // Вопрос
        COMPLAINT,      // Жалоба
        ACKNOWLEDGMENT  // Подтверждение
    }

    /**
     * Определяет intent сообщения
     */
    private MessageIntent detectIntent(String message) {
        String lower = message.toLowerCase();

        // Gratitude patterns
        List<String> gratitudePatterns = List.of(
                "спасибо", "благодарю", "thanks", "thank you",
                "помогло", "получилось", "работает", "решил",
                "всё понятно", "понял", "разобрался"
        );

        // Acknowledgment patterns
        List<String> acknowledgmentPatterns = List.of(
                "хорошо", "ладно", "ok", "окей", "понятно"
        );

        // Check gratitude
        if (gratitudePatterns.stream().anyMatch(lower::contains)) {
            return MessageIntent.GRATITUDE;
        }

        // Check acknowledgment
        if (acknowledgmentPatterns.stream().anyMatch(lower::contains)
                && message.length() < 30) { // Short messages
            return MessageIntent.ACKNOWLEDGMENT;
        }

        // Default to question
        return MessageIntent.QUESTION;
    }

    /**
     * Генерирует простой ответ на благодарность
     */
    private String generateGratitudeResponse() {
        List<String> responses = List.of(
                "Рады были помочь! Если возникнут ещё вопросы - обращайтесь.",
                "Отлично! Желаем успешной работы.",
                "Рады, что всё получилось! Обращайтесь, если понадобится помощь.",
                "Всегда пожалуйста! Если будут вопросы - мы здесь."
        );

        // Random response
        int index = (int) (Math.random() * responses.size());
        return responses.get(index);
    }


    /**
     * Обрабатывает запрос пользователя в Support Chat
     */
    @Transactional
    public SupportChatResponse handleUserMessage(SupportChatRequest request) {
        log.info("📩 Handling support request from: {}", request.getUserEmail());

        // 1. Найти или создать пользователя
        SupportUser user = findOrCreateUser(request.getUserEmail());

        // 2. Найти существующий тикет (если указан номер)
        SupportTicket ticket = null;
        if (request.getTicketNumber() != null) {
            ticket = ticketRepository.findByTicketNumber(request.getTicketNumber())
                    .orElseThrow(() -> new RuntimeException("Ticket not found: " + request.getTicketNumber()));

            // 3. Сохранить сообщение пользователя в существующий тикет
            saveUserMessage(ticket, user, request.getMessage());
        }

        // 4. Если AI отключен - создать тикет и вернуть ответ
        if (!aiEnabled) {
            if (ticket == null) {
                ticket = createTicket(request, user);
            }
            return buildResponseWithoutAI(ticket);
        }

        // 5. Определить intent сообщения
        MessageIntent intent = detectIntent(request.getMessage());

        // 6. Если это благодарность и тикет существует - простой ответ без RAG
        if (ticket != null && (intent == MessageIntent.GRATITUDE || intent == MessageIntent.ACKNOWLEDGMENT)) {
            log.info("💬 Detected {} intent - simple response", intent);

            String simpleAnswer = generateGratitudeResponse();

            // Сохранить AI ответ
            saveAIMessage(ticket, simpleAnswer, List.of(), BigDecimal.valueOf(1.0));

            // Обновить статус
            ticket.setStatus("in_progress");
            ticketRepository.save(ticket);

            return SupportChatResponse.builder()
                    .ticketNumber(ticket.getTicketNumber())
                    .status(ticket.getStatus())
                    .answer(simpleAnswer)
                    .isAiGenerated(true)
                    .confidenceScore(BigDecimal.valueOf(1.0))
                    .sources(List.of())
                    .needsHumanAgent(false)
                    .timestamp(LocalDateTime.now())
                    .messageCount((int) messageRepository.countByTicket(ticket))
                    .build();
        }

        // 7. Построить сообщения с контекстом пользователя (не тикета!)
        List<Message> messages = buildMessagesWithUserContext(user, request, ticket);

        // 8. Clear thread local context vor dem tool loop
        de.jivz.supportservice.service.orchestrator.ThreadLocalTicketContext.clear();

        // 9. Запустить tool execution loop mit llmProvider
        String llmProvider = request.getLlmProvider() != null ? request.getLlmProvider() : "remote";
        log.info("🤖 Using LLM provider: {}", llmProvider);
        String aiAnswer = toolExecutionOrchestrator.executeToolLoop(messages, aiTemperature, llmProvider);

        // 10. После AI-обработки проверить, был ли создан GitHub issue через tool
        String createdTicketNumber = de.jivz.supportservice.service.orchestrator.ThreadLocalTicketContext.getTicketNumber();
        String gitHubIssueUrl = de.jivz.supportservice.service.orchestrator.ThreadLocalTicketContext.getGitHubIssueUrl();

        if (ticket == null && createdTicketNumber != null) {
            // GitHub Issue был создан - создать соответствующий Ticket в БД
            ticket = createTicketFromGitHubIssue(request, user, createdTicketNumber, gitHubIssueUrl);
            log.info("🎫 Created ticket from GitHub issue: {} -> {}", createdTicketNumber, ticket.getTicketNumber());
        }

        // 11. Clear context nach der Verwendung
        de.jivz.supportservice.service.orchestrator.ThreadLocalTicketContext.clear();

        // 12. Если тикет создан - сохранить ответ и обновить
        if (ticket != null) {
            // Сохранить начальное сообщение пользователя, если еще не сохранено
            long msgCount = messageRepository.countByTicket(ticket);
            if (msgCount == 0) {
                saveUserMessage(ticket, user, request.getMessage());
            }

            // Извлечь источники из ответа
            List<String> sources = extractSourcesFromAnswer(aiAnswer);

            // Определить confidence score
            BigDecimal confidence = calculateConfidence(sources);

            // Проверить эскалацию
            boolean needsHuman = shouldEscalateToHuman(confidence, ticket, sources);
            String escalationReason = needsHuman ? determineEscalationReason(confidence, ticket, sources) : null;

            // Сохранить ответ AI
            saveAIMessage(ticket, aiAnswer, sources, confidence);

            // Обновить статус тикета
            updateTicketStatus(ticket, needsHuman);

            // Построить ответ
            return buildResponse(ticket, aiAnswer, sources, confidence, needsHuman, escalationReason);
        } else {
            // Тикет не был создан - простой ответ без тикета
            return SupportChatResponse.builder()
                    .ticketNumber(null)
                    .status("resolved")
                    .answer(aiAnswer)
                    .isAiGenerated(true)
                    .confidenceScore(BigDecimal.valueOf(0.95))
                    .sources(List.of())
                    .needsHumanAgent(false)
                    .timestamp(LocalDateTime.now())
                    .messageCount(0)
                    .build();
        }
    }

    /**
     * Находит пользователя по email или создает нового
     */
    private SupportUser findOrCreateUser(String email) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    log.info("👤 Creating new user: {}", email);
                    SupportUser newUser = SupportUser.builder()
                            .email(email)
                            .fullName("Customer") // Базовое имя
                            .companyName("Unknown")
                            .companyInn("0000000000")
                            .isVerified(false)
                            .isActive(true)
                            .build();
                    return userRepository.save(newUser);
                });
    }

    /**
     * Создает новый тикет
     */
    private SupportTicket createTicket(SupportChatRequest request, SupportUser user) {
        String ticketNumber = generateTicketNumber();
        log.info("🎫 Creating new ticket: {}", ticketNumber);

        SupportTicket ticket = SupportTicket.builder()
                .ticketNumber(ticketNumber)
                .user(user)
                .subject(extractSubject(request.getMessage()))
                .description(request.getMessage())
                .category(request.getCategory() != null ? request.getCategory() : "other")
                .priority(request.getPriority() != null ? request.getPriority() : "medium")
                .status("open")
                .orderId(request.getOrderId())
                .productId(request.getProductId())
                .errorCode(request.getErrorCode())
                .build();

        return ticketRepository.save(ticket);
    }

    /**
     * Создает Ticket из GitHub Issue
     */
    private SupportTicket createTicketFromGitHubIssue(SupportChatRequest request, SupportUser user,
                                                      String gitHubTicketNumber, String gitHubIssueUrl) {
        String ticketNumber = generateTicketNumber();
        log.info("🎫 Creating ticket from GitHub issue: {} -> {}", gitHubTicketNumber, ticketNumber);

        SupportTicket ticket = SupportTicket.builder()
                .ticketNumber(ticketNumber)
                .user(user)
                .subject(extractSubject(request.getMessage()))
                .description(request.getMessage())
                .category(request.getCategory() != null ? request.getCategory() : "other")
                .priority(request.getPriority() != null ? request.getPriority() : "medium")
                .status("open")
                .orderId(request.getOrderId())
                .productId(request.getProductId())
                .errorCode(request.getErrorCode())
                .build();

        // Store GitHub Issue reference in metadata or external reference field
        // You might need to add a field to SupportTicket entity for this
        // For now, we'll add it to the description
        if (gitHubIssueUrl != null) {
            ticket.setDescription(
                ticket.getDescription() +
                "\n\n**GitHub Issue:** " + gitHubTicketNumber + "\n" +
                "**URL:** " + gitHubIssueUrl
            );
        }

        return ticketRepository.save(ticket);
    }

    /**
     * Генерирует номер тикета
     */
    private String generateTicketNumber() {
        return String.format("TICK-%d-%04d",
                LocalDateTime.now().getYear(),
                (int) (Math.random() * 10000));
    }

    /**
     * Извлекает тему из сообщения (первые 100 символов)
     */
    private String extractSubject(String message) {
        if (message.length() <= 100) {
            return message;
        }
        return message.substring(0, 97) + "...";
    }

    /**
     * Сохраняет сообщение пользователя
     */
    private void saveUserMessage(SupportTicket ticket, SupportUser user, String message) {
        TicketMessage userMessage = TicketMessage.builder()
                .ticket(ticket)
                .senderType(TicketMessage.SenderType.CUSTOMER.getValue())
                .senderId(user.getId().toString())
                .senderName(user.getFullName())
                .message(message)
                .isAiGenerated(false)
                .isInternal(false)
                .build();

        messageRepository.save(userMessage);
        log.debug("💬 Saved user message for ticket: {}", ticket.getTicketNumber());
    }

    /**
     * Сохраняет ответ AI
     */
    private void saveAIMessage(SupportTicket ticket, String answer, List<String> sources, BigDecimal confidence) {
        TicketMessage aiMessage = TicketMessage.builder()
                .ticket(ticket)
                .senderType(TicketMessage.SenderType.AI.getValue())
                .senderId("support-ai")
                .senderName("AI Assistant")
                .message(answer)
                .isAiGenerated(true)
                .ragSources(sources.toArray(new String[0]))
                .confidenceScore(confidence)
                .isInternal(false)
                .build();

        messageRepository.save(aiMessage);
        log.debug("🤖 Saved AI message for ticket: {}", ticket.getTicketNumber());
    }

    /**
     * Строит сообщения с контекстом пользователя и опционально тикета для AI
     */
    private List<Message> buildMessagesWithUserContext(SupportUser user, SupportChatRequest request, SupportTicket ticket) {
        // Получить все MCP tools (включая RAG и ticket creation)
        List<ToolDefinition> tools = mcpFactory.getAllToolDefinitions();

        // Построить сообщения
        List<Message> messages = new ArrayList<>();

        // 1. System prompt с контекстом
        String systemPrompt = buildSystemPromptWithUserContext(user, request, ticket, tools);
        messages.add(new Message("system", systemPrompt));

        // 2. История тикета (если тикет существует)
        if (ticket != null) {
            List<TicketMessage> history = messageRepository.findByTicketOrderByCreatedAtAsc(ticket);
            int startIndex = Math.max(0, history.size() - 5);

            for (int i = startIndex; i < history.size(); i++) {
                TicketMessage msg = history.get(i);
                String role = msg.getSenderType().equals("customer") ? "user" : "assistant";
                messages.add(new Message(role, msg.getMessage()));
            }
        }

        // 3. Текущее сообщение
        messages.add(new Message("user", request.getMessage()));

        log.info("📝 Built {} messages for support chat", messages.size());
        return messages;
    }

    /**
     * Строит system prompt с контекстом пользователя, запроса и опционально тикета
     */
    private String buildSystemPromptWithUserContext(SupportUser user, SupportChatRequest request, SupportTicket ticket, List<ToolDefinition> tools) {
        StringBuilder prompt = new StringBuilder();

        // Базовый support assistant prompt
        String basePrompt = promptLoader.loadPrompt("support-assistant");
        if (basePrompt != null) {
            prompt.append(basePrompt).append("\n\n");
        }

        // Добавить секцию с MCP tools
        String toolsPrompt = promptLoader.buildSystemPromptWithTools(tools);
        prompt.append(toolsPrompt).append("\n\n");

        // Добавить контекст пользователя
        prompt.append("## USER CONTEXT:\n");
        prompt.append(String.format("- User Email: %s\n", user.getEmail()));
        prompt.append(String.format("- User Name: %s\n", user.getFullName()));
        prompt.append(String.format("- Company: %s\n", user.getCompanyName()));
        prompt.append(String.format("- Loyalty Tier: %s\n", user.getLoyaltyTier()));
        prompt.append(String.format("- Verified: %s\n", user.getIsVerified() ? "Yes" : "No"));

        // Добавить контекст запроса
        if (request.getCategory() != null) {
            prompt.append(String.format("- Request Category: %s\n", request.getCategory()));
        }
        if (request.getPriority() != null) {
            prompt.append(String.format("- Request Priority: %s\n", request.getPriority()));
        }
        if (request.getOrderId() != null) {
            prompt.append(String.format("- Related Order: %s\n", request.getOrderId()));
        }
        if (request.getProductId() != null) {
            prompt.append(String.format("- Related Product: %s\n", request.getProductId()));
        }
        if (request.getErrorCode() != null) {
            prompt.append(String.format("- Error Code: %s\n", request.getErrorCode()));
        }

        // Добавить контекст тикета (если существует)
        if (ticket != null) {
            prompt.append("\n## EXISTING TICKET CONTEXT:\n");
            prompt.append(String.format("- Ticket Number: %s\n", ticket.getTicketNumber()));
            prompt.append(String.format("- Status: %s\n", ticket.getStatus()));
            prompt.append(String.format("- Subject: %s\n", ticket.getSubject()));
            prompt.append("This is a continuation of an existing conversation.\n");
        } else {
            prompt.append("\n## NEW REQUEST:\n");
            prompt.append("This is a new support request. Assess if a support ticket needs to be created.\n");
        }

        // Инструкция использовать RAG für поиска в FAQ
        prompt.append("\n## IMPORTANT INSTRUCTIONS:\n");
        prompt.append("1. **ALWAYS** use the `rag:search_documents` tool to search the FAQ knowledge base first.\n");
        prompt.append("2. **Decide if a GitHub issue is needed**: If the issue is complex, requires human review, or cannot be resolved from FAQ, use the `git:create_github_issue` tool to create a GitHub issue as support ticket.\n");
        prompt.append("3. **Simple questions**: If you can fully answer from FAQ, provide the answer directly without creating an issue.\n");
        prompt.append("4. **GitHub issue criteria**: Create issue for: critical issues, billing problems, account issues, complex technical problems, escalations.\n");
        prompt.append("5. **Issue format**: When creating GitHub issue, use clear title and detailed body. Add labels like 'support', 'bug', 'question' as appropriate.\n");
        prompt.append("\nThe FAQ contains information about:\n");
        prompt.append("- Authorization and authentication\n");
        prompt.append("- Catalog and pricing\n");
        prompt.append("- Order processing and tracking\n");
        prompt.append("- Payment and billing\n");
        prompt.append("- Delivery and shipping\n");
        prompt.append("- Returns and exchanges\n");
        prompt.append("- API integration\n");

        return prompt.toString();
    }

    /**
     * Извлекает источники из ответа с фильтрацией только FAQ документов
     */
    private List<String> extractSourcesFromAnswer(String answer) {
        List<String> sources = new ArrayList<>();

        // Ищем секцию с источниками
        if (answer.contains("📚") && (answer.contains("Источники") || answer.contains("Sources") || answer.contains("Quellen"))) {
            String[] lines = answer.split("\n");
            boolean inSourcesSection = false;

            for (String line : lines) {
                if (line.contains("📚") && (line.contains("Источники") || line.contains("Sources") || line.contains("Quellen"))) {
                    inSourcesSection = true;
                    continue;
                }

                if (inSourcesSection) {
                    // Если встретили новую секцию - выход
                    if (line.trim().startsWith("#") || line.trim().startsWith("---")) {
                        break;
                    }

                    // Извлекаем название документа из маркированного списка
                    if (line.trim().matches("^\\d+\\.\\s*`.*`$")) {
                        String source = line.replaceAll("^\\d+\\.\\s*`", "").replaceAll("`.*", "").trim();

                        // ⭐ ФИЛЬТР: только FAQ документы
                        if (!source.isEmpty() && isFAQDocument(source)) {
                            sources.add(source);
                        }
                    }
                }
            }
        }

        log.debug("📚 Extracted {} FAQ sources from answer", sources.size());
        return sources;
    }

    /**
     * Проверяет, является ли документ FAQ документом
     */
    private boolean isFAQDocument(String documentName) {
        // Список разрешенных FAQ документов
        List<String> allowedFAQPatterns = List.of(
                "webshop_faq",
                "faq",
                "support",
                "help",
                "guide"
        );

        String lowerName = documentName.toLowerCase();

        // Исключаем технические документы
        if (lowerName.contains("architecture") ||
                lowerName.contains("quickstart") ||
                lowerName.contains("implementation") ||
                lowerName.contains("feature") ||
                lowerName.contains("setup")) {
            return false;
        }

        // Проверяем соответствие FAQ паттернам
        return allowedFAQPatterns.stream()
                .anyMatch(lowerName::contains);
    }
    /**
     * Вычисляет confidence score на основе наличия FAQ источников
     */
    private BigDecimal calculateConfidence(List<String> sources) {
        // Нет источников из FAQ
        if (sources.isEmpty()) {
            log.debug("📊 Confidence: 0.3 (no FAQ sources)");
            return BigDecimal.valueOf(0.3);
        }

        // 1 FAQ источник - средний confidence
        if (sources.size() == 1) {
            log.debug("📊 Confidence: 0.75 (1 FAQ source)");
            return BigDecimal.valueOf(0.75);
        }

        // 2+ FAQ источника - высокий confidence
        log.debug("📊 Confidence: 0.95 ({} FAQ sources)", sources.size());
        return BigDecimal.valueOf(0.95);
    }

    /**
     * Определяет, нужна ли эскалация к человеку
     */
    private boolean shouldEscalateToHuman(BigDecimal confidence, SupportTicket ticket, List<String> sources) {
        // Критический приоритет - ВСЕГДА к человеку
        if ("critical".equals(ticket.getPriority())) {
            log.info("🚨 Critical priority - escalating to human");
            return true;
        }

        // Нет FAQ источников - к человеку
        if (sources.isEmpty()) {
            log.info("❓ No FAQ sources found - escalating to human");
            return true;
        }

        // Низкий confidence - к человеку
        if (confidence.compareTo(BigDecimal.valueOf(confidenceThreshold)) < 0) {
            log.info("⚠️ Low confidence ({}) - escalating to human", confidence);
            return true;
        }

        // Во всех остальных случаях - AI может обработать
        log.info("✅ AI can handle (confidence: {}, sources: {})", confidence, sources.size());
        return false;
    }

    /**
     * Определяет причину эскалации
     */
    private String determineEscalationReason(BigDecimal confidence, SupportTicket ticket, List<String> sources) {
        if ("critical".equals(ticket.getPriority())) {
            return "Critical priority issue";
        }
        if (sources.isEmpty()) {
            return "No relevant FAQ information found";
        }
        if (confidence.compareTo(BigDecimal.valueOf(confidenceThreshold)) < 0) {
            return "Low confidence in AI response";
        }
        return "Complex issue requiring human review";
    }

    /**
     * Обновляет статус тикета
     */
    private void updateTicketStatus(SupportTicket ticket, boolean needsHuman) {
        if (needsHuman) {
            ticket.setStatus("waiting_agent");
            ticket.setAssignedTo("support-team");
        } else {
            ticket.setStatus("in_progress");
        }

        // Установить время первого ответа, если это первый ответ
        if (ticket.getFirstResponseAt() == null) {
            ticket.setFirstResponseAt(LocalDateTime.now());
            long minutes = java.time.Duration.between(ticket.getCreatedAt(), LocalDateTime.now()).toMinutes();
            ticket.setFirstResponseTimeMinutes((int) minutes);
        }

        ticketRepository.save(ticket);
    }

    /**
     * Строит ответ без AI (если AI отключен)
     */
    private SupportChatResponse buildResponseWithoutAI(SupportTicket ticket) {
        return SupportChatResponse.builder()
                .ticketNumber(ticket.getTicketNumber())
                .status(ticket.getStatus())
                .answer("Ваш запрос принят. Специалист свяжется с вами в ближайшее время.")
                .isAiGenerated(false)
                .needsHumanAgent(true)
                .escalationReason("AI is disabled")
                .timestamp(LocalDateTime.now())
                .messageCount((int) messageRepository.countByTicket(ticket))
                .build();
    }

    /**
     * Строит финальный ответ
     */
    private SupportChatResponse buildResponse(SupportTicket ticket, String answer, List<String> sources,
                                              BigDecimal confidence, boolean needsHuman, String escalationReason) {
        // Добавить примечание об эскалации к ответу
        String finalAnswer = answer;
        if (needsHuman && !answer.contains("передан специалисту")) {
            finalAnswer += "\n\n⚠️ **Примечание:** Ваш вопрос передан специалисту службы поддержки для более детального рассмотрения.";
        }

        return SupportChatResponse.builder()
                .ticketNumber(ticket.getTicketNumber())
                .status(ticket.getStatus())
                .answer(finalAnswer)
                .isAiGenerated(true)
                .confidenceScore(confidence)
                .sources(sources)
                .needsHumanAgent(needsHuman)
                .escalationReason(escalationReason)
                .timestamp(LocalDateTime.now())
                .messageCount((int) messageRepository.countByTicket(ticket))
                .firstResponseAt(ticket.getFirstResponseAt())
                .slaBreached(ticket.getSlaBreached())
                .build();
    }
}