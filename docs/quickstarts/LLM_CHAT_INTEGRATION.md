# LLM Chat Service Integration Guide

This guide shows how to integrate the llm-chat-service with other services in the AI Advent Challenge ecosystem.

## Service Overview

- **Port**: 8090
- **Base URL**: http://localhost:8090
- **Protocol**: HTTP REST
- **Response**: Reactive (Mono)

## Integration Patterns

### 1. Spring Boot Service Integration (WebClient)

For other Spring Boot services in the ecosystem:

```java
// Configuration
@Configuration
public class LlmClientConfig {

    @Bean
    public WebClient llmWebClient() {
        return WebClient.builder()
            .baseUrl("http://localhost:8090")
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
}

// Service
@Service
@RequiredArgsConstructor
public class LlmIntegrationService {

    private final WebClient llmWebClient;

    public Mono<ChatResponse> chat(String message) {
        ChatRequest request = ChatRequest.builder()
            .message(message)
            .temperature(0.7)
            .build();

        return llmWebClient.post()
            .uri("/api/chat")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ChatResponse.class);
    }
}

// Controller
@RestController
@RequestMapping("/api/enhanced")
@RequiredArgsConstructor
public class EnhancedChatController {

    private final LlmIntegrationService llmService;

    @PostMapping("/chat")
    public Mono<String> enhancedChat(@RequestBody Map<String, String> request) {
        return llmService.chat(request.get("message"))
            .map(ChatResponse::getResponse);
    }
}
```

### 2. Frontend Integration (Vue 3 / TypeScript)

```typescript
// services/llmChatService.ts
interface ChatRequest {
  message: string;
  model?: string;
  temperature?: number;
  maxTokens?: number;
  systemPrompt?: string;
}

interface ChatResponse {
  response: string;
  model: string;
  timestamp: string;
  processingTimeMs: number;
  tokensGenerated: number;
  done: boolean;
  error?: string;
  metadata?: any;
}

export class LlmChatService {
  private baseUrl = 'http://localhost:8090/api';

  async sendMessage(request: ChatRequest): Promise<ChatResponse> {
    const response = await fetch(`${this.baseUrl}/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    return await response.json();
  }

  async getStatus() {
    const response = await fetch(`${this.baseUrl}/status`);
    return await response.json();
  }
}

// Vue component
<script setup lang="ts">
import { ref } from 'vue';
import { LlmChatService } from '@/services/llmChatService';

const llmService = new LlmChatService();
const message = ref('');
const response = ref('');
const loading = ref(false);

async function sendMessage() {
  loading.value = true;
  try {
    const result = await llmService.sendMessage({
      message: message.value,
      temperature: 0.7,
    });
    response.value = result.response;
  } catch (error) {
    console.error('Error:', error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="llm-chat">
    <textarea v-model="message" placeholder="Enter your message" />
    <button @click="sendMessage" :disabled="loading">
      {{ loading ? 'Processing...' : 'Send' }}
    </button>
    <div v-if="response" class="response">{{ response }}</div>
  </div>
</template>
```

### 3. OpenRouter Service Integration

Add local LLM as fallback in openrouter-service:

```java
@Service
@RequiredArgsConstructor
public class HybridLlmService {

    private final WebClient openRouterClient;
    private final WebClient llmChatClient;

    public Mono<String> chat(String message, boolean useLocal) {
        if (useLocal) {
            // Use local LLM
            return llmChatClient.post()
                .uri("/api/chat")
                .bodyValue(Map.of("message", message))
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .map(ChatResponse::getResponse);
        } else {
            // Use OpenRouter (cloud)
            return openRouterClient.post()
                .uri("/chat/completions")
                .bodyValue(buildOpenRouterRequest(message))
                .retrieve()
                .bodyToMono(String.class);
        }
    }
}
```

### 4. MCP Server Integration

Expose local LLM as MCP tool:

```java
@Service
public class LlmMcpService extends BaseMCPService {

    private final WebClient llmWebClient;

    @Override
    public List<ToolDefinition> getTools() {
        return List.of(
            ToolDefinition.builder()
                .name("local_llm_chat")
                .description("Chat with local LLM (Ollama)")
                .parameters(Map.of(
                    "message", "The message to send to local LLM",
                    "model", "Optional model name (llama2, mistral, etc.)"
                ))
                .build()
        );
    }

    @Override
    public Mono<String> executeTool(String toolName, Map<String, Object> params) {
        if ("local_llm_chat".equals(toolName)) {
            return llmWebClient.post()
                .uri("/api/chat")
                .bodyValue(params)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .map(ChatResponse::getResponse);
        }
        return Mono.error(new IllegalArgumentException("Unknown tool: " + toolName));
    }
}
```

### 5. Agent Service Integration

Use local LLM for agent reasoning:

```java
@Service
@RequiredArgsConstructor
public class LocalLlmAgent {

    private final WebClient llmWebClient;

    public Mono<AgentResponse> processTask(AgentTask task) {
        // Build system prompt for agent
        String systemPrompt = """
            You are an AI agent tasked with: %s
            Think step by step and provide a clear response.
            """.formatted(task.getGoal());

        ChatRequest request = ChatRequest.builder()
            .message(task.getInput())
            .systemPrompt(systemPrompt)
            .temperature(0.3) // Lower for more focused reasoning
            .maxTokens(1000)
            .build();

        return llmWebClient.post()
            .uri("/api/chat")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ChatResponse.class)
            .map(this::mapToAgentResponse);
    }
}
```

## Configuration Properties

Add to other services' `application.properties`:

```properties
# LLM Chat Service Integration
llm.chat.enabled=true
llm.chat.base-url=http://localhost:8090
llm.chat.timeout-seconds=120
llm.chat.fallback-enabled=true
```

## Docker Compose Integration

Add to `docker-compose.yml`:

```yaml
services:
  llm-chat-service:
    build: ./backend/llm-chat-service
    ports:
      - "8090:8090"
    environment:
      - OLLAMA_BASE_URL=http://ollama:11434
      - OLLAMA_MODEL=llama2
      - OLLAMA_TEMPERATURE=0.7
    depends_on:
      - ollama
    networks:
      - ai-network

  ollama:
    image: ollama/ollama:latest
    ports:
      - "11434:11434"
    volumes:
      - ollama-data:/root/.ollama
    networks:
      - ai-network

  openrouter-service:
    depends_on:
      - llm-chat-service
    environment:
      - LLM_CHAT_URL=http://llm-chat-service:8090
```

## Load Balancing Pattern

For high availability, use multiple LLM instances:

```java
@Configuration
public class LlmLoadBalancerConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder llmWebClientBuilder() {
        return WebClient.builder();
    }
}

// Use service discovery (Eureka, Consul, etc.)
// Request routing: http://llm-chat-service/api/chat
```

## Error Handling

Implement circuit breaker for resilience:

```java
@Service
public class ResilientLlmService {

    private final WebClient llmWebClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public Mono<ChatResponse> chatWithFallback(ChatRequest request) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry
            .circuitBreaker("llm-chat-service");

        return llmWebClient.post()
            .uri("/api/chat")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ChatResponse.class)
            .transform(CircuitBreakerOperator.of(circuitBreaker))
            .onErrorResume(error -> {
                log.error("LLM service error, using fallback", error);
                return Mono.just(buildFallbackResponse());
            });
    }
}
```

## Monitoring Integration

Add metrics collection:

```java
@Service
public class MonitoredLlmService {

