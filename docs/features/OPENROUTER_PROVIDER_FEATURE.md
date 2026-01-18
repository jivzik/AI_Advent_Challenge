# OpenRouter Provider Feature

## 📋 Quick Summary
OpenRouter Provider ist ein intelligenter LLM-Proxy-Service, der Multi-Model-Zugriff über OpenRouter API bereitstellt mit automatischer MCP Tool-Integration, zweistufigem Conversation History Management (RAM + PostgreSQL) und kontextbasierter Tool-Ausführung.

## 🎯 Use Cases
- **Use Case 1**: Einheitlicher Zugriff auf mehrere LLM-Modelle (Claude, GPT-4, Llama, Gemini) über eine API
- **Use Case 2**: Automatische Tool-Ausführung basierend auf natürlichsprachlichen Anfragen
- **Use Case 3**: Persistente Conversation History über mehrere Sessions mit Performance-Optimierung
- **Use Case 4**: RAG-basierte Dokumentensuche mit automatischer Quellenzuordnung

## 🏗️ Architecture Overview

### High-Level Diagram (ASCII)
```
Frontend/Client
      │
      ↓
OpenRouter Service (port 8084)
      │
      ├─────────────┬────────────┬──────────────┐
      │             │            │              │
[ChatWithTools] [History]  [Prompts]    [MCP Factory]
      │             │                           │
      ↓             ↓                           ↓
OpenRouter API  PostgreSQL         ┌───────────┼───────────┐
                (memory_entries)   │           │           │
                                   ↓           ↓           ↓
                              [MCP Server] [RAG MCP] [Docker MCP]
                              (port 8081)  (port 8086) (port 8083)
```

### Key Components

1. **ChatWithToolsController** (`backend/openrouter-service/src/main/java/.../controller/ChatWithToolsController.java`)
   - Purpose: REST API for LLM chat with automatic tool execution
   - Endpoints: `/tools/chat`, `/tools/available`, `/tools/servers`
   - Dependencies: ChatWithToolsService, MCPFactory

2. **ChatWithToolsService** (`backend/openrouter-service/src/main/java/.../service/ChatWithToolsService.java`)
   - Purpose: Orchestrates tool execution loop and context detection
   - Methods: `sendMessageWithTools()`, `detectContext()`, `executeToolCalls()`
   - Dependencies: OpenRouter API Client, MCPFactory, ConversationHistoryService

3. **MCPFactory** (`backend/openrouter-service/src/main/java/.../mcp/MCPFactory.java`)
   - Purpose: Routes tool calls to appropriate MCP service
   - Pattern: Factory Pattern for MCP service selection
   - Supported: GoogleMcp, RagMcp, DockerMonitorMcp

4. **ConversationHistoryService** (`backend/openrouter-service/src/main/java/.../service/ConversationHistoryService.java`)
   - Purpose: Two-level caching (L1: RAM, L2: PostgreSQL)
   - Methods: `addMessage()`, `getHistory()`, `clearHistory()`
   - Dependencies: MemoryRepository (JPA)

## 💻 Complete Code Examples

### Example 1: Simple Chat Request

```bash
# File: N/A (curl command)
# Simple chat without tools
curl -X POST "http://localhost:8084/api/v1/openrouter/chat/simple" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Explain microservices architecture"
  }'
```

**Response:**
```json
{
  "response": "Microservices architecture is a design pattern where...",
  "model": "anthropic/claude-3.5-sonnet",
  "tokensUsed": 245
}
```

**Explanation:**
- Direct LLM call without tool execution
- Uses default model from configuration
- No conversation history

### Example 2: Chat with Automatic Tool Execution

```bash
# File: N/A (curl command)
# Chat that triggers RAG tool automatically
curl -X POST "http://localhost:8084/api/v1/openrouter/tools/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Search for microservices in my documents",
    "conversationId": "conv-123",
    "temperature": 0.7,
    "maxTokens": 1500
  }'
```

