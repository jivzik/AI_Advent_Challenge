package de.jivz.ai_challenge.openrouterservice.service;

import de.jivz.ai_challenge.openrouterservice.dto.Message;
import de.jivz.ai_challenge.openrouterservice.persistence.MemoryRepository;
import de.jivz.ai_challenge.openrouterservice.persistence.entity.MemoryEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * HistoryPersistenceService - Абстракция для работы с персистентностью истории
 *
 * Single Responsibility Principle:
 * - Только сохранение и загрузка истории из БД
 * - Не содержит бизнес-логику чата
 * - Чистое разделение ответственности
 *
 * Функциональность:
 * - Загрузка истории из PostgreSQL
 * - Сохранение отдельного сообщения
 * - Сохранение множества сообщений (batch operation)
 * - Удаление истории
 * - Получение статистики
 *
 * Strategy Pattern:
 * - Может быть реализована для разных хранилищ (PostgreSQL, MongoDB, Redis)
 * - Текущая реализация: PostgreSQL через MemoryRepository
 */
@Slf4j
@Service
public class HistoryPersistenceService {

    private final MemoryRepository memoryRepository;

    public HistoryPersistenceService(MemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
        log.info("HistoryPersistenceService initialized");
    }

    /**
     * Загружает полную историю конверсации из БД
     *
     * @param conversationId ID конверсации
     * @return список сообщений в хронологическом порядке
     */
    @Transactional(readOnly = true)
    public List<Message> loadHistory(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            log.debug("Cannot load history: conversationId is empty");
            return new ArrayList<>();
        }