    private final WebClient llmWebClient;
    private final MeterRegistry meterRegistry;

    public Mono<ChatResponse> chat(ChatRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);

        return llmWebClient.post()
            .uri("/api/chat")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ChatResponse.class)
            .doOnSuccess(response -> {
                sample.stop(meterRegistry.timer("llm.chat.duration"));
                meterRegistry.counter("llm.chat.success").increment();
            })
            .doOnError(error -> {
                sample.stop(meterRegistry.timer("llm.chat.duration"));
                meterRegistry.counter("llm.chat.error").increment();
            });
    }
}
```

## Rate Limiting

Implement request throttling:

```java
@Component
public class LlmRateLimiter {

    private final RateLimiter rateLimiter = RateLimiter.create(10.0); // 10 requests/sec

    public Mono<ChatResponse> limitedChat(ChatRequest request) {
        if (!rateLimiter.tryAcquire()) {
            return Mono.error(new RateLimitExceededException());
        }

        return llmWebClient.post()
            .uri("/api/chat")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ChatResponse.class);
    }
}
```

## Best Practices

1. **Connection Pooling**: Use WebClient with connection pool
2. **Timeouts**: Set appropriate read/write timeouts
3. **Retry Logic**: Implement exponential backoff for failures
4. **Caching**: Cache frequent responses to reduce load
5. **Async Processing**: Use reactive patterns throughout
6. **Health Checks**: Monitor service availability
7. **Graceful Degradation**: Have fallback mechanisms

## Example: Complete Integration

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class CompleteLlmIntegration {

    private final WebClient llmWebClient;
    private final MeterRegistry metrics;
    private final CircuitBreaker circuitBreaker;
    private final CacheManager cacheManager;

    @Cacheable("llm-responses")
    public Mono<ChatResponse> chat(String message) {
        return Mono.defer(() -> {
            log.info("Sending message to local LLM: {}", message);

            ChatRequest request = ChatRequest.builder()
                .message(message)
                .temperature(0.7)
                .maxTokens(500)
                .build();

            return llmWebClient.post()
                .uri("/api/chat")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse.class)
                .timeout(Duration.ofSeconds(120))
                .transform(CircuitBreakerOperator.of(circuitBreaker))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .doOnSuccess(response -> {
                    metrics.counter("llm.chat.success").increment();
                    log.info("LLM response received in {} ms",
                        response.getProcessingTimeMs());
                })
                .doOnError(error -> {
                    metrics.counter("llm.chat.error").increment();
                    log.error("LLM chat failed", error);
                })
                .onErrorResume(this::handleError);
        });
    }

    private Mono<ChatResponse> handleError(Throwable error) {
        return Mono.just(ChatResponse.builder()
            .error("Service temporarily unavailable: " + error.getMessage())
            .done(false)
            .build());
    }
}
```

## Testing Integration

```java
@SpringBootTest
@AutoConfigureWebTestClient
class LlmIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testChatIntegration() {
        ChatRequest request = ChatRequest.builder()
            .message("Test message")
            .build();

        webTestClient.post()
            .uri("http://localhost:8090/api/chat")
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody(ChatResponse.class)
            .value(response -> {
                assertNotNull(response.getResponse());
                assertTrue(response.getDone());
            });
    }
}
```

For more details, see:
- [README.md](README.md) - Complete service documentation
- [QUICKSTART.md](QUICKSTART.md) - Getting started guide