**Response:**
```json
{
  "answer": "Based on your documents, I found 5 references to microservices...",
  "conversationId": "conv-123",
  "sources": [
    {
      "documentId": "doc-456",
      "snippet": "Microservices pattern enables...",
      "relevance": 0.95
    }
  ],
  "toolsUsed": ["rag:search_documents"],
  "iterations": 2
}
```

**Explanation:**
- LLM automatically detects need for document search
- Tool execution loop:
  1. LLM requests `rag:search_documents` tool
  2. MCPFactory routes to RagMcpService
  3. Results fed back to LLM
  4. LLM generates final answer with sources
- History saved to PostgreSQL

### Example 3: Tool Execution Loop Implementation

```java
// File: backend/openrouter-service/src/main/java/.../service/ChatWithToolsService.java
@Service
@Slf4j
public class ChatWithToolsService {
    
    private static final int MAX_ITERATIONS = 10;
    
    public ChatResponse sendMessageWithTools(ChatRequest request) {
        String conversationId = request.getConversationId();
        List<Message> messages = buildMessages(request);
        
        int iteration = 0;
        List<String> toolsUsed = new ArrayList<>();
        
        // Tool execution loop
        while (iteration < MAX_ITERATIONS) {
            iteration++;
            log.debug("Tool loop iteration {}/{}", iteration, MAX_ITERATIONS);
            
            // 1. Call OpenRouter API
            String jsonResponse = openRouterClient.chat(messages, toolDefinitions);
            ToolResponse toolResponse = parseToolResponse(jsonResponse);
            
            // 2. Check if final answer
            if ("final".equals(toolResponse.getStep())) {
                // Save to history and return
                historyService.addMessage(conversationId, 
                    new Message("assistant", toolResponse.getAnswer()));
                
                return ChatResponse.builder()
                    .answer(toolResponse.getAnswer())
                    .conversationId(conversationId)
                    .sources(extractSources(messages))
                    .toolsUsed(toolsUsed)
                    .iterations(iteration)
                    .build();
            }
            
            // 3. Execute tools if requested
            if ("tool".equals(toolResponse.getStep())) {
                List<ToolCall> toolCalls = toolResponse.getToolCalls();
                
                for (ToolCall toolCall : toolCalls) {
                    log.info("Executing tool: {}", toolCall.getName());
                    
                    // Execute via MCPFactory
                    Object result = mcpFactory.executeTool(
                        toolCall.getName(), 
                        toolCall.getArguments()
                    );
                    
                    // Add result as user message
                    messages.add(new Message("user", 
                        "Tool result: " + toJson(result)));
                    
                    toolsUsed.add(toolCall.getName());
                }
            }
        }
        
        throw new TooManyIterationsException(
            "Max iterations (" + MAX_ITERATIONS + ") exceeded"
        );
    }
}
```

**Explanation:**
- **Iteration Limit**: Prevents infinite loops (max 10)
- **Step Detection**: "tool" = execute tools, "final" = return answer
- **MCPFactory**: Delegates tool execution to appropriate service
- **History Management**: Saves conversation to PostgreSQL
- **Source Extraction**: Parses RAG sources from messages

## 📂 File Structure

```
backend/openrouter-service/
├── src/main/java/de/jivz/openrouter/
│   ├── OpenRouterServiceApplication.java          # Spring Boot main
│   ├── controller/
│   │   ├── ChatWithToolsController.java           # Tools API endpoint
│   │   └── OpenRouterChatController.java          # Basic chat API
│   ├── service/
│   │   ├── ChatWithToolsService.java              # Tool execution loop
│   │   ├── OpenRouterAiChatService.java           # Simple chat
│   │   ├── ConversationHistoryService.java        # History management
│   │   ├── HistoryPersistenceService.java         # PostgreSQL persistence
│   │   └── PromptLoaderService.java               # System prompts
│   ├── mcp/
│   │   ├── MCPFactory.java                        # Tool router
│   │   ├── GoogleMcpService.java                  # Google Tasks integration
│   │   ├── RagMcpService.java                     # RAG search integration
│   │   └── DockerMonitorMcpService.java           # Docker monitoring
│   ├── model/
│   │   ├── ChatRequest.java                       # Request DTO
│   │   ├── ChatResponse.java                      # Response DTO
│   │   ├── Message.java                           # Chat message
│   │   ├── ToolResponse.java                      # Tool execution response
│   │   └── MemoryEntry.java                       # JPA entity for history
│   └── repository/
│       └── MemoryRepository.java                  # JPA repository
└── src/main/resources/
    ├── application.properties                      # Main configuration
    └── prompts/
        ├── developer_assistant_prompt.md           # System prompts
        └── meta_prompting.md                       # Meta-prompt templates
```


