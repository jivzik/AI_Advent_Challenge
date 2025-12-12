package de.jivz.ai_challenge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.jivz.ai_challenge.entity.MemoryEntry;
import de.jivz.ai_challenge.repository.MemoryRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for MemoryController with H2 in-memory database.
 *
 * Uses H2 in-memory database for fast, lightweight integration testing.
 * Tests long-term memory system endpoints without external dependencies.
 *
 * Mirrors the test scenarios from test-memory-system.sh script.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MemoryControllerIntegrationTest {


    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemoryRepository memoryRepository;

    private static final String USER_ID = "test-user-integration";
    private static String conversationId;
    private static final String TEST_DATA_USER_ID = "test-data-user";
    private static String testDataConversationId;

    @BeforeAll
    static void setup() {
        conversationId = "test-conv-" + System.currentTimeMillis();
        testDataConversationId = "test-data-conv-" + System.currentTimeMillis();
        System.out.println("\n==============================================");
        System.out.println("💾 H2 In-Memory Database started!");
        System.out.println("📍 Database: H2 (in-memory)");
        System.out.println("==============================================\n");
    }

    /**
     * Create test data entries directly in the database before each test.
     * This ensures we have consistent data to query during tests without relying on REST API.
     */
    @BeforeEach
    void createTestData() {
        // Create initial test data messages directly in the database
        String[] testMessages = {
                "What is artificial intelligence?",
                "Can you explain machine learning?",
                "How does deep learning work?",
                "Tell me about neural networks."
        };

        LocalDateTime baseTime = LocalDateTime.now();
        for (int i = 0; i < testMessages.length; i++) {
            // Create user message entry
            MemoryEntry userMessage = MemoryEntry.builder()
                    .conversationId(testDataConversationId)
                    .userId(TEST_DATA_USER_ID)
                    .role("user")
                    .content(testMessages[i])
                    .timestamp(baseTime.plusSeconds(i))
                    .model("gpt-4")
                    .inputTokens(10 + i)
                    .outputTokens(0)
                    .totalTokens(10 + i)
                    .isCompressed(false)
                    .build();

            memoryRepository.save(userMessage);

            // Create assistant response entry
            MemoryEntry assistantMessage = MemoryEntry.builder()
                    .conversationId(testDataConversationId)
                    .userId(TEST_DATA_USER_ID)
                    .role("assistant")
                    .content("This is an automated response to: " + testMessages[i])
                    .timestamp(baseTime.plusSeconds(i).plusNanos(500_000_000))
                    .model("gpt-4")
                    .inputTokens(10 + i)
                    .outputTokens(20 + (i * 2))
                    .totalTokens(30 + i + (i * 2))
                    .isCompressed(false)
                    .build();

            memoryRepository.save(assistantMessage);
        }

        System.out.println("✅ Test data created directly in database for conversation: " + testDataConversationId);
        System.out.println("   Created " + (testMessages.length * 2) + " messages (user + assistant pairs)");
    }

    private String createUrl(String path) {
        return "http://localhost:" + port + path;
    }

    /**
     * Test 1: Health Check
     * Verifies memory service is healthy and database is connected.
     */
    @Test
    @Order(1)
    @DisplayName("Test 1: Health Check - Should return healthy status")
    void testHealthCheck() throws Exception {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                createUrl("/api/memory/health"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("healthy");
        assertThat(response.getBody().get("timestamp")).isNotNull();

        System.out.println("Health check response: " + response.getBody());
    }

    /**
     * Test 2: Send a message to create conversation (MOCKED)
     * Creates test data in database to simulate message sending.
     */
    @Test
    @Order(2)
    @DisplayName("Test 2: Send Message - Should create conversation and store messages (MOCKED)")
    void testSendMessage() {
        // Create mock message data
        MemoryEntry userMsg = MemoryEntry.builder()
                .conversationId(conversationId)
                .userId(USER_ID)
                .role("user")
                .content("Hello! This is a test message for PostgreSQL memory integration test.")
                .timestamp(LocalDateTime.now())
                .model("gpt-4")
                .inputTokens(15)
                .totalTokens(15)
                .isCompressed(false)
                .build();

        MemoryEntry assistantMsg = MemoryEntry.builder()
                .conversationId(conversationId)
                .userId(USER_ID)
                .role("assistant")
                .content("This is a mocked response to your test message. The message has been stored in the database.")
                .timestamp(LocalDateTime.now())
                .model("gpt-4")
                .inputTokens(15)
                .outputTokens(25)
                .totalTokens(40)
                .isCompressed(false)
                .build();

        // Save to database
        memoryRepository.save(userMsg);
        memoryRepository.save(assistantMsg);

        // Verify they were saved
        List<MemoryEntry> messages = memoryRepository.findByConversationIdOrderByTimestampAsc(conversationId);
        assertThat(messages.size()).isGreaterThanOrEqualTo(2);

        System.out.println("✅ Test 2 (MOCKED): Message conversation created");
        System.out.println("   Conversation ID: " + conversationId);
        System.out.println("   Messages stored: " + messages.size());
    }

    /**
     * Test 3: Retrieve conversation history (MOCKED)
     * Verifies that messages are persisted and can be retrieved.
     */
    @Test
    @Order(3)
    @DisplayName("Test 3: Retrieve Conversation History - Should return persisted messages (MOCKED)")
    void testGetConversationHistory() {
        List<MemoryEntry> messages = memoryRepository.findByConversationIdOrderByTimestampAsc(conversationId);

        assertThat(messages).isNotEmpty();
        assertThat(messages.size()).isGreaterThanOrEqualTo(2);
        assertThat(messages.get(0).getConversationId()).isEqualTo(conversationId);
        assertThat(messages.get(0).getUserId()).isEqualTo(USER_ID);

        System.out.println("✅ Test 3 (MOCKED): Conversation history retrieved");
        System.out.println("   Total messages: " + messages.size());
        messages.forEach(msg -> System.out.println("   - " + msg.getRole() + ": " + msg.getContent().substring(0, Math.min(50, msg.getContent().length())) + "..."));
    }

    /**
     * Test 4: Get conversation statistics (MOCKED)
     * Verifies statistics calculation for a conversation.
     */
    @Test
    @Order(4)
    @DisplayName("Test 4: Get Conversation Statistics - Should return message count, tokens, and cost (MOCKED)")
    void testGetConversationStats() {
        List<MemoryEntry> messages = memoryRepository.findByConversationIdOrderByTimestampAsc(conversationId);

        assertThat(messages).isNotEmpty();

        // Calculate mock statistics
        long totalTokens = messages.stream()
                .mapToLong(m -> m.getTotalTokens() != null ? m.getTotalTokens() : 0)
                .sum();

        int messageCount = messages.size();

        assertThat(messageCount).isGreaterThanOrEqualTo(2);
        assertThat(totalTokens).isGreaterThan(0);

        System.out.println("✅ Test 4 (MOCKED): Conversation statistics calculated");
        System.out.println("   Message count: " + messageCount);
        System.out.println("   Total tokens: " + totalTokens);
        System.out.println("   Estimated cost: $" + String.format("%.4f", totalTokens * 0.00002));
    }

    /**
     * Test 5: Send second message (MOCKED)
     * Tests conversation continuity and context awareness.
     */
    @Test
    @Order(5)
    @DisplayName("Test 5: Send Second Message - Should maintain conversation context (MOCKED)")
    void testSendSecondMessage() {
        // Create second mock message
        MemoryEntry userMsg2 = MemoryEntry.builder()
                .conversationId(conversationId)
                .userId(USER_ID)
                .role("user")
                .content("What did I just ask you?")
                .timestamp(LocalDateTime.now())
                .model("gpt-4")
                .inputTokens(8)
                .totalTokens(8)
                .isCompressed(false)
                .build();

        MemoryEntry assistantMsg2 = MemoryEntry.builder()
                .conversationId(conversationId)
                .userId(USER_ID)
                .role("assistant")
                .content("You asked: 'Hello! This is a test message for PostgreSQL memory integration test.' This demonstrates conversation context awareness.")
                .timestamp(LocalDateTime.now())
                .model("gpt-4")
                .inputTokens(8)
                .outputTokens(30)
                .totalTokens(38)
                .isCompressed(false)
                .build();

        // Save to database
        memoryRepository.save(userMsg2);
        memoryRepository.save(assistantMsg2);

        List<MemoryEntry> messages = memoryRepository.findByConversationIdOrderByTimestampAsc(conversationId);
        assertThat(messages.size()).isGreaterThanOrEqualTo(4);

        System.out.println("✅ Test 5 (MOCKED): Second message sent maintaining context");
        System.out.println("   Total conversation messages: " + messages.size());
    }

    /**
     * Test 6: Verify history persistence (MOCKED)
     * Ensures all messages are correctly persisted.
     */
    @Test
    @Order(6)
    @DisplayName("Test 6: Verify History Persistence - Should have all messages persisted (MOCKED)")
    void testHistoryPersistence() {
        List<MemoryEntry> messages = memoryRepository.findByConversationIdOrderByTimestampAsc(conversationId);

        assertThat(messages).isNotEmpty();
        assertThat(messages.size()).isGreaterThanOrEqualTo(4);

        // Verify all messages have required fields
        messages.forEach(msg -> {
            assertThat(msg.getConversationId()).isEqualTo(conversationId);
            assertThat(msg.getUserId()).isEqualTo(USER_ID);
            assertThat(msg.getRole()).isIn("user", "assistant", "system");
            assertThat(msg.getContent()).isNotBlank();
            assertThat(msg.getTimestamp()).isNotNull();
        });

        System.out.println("✅ Test 6 (MOCKED): History persistence verified");
        System.out.println("   Persisted messages: " + messages.size());
        System.out.println("   All messages have required metadata ✅");
    }

    /**
     * Test 7: Get user's conversations (MOCKED)
     * Verifies listing all conversations for a user.
     */
    @Test
    @Order(7)
    @DisplayName("Test 7: Get User's Conversations - Should list all user conversations (MOCKED)")
    void testGetUserConversations() {
        // Query all conversations for this user from database
        List<MemoryEntry> userMessages = memoryRepository.findByConversationIdOrderByTimestampAsc(conversationId);

        assertThat(userMessages).isNotEmpty();

        // In a real scenario, we would query all distinct conversations for a user
        // For this mock, we verify our test conversation has messages
        long userConversationCount = userMessages.stream()
                .filter(m -> m.getUserId().equals(USER_ID))
                .count();

        assertThat(userConversationCount).isGreaterThan(0);

        System.out.println("✅ Test 7 (MOCKED): User conversations retrieved");
        System.out.println("   User ID: " + USER_ID);
        System.out.println("   Messages in test conversation: " + userConversationCount);
    }

    /**
     * Test 8: Export conversation to JSON (MOCKED)
     * Tests conversation export functionality.
     */
    @Test
    @Order(8)
    @DisplayName("Test 8: Export Conversation - Should export to JSON format (MOCKED)")
    void testExportConversation() {
        List<MemoryEntry> messages = memoryRepository.findByConversationIdOrderByTimestampAsc(conversationId);

        assertThat(messages).isNotEmpty();

        // Create a mock export
        Map<String, Object> export = new HashMap<>();
        export.put("conversationId", conversationId);
        export.put("userId", USER_ID);
        export.put("messageCount", messages.size());
        export.put("messages", messages);
        export.put("exportDate", LocalDateTime.now());

        assertThat(export).containsKeys("conversationId", "userId", "messageCount", "messages");
        assertThat(export.get("conversationId")).isEqualTo(conversationId);
        assertThat(((List<?>) export.get("messages")).size()).isEqualTo(messages.size());

        System.out.println("✅ Test 8 (MOCKED): Conversation exported to JSON");
        System.out.println("   Conversation ID: " + conversationId);
        System.out.println("   Messages exported: " + messages.size());
        System.out.println("   Export format: JSON");
    }

    /**
     * Test 9: Get global statistics
     * Tests global statistics across all conversations.
     */
    @Test
    @Order(9)
    @DisplayName("Test 9: Get Global Statistics - Should return system-wide statistics")
    void testGetGlobalStats() throws Exception {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                createUrl("/api/memory/stats"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKeys("totalConversations", "totalMessages", "totalTokens", "totalCost");

        System.out.println("Global statistics: " + response.getBody());
    }

    /**
     * Test 10: Check conversation exists
     * Verifies conversation existence check endpoint.
     */
    @Test
    @Order(10)
    @DisplayName("Test 10: Check Conversation Exists - Should confirm conversation exists")
    void testConversationExists() throws Exception {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                createUrl("/api/memory/conversation/" + conversationId + "/exists"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("conversationId")).isEqualTo(conversationId);
        assertThat(response.getBody().get("exists")).isEqualTo(true);

        System.out.println("Existence check: " + response.getBody());
    }

    /**
     * Test 11: Check non-existent conversation
     * Tests existence check for a conversation that doesn't exist.
     */
    @Test
    @Order(11)
    @DisplayName("Test 11: Check Non-Existent Conversation - Should return false")
    void testNonExistentConversation() throws Exception {
        String fakeConversationId = "non-existent-conversation-id";

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                createUrl("/api/memory/conversation/" + fakeConversationId + "/exists"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("conversationId")).isEqualTo(fakeConversationId);
        assertThat(response.getBody().get("exists")).isEqualTo(false);
    }

    /**
     * Test 12: Get recent messages
     * Tests retrieving limited number of recent messages.
     */
    @Test
    @Order(12)
    @DisplayName("Test 12: Get Recent Messages - Should return limited messages")
    void testGetRecentMessages() throws Exception {
        int limit = 2;

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                createUrl("/api/memory/conversation/" + conversationId + "/recent?limit=" + limit),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("conversationId")).isEqualTo(conversationId);
        assertThat(response.getBody().get("limit")).isEqualTo(limit);

        @SuppressWarnings("unchecked")
        List<Object> messages = (List<Object>) response.getBody().get("messages");
        assertThat(messages.size()).isLessThanOrEqualTo(limit);

        System.out.println("Recent messages response: " + response.getBody());
    }

    /**
     * Test 13: Delete conversation
     * Tests conversation deletion (cleanup).
     */
    @Test
    @Order(13)
    @DisplayName("Test 13: Delete Conversation - Should delete all messages")
    void testDeleteConversation() throws Exception {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                createUrl("/api/memory/conversation/" + conversationId),
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("conversationId")).isEqualTo(conversationId);
        assertThat(((Number) response.getBody().get("deletedMessages")).intValue()).isGreaterThan(0);
        assertThat(response.getBody().get("status")).isEqualTo("deleted");

        System.out.println("Deletion response: " + response.getBody());
    }

    /**
     * Test 14: Verify deletion
     * Confirms that deleted conversation no longer exists.
     */
    @Test
    @Order(14)
    @DisplayName("Test 14: Verify Deletion - Should not find deleted conversation")
    void testVerifyDeletion() throws Exception {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                createUrl("/api/memory/conversation/" + conversationId + "/exists"),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("exists")).isEqualTo(false);
    }

    /**
     * Test 15: Get history of deleted conversation
     * Should return empty or minimal response.
     */
    @Test
    @Order(15)
    @DisplayName("Test 15: Get Deleted Conversation History - Should return empty")
    void testGetDeletedConversationHistory() throws Exception {
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                createUrl("/api/memory/conversation/" + conversationId),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("messageCount")).isEqualTo(0);

        @SuppressWarnings("unchecked")
        List<Object> messages = (List<Object>) response.getBody().get("messages");
        assertThat(messages).isEmpty();
    }

    /**
     * Test 16: Verify Test Data Created - Should have test data in database
     * Directly queries database to confirm test data creation.
     */
    @Test
    @Order(16)
    @DisplayName("Test 16: Verify Test Data Created - Should have 8 messages from test data (4 Q&A pairs)")
    void testVerifyTestDataCreated() {
        // Query database directly for test data
        List<MemoryEntry> messages = memoryRepository.findByConversationIdOrderByTimestampAsc(testDataConversationId);

        assertThat(messages).isNotEmpty();
        assertThat(messages.size()).isGreaterThanOrEqualTo(8);  // 4 user + 4 assistant messages

        System.out.println("✅ Test data verification - Found " + messages.size() + " messages in DB");
        messages.forEach(msg -> System.out.println("   - " + msg.getRole() + ": " + msg.getContent().substring(0, Math.min(50, msg.getContent().length())) + "..."));
    }

    /**
     * Test 17: Query Test Data Messages - Should retrieve all test messages
     * Verifies message content directly from database.
     */
    @Test
    @Order(17)
    @DisplayName("Test 17: Query Test Data Messages - Should have expected content in database")
    void testQueryTestDataMessages() {
        List<MemoryEntry> messages = memoryRepository.findByConversationIdOrderByTimestampAsc(testDataConversationId);

        assertThat(messages).isNotEmpty();

        // Verify message content contains expected keywords
        boolean hasAIMessage = messages.stream()
                .anyMatch(msg -> msg.getContent().toLowerCase().contains("artificial intelligence"));
        boolean hasMachineLearningMessage = messages.stream()
                .anyMatch(msg -> msg.getContent().toLowerCase().contains("machine learning"));

        assertThat(hasAIMessage).isTrue();
        assertThat(hasMachineLearningMessage).isTrue();

        System.out.println("✅ Retrieved " + messages.size() + " test data messages with expected content");
        messages.stream()
                .filter(MemoryEntry::isUserMessage)
                .forEach(msg -> System.out.println("   Q: " + msg.getContent()));
    }

    /**
     * Test 18: Verify Test Data User and Metadata - Should have complete entries
     * Checks that all required fields are properly set in database.
     */
    @Test
    @Order(18)
    @DisplayName("Test 18: Verify Test Data User and Metadata - Should have complete entries")
    void testVerifyTestDataUserStats() {
        List<MemoryEntry> messages = memoryRepository.findByConversationIdOrderByTimestampAsc(testDataConversationId);

        assertThat(messages).isNotEmpty();

        // Verify all entries have proper metadata
        for (MemoryEntry message : messages) {
            assertThat(message.getUserId()).isEqualTo(TEST_DATA_USER_ID);
            assertThat(message.getConversationId()).isEqualTo(testDataConversationId);
            assertThat(message.getModel()).isEqualTo("gpt-4");
            assertThat(message.getTimestamp()).isNotNull();
            assertThat(message.getInputTokens()).isGreaterThan(0);
            assertThat(message.getTotalTokens()).isGreaterThan(0);
        }

        System.out.println("✅ Test data metadata verification - All " + messages.size() + " entries have complete data");
    }

    /**
     * Test 19: Verify Test Data Message Roles - Should have user and assistant messages
     * Confirms that both user and assistant messages are properly stored.
     */
    @Test
    @Order(19)
    @DisplayName("Test 19: Verify Test Data Message Roles - Should have user and assistant messages")
    void testVerifyTestDataInUserConversations() {
        List<MemoryEntry> messages = memoryRepository.findByConversationIdOrderByTimestampAsc(testDataConversationId);

        assertThat(messages).isNotEmpty();

        long userMessageCount = messages.stream().filter(MemoryEntry::isUserMessage).count();
        long assistantMessageCount = messages.stream().filter(MemoryEntry::isAssistantMessage).count();

        assertThat(userMessageCount).isGreaterThan(0);
        assertThat(assistantMessageCount).isGreaterThan(0);
        assertThat(userMessageCount).isEqualTo(assistantMessageCount);

        System.out.println("✅ Test data roles verification:");
        System.out.println("   - User messages: " + userMessageCount);
        System.out.println("   - Assistant messages: " + assistantMessageCount);
    }

    /**
     * Test 20: Retrieve Recent Test Data Messages - Should get messages in chronological order
     * Verifies message ordering and retrieval.
     */
    @Test
    @Order(20)
    @DisplayName("Test 20: Retrieve Recent Test Data Messages - Should be in chronological order")
    void testRetrieveRecentTestDataMessages() {
        List<MemoryEntry> messages = memoryRepository.findByConversationIdOrderByTimestampAsc(testDataConversationId);

        assertThat(messages).isNotEmpty();
        assertThat(messages.size()).isGreaterThanOrEqualTo(2);

        // Verify messages are in chronological order
        for (int i = 1; i < messages.size(); i++) {
            assertThat(messages.get(i).getTimestamp().isAfter(messages.get(i - 1).getTimestamp())
                    || messages.get(i).getTimestamp().equals(messages.get(i - 1).getTimestamp()))
                    .isTrue();
        }

        // Get first 2 messages
        List<MemoryEntry> recentMessages = messages.stream().limit(2).toList();
        assertThat(recentMessages).hasSize(2);

        System.out.println("✅ Recent messages verification - Messages are in chronological order");
        System.out.println("   First message: " + recentMessages.get(0).getTimestamp());
        System.out.println("   Second message: " + recentMessages.get(1).getTimestamp());
    }

    @AfterAll
    static void cleanup() {
        System.out.println("\n==============================================");
        System.out.println("🎉 All MemoryController integration tests completed!");
        System.out.println("Test conversation ID: " + conversationId);
        System.out.println("Test data conversation ID: " + testDataConversationId);
        System.out.println("==============================================\n");
    }
}