        try {
            List<MemoryEntry> entries = memoryRepository.findByConversationIdOrderByTimestampAsc(conversationId);

            if (entries.isEmpty()) {
                log.debug("No history found for conversationId: {}", conversationId);
                return new ArrayList<>();
            }

            List<Message> messages = entries.stream()
                    .map(entry -> new Message(entry.getRole(), entry.getContent()))
                    .collect(Collectors.toList());

            log.debug("📥 Loaded {} messages from DB for conversationId: {}", messages.size(), conversationId);
            return messages;

        } catch (Exception e) {
            log.error("❌ Error loading history for conversationId {}: {}", conversationId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Загружает только оригинальные (не сжатые) сообщения
     *
     * @param conversationId ID конверсации
     * @return список оригинальных сообщений
     */
    @Transactional(readOnly = true)
    public List<Message> loadOriginalHistory(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            log.debug("Cannot load original history: conversationId is empty");
            return new ArrayList<>();
        }

        try {
            List<MemoryEntry> entries = memoryRepository.findByConversationIdAndIsCompressedFalseOrderByTimestampAsc(conversationId);

            List<Message> messages = entries.stream()
                    .map(entry -> new Message(entry.getRole(), entry.getContent()))
                    .collect(Collectors.toList());

            log.debug("📥 Loaded {} original messages from DB for conversationId: {}", messages.size(), conversationId);
            return messages;

        } catch (Exception e) {
            log.error("❌ Error loading original history for conversationId {}: {}", conversationId, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Сохраняет одно сообщение в БД
     *
     * @param conversationId ID конверсации
     * @param role роль сообщения (user, assistant, system)
     * @param content содержимое сообщения
     * @param model используемая модель (опционально)
     */
    @Transactional
    public void saveMessage(String conversationId, String role, String content, String model) {
        if (conversationId == null || conversationId.isBlank() || content == null || content.isBlank()) {
            log.warn("Cannot save message: conversationId or content is empty");
            return;
        }

        try {
            MemoryEntry entry = MemoryEntry.builder()
                    .conversationId(conversationId)
                    .role(role)
                    .content(content)
                    .model(model)
                    .timestamp(LocalDateTime.now())
                    .isCompressed(false)
                    .build();

            memoryRepository.save(entry);
            log.debug("💾 Saved {} message to DB for conversationId: {}", role, conversationId);

        } catch (Exception e) {
            log.error("❌ Error saving message for conversationId {}: {}", conversationId, e.getMessage());
        }
    }

    /**
     * Сохраняет одно сообщение с дополнительными метриками
     *
     * @param conversationId ID конверсации
     * @param role роль сообщения
     * @param content содержимое
     * @param model модель
     * @param inputTokens количество входных токенов
     * @param outputTokens количество выходных токенов
     * @param responseTimeMs время ответа в миллисекундах
     */
    @Transactional
    public void saveMessageWithMetrics(String conversationId, String role, String content, String model,
                                       Integer inputTokens, Integer outputTokens, Long responseTimeMs) {
        if (conversationId == null || conversationId.isBlank() || content == null || content.isBlank()) {
            log.warn("Cannot save message: conversationId or content is empty");
            return;
        }

        try {
            Integer totalTokens = null;
            if (inputTokens != null && outputTokens != null) {
                totalTokens = inputTokens + outputTokens;
            }

            MemoryEntry entry = MemoryEntry.builder()
                    .conversationId(conversationId)
                    .role(role)
                    .content(content)
                    .model(model)
                    .timestamp(LocalDateTime.now())
                    .inputTokens(inputTokens)
                    .outputTokens(outputTokens)
                    .totalTokens(totalTokens)
                    .responseTimeMs(responseTimeMs)
                    .isCompressed(false)
                    .build();

            memoryRepository.save(entry);
            log.debug("💾 Saved {} message with metrics to DB for conversationId: {}", role, conversationId);

        } catch (Exception e) {
            log.error("❌ Error saving message with metrics for conversationId {}: {}", conversationId, e.getMessage());
        }
    }

    /**
     * Сохраняет множество сообщений за раз (batch operation)
     *
     * @param conversationId ID конверсации
     * @param messages список сообщений (Message DTO)
     * @param model используемая модель
     */
    @Transactional
    public void saveMessages(String conversationId, List<Message> messages, String model) {
        if (conversationId == null || conversationId.isBlank() || messages == null || messages.isEmpty()) {
            log.debug("No messages to save for conversationId: {}", conversationId);
            return;
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            List<MemoryEntry> entries = messages.stream()
                    .map(msg -> MemoryEntry.builder()
                            .conversationId(conversationId)
                            .role(msg.getRole())
                            .content(msg.getContent())
                            .model(model)
                            .timestamp(now)
                            .isCompressed(false)
                            .build())
                    .collect(Collectors.toList());

            memoryRepository.saveAll(entries);
            log.info("💾 Saved {} messages to DB for conversationId: {}", messages.size(), conversationId);

        } catch (Exception e) {
            log.error("❌ Error saving batch messages for conversationId {}: {}", conversationId, e.getMessage());
        }
    }

    /**
     * Удаляет всю историю конверсации из БД
     *
     * @param conversationId ID конверсации
     */
    @Transactional
    public void deleteHistory(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            log.warn("Cannot delete history: conversationId is empty");
            return;
        }

        try {
            int deleted = memoryRepository.deleteByConversationId(conversationId);
            log.info("🗑️ Deleted {} messages for conversationId: {}", deleted, conversationId);

        } catch (Exception e) {
            log.error("❌ Error deleting history for conversationId {}: {}", conversationId, e.getMessage());
        }
    }

    /**
     * Проверяет существование истории для конверсации
     *
     * @param conversationId ID конверсации
     * @return true если есть хотя бы одно сообщение
     */
    @Transactional(readOnly = true)
    public boolean historyExists(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return false;
        }

        try {
            return memoryRepository.existsByConversationId(conversationId);
        } catch (Exception e) {
            log.error("Error checking history existence: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Получает количество сообщений в конверсации
     *
     * @param conversationId ID конверсации
     * @return количество сообщений
     */
    @Transactional(readOnly = true)
    public long getMessageCount(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return 0;
        }

        try {
            return memoryRepository.countByConversationId(conversationId);
        } catch (Exception e) {
            log.error("Error counting messages: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Получает статистику конверсации (токены, стоимость, количество сообщений)
     *
     * @param conversationId ID конверсации
     * @return массив с [totalTokens, totalCost, messageCount] или null
     */
    @Transactional(readOnly = true)
    public Object[] getConversationStats(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return null;
        }

        try {
            return memoryRepository.getConversationStats(conversationId);
        } catch (Exception e) {
            log.error("Error getting conversation stats: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Получает все уникальные конверсации с информацией
     * Возвращает список Map с информацией о каждой конверсации
     *
     * @return список Map-ов с conversationId, firstMessage, lastMessageTime, messageCount
     */
    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> getAllConversationSummaries() {
        try {
            List<String> conversationIds = memoryRepository.findAllConversationIds();
            List<java.util.Map<String, Object>> summaries = new ArrayList<>();

            for (String convId : conversationIds) {
                try {
                    // Получаем первое сообщение (для превью)
                    MemoryEntry first = memoryRepository.findFirstByConversationIdOrderByTimestampAsc(convId);
                    if (first == null) continue;

                    // Получаем последнее сообщение (для времени)
                    MemoryEntry last = memoryRepository.findFirstByConversationIdOrderByTimestampDesc(convId);

                    // Считаем сообщения
                    long count = memoryRepository.countByConversationId(convId);

                    java.util.Map<String, Object> summary = new java.util.HashMap<>();
                    summary.put("conversationId", convId);
                    summary.put("firstMessage", first.getContent().length() > 50 ?
                            first.getContent().substring(0, 50) + "..." :
                            first.getContent());
                    summary.put("lastMessageTime", last != null ? last.getTimestamp().toString() : first.getTimestamp().toString());
                    summary.put("messageCount", count);
                    summary.put("hasCompression", false); // TODO: реализовать если нужно

                    summaries.add(summary);

                } catch (Exception e) {
                    log.warn("Error processing conversation {}: {}", convId, e.getMessage());
                }
            }

            log.info("📋 Retrieved {} conversation summaries", summaries.size());
            return summaries;

        } catch (Exception e) {
            log.error("Error getting all conversation summaries: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}