## 🔌 API Reference

### POST /api/v1/openrouter/tools/chat
Chat with automatic MCP tool execution.

**Request:**
```bash
curl -X POST "http://localhost:8084/api/v1/openrouter/tools/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Search for microservices in my documents",
    "conversationId": "conv-123",
    "temperature": 0.7,
    "maxTokens": 1500,
    "model": "anthropic/claude-3.5-sonnet"
  }'
```

**Response:**
```json
{
  "answer": "Based on your documents, I found 5 references...",
  "conversationId": "conv-123",
  "sources": [
    {
      "documentId": "doc-456",
      "snippet": "Microservices pattern...",
      "relevance": 0.95
    }
  ],
  "toolsUsed": ["rag:search_documents"],
  "iterations": 2
}
```

**Parameters:**
- `message` (required): User message
- `conversationId` (optional): Conversation identifier for history
- `temperature` (optional): 0.0-1.0, default 0.7
- `maxTokens` (optional): Max response tokens, default 1000
- `model` (optional): Model name, default from config

**Status Codes:**
- 200: Success
- 400: Invalid request
- 500: Server error or tool execution failure

### GET /api/v1/openrouter/tools/available
List all available MCP tools.

**Request:**
```bash
curl "http://localhost:8084/api/v1/openrouter/tools/available"
```

**Response:**
```json
{
  "tools": [
    {
      "name": "rag:search_documents",
      "description": "Search in indexed documents",
      "service": "rag-mcp"
    },
    {
      "name": "google:tasks_list",
      "description": "List Google Tasks",
      "service": "mcp-server"
    }
  ]
}
```

### POST /api/v1/openrouter/chat/simple
Simple chat without tools.

**Request:**
```bash
curl -X POST "http://localhost:8084/api/v1/openrouter/chat/simple" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Explain microservices"
  }'
```

**Response:**
```json
{
  "response": "Microservices architecture is...",
  "model": "anthropic/claude-3.5-sonnet",
  "tokensUsed": 245
}
```

### GET /api/v1/openrouter/chat/history/{conversationId}
Retrieve conversation history.

**Request:**
```bash
curl "http://localhost:8084/api/v1/openrouter/chat/history/conv-123"
```

**Response:**
```json
{
  "conversationId": "conv-123",
  "messages": [
    {
      "role": "user",
      "content": "Search for microservices",
      "timestamp": "2026-01-13T10:30:00Z"
    },
    {
      "role": "assistant",
      "content": "I found 5 references...",
      "timestamp": "2026-01-13T10:30:05Z"
    }
  ],
  "messageCount": 2
}
```

### DELETE /api/v1/openrouter/chat/history/{conversationId}
Delete conversation history.

**Request:**
```bash
curl -X DELETE "http://localhost:8084/api/v1/openrouter/chat/history/conv-123"
```

**Response:**
```json
{
  "success": true,
  "message": "History deleted for conversation: conv-123"
}
```

## ⚙️ Configuration

### Required Properties

File: `backend/openrouter-service/src/main/resources/application.properties`

```properties
# Server
server.port=8084

# OpenRouter API (REQUIRED)
spring.ai.openrouter.api-key=${OPENROUTER_API_KEY}
spring.ai.openrouter.base-url=https://openrouter.ai/api/v1

# Default Model
spring.ai.openrouter.default-model=anthropic/claude-3.5-sonnet

# PostgreSQL (REQUIRED for history persistence)
spring.datasource.url=jdbc:postgresql://localhost:5432/ai_challenge_db
spring.datasource.username=local_user
spring.datasource.password=local_password
spring.jpa.hibernate.ddl-auto=update
```

