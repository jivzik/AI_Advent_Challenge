package de.jivz.ai_challenge.openrouterservice.service;

import de.jivz.ai_challenge.openrouterservice.dto.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ConversationHistoryService - Управление историей диалогов
 *
 * ⭐ Функциональность:
 * - Гибридный подход: L1 кеш (RAM) + L2 кеш (PostgreSQL БД)
 * - Быстрая загрузка активных конверсаций из памяти
 * - Персистентное хранение в БД для восстановления после перезагрузки
 * - Multi-turn диалоги в одной сессии
 *
 * Architecture:
 * - ConcurrentHashMap: L1 кеш для активных конверсаций
 * - HistoryPersistenceService: L2 кеш в PostgreSQL
 * - Strategy Pattern: двухуровневое кеширование
 *
 * Flow:
 * 1. getHistory() - сначала проверяет RAM, затем БД
 * 2. addMessage() - добавляет в RAM кеш и сохраняет в БД
 * 3. saveMessages() - батч операция для нескольких сообщений
 * 4. clearHistory() - удаляет из RAM и БД
 *
 * Principles:
 * - Dependency Injection: HistoryPersistenceService внедряется
 * - Single Responsibility: управление историей, без бизнес-логики
 * - Open/Closed: легко добавить другие реализации HistoryPersistenceService
 */
@Slf4j
@Service
public class ConversationHistoryService {

    private final HistoryPersistenceService persistenceService;

    // L1 кеш: conversationId -> List of Messages (в памяти)
    // Используется для быстрого доступа к активным конверсациям
    private final Map<String, List<Message>> conversationCache = new ConcurrentHashMap<>();

    public ConversationHistoryService(HistoryPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
        log.info("ConversationHistoryService initialized with persistence layer");
    }

    /**
     * Загружает историю конверсации.
     *
     * Strategy (Two-level cache):
     * 1. Проверить L1 кеш (RAM/ConcurrentHashMap)
     * 2. Если не найдено, загрузить из L2 кеша (PostgreSQL)
     * 3. Поместить в L1 кеш для будущих запросов
     *
     * @param conversationId ID конверсации
     * @return Список сообщений в хронологическом порядке
     */
    public List<Message> getHistory(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            log.debug("No conversationId provided, returning empty history");
            return new ArrayList<>();
        }

        // Level 1: Проверяем RAM кеш
        List<Message> cached = conversationCache.get(conversationId);
        if (cached != null) {
            log.debug("📦 L1 cache HIT: Retrieved {} messages from RAM for: {}", cached.size(), conversationId);
            return new ArrayList<>(cached); // Возвращаем копию для иммутабельности
        }

        // Level 2: Загружаем из БД
        log.debug("📦 L1 cache MISS: Loading from database for: {}", conversationId);
        List<Message> dbMessages = persistenceService.loadHistory(conversationId);

        // Кешируем в RAM если есть данные
        if (!dbMessages.isEmpty()) {
            conversationCache.put(conversationId, new ArrayList<>(dbMessages));
            log.debug("📦 L2 cache HIT: Loaded {} messages from DB and cached in RAM", dbMessages.size());
        } else {
            log.debug("📦 L2 cache MISS: No history found in DB for: {}", conversationId);
        }

