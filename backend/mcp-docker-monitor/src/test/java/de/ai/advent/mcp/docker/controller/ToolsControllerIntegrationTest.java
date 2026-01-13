package de.ai.advent.mcp.docker.controller;

import de.ai.advent.mcp.docker.model.ToolCallRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ToolsController
 */
@SpringBootTest
@AutoConfigureMockMvc
class ToolsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testListTools() throws Exception {
        mockMvc.perform(get("/api/tools")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("list_containers"))
            .andExpect(jsonPath("$[0].description").exists())
            .andExpect(jsonPath("$[0].inputSchema").exists());
    }

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testCallToolWithMissingRequiredArgument() throws Exception {
        ToolCallRequest request = new ToolCallRequest();
        //request.setName("list_containers");
        request.setArguments(new HashMap<>()); // Missing container_name for get_container_logs

        mockMvc.perform(post("/api/tools/call")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.toolName").exists());
    }

    @Test
    void testCallToolWithInvalidToolName() throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("host", "localhost");
        arguments.put("username", "test");

        ToolCallRequest request = new ToolCallRequest("invalid_tool_name", arguments);

        mockMvc.perform(post("/api/tools/call")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.toolName").value("invalid_tool_name"));
    }

    @Test
    void testCallToolWithInvalidArguments() throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("host", "localhost");
        arguments.put("username", "test");
        arguments.put("port", "invalid-port"); // Should be integer

        ToolCallRequest request = new ToolCallRequest("list_containers", arguments);

        mockMvc.perform(post("/api/tools/call")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testCallToolResponseHasTimestamp() throws Exception {
        ToolCallRequest request = new ToolCallRequest();
        //request.setName("list_containers");
        request.setArguments(new HashMap<>());

        mockMvc.perform(post("/api/tools/call")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.timestamp").isNumber())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testCallToolWithValidName() throws Exception {
        // Test with a valid tool name structure (execution may fail due to missing SSH config)
        Map<String, Object> arguments = new HashMap<>();
        ToolCallRequest request = new ToolCallRequest("list_containers", arguments);

        mockMvc.perform(post("/api/tools/call")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(jsonPath("$.success").exists())
            .andExpect(jsonPath("$.toolName").value("list_containers"));
    }

    @Test
    void testCallToolErrorResponseStructure() throws Exception {
        ToolCallRequest request = new ToolCallRequest("invalid_tool", new HashMap<>());

        mockMvc.perform(post("/api/tools/call")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.timestamp").isNumber())
            .andExpect(jsonPath("$.toolName").exists());
    }
}