### Optional Properties

```properties
# Model parameters
spring.ai.openrouter.default-temperature=0.7
spring.ai.openrouter.default-max-tokens=1000

# MCP Services (URLs for tool integration)
mcp.google.base-url=http://localhost:8081
mcp.rag.base-url=http://localhost:8086
mcp.docker.monitor.base-url=http://localhost:8083

# Logging
logging.level.de.jivz.openrouter=DEBUG
logging.level.de.jivz.openrouter.service.ChatWithToolsService=DEBUG

# Swagger UI
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```

### Environment Variables

```bash
# Required
export OPENROUTER_API_KEY='sk-or-v1-xxx...'

# Optional - Database
export DATABASE_URL='jdbc:postgresql://localhost:5432/ai_challenge_db'
export DATABASE_USER='local_user'
export DATABASE_PASSWORD='local_password'

# Optional - MCP Services
export MCP_SERVER_URL='http://localhost:8081'
export RAG_MCP_URL='http://localhost:8086'
```

### Supported Models

OpenRouter supports 100+ models. Popular choices:

```properties
# Anthropic
anthropic/claude-3.5-sonnet          # Best for coding (default)
anthropic/claude-3-opus              # Most capable

# OpenAI
openai/gpt-4-turbo                   # Fast and capable
openai/gpt-4o                        # Latest GPT-4

# Meta
meta-llama/llama-3.1-405b-instruct   # Open source, powerful

# Google
google/gemini-pro-1.5                # Long context window

# Mistral
mistralai/mistral-large              # European alternative
```

Full list: https://openrouter.ai/models

## 🚀 Quick Start Guide

### Step 1: Set Up Environment

```bash
# Clone repository (if not already done)
cd backend/openrouter-service

# Set API key
export OPENROUTER_API_KEY='sk-or-v1-your-key-here'

# Verify API key is set
echo $OPENROUTER_API_KEY
```

### Step 2: Configure PostgreSQL

```bash
# Start PostgreSQL (Docker)
docker run -d \
  --name postgres-ai \
  -e POSTGRES_USER=local_user \
  -e POSTGRES_PASSWORD=local_password \
  -e POSTGRES_DB=ai_challenge_db \
  -p 5432:5432 \
  postgres:15

# Verify database is running
psql -h localhost -U local_user -d ai_challenge_db -c "\dt"
```

### Step 3: Build and Run

```bash
# Build the service
mvn clean install -DskipTests

# Run the service
mvn spring-boot:run

# Expected output:
# Started OpenRouterServiceApplication in X.XXX seconds
# Server listening on port 8084
```

### Step 4: Verify Installation

```bash
# Check health
curl http://localhost:8084/actuator/health

# Test simple chat
curl -X POST "http://localhost:8084/api/v1/openrouter/chat/simple" \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello, how are you?"}' | jq

# List available tools
curl http://localhost:8084/api/v1/openrouter/tools/available | jq

# Open Swagger UI
open http://localhost:8084/swagger-ui.html
```

### Step 5: Test Tool Execution

```bash
# Test RAG search (requires RAG MCP service running)
curl -X POST "http://localhost:8084/api/v1/openrouter/tools/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Search for Spring Boot in my documents",
    "conversationId": "test-123"
  }' | jq
```

## 🧪 Testing

### Manual Testing

```bash
#!/bin/bash
# File: test-openrouter-service.sh

BASE_URL="http://localhost:8084/api/v1/openrouter"

echo "=== OpenRouter Service Test Suite ==="

# Test 1: Simple chat
echo "1. Testing simple chat..."
curl -s -X POST "$BASE_URL/chat/simple" \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello"}' | jq

# Test 2: Chat with tools
echo "2. Testing chat with tools..."
curl -s -X POST "$BASE_URL/tools/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message":"What is the weather?",
    "conversationId":"test-conv-1"
  }' | jq

# Test 3: Get history
echo "3. Testing history retrieval..."
curl -s "$BASE_URL/chat/history/test-conv-1" | jq

# Test 4: List tools
echo "4. Listing available tools..."
curl -s "$BASE_URL/tools/available" | jq

# Test 5: Delete history
echo "5. Testing history deletion..."
curl -s -X DELETE "$BASE_URL/chat/history/test-conv-1" | jq

echo "=== Tests Completed ==="
```

