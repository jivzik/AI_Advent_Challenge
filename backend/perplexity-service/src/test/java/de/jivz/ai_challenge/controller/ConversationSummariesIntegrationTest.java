package de.jivz.ai_challenge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.ai_challenge.dto.ConversationSummaryDTO;
import de.jivz.ai_challenge.entity.MemoryEntry;
import de.jivz.ai_challenge.repository.MemoryRepository;
import de.jivz.ai_challenge.service.MemoryService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * 📋 Integration Tests for Conversation Summaries API
 *
 * Tests for the new endpoints:
 * - GET /api/memory/conversations (all conversations)
 * - GET /api/memory/user/{userId}/conversations (user conversations)
 * - DELETE /api/memory/conversation/{conversationId} (delete conversation)
 *
 * Uses H2 in-memory database for fast, isolated testing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("🧪 Conversation Summaries API Integration Tests")
class ConversationSummariesIntegrationTest {

    @LocalServerPort
    private int port;


    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemoryRepository memoryRepository;

    @Autowired
    private MemoryService memoryService;

    private static final String USER_1 = "user-summaries-1";
    private static final String USER_2 = "user-summaries-2";
    private static String conversationId1;
    private static String conversationId2;
    private static String conversationId3;

    @BeforeAll
    static void beforeAll() {
        conversationId1 = "conv-summaries-" + System.currentTimeMillis() + "-1";
        conversationId2 = "conv-summaries-" + System.currentTimeMillis() + "-2";
        conversationId3 = "conv-summaries-" + System.currentTimeMillis() + "-3";

        System.out.println("\n" + "=".repeat(70));
        System.out.println("🧪 CONVERSATION SUMMARIES INTEGRATION TESTS");
        System.out.println("=".repeat(70));
    }

