/*
package de.jivz.mcp.service;

import de.jivz.mcp.model.McpTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

*/
/**
 * Integration Test für das Multi-Provider MCP System
 *//*

@ExtendWith(MockitoExtension.class)
class McpServerServiceIntegrationTest {

    @Test
    void testMultiProviderInitialization() {
        // Arrange
        NativeToolProvider nativeProvider = new NativeToolProvider();
        PerplexityToolProvider perplexityProvider = new PerplexityToolProvider();
        List<ToolProvider> providers = List.of(nativeProvider, perplexityProvider);

        // Act
        McpServerService service = new McpServerService(providers);

        // Assert
        assertEquals(2, service.listProviders().size(), "Should have 2 providers");
        assertTrue(service.listProviders().contains("native"), "Should contain native provider");
        assertTrue(service.listProviders().contains("perplexity"), "Should contain perplexity provider");
        assertEquals(7, service.listTools().size(), "Should have 7 total tools");
    }

    @Test
    void testListToolsByProvider() {
        // Arrange
        NativeToolProvider nativeProvider = new NativeToolProvider();
        PerplexityToolProvider perplexityProvider = new PerplexityToolProvider();
        McpServerService service = new McpServerService(List.of(nativeProvider, perplexityProvider));

        // Act
        List<McpTool> nativeTools = service.listToolsByProvider("native");
        List<McpTool> perplexityTools = service.listToolsByProvider("perplexity");

        // Assert
        assertEquals(5, nativeTools.size(), "Native provider should have 5 tools");
        assertEquals(2, perplexityTools.size(), "Perplexity provider should have 2 tools");
    }

    @Test
    void testListToolsByProvider_InvalidProvider() {
        // Arrange
        NativeToolProvider nativeProvider = new NativeToolProvider();
        McpServerService service = new McpServerService(List.of(nativeProvider));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> service.listToolsByProvider("invalid"),
                "Should throw exception for invalid provider");
    }

    @Test
    void testExecuteNativeTool() {
        // Arrange
        NativeToolProvider nativeProvider = new NativeToolProvider();
        McpServerService service = new McpServerService(List.of(nativeProvider));
        Map<String, Object> args = Map.of("a", 10, "b", 5);

        // Act
        Object result = service.executeTool("add_numbers", args);

        // Assert
        assertEquals(15, result, "Should add numbers correctly");
    }

    @Test
    void testExecutePerplexityTool() {
        // Arrange
        PerplexityToolProvider perplexityProvider = new PerplexityToolProvider();
        McpServerService service = new McpServerService(List.of(perplexityProvider));
        Map<String, Object> args = Map.of(
                "query", "Test query",
                "temperature", 0.5
        );

        // Act
        Object result = service.executeTool("perplexity_search", args);

        // Assert
        assertNotNull(result, "Should return a result");
        assertTrue(result instanceof Map, "Result should be a map");

        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals("Test query", resultMap.get("query"));
        assertEquals("mock", resultMap.get("status"), "Should be mock implementation");
    }

    @Test
    void testExecuteTool_UnknownTool() {
        // Arrange
        NativeToolProvider nativeProvider = new NativeToolProvider();
        McpServerService service = new McpServerService(List.of(nativeProvider));

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> service.executeTool("unknown_tool", Map.of()),
                "Should throw exception for unknown tool");
    }

    @Test
    void testGetTool() {
        // Arrange
        NativeToolProvider nativeProvider = new NativeToolProvider();
        McpServerService service = new McpServerService(List.of(nativeProvider));

        // Act
        var tool = service.getTool("add_numbers");

        // Assert
        assertTrue(tool.isPresent(), "Tool should be found");
        assertEquals("add_numbers", tool.get().getName());
    }

    @Test
    void testGetTool_NotFound() {
        // Arrange
        NativeToolProvider nativeProvider = new NativeToolProvider();
        McpServerService service = new McpServerService(List.of(nativeProvider));

        // Act
        var tool = service.getTool("non_existent_tool");

        // Assert
        assertFalse(tool.isPresent(), "Tool should not be found");
    }

    @Test
    void testGetStatistics() {
        // Arrange
        NativeToolProvider nativeProvider = new NativeToolProvider();
        PerplexityToolProvider perplexityProvider = new PerplexityToolProvider();
        McpServerService service = new McpServerService(List.of(nativeProvider, perplexityProvider));

        // Act
        Map<String, Object> stats = service.getStatistics();

        // Assert
        assertEquals(2, stats.get("total_providers"));
        assertEquals(7, stats.get("total_tools"));

        @SuppressWarnings("unchecked")
        Map<String, Integer> providerStats = (Map<String, Integer>) stats.get("tools_by_provider");
        assertEquals(5, providerStats.get("native"));
        assertEquals(2, providerStats.get("perplexity"));
    }

    @Test
    void testNativeProvider_ReverseString() {
        // Arrange
        NativeToolProvider nativeProvider = new NativeToolProvider();
        McpServerService service = new McpServerService(List.of(nativeProvider));
        Map<String, Object> args = Map.of("text", "Hello");

        // Act
        Object result = service.executeTool("reverse_string", args);

        // Assert
        assertEquals("olleH", result);
    }

    @Test
    void testNativeProvider_CalculateFibonacci() {
        // Arrange
        NativeToolProvider nativeProvider = new NativeToolProvider();
        McpServerService service = new McpServerService(List.of(nativeProvider));
        Map<String, Object> args = Map.of("n", 7);

        // Act
        Object result = service.executeTool("calculate_fibonacci", args);

        // Assert
        assertEquals(13, result, "7th Fibonacci number should be 13");
    }

    @Test
    void testNativeProvider_CountWords() {
        // Arrange
        NativeToolProvider nativeProvider = new NativeToolProvider();
        McpServerService service = new McpServerService(List.of(nativeProvider));
        Map<String, Object> args = Map.of("text", "Hello World Test");

        // Act
        Object result = service.executeTool("count_words", args);

        // Assert
        assertTrue(result instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) result;
        assertEquals(3, stats.get("word_count"));
    }
}

*/