### Integration Tests

```java
// File: backend/openrouter-service/src/test/java/.../ChatWithToolsServiceTest.java
@SpringBootTest
class ChatWithToolsServiceTest {
    
    @Autowired
    private ChatWithToolsService chatService;
    
    @Test
    void testSimpleChatWithoutTools() {
        // Given
        ChatRequest request = ChatRequest.builder()
            .message("Hello, how are you?")
            .conversationId("test-1")
            .build();
        
        // When
        ChatResponse response = chatService.sendMessageWithTools(request);
        
        // Then
        assertThat(response.getAnswer()).isNotEmpty();
        assertThat(response.getToolsUsed()).isEmpty();
        assertThat(response.getIterations()).isEqualTo(1);
    }
    
    @Test
    void testChatWithToolExecution() {
        // Given
        ChatRequest request = ChatRequest.builder()
            .message("Search for microservices in documents")
            .conversationId("test-2")
            .build();
        
        // When
        ChatResponse response = chatService.sendMessageWithTools(request);
        
        // Then
        assertThat(response.getAnswer()).isNotEmpty();
        assertThat(response.getToolsUsed()).contains("rag:search_documents");
        assertThat(response.getIterations()).isGreaterThan(1);
    }
}
```

## 🔧 Troubleshooting

### Problem: Service doesn't start

**Symptom:**
```
Error creating bean with name 'openRouterClient'
```

**Solution:**
```bash
# Check API key is set
echo $OPENROUTER_API_KEY

# If empty, set it
export OPENROUTER_API_KEY='sk-or-v1-your-key-here'

# Restart service
mvn spring-boot:run
```

### Problem: Database connection failed

**Symptom:**
```
Connection to localhost:5432 refused
```

**Solution:**
```bash
# Check PostgreSQL is running
docker ps | grep postgres

# If not running, start it
docker start postgres-ai

# Or run new container
docker run -d --name postgres-ai \
  -e POSTGRES_USER=local_user \
  -e POSTGRES_PASSWORD=local_password \
  -e POSTGRES_DB=ai_challenge_db \
  -p 5432:5432 postgres:15
```

### Problem: Tool execution fails

**Symptom:**
```json
{
  "error": "Failed to execute tool: rag:search_documents"
}
```

**Solution:**
1. Check MCP services are running:
```bash
# Check MCP server
curl http://localhost:8081/api/status

# Check RAG MCP
curl http://localhost:8086/api/status
```

2. Verify MCP URLs in configuration:
```properties
mcp.rag.base-url=http://localhost:8086
```

### Problem: History not persisting

**Symptom:**
History is lost after restart.

**Solution:**
1. Check PostgreSQL connection
2. Verify `spring.jpa.hibernate.ddl-auto=update`
3. Check table exists:
```sql
SELECT * FROM memory_entries LIMIT 5;
```

## 💡 Best Practices

### 1. Conversation Management

✅ **DO:**
- Use meaningful conversation IDs
- Clear old conversations periodically
- Implement conversation timeouts

❌ **DON'T:**
- Don't reuse conversation IDs for different topics
- Don't store sensitive data in conversation history
- Don't create infinite conversations

### 2. Model Selection

✅ **DO:**
- Use Claude 3.5 Sonnet for coding tasks
- Use GPT-4o for general tasks
- Use Llama for cost-sensitive applications

❌ **DON'T:**
- Don't use expensive models for simple tasks
- Don't hardcode model names in client code
- Don't ignore model context limits

### 3. Tool Execution

✅ **DO:**
- Implement timeouts for tool calls
- Log all tool executions
- Handle tool failures gracefully

❌ **DON'T:**
- Don't execute untrusted tools
- Don't ignore tool execution errors
- Don't create circular tool dependencies

## 📚 Related Documentation