        return dbMessages;
    }

    /**
     * Получает только оригинальные (не сжатые) сообщения
     *
     * @param conversationId ID конверсации
     * @return Список оригинальных сообщений
     */
    public List<Message> getOriginalHistory(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return new ArrayList<>();
        }

        // Загружаем оригинальные только из БД (нет кеша)
        return persistenceService.loadOriginalHistory(conversationId);
    }

    /**
     * Добавляет одно сообщение к конверсации.
     *
     * Сохраняет в оба уровня:
     * 1. L1 кеш (RAM) - для быстрого доступа
     * 2. L2 кеш (БД) - для персистентности
     *
     * @param conversationId ID конверсации
     * @param role роль (user, assistant, system)
     * @param content содержимое сообщения
     * @param model используемая модель (опционально)
     */
    public void addMessage(String conversationId, String role, String content, String model) {
        if (conversationId == null || conversationId.isBlank()) {
            log.warn("Cannot add message: conversationId is null or empty");
            return;
        }

        // Добавляем в L1 кеш
        Message message = new Message(role, content);
        conversationCache.computeIfAbsent(conversationId, k -> new ArrayList<>())
                .add(message);

        // Сохраняем в L2 кеш (БД)
        persistenceService.saveMessage(conversationId, role, content, model);

        log.debug("Added {} message to conversationId: {}", role, conversationId);
    }

    /**
     * Добавляет сообщение с метриками (токены, время ответа)
     *
     * @param conversationId ID конверсации
     * @param role роль
     * @param content содержимое
     * @param model модель
     * @param inputTokens входные токены
     * @param outputTokens выходные токены
     * @param responseTimeMs время ответа
     */
    public void addMessageWithMetrics(String conversationId, String role, String content, String model,
                                      Integer inputTokens, Integer outputTokens, Long responseTimeMs) {
        if (conversationId == null || conversationId.isBlank()) {
            log.warn("Cannot add message: conversationId is null or empty");
            return;
        }

        // Добавляем в L1 кеш
        Message message = new Message(role, content);
        conversationCache.computeIfAbsent(conversationId, k -> new ArrayList<>())
                .add(message);

        // Сохраняем в L2 кеш (БД) с метриками
        persistenceService.saveMessageWithMetrics(conversationId, role, content, model,
                inputTokens, outputTokens, responseTimeMs);

        log.debug("Added {} message with metrics to conversationId: {}", role, conversationId);
    }

    /**
     * Сохраняет полный список сообщений (батч операция)
     *
     * @param conversationId ID конверсации
     * @param history полный список сообщений
     * @param model используемая модель
     */
    public void saveMessages(String conversationId, List<Message> history, String model) {
        if (conversationId == null || conversationId.isBlank()) {
            log.warn("Cannot save history: conversationId is null or empty");
            return;
        }

        if (history == null || history.isEmpty()) {
            log.debug("No messages to save");
            return;
        }

        // Обновляем L1 кеш
        conversationCache.put(conversationId, new ArrayList<>(history));

        // Сохраняем в L2 кеш (БД)
        persistenceService.saveMessages(conversationId, history, model);

        log.info("💾 Saved {} messages to conversationId: {}", history.size(), conversationId);
    }

    /**
     * Альтернативная сигнатура saveHistory для обратной совместимости
     */
    public void saveHistory(String conversationId, List<Message> history) {
        saveMessages(conversationId, history, null);
    }

    /**
     * Очищает историю конверсации из обоих уровней кеша
     *
     * @param conversationId ID конверсации
     */
    public void clearHistory(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            log.warn("Cannot clear history: conversationId is null or empty");
            return;
        }

        // Удаляем из L1 кеша (RAM)
        conversationCache.remove(conversationId);

        // Удаляем из L2 кеша (БД)
        persistenceService.deleteHistory(conversationId);

        log.info("🗑️ Cleared history for conversationId: {} from both cache levels", conversationId);
    }

    /**
     * Возвращает количество активных конверсаций в L1 кеше
     *
     * @return количество конверсаций в памяти
     */
    public int getConversationCount() {
        return conversationCache.size();
    }

    /**
     * Очищает L1 кеш (осторожно! Данные остаются в БД)
     */
    public void clearL1Cache() {
        conversationCache.clear();
        log.info("Cleared L1 cache (in-memory). Data preserved in database.");
    }

    /**
     * Получает количество сообщений в конверсации из БД
     *
     * @param conversationId ID конверсации
     * @return количество сообщений
     */
    public long getMessageCount(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return 0;
        }
        return persistenceService.getMessageCount(conversationId);
    }

    /**
     * Получает статистику конверсации
     *
     * @param conversationId ID конверсации
     * @return массив [totalTokens, totalCost, messageCount]
     */
    public Object[] getConversationStats(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return null;
        }
        return persistenceService.getConversationStats(conversationId);
    }

    /**
     * Проверяет существование истории
     *
     * @param conversationId ID конверсации
     * @return true если есть хотя бы одно сообщение
     */
    public boolean historyExists(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return false;
        }
        return persistenceService.historyExists(conversationId);
    }
}