    /**
     * Setup test data: Create multiple conversations with messages
     */
    @BeforeEach
    void setupTestData() {
        // Удалим старые данные
        try {
            memoryRepository.deleteAll();
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Helper method to create test conversations
     */
    @Transactional
    protected void createTestConversations() {
        LocalDateTime baseTime = LocalDateTime.now();
        for (int i = 0; i < 5; i++) {
            // User message
            MemoryEntry userMsg = MemoryEntry.builder()
                    .conversationId(conversationId1)
                    .userId(USER_1)
                    .role("user")
                    .content("User message " + (i + 1) + " in conversation 1")
                    .timestamp(baseTime.plusSeconds(i * 2))
                    .totalTokens(10 + i)
                    .isCompressed(false)
                    .build();
            memoryRepository.save(userMsg);

            // Assistant message
            MemoryEntry assistantMsg = MemoryEntry.builder()
                    .conversationId(conversationId1)
                    .userId(USER_1)
                    .role("assistant")
                    .content("Assistant response " + (i + 1) + " to conversation 1")
                    .timestamp(baseTime.plusSeconds(i * 2 + 1))
                    .model("gpt-4")
                    .totalTokens(20 + i)
                    .isCompressed(false)
                    .build();
            memoryRepository.save(assistantMsg);
        }

        // ========== CONVERSATION 2: User 1, 3 messages ==========
        LocalDateTime baseTime2 = baseTime.plusSeconds(20);
        for (int i = 0; i < 3; i++) {
            MemoryEntry userMsg = MemoryEntry.builder()
                    .conversationId(conversationId2)
                    .userId(USER_1)
                    .role("user")
                    .content("Question " + (i + 1) + " in second conversation")
                    .timestamp(baseTime2.plusSeconds(i * 2))
                    .totalTokens(8 + i)
                    .isCompressed(false)
                    .build();
            memoryRepository.save(userMsg);

            MemoryEntry assistantMsg = MemoryEntry.builder()
                    .conversationId(conversationId2)
                    .userId(USER_1)
                    .role("assistant")
                    .content("Answer " + (i + 1) + " in second conversation")
                    .timestamp(baseTime2.plusSeconds(i * 2 + 1))
                    .model("gpt-4")
                    .totalTokens(15 + i)
                    .isCompressed(false)
                    .build();
            memoryRepository.save(assistantMsg);
        }

        // ========== CONVERSATION 3: User 2, 2 messages ==========
        LocalDateTime baseTime3 = baseTime2.plusSeconds(15);
        for (int i = 0; i < 2; i++) {
            MemoryEntry userMsg = MemoryEntry.builder()
                    .conversationId(conversationId3)
                    .userId(USER_2)
                    .role("user")
                    .content("Different user message " + (i + 1))
                    .timestamp(baseTime3.plusSeconds(i * 2))
                    .totalTokens(12 + i)
                    .isCompressed(false)
                    .build();
            memoryRepository.save(userMsg);

            MemoryEntry assistantMsg = MemoryEntry.builder()
                    .conversationId(conversationId3)
                    .userId(USER_2)
                    .role("assistant")
                    .content("Response to different user " + (i + 1))
                    .timestamp(baseTime3.plusSeconds(i * 2 + 1))
                    .model("gpt-4")
                    .totalTokens(18 + i)
                    .isCompressed(false)
                    .build();
            memoryRepository.save(assistantMsg);
        }

        System.out.println("\n✅ Test data created:");
        System.out.println("   - Conversation 1 (User 1): " + conversationId1 + " (10 messages)");
        System.out.println("   - Conversation 2 (User 1): " + conversationId2 + " (6 messages)");
        System.out.println("   - Conversation 3 (User 2): " + conversationId3 + " (4 messages)");
    }

    // ============================================================================
    // TEST 1: Get All Conversations (Summary)
    // ============================================================================

    @Test
    @Order(1)
    @Transactional
    @DisplayName("Test 1: MemoryService.getConversationSummaries() - Should return all conversations")
    void testGetAllConversationSummaries() {
        System.out.println("\n" + "-".repeat(70));
        System.out.println("TEST 1: Get All Conversation Summaries");
        System.out.println("-".repeat(70));

        // Create test data
        createTestConversations();

        // ✅ Call service directly
        List<ConversationSummaryDTO> summaries = memoryService.getConversationSummaries();

        // ✅ Assertions
        assertThat(summaries).isNotNull();
        assertThat(summaries).hasSize(3);

        // ✅ Verify all summaries have required fields
        summaries.forEach(summary -> {
            assertThat(summary.getConversationId()).isNotBlank();
            assertThat(summary.getFirstMessage()).isNotBlank();
            assertThat(summary.getLastMessageTime()).isNotNull();
            assertThat(summary.getMessageCount()).isGreaterThan(0);
            assertThat(summary.getUserId()).isNotNull();
        });

        // ✅ Verify sorting: newest first (DESC by lastMessageTime)
        for (int i = 0; i < summaries.size() - 1; i++) {
            long currentTime = summaries.get(i).getLastMessageTime().atZone(ZoneId.systemDefault()).toEpochSecond();
            long nextTime = summaries.get(i + 1).getLastMessageTime().atZone(ZoneId.systemDefault()).toEpochSecond();
            assertThat(currentTime).isGreaterThanOrEqualTo(nextTime);
        }

        // 📋 Print results
        System.out.println("✅ Total Summaries: " + summaries.size());
        summaries.forEach(summary -> {
            System.out.println("\n   📌 " + summary.getConversationId());
            System.out.println("      First: " + summary.getFirstMessage());
            System.out.println("      Messages: " + summary.getMessageCount());
            System.out.println("      Compressed: " + summary.isHasCompression());
            System.out.println("      User: " + summary.getUserId());
        });
    }

    // ============================================================================
    // TEST 2: Get User Conversations (Filtered by UserId)
    // ============================================================================

    @Test
    @Order(2)
    @Transactional
    @DisplayName("Test 2: MemoryService.getConversationSummariesForUser() - Should filter by userId")
    void testGetUserConversationSummaries() {
        System.out.println("\n" + "-".repeat(70));
        System.out.println("TEST 2: Get User Conversation Summaries (Filtered)");
        System.out.println("-".repeat(70));

        // Create test data
        createTestConversations();

        // ✅ Call service directly for USER_1
        List<ConversationSummaryDTO> summaries = memoryService.getConversationSummariesForUser(USER_1);

        // ✅ Assertions
        assertThat(summaries).isNotNull();
        assertThat(summaries).hasSize(2); // USER_1 has 2 conversations

        // ✅ Verify all summaries belong to USER_1
        summaries.forEach(summary -> {
            assertThat(summary.getUserId()).isEqualTo(USER_1);
            assertThat(summary.getConversationId()).isIn(conversationId1, conversationId2);
        });

        // ✅ Verify sorting: newest first
        for (int i = 0; i < summaries.size() - 1; i++) {
            long currentTime = summaries.get(i).getLastMessageTime().atZone(ZoneId.systemDefault()).toEpochSecond();
            long nextTime = summaries.get(i + 1).getLastMessageTime().atZone(ZoneId.systemDefault()).toEpochSecond();
            assertThat(currentTime).isGreaterThanOrEqualTo(nextTime);
        }

        // ✅ Verify message counts
        ConversationSummaryDTO conv1Summary = summaries.stream()
                .filter(s -> s.getConversationId().equals(conversationId1))
                .findFirst()
                .orElse(null);

        ConversationSummaryDTO conv2Summary = summaries.stream()
                .filter(s -> s.getConversationId().equals(conversationId2))
                .findFirst()
                .orElse(null);

        assertThat(conv1Summary).isNotNull();
        assertThat(conv2Summary).isNotNull();
        assertThat(conv1Summary.getMessageCount()).isEqualTo(10);
        assertThat(conv2Summary.getMessageCount()).isEqualTo(6);

        // 📋 Print results
        System.out.println("✅ Total Summaries for " + USER_1 + ": " + summaries.size());
        summaries.forEach(summary -> {
            System.out.println("\n   📌 " + summary.getConversationId());
            System.out.println("      Messages: " + summary.getMessageCount());
            System.out.println("      Compressed: " + summary.isHasCompression());
        });
    }

    // ============================================================================
    // TEST 3: Delete Conversation and Verify
    // ============================================================================

    @Test
    @Order(3)
    @Transactional
    @DisplayName("Test 3: Delete Conversation - Should remove from database")
    void testDeleteConversation() {
        System.out.println("\n" + "-".repeat(70));
        // Create test data
        createTestConversations();

        // ✅ Verify conversation exists before deletion
        long countBefore = memoryRepository.countByConversationId(conversationId1);
        assertThat(countBefore).isEqualTo(10); // 5 user + 5 assistant messages

        System.out.println("✅ Before deletion: " + countBefore + " messages");

        // 🗑️ Delete the conversation
        int deletedCount = memoryRepository.deleteByConversationId(conversationId1);

        // ✅ Verify delete count
        assertThat(deletedCount).isEqualTo(10);

        // ✅ Verify conversation is actually deleted from DB
        long countAfter = memoryRepository.countByConversationId(conversationId1);
        assertThat(countAfter).isZero();

        System.out.println("✅ Deleted Messages: " + deletedCount);
        System.out.println("✅ After deletion: " + countAfter + " messages");

        // ✅ Verify other conversations still exist
        long conv2Count = memoryRepository.countByConversationId(conversationId2);
        long conv3Count = memoryRepository.countByConversationId(conversationId3);
        assertThat(conv2Count).isEqualTo(6);
        assertThat(conv3Count).isEqualTo(4);

        System.out.println("✅ Other conversations preserved:");
        System.out.println("   - Conversation 2: " + conv2Count + " messages");
        System.out.println("   - Conversation 3: " + conv3Count + " messages");

        // ✅ Verify summaries no longer include deleted conversation
        List<ConversationSummaryDTO> summaries = memoryService.getConversationSummaries();
        assertThat(summaries).hasSize(2);
        assertThat(summaries.stream()
                .map(ConversationSummaryDTO::getConversationId)
                .toList())
                .doesNotContain(conversationId1);

        System.out.println("✅ Summaries updated: now has " + summaries.size() + " conversations");
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    @AfterAll
    static void afterAll() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("✅ ALL TESTS COMPLETED");
        System.out.println("=".repeat(70) + "\n");
    }
}