- **[OpenRouter Service Architecture](../architecture/OPENROUTER_SERVICE_ARCHITECTURE.md)** - Detailed architecture
- **[MCP Multi-Provider Architecture](../architecture/MCP_MULTI_PROVIDER_ARCHITECTURE.md)** - MCP integration
- **[RAG MCP Integration](../architecture/RAG_MCP_INTEGRATION.md)** - RAG service details

## 🎓 Summary

OpenRouter Provider Feature provides:

✅ **Multi-Model Access**: 100+ models via single API  
✅ **Automatic Tool Execution**: LLM-driven tool selection  
✅ **Persistent History**: Two-level caching (RAM + PostgreSQL)  
✅ **Production Ready**: Error handling, retry logic, logging  
✅ **Developer Friendly**: Swagger UI, comprehensive docs  
✅ **Extensible**: Easy to add new MCP services  

**Quick Reference:**
- Port: **8084**
- Swagger UI: **http://localhost:8084/swagger-ui.html**
- Default Model: **anthropic/claude-3.5-sonnet**
- Max Tool Iterations: **10**
   psql -h localhost -U local_user -d ai_challenge_db
   ```

3. Проверить логи:
   ```bash
   tail -f logs/openrouter-service.log
   ```

### Инструменты не работают

1. Проверить доступность MCP-сервисов:
   ```bash
   curl http://localhost:8086/health  # RAG
   curl http://localhost:8081/health  # Google
   curl http://localhost:8083/health  # Docker
   ```

2. Проверить список зарегистрированных серверов:
   ```bash
   curl "http://localhost:8084/api/v1/openrouter/tools/servers"
   ```

---

## Связанная документация

### Детальная архитектура
- 📖 [OpenRouter Service Architecture](../architecture/OPENROUTER_SERVICE_ARCHITECTURE.md) - **ОСНОВНАЯ ДОКУМЕНТАЦИЯ**

### Другие компоненты
- [MCP Multi-Provider Architecture](../architecture/MCP_MULTI_PROVIDER_ARCHITECTURE.md)
- [Conversation History Implementation](../architecture/CONVERSATION_HISTORY_IMPLEMENTATION.md)
- [RAG MCP Integration](../architecture/RAG_MCP_INTEGRATION.md)

### Setup Guides
- [Chatbot Deployment Guide](../setup/CHATBOT_DEPLOYMENT_GUIDE.md)
- [PostgreSQL Memory Setup](../setup/POSTGRESQL_MEMORY_SETUP.md)

---

**Версия**: 1.0.0  
**Дата обновления**: 2026-01-12  
**Статус**: ✅ Production Ready

1. Dropdown oder Radio-Buttons für Provider-Auswahl hinzufügen
2. `provider`-Feld im Request setzen
3. Optional: Provider-Icon im Chat anzeigen

### Erweiterte Features

- [ ] Provider-spezifische Features im UI anzeigen
- [ ] Model-Auswahl im Frontend
- [ ] Top-P Parameter hinzufügen
- [ ] Tool Calls Integration
- [ ] Streaming Support

## Fehlerbehandlung

Die Implementierung enthält umfassende Fehlerbehandlung:

- ✅ 4xx Client Errors
- ✅ 5xx Server Errors
- ✅ Leere Response-Validierung
- ✅ Ausführliche Logging
- ✅ ExternalServiceException für API-Fehler

## Logging

Das System loggt wichtige Informationen:

```
✅ OpenRouterToolClient initialized with WebClient and model: anthropic/claude-3.5-sonnet
🚀 Calling OpenRouter API with model: anthropic/claude-3.5-sonnet, temperature: 0.7 and 2 messages
💬 Reply preview: Hello! How can I help you today?...
💰 Usage: 35 tokens
```

## Zusammenfassung

Der OpenRouter Provider ist vollständig implementiert und einsatzbereit. Er funktioniert parallel zum Perplexity Provider und kann über das `provider`-Feld im ChatRequest ausgewählt werden. Alle bestehenden Features (JSON Mode, Temperature Control, Conversation History, etc.) werden unterstützt.

