package de.jivz.ai_challenge.openrouterservice.service;

import de.jivz.ai_challenge.openrouterservice.dto.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service für die Verwaltung der Gesprächshistorie.
 *
 * ⭐ Funktionalität:
 * - Speichert Konversationen temporär im RAM-Cache
 * - Ermöglicht Multi-Turn Gespräche in einer Session
 * - Cache wird gelöscht wenn ein neues Gespräch startet
 *
 * Architecture:
 * - ConcurrentHashMap: Fast cache für aktive Konversationen
 * - Keine Persistierung - nur für aktive Sessions
 *
 * Flow:
 * 1. getHistory() - prüft Cache für bestehende Konversation
 * 2. addMessage() - fügt eine Nachricht zur Historie hinzu
 * 3. clearHistory() - löscht die Historie (z.B. bei neuem Gespräch)
 */
@Slf4j
@Service
public class ConversationHistoryService {

    // In-Memory cache: conversationId -> List of Messages
    // Verwendet für schnelle Zugriffe während aktiver Konversationen
    private final Map<String, List<Message>> conversations = new ConcurrentHashMap<>();

    /**
     * Ruft die Konversationshistorie ab.
     *
     * Load strategy:
     * 1. Prüfe RAM-Cache
     * 2. Wenn nicht vorhanden, return empty list (wird bei erster Nachricht erstellt)
     *
     * @param conversationId die Konversations-ID
     * @return Liste der Nachrichten in der Konversation
     */
    public List<Message> getHistory(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            log.debug("No conversationId provided, returning empty history");
            return new ArrayList<>();
        }

        // Prüfe RAM-Cache
        List<Message> cached = conversations.get(conversationId);
        if (cached != null) {
            log.debug("📦 Retrieved {} messages from cache for: {}", cached.size(), conversationId);
            return new ArrayList<>(cached); // Return Kopie um externe Änderungen zu verhindern
        }

        // Nicht gefunden - return empty (wird bei erster Nachricht erstellt)
        log.debug("No history found for conversationId: {}", conversationId);
        return new ArrayList<>();
    }

    /**
     * Fügt eine Nachricht zur Konversationshistorie hinzu.
     *
     * @param conversationId die Konversations-ID
     * @param role die Rolle (user oder assistant)
     * @param content der Nachrichteninhalt
     */
    public void addMessage(String conversationId, String role, String content) {
        if (conversationId == null || conversationId.isBlank()) {
            log.warn("Cannot add message: conversationId is null or empty");
            return;
        }

        conversations.computeIfAbsent(conversationId, k -> new ArrayList<>())
                .add(new Message(role, content));

        log.debug("Added {} message to conversationId: {}", role, conversationId);
    }

    /**
     * Speichert die vollständige Konversationshistorie.
     *
     * @param conversationId die Konversations-ID
     * @param history die komplette Liste der Nachrichten
     */
    public void saveHistory(String conversationId, List<Message> history) {
        if (conversationId == null || conversationId.isBlank()) {
            log.warn("Cannot save history: conversationId is null or empty");
            return;
        }

        // Update RAM-Cache
        conversations.put(conversationId, new ArrayList<>(history));
        log.debug("💾 Updated cache: {} messages for conversationId: {}",
                history.size(), conversationId);
    }

    /**
     * Löscht die Historie aus dem Cache.
     *
     * @param conversationId die Konversations-ID
     */
    public void clearHistory(String conversationId) {
        if (conversationId != null && !conversationId.isBlank()) {
            conversations.remove(conversationId);
            log.info("🗑️ Cleared history cache for conversationId: {}", conversationId);
        }
    }

    /**
     * Gibt die Anzahl der aktiven Konversationen zurück.
     *
     * @return Anzahl der Konversationen
     */
    public int getConversationCount() {
        return conversations.size();
    }
}

