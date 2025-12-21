package de.jivz.ai_challenge.openrouterservice.controller;

import de.jivz.ai_challenge.openrouterservice.dto.ChatRequest;
import de.jivz.ai_challenge.openrouterservice.dto.ChatResponse;
import de.jivz.ai_challenge.openrouterservice.service.OpenRouterAiChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OpenRouterChatController.class)
class OpenRouterChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OpenRouterAiChatService chatService;

    @Test
    void testSimpleChat() throws Exception {
        ChatResponse mockResponse = ChatResponse.builder()
                .reply("Test response")
                .responseTimeMs(100L)
                .finishReason("stop")
                .build();

        when(chatService.chat("Test message")).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/openrouter/chat/simple")
                .param("message", "Test message")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Test response"));
    }

    @Test
    void testFullChat() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .message("Test message")
                .model("openrouter/auto")
                .temperature(0.5)
                .maxTokens(100)
                .build();

        ChatResponse mockResponse = ChatResponse.builder()
                .reply("Test response")
                .model("openrouter/auto")
                .responseTimeMs(100L)
                .finishReason("stop")
                .build();

        when(chatService.chatWithRequest(any(ChatRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/openrouter/chat/full")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Test message\",\"model\":\"openrouter/auto\",\"temperature\":0.5,\"maxTokens\":100}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("openrouter/auto"));
    }

    @Test
    void testHealth() throws Exception {
        mockMvc.perform(get("/api/v1/openrouter/chat/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OpenRouter Chat Service is running"));
    }

    @Test
    void testJsonChat() throws Exception {
        ChatResponse mockResponse = ChatResponse.builder()
                .reply("{\"response\": \"test\", \"status\": \"success\"}")
                .responseTimeMs(100L)
                .finishReason("stop")
                .build();

        when(chatService.chat(any(String.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/openrouter/chat/json")
                .param("message", "Test message")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").exists());
    }
}

