package de.jivz.ai_challenge.service;

import de.jivz.ai_challenge.dto.CompressionInfo;
import de.jivz.ai_challenge.dto.Message;
import de.jivz.ai_challenge.service.openrouter.OpenRouterToolClient;
import de.jivz.ai_challenge.service.openrouter.model.OpenRouterResponseWithMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for compressing dialog history using summary mechanism.
 * Implements automatic compression after N messages threshold.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DialogCompressionService {

    private final OpenRouterToolClient openRouterClient;
    private final ConversationHistoryService conversationHistoryService;
    private final MemoryService memoryService;  // ⭐ NEW: For saving summary to DB

    // Compression threshold - after how many messages to create summary
    private static final int COMPRESSION_THRESHOLD = 5;

    // Suffix for compressed conversation IDs
    private static final String COMPRESSED_SUFFIX = "_compressed";

    /**
     * Checks if conversation needs compression and performs it if needed.
     *
     * @param conversationId original conversation ID
     * @return true if compression was performed
     */
    public boolean checkAndCompress(String conversationId) {
        List<Message> history = conversationHistoryService.getHistory(conversationId);

        if (shouldCompress(history)) {
            log.info("🔄 Compression triggered for conversation: {} (messages: {})",
                    conversationId, history.size());
            compressHistory(conversationId, history);
            return true;
        }

        return false;
    }

    /**
     * Determines if history should be compressed.
     */
    private boolean shouldCompress(List<Message> history) {
        // Remove system messages from count
        long messageCount = history.stream()
                .filter(msg -> !"system".equals(msg.getRole()))
                .count();

        return messageCount >= COMPRESSION_THRESHOLD &&
                messageCount % COMPRESSION_THRESHOLD == 0;
    }

    /**
     * Compresses the conversation history by creating a summary.
     * Keeps system prompt + summary + last few messages.
     */
    private void compressHistory(String conversationId, List<Message> fullHistory) {
        String compressedId = conversationId + COMPRESSED_SUFFIX;

        // Separate system messages from conversation
        List<Message> systemMessages = new ArrayList<>();
        List<Message> conversationMessages = new ArrayList<>();

        for (Message msg : fullHistory) {
            if ("system".equals(msg.getRole())) {
                systemMessages.add(msg);
            } else {
                conversationMessages.add(msg);
            }
        }

        // Calculate how many messages to summarize
        int messagesToSummarize = conversationMessages.size() - 2; // Keep last 2 messages
        if (messagesToSummarize <= 0) {
            log.warn("Not enough messages to compress");
            return;
        }

        // Get messages for summary (exclude last 2)
        List<Message> messagesToCompress = conversationMessages.subList(0, messagesToSummarize);

        // Create summary
        String summary = createSummary(messagesToCompress);

        // ⭐ SAVE SUMMARY TO POSTGRESQL
        memoryService.saveSummary(
                conversationId,
                summary,
                messagesToCompress.size(),
                LocalDateTime.now()
        );
        log.info("✅ Summary saved to PostgreSQL for conversation: {}", conversationId);

        // Build compressed history: system + summary + last messages
        List<Message> compressedHistory = new ArrayList<>(systemMessages);
        compressedHistory.add(new Message("user",
                "📝 SUMMARY предыдущего диалога:\n\n" + summary));
        compressedHistory.add(new Message("assistant",
                "Понял, учту информацию из summary в дальнейшем разговоре."));

        // Add last 2 messages
        compressedHistory.addAll(conversationMessages.subList(messagesToSummarize, conversationMessages.size()));

        // Save compressed version
        conversationHistoryService.saveHistory(compressedId, compressedHistory);

        log.info("✅ Compressed history saved: {} -> {} messages",
                fullHistory.size(), compressedHistory.size());
        log.info("📊 Summary created for {} messages", messagesToCompress.size());
    }

    /**
     * Creates a summary of messages using AI model.
     */
    private String createSummary(List<Message> messages) {
        log.info("🤖 Creating summary for {} messages...", messages.size());

        // Build conversation text
        StringBuilder conversationText = new StringBuilder();
        for (Message msg : messages) {
            conversationText.append(msg.getRole().toUpperCase())
                    .append(": ")
                    .append(msg.getContent())
                    .append("\n\n");
        }

        // Create summary request
        String summaryPrompt = String.format("""
                Проанализируй следующий диалог и создай краткое резюме на русском языке.
                
                Резюме должно содержать:
                1. Основные темы и вопросы, которые обсуждались
                2. Ключевые факты, решения и выводы
                3. Важный контекст для продолжения разговора
                
                Будь кратким, но сохрани всю важную информацию (2-3 абзаца).
                
                ДИАЛОГ:
                %s
                
                РЕЗЮМЕ:
                """, conversationText);

        List<Message> summaryRequest = List.of(
                new Message("user", summaryPrompt)
        );

        try {
            OpenRouterResponseWithMetrics response =
                    openRouterClient.requestCompletionWithMetrics(summaryRequest, 0.3, null);

            log.info("✅ Summary created. Tokens: {} input, {} output",
                    response.getInputTokens(), response.getOutputTokens());

            return response.getReply();

        } catch (Exception e) {
            log.error("❌ Failed to create summary: {}", e.getMessage());
            return "Резюме недоступно из-за ошибки: " + e.getMessage();
        }
    }

    /**
     * Gets the compressed version of conversation if exists.
     */
    public List<Message> getCompressedHistory(String conversationId) {
        String compressedId = conversationId + COMPRESSED_SUFFIX;
        return conversationHistoryService.getHistory(compressedId);
    }

    /**
     * Checks if compressed version exists.
     */
    public boolean hasCompressedVersion(String conversationId) {
        String compressedId = conversationId + COMPRESSED_SUFFIX;
        return !conversationHistoryService.getHistory(compressedId).isEmpty();
    }

    /**
     * ⭐ NEW: Get comprehensive compression information for a conversation.
     * Contains all business logic for calculating compression stats.
     *
     * @param conversationId the conversation identifier
     * @return CompressionInfo with all metrics
     */
    public CompressionInfo getCompressionInfo(String conversationId) {
        log.debug("Getting compression info for conversationId: {}", conversationId);

        try {
            // Get full history
            List<Message> fullHistory = conversationHistoryService.getHistory(conversationId);
            int fullSize = fullHistory.size();

            // Check if compressed version exists
            boolean isCompressed = hasCompressedVersion(conversationId);

            if (isCompressed) {
                // Calculate compression metrics
                List<Message> compressedHistory = getCompressedHistory(conversationId);
                int compressedSize = compressedHistory.size();
                int messagesSaved = fullSize - compressedSize;
                double compressionRatioValue = fullSize > 0 ? ((double) messagesSaved / fullSize) * 100 : 0;
                String compressionRatio = String.format("%.1f%%", compressionRatioValue);

                // Estimate token savings (rough estimate: ~100 tokens per message)
                int estimatedTokensSaved = messagesSaved * 100;

                log.debug("Compression active: {} -> {} messages ({} saved, {})",
                        fullSize, compressedSize, messagesSaved, compressionRatio);

                return CompressionInfo.builder()
                        .conversationId(conversationId)
                        .fullHistorySize(fullSize)
                        .compressedHistorySize(compressedSize)
                        .isCompressed(true)
                        .messagesSaved(messagesSaved)
                        .compressionRatio(compressionRatio)
                        .estimatedTokensSaved(estimatedTokensSaved)
                        .timestamp(Instant.now().toString())
                        .build();
            } else {
                // No compression yet
                log.debug("No compression yet for conversation: {}", conversationId);

                return CompressionInfo.builder()
                        .conversationId(conversationId)
                        .fullHistorySize(fullSize)
                        .compressedHistorySize(fullSize)
                        .isCompressed(false)
                        .messagesSaved(0)
                        .compressionRatio("0%")
                        .estimatedTokensSaved(0)
                        .timestamp(Instant.now().toString())
                        .build();
            }

        } catch (Exception e) {
            log.error("Failed to get compression info for {}: {}", conversationId, e.getMessage());

            // Return safe default
            return CompressionInfo.builder()
                    .conversationId(conversationId)
                    .fullHistorySize(0)
                    .compressedHistorySize(0)
                    .isCompressed(false)
                    .messagesSaved(0)
                    .compressionRatio("0%")
                    .estimatedTokensSaved(0)
                    .timestamp(Instant.now().toString())
                    .build();
        }
    }
}