# LLM Chat Service

A Spring Boot 3.4.0 microservice for integrating with local Ollama LLM instances. Provides REST API for sending chat messages and receiving AI-generated responses.

## Features

- **Spring Boot 3.4.0** with Java 21
- **WebFlux** reactive HTTP client for non-blocking LLM API calls
- **Ollama Integration** via WebClient with configurable timeouts
- **Configuration Properties** for flexible LLM configuration
- **REST API** endpoints for chat, status, and model information
- **Error Handling** with proper error responses
- **Logging** with SLF4J for debugging and monitoring

## Architecture

```
ChatController (REST)
    ↓
LlmChatService (Business Logic)
    ↓
WebClient (Reactive HTTP)
    ↓
Ollama API (http://localhost:11434)
```

## Prerequisites

1. **Java 21** installed
2. **Maven 3.9+** installed
3. **Ollama** running locally at `http://localhost:11434`

### Install and Start Ollama

```bash
# Install Ollama (macOS/Linux)
curl -fsSL https://ollama.ai/install.sh | sh

# Start Ollama service
ollama serve

# Pull a model (in another terminal)
ollama pull llama2
ollama pull mistral
ollama pull codellama
```

## Configuration

### Environment Variables

Create a `.env` file or set environment variables:

```bash
# Ollama Configuration
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=llama2
OLLAMA_TEMPERATURE=0.7
OLLAMA_MAX_TOKENS=2000
OLLAMA_TIMEOUT=120
```

### Application Properties

Default configuration in `src/main/resources/application.properties`:

```properties
server.port=8090
llm.ollama.base-url=http://localhost:11434
llm.ollama.model=llama2
llm.ollama.temperature=0.7
llm.ollama.max-tokens=2000
llm.ollama.timeout-seconds=120
```

## Build and Run

### Build the Service

```bash
# Build with Maven
cd backend/llm-chat-service
./mvnw clean install -DskipTests

# Or from backend aggregator
cd backend
mvn clean install -DskipTests
```

### Run the Service

```bash
# Using Maven
cd backend/llm-chat-service
./mvnw spring-boot:run

# Or with Java
java -jar target/llm-chat-service-0.0.1-SNAPSHOT.jar
```

Service will start on `http://localhost:8090`

## API Endpoints

### POST /api/chat

Send a message to the LLM and get a response.

**Request:**
```json
{
  "message": "Explain Spring Boot in simple terms",
  "model": "llama2",
  "temperature": 0.7,
  "maxTokens": 500,
  "systemPrompt": "You are a helpful coding assistant",
  "stream": false
}
```

**Response:**
```json
{
  "response": "Spring Boot is a framework that...",
  "model": "llama2",
  "timestamp": "2026-01-20T10:30:00",
  "processingTimeMs": 2340,
  "tokensGenerated": 156,
  "done": true,
  "error": null,
  "metadata": {
    "totalDuration": 2340000000,
    "loadDuration": 45000000,
    "promptEvalCount": 12,
    "promptEvalDuration": 234000000,
    "evalDuration": 2061000000
  }
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8090/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What is Spring Boot?",
    "temperature": 0.7
  }'
```

### GET /api/status

Check service health and configuration.

**Response:**
```json
{
  "service": "llm-chat-service",
  "status": "UP",
  "ollamaBaseUrl": "http://localhost:11434",
  "defaultModel": "llama2",
  "defaultTemperature": 0.7,
  "defaultMaxTokens": 2000,
  "timeoutSeconds": 120
}
```

### GET /api/models

Get current model configuration.

**Response:**
```json
{
  "currentModel": "llama2",
  "baseUrl": "http://localhost:11434",
  "note": "To change model, use 'model' parameter in chat request or update OLLAMA_MODEL env variable"
}
```

## Request Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `message` | String | Yes | - | User's message to send to LLM |
| `model` | String | No | llama2 | Model to use (llama2, mistral, codellama, etc.) |
| `temperature` | Double | No | 0.7 | Controls randomness (0.0-2.0) |
| `maxTokens` | Integer | No | 2000 | Maximum response length |
| `systemPrompt` | String | No | null | System instructions for LLM |
| `stream` | Boolean | No | false | Enable streaming responses |

## Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `response` | String | Generated response from LLM |
| `model` | String | Model used for generation |
| `timestamp` | LocalDateTime | When response was generated |
| `processingTimeMs` | Long | Time taken to generate response (ms) |
| `tokensGenerated` | Integer | Number of tokens in response |
| `done` | Boolean | Whether generation completed successfully |
| `error` | String | Error message if request failed |
| `metadata` | Object | Additional Ollama performance metrics |

## Error Handling

The service provides detailed error responses:

```json
{
  "response": null,
  "model": "llama2",
  "timestamp": "2026-01-20T10:30:00",
  "processingTimeMs": 150,
  "tokensGenerated": 0,
  "done": false,
  "error": "Failed to get response from Ollama: Connection refused"
}
```

Common errors:
- **Connection refused**: Ollama is not running
- **Model not found**: Model needs to be pulled with `ollama pull <model>`
- **Timeout**: Request took longer than configured timeout

## Testing

### Manual Testing

```bash
# Test with default model
curl -X POST http://localhost:8090/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello!"}'

# Test with specific model
curl -X POST http://localhost:8090/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Write a Python hello world",
    "model": "codellama",
    "temperature": 0.3
  }'

# Check status
curl http://localhost:8090/api/status

# Check models
curl http://localhost:8090/api/models
```

### Integration with Frontend

Add this service to the backend ecosystem:

```bash
# Update start-backend.sh
echo "Starting llm-chat-service on port 8090..."
cd backend/llm-chat-service && ./mvnw spring-boot:run &

# Or add to docker-compose
```

## Available Ollama Models

Popular models you can use:

```bash
# General purpose
ollama pull llama2        # Meta's LLaMA 2 (7B)
ollama pull mistral       # Mistral 7B
ollama pull llama3        # Meta's LLaMA 3 (8B)

# Coding
ollama pull codellama     # Code-specialized LLaMA
ollama pull deepseek-coder # DeepSeek Coder

# Conversational
ollama pull neural-chat   # Intel's conversational model
ollama pull vicuna        # Fine-tuned LLaMA

# Small/Fast
ollama pull phi           # Microsoft Phi-2 (2.7B)
ollama pull tinyllama     # TinyLlama (1.1B)
```

## Performance Tuning

### Timeout Configuration

Adjust timeout for larger models:

```properties
# For large models (70B+)
llm.ollama.timeout-seconds=300

# For small models (7B)
llm.ollama.timeout-seconds=60
```

### Temperature Settings

- **0.0-0.3**: Focused, deterministic (code generation, factual answers)
- **0.4-0.7**: Balanced (default, general conversation)
- **0.8-1.5**: Creative (storytelling, brainstorming)
- **1.6-2.0**: Very creative (experimental)

### Max Tokens

- **Short answers**: 100-500 tokens
- **Medium responses**: 500-1500 tokens
- **Long form**: 1500-4000 tokens

## Troubleshooting

### Ollama Not Running

```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# Start Ollama
ollama serve
```

### Model Not Found

```bash
# List installed models
ollama list

# Pull missing model
ollama pull llama2
```

### Port Already in Use

```bash
# Change port in application.properties
server.port=8091
```

## Integration Examples

### Frontend (Vue 3)

```typescript
async function sendMessage(message: string) {
  const response = await fetch('http://localhost:8090/api/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      message,
      temperature: 0.7
    })
  });
  return await response.json();
}
```

### Backend (Spring Boot)

```java
@Service
public class ChatIntegrationService {

    @Value("${llm.chat.url:http://localhost:8090}")
    private String llmChatUrl;

    private final WebClient webClient;

    public Mono<ChatResponse> chat(String message) {
        return webClient.post()
            .uri(llmChatUrl + "/api/chat")
            .bodyValue(Map.of("message", message))
            .retrieve()
            .bodyToMono(ChatResponse.class);
    }
}
```

## Project Structure

```
llm-chat-service/
├── pom.xml
├── README.md
└── src/main/
    ├── java/de/jivz/llmchatservice/
    │   ├── LlmChatServiceApplication.java
    │   ├── config/
    │   │   ├── LlmProperties.java
    │   │   └── WebClientConfig.java
    │   ├── controller/
    │   │   └── ChatController.java
    │   ├── service/
    │   │   └── LlmChatService.java
    │   └── dto/
    │       ├── ChatRequest.java
    │       ├── ChatResponse.java
    │       ├── OllamaRequest.java
    │       └── OllamaResponse.java
    └── resources/
        └── application.properties
```

## Technology Stack

- **Spring Boot 3.4.0**
- **Java 21**
- **Spring WebFlux** (Reactive)
- **Lombok** (Boilerplate reduction)
- **Jackson** (JSON processing)
- **Reactor Netty** (HTTP client)

## License

Part of AI Advent Challenge project.
