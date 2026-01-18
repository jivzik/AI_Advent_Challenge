# OpenRouter Service - Архитектура и функциональность

## 📋 Общее описание

**OpenRouter Service** — это микросервис для интеграции с OpenRouter API, предоставляющий унифицированный доступ к различным LLM-моделям с поддержкой MCP (Model Context Protocol) инструментов, управления историей диалогов и персистентного хранения в PostgreSQL.

### Основное назначение
- Единая точка входа для взаимодействия с различными LLM через OpenRouter API
- Автоматическое управление инструментами (MCP Tools) для расширения возможностей LLM
- Интеллектуальное управление контекстом и историей диалогов
- Персистентное хранение диалогов в PostgreSQL

### Технологический стек
- **Фреймворк**: Spring Boot 3.x
- **Язык**: Java 17
- **База данных**: PostgreSQL (JPA/Hibernate)
- **HTTP клиент**: Spring WebFlux (WebClient)
- **Документация API**: SpringDoc OpenAPI 3 (Swagger UI)
- **Сериализация**: Jackson (JSON)
- **Утилиты**: Lombok

---

## 🏗 Архитектура системы

### 1. Архитектурные слои

```
┌─────────────────────────────────────────────────────────────┐
│                     REST API Layer                          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  ChatWithToolsController   │ OpenRouterChatController│  │
│  │  /api/v1/openrouter/tools  │ /api/v1/openrouter/chat │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    Service Layer                            │
│  ┌──────────────────┐  ┌──────────────────────────────┐    │
│  │ ChatWithTools    │  │ OpenRouterAiChatService      │    │
│  │ Service          │  │ (Simple chat)                │    │
│  └──────────────────┘  └──────────────────────────────┘    │
│                                                              │
│  ┌──────────────────┐  ┌──────────────────────────────┐    │
│  │ Conversation     │  │ HistoryPersistence           │    │
│  │ HistoryService   │  │ Service                      │    │
│  └──────────────────┘  └──────────────────────────────┘    │
│                                                              │
│  ┌──────────────────┐                                       │
│  │ PromptLoader     │  (System prompts & context)          │
│  │ Service          │                                       │
│  └──────────────────┘                                       │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    MCP Integration Layer                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │             MCPFactory (Tool Router)                  │  │
│  └──────────────────────────────────────────────────────┘  │
│                            ↓                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌──────────────────┐   │
│  │ GoogleMCP   │  │ RagMcp      │  │ DockerMonitorMcp │   │
│  │ Service     │  │ Service     │  │ Service          │   │
│  └─────────────┘  └─────────────┘  └──────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                  External Integration Layer                 │
│  ┌──────────────────┐  ┌──────────────────────────────┐    │
│  │ OpenRouter API   │  │ MCP Services                 │    │
│  │ (WebClient)      │  │ (REST clients)               │    │
│  └──────────────────┘  └──────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   Persistence Layer                         │
│  ┌──────────────────┐  ┌──────────────────────────────┐    │
│  │ MemoryRepository │  │ MemoryEntry (JPA Entity)     │    │
│  │ (JPA)            │  │ (memory_entries table)       │    │
│  └──────────────────┘  └──────────────────────────────┘    │
│                     PostgreSQL Database                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔑 Ключевые компоненты

### 1. Controllers (REST API)

#### 1.1 ChatWithToolsController
**Путь**: `/api/v1/openrouter/tools`

**Назначение**: Обработка запросов с автоматическим использованием MCP-инструментов

**Endpoints**:
- `POST /chat` - Полноценный чат с поддержкой инструментов
- `POST /chat/simple` - Упрощенный чат (только message в параметрах)
- `GET /available` - Список доступных MCP-инструментов
- `GET /servers` - Список зарегистрированных MCP-серверов

**Особенности**:
- Автоматическое определение необходимости вызова инструментов
- Циклическое выполнение (tool execution loop)
- Интеграция с историей диалогов

#### 1.2 OpenRouterChatController
**Путь**: `/api/v1/openrouter/chat`

**Назначение**: Базовые операции чата и управление историей диалогов

**Endpoints**:
- `POST /simple` - Простой чат без истории
- `POST /full` - Чат с полным контролем параметров и сохранением истории
- `GET /history/{conversationId}` - Получение истории диалога
- `DELETE /history/{conversationId}` - Удаление истории диалога
- `GET /conversations` - Список всех диалогов
- `GET /conversations/{conversationId}/summary` - Сводка по диалогу

**Особенности**:
- Управление conversation history
- Поддержка различных моделей и параметров (temperature, maxTokens)
- Персистентность в PostgreSQL

---

### 2. Service Layer

#### 2.1 ChatWithToolsService
**Ключевой сервис для обработки запросов с инструментами**

**Основной workflow (Tool Execution Loop)**:
```
1. Построить messages (system + history + user)
   ↓
2. Отправить в OpenRouter API
   ↓
3. Получить JSON response: {step, tool_calls, answer}
   ↓
4. Анализ step:
   ├─ step="final" → Вернуть answer
   └─ step="tool"  → Выполнить tool_calls
      ↓
      Вызвать MCP tools через MCPFactory
      ↓
      Добавить результаты как user message
      ↓
      Повторить с шага 2 (max 10 итераций)
```

**Ключевые методы**:
- `chatWithTools(ChatRequest)` - Главная точка входа
- `executeToolLoop()` - Основной цикл выполнения инструментов
- `buildMessages()` - Построение контекста с историей
- `detectContextViaLlm()` - Интеллектуальное определение контекста через LLM
- `executeMcpTool()` - Выполнение MCP-инструментов

**Фичи**:
- ✅ Автоматическое определение контекста (docker, tasks, calendar, default)
- ✅ Извлечение источников из RAG results
- ✅ Retry при ошибках парсинга JSON
- ✅ Поддержка до 10 итераций tool-loop
- ✅ Красивое форматирование источников в ответе

#### 2.2 OpenRouterAiChatService
**Простой WebClient-based сервис для базовых чатов**

**Особенности**:
- Прямое взаимодействие с OpenRouter API
- Без tool execution loop
- Быстрые ответы для простых запросов
- Логирование token usage

#### 2.3 ConversationHistoryService
**Гибридное управление историей диалогов (L1 + L2 кеш)**

**Архитектура кеширования**:
```
┌──────────────────────────────────────┐
│   L1 Cache (ConcurrentHashMap)      │
│   RAM - быстрый доступ              │
└──────────────────────────────────────┘
              ↓ (cache miss)
┌──────────────────────────────────────┐
│   L2 Cache (PostgreSQL)              │
│   Персистентное хранилище            │
└──────────────────────────────────────┘
```

**Ключевые методы**:
- `getHistory(conversationId)` - Загрузка с двухуровневым кешем
- `addMessage(...)` - Добавление в RAM + DB
- `saveMessages(...)` - Batch операция
- `clearHistory(...)` - Очистка RAM + DB

**Principles**:
- Single Responsibility: только управление историей
- Strategy Pattern: легко заменить реализацию L2 кеша
- Dependency Injection: HistoryPersistenceService

#### 2.4 HistoryPersistenceService
**Абстракция для работы с БД**

**Функциональность**:
- Загрузка полной истории
- Загрузка только оригинальных сообщений (не compressed)
- Сохранение одного или множества сообщений
- Удаление истории
- Получение статистики

**Strategy Pattern**: Может быть реализован для разных хранилищ (PostgreSQL, MongoDB, Redis)

#### 2.5 PromptLoaderService
**Управление системными промптами и контекстом**

**Функциональность**:
- Загрузка промптов из ресурсов
- Генерация system prompts с описанием MCP tools
- Контекстно-зависимые промпты
- JSON correction prompts
- Context detection prompts

---

### 3. MCP Integration Layer

#### 3.1 MCPFactory (Tool Router)
**Централизованная фабрика для маршрутизации вызовов MCP-инструментов**

**Архитектура**:
```java
Map<String, MCPService> serviceMap
├─ "google" → GoogleMCPService
├─ "rag" → RagMcpService
└─ "docker" → DockerMonitorMcpService
```

**Методы**:
- `route(fullToolName, params)` - Маршрутизация к нужному MCP-сервису
  - Формат: `"server:tool"` (например, `"google:tasks_list"`)
- `getAllToolDefinitions()` - Получение всех доступных инструментов
- `getRegisteredServers()` - Список зарегистрированных серверов

**Пример использования**:
```java
MCPToolResult result = mcpFactory.route(
    "rag:search_documents", 
    Map.of("query", "Spring Boot")
);
```

#### 3.2 MCP Services (Implementations)
**Интерфейс**: `MCPService`

**Реализации**:
1. **GoogleMCPService** - Google Tasks, Calendar, Gmail
2. **RagMcpService** - Поиск документов (RAG)
3. **DockerMonitorMcpService** - Мониторинг Docker контейнеров

**Базовый класс**: `BaseMCPService` - общая логика WebClient вызовов

---

### 4. Persistence Layer

#### 4.1 MemoryEntry (JPA Entity)
**Таблица**: `memory_entries`

**Поля**:
- `id` (BIGINT, PK, AUTO_INCREMENT)
- `conversation_id` (VARCHAR, indexed) - ID диалога
- `user_id` (VARCHAR, nullable) - ID пользователя
- `role` (VARCHAR) - роль: user/assistant/system
- `content` (TEXT) - содержимое сообщения
- `timestamp` (TIMESTAMP) - время создания
- `model` (VARCHAR, nullable) - использованная модель
- `is_compressed` (BOOLEAN, default false) - флаг сжатия
- `prompt_tokens`, `completion_tokens`, `total_tokens` (INTEGER, nullable)
- `estimated_cost` (DECIMAL, nullable)
- `response_time_ms` (BIGINT, nullable)

**Индексы**:
- `idx_conversation_id` - быстрый поиск по conversationId
- `idx_user_id` - поиск по пользователю
- `idx_timestamp` - сортировка по времени
- `idx_conversation_timestamp` - комбинированный индекс

#### 4.2 MemoryRepository (JPA Repository)
**Интерфейс**: `JpaRepository<MemoryEntry, Long>`

**Custom queries**:
- `findByConversationIdOrderByTimestampAsc()`
- `findByConversationIdAndIsCompressedFalseOrderByTimestampAsc()`
- `findAllConversationIds()` (custom @Query)
- `deleteByConversationId()`

---

## 🔄 Основные потоки данных

### 1. Простой чат без инструментов

```
User Request
    ↓
ChatController.simpleChat()
    ↓
OpenRouterAiChatService.chat()
    ↓
OpenRouter API (WebClient)
    ↓
ChatResponse → User
```

### 2. Чат с инструментами (Tool Execution Loop)

```
User Request
    ↓
ChatWithToolsController.chatWithTools()
    ↓
ChatWithToolsService.chatWithTools()
    ↓
┌─────────────────────────────────────┐
│  Tool Execution Loop (max 10 iter) │
│  ┌───────────────────────────────┐ │
│  │ 1. Build messages with        │ │
│  │    history + context          │ │
│  │         ↓                     │ │
│  │ 2. Call OpenRouter API        │ │
│  │         ↓                     │ │
│  │ 3. Parse JSON response        │ │
│  │    {step, tool_calls, answer} │ │
│  │         ↓                     │ │
│  │ 4. IF step == "tool":         │ │
│  │    Execute via MCPFactory     │ │
│  │    Add results → repeat       │ │
│  │         ↓                     │ │
│  │ 5. IF step == "final":        │ │
│  │    Return answer → exit       │ │
│  └───────────────────────────────┘ │
└─────────────────────────────────────┘
    ↓
Save to History (HistoryPersistenceService)
    ↓
ChatResponse → User
```

### 3. Управление историей диалогов

#### Чтение истории:
```
getHistory(conversationId)
    ↓
Check L1 Cache (RAM)
    ├─ HIT → Return from RAM
    └─ MISS ↓
Check L2 Cache (PostgreSQL)
    ├─ HIT → Load + Cache in RAM → Return
    └─ MISS → Return empty
```

#### Запись истории:
```
addMessage(conversationId, role, content)
    ↓
Add to L1 Cache (RAM)
    ↓
HistoryPersistenceService.save()
    ↓
PostgreSQL (memory_entries table)
```

---

## 🔧 Конфигурация

### application.properties

```properties
# Server Configuration
server.port=8084
server.servlet.context-path=/

# OpenRouter API Configuration
spring.ai.openrouter.api-key=${OPENROUTER_API_KEY}
spring.ai.openrouter.base-url=https://openrouter.ai/api/v1
spring.ai.openrouter.default-model=anthropic/claude-3.5-sonnet
spring.ai.openrouter.default-temperature=0.7
spring.ai.openrouter.default-max-tokens=1000
spring.ai.openrouter.default-top-p=0.9

# MCP Server Configuration
mcp.google.enabled=${MCP_GOOGLE_ENABLED:false}
mcp.google.base-url=${MCP_GOOGLE_URL:http://localhost:8081}
mcp.perplexity.url=http://localhost:3001
mcp.docker.monitor.base-url=http://localhost:8083
mcp.rag.base-url=http://localhost:8086

# PostgreSQL Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/ai_challenge_db
spring.datasource.username=local_user
spring.datasource.password=local_password
spring.jpa.hibernate.ddl-auto=validate

# Swagger UI
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```

### Переменные окружения

```bash
export OPENROUTER_API_KEY='your-openrouter-api-key'
export MCP_GOOGLE_ENABLED=true
export MCP_GOOGLE_URL=http://localhost:8081
```

---

## 📊 DTO структуры

### ChatRequest
```java
{
  "message": "User message",
  "conversationId": "conv-uuid",
  "userId": "user-123",
  "model": "anthropic/claude-3.5-sonnet",
  "temperature": 0.7,
  "maxTokens": 1000
}
```

### ChatResponse
```java
{
  "reply": "Assistant response",
  "model": "anthropic/claude-3.5-sonnet",
  "responseTimeMs": 1234,
  "finishReason": "stop",
  "sources": ["doc1.pdf", "doc2.md"]  // если использовался RAG
}
```

### ToolResponse (JSON от OpenRouter)
```java
{
  "step": "tool" | "final",
  "tool_calls": [
    {
      "name": "rag:search_documents",
      "arguments": {
        "query": "Spring Boot",
        "topK": 5
      }
    }
  ],
  "answer": "Final answer text"
}
```

### MCPToolResult
```java
{
  "success": true,
  "result": { /* tool-specific data */ },
  "error": null,
  "toolName": "rag:search_documents",
  "timestamp": 1234567890
}
```

---

## 🚀 API Endpoints

### Chat with Tools

#### POST /api/v1/openrouter/tools/chat
Полноценный чат с автоматическим использованием инструментов

**Request**:
```json
{
  "message": "Search for information about microservices in my documents",
  "conversationId": "conv-123",
  "temperature": 0.7
}
```

**Response**:
```json
{
  "reply": "Based on the documents, microservices architecture...",
  "model": "anthropic/claude-3.5-sonnet",
  "responseTimeMs": 2345,
  "finishReason": "stop"
}
```

#### GET /api/v1/openrouter/tools/available
Список доступных MCP-инструментов

**Response**:
```json
[
  {
    "name": "rag:search_documents",
    "description": "Search documents in the knowledge base",
    "inputSchema": { /* JSON Schema */ }
  },
  {
    "name": "google:tasks_list",
    "description": "List Google Tasks",
    "inputSchema": { /* JSON Schema */ }
  }
]
```

### Basic Chat

#### POST /api/v1/openrouter/chat/simple
Простой чат без истории

**Request**: Query param `message=Hello`

**Response**: ChatResponse JSON

#### POST /api/v1/openrouter/chat/full
Чат с полным контролем параметров

**Request**: ChatRequest JSON (см. выше)

**Response**: ChatResponse JSON

### History Management

#### GET /api/v1/openrouter/chat/history/{conversationId}
Получить историю диалога

**Response**:
```json
{
  "conversationId": "conv-123",
  "messages": [
    {
      "role": "user",
      "content": "Hello",
      "timestamp": "2026-01-12T10:00:00"
    },
    {
      "role": "assistant",
      "content": "Hi! How can I help?",
      "timestamp": "2026-01-12T10:00:02"
    }
  ]
}
```

#### DELETE /api/v1/openrouter/chat/history/{conversationId}
Удалить историю диалога

**Response**: 204 No Content

#### GET /api/v1/openrouter/chat/conversations
Список всех диалогов

**Response**:
```json
{
  "conversations": [
    {
      "conversationId": "conv-123",
      "messageCount": 10,
      "lastMessageAt": "2026-01-12T10:30:00"
    }
  ]
}
```

---

## 🎯 Основные функции

### 1. ✅ Multi-model Support
- Поддержка всех моделей доступных через OpenRouter API
- Динамическое переключение между моделями
- Модели: Claude, GPT-4, Llama, Gemini и др.

### 2. ✅ MCP Tool Integration
- Автоматическое определение необходимости инструментов
- Параллельное выполнение нескольких инструментов
- Поддержка custom MCP-серверов через MCPFactory
- Интеграция с RAG, Google Services, Docker Monitor

### 3. ✅ Conversation History Management
- Двухуровневое кеширование (L1: RAM, L2: PostgreSQL)
- Поддержка multi-turn диалогов
- Персистентность между перезапусками
- Эффективные индексы для быстрого поиска

### 4. ✅ Context Intelligence
- Автоматическое определение контекста через LLM
- Контекстно-зависимые system prompts
- Оптимизация для разных сценариев (docker, tasks, calendar, default)

### 5. ✅ Source Attribution
- Автоматическое извлечение источников из RAG results
- Красивое форматирование списка источников
- Добавление ссылок на документы в ответе

### 6. ✅ Error Handling & Retry
- Автоматический retry при ошибках парсинга JSON
- JSON correction prompts
- Graceful degradation при недоступности MCP-сервисов

### 7. ✅ Metrics & Monitoring
- Логирование token usage
- Tracking response time
- Cost estimation (prompt_tokens, completion_tokens)
- Swagger UI для тестирования API

---

## 🔐 Безопасность

### API Keys
- OpenRouter API key через переменные окружения
- Не хранится в коде или git

### Database Security
- JPA с prepared statements (защита от SQL injection)
- Connection pooling
- Transaction management

### Input Validation
- Spring Validation для DTO
- Sanitization user inputs
- Max length для сообщений

---

## 📈 Performance Optimization

### Caching Strategy
1. **L1 Cache (RAM)**: ConcurrentHashMap для активных диалогов
2. **L2 Cache (PostgreSQL)**: Персистентное хранилище
3. **Индексы**: Оптимизированные для частых запросов

### Database Optimization
- Batch inserts для множества сообщений
- Индексы на conversation_id, timestamp
- Lazy loading для больших диалогов

### WebClient Configuration
- Connection pooling
- Timeout settings
- Reactive non-blocking I/O

---

## 🧪 Testing

### Ручное тестирование

**Swagger UI**: http://localhost:8084/swagger-ui.html

**curl примеры**:

```bash
# Простой чат
curl -X POST "http://localhost:8084/api/v1/openrouter/chat/simple?message=Hello" \
  -H "Content-Type: application/json"

# Чат с инструментами
curl -X POST "http://localhost:8084/api/v1/openrouter/tools/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Search for Spring Boot in my documents",
    "conversationId": "test-conv-1",
    "temperature": 0.7
  }'

# Получить историю
curl "http://localhost:8084/api/v1/openrouter/chat/history/test-conv-1"

# Список инструментов
curl "http://localhost:8084/api/v1/openrouter/tools/available"
```

---

## 🗂 Структура проекта

```
backend/openrouter-service/
├── src/main/java/de/jivz/ai_challenge/openrouterservice/
│   ├── OpenrouterServiceApplication.java
│   │
│   ├── config/
│   │   ├── OpenRouterProperties.java       # Конфигурация API
│   │   ├── OpenRouterWebClientConfig.java  # WebClient setup
│   │   ├── OpenApiConfig.java              # Swagger config
│   │   └── WebConfig.java                  # CORS, etc.
│   │
│   ├── controller/
│   │   ├── ChatWithToolsController.java    # Tool-based chat API
│   │   └── OpenRouterChatController.java   # Basic chat + history API
│   │
│   ├── service/
│   │   ├── ChatWithToolsService.java       # ⭐ Tool execution loop
│   │   ├── OpenRouterAiChatService.java    # Simple chat service
│   │   ├── ConversationHistoryService.java # ⭐ L1+L2 cache manager
│   │   ├── HistoryPersistenceService.java  # DB abstraction layer
│   │   └── PromptLoaderService.java        # System prompts
│   │
│   ├── mcp/
│   │   ├── MCPFactory.java                 # ⭐ Tool router
│   │   ├── MCPService.java                 # Interface
│   │   ├── BaseMCPService.java             # Base implementation
│   │   ├── GoogleMCPService.java
│   │   ├── RagMcpService.java
│   │   ├── DockerMonitorMcpService.java
│   │   └── model/
│   │       ├── ToolDefinition.java
│   │       └── MCPToolResult.java
│   │
│   ├── persistence/
│   │   ├── entity/
│   │   │   └── MemoryEntry.java            # JPA Entity
│   │   └── MemoryRepository.java           # JPA Repository
│   │
│   └── dto/
│       ├── ChatRequest.java
│       ├── ChatResponse.java
│       ├── Message.java
│       ├── ToolResponse.java               # JSON от OpenRouter
│       ├── OpenRouterApiRequest.java
│       └── OpenRouterApiResponse.java
│
├── src/main/resources/
│   ├── application.properties
│   └── prompts/                            # System prompt templates
│
└── pom.xml
```

---

## 🔄 Интеграции

### 1. OpenRouter API
- **URL**: https://openrouter.ai/api/v1
- **Auth**: Bearer token в заголовках
- **Models**: Все доступные модели через единый endpoint

### 2. MCP Services
- **Google Service** (8081): Tasks, Calendar, Gmail
- **RAG Service** (8086): Document search, indexing
- **Docker Monitor** (8083): Container stats, logs

### 3. PostgreSQL Database
- **Port**: 5432
- **Database**: ai_challenge_db
- **Table**: memory_entries

---

## 📚 Design Patterns

### 1. Factory Pattern (MCPFactory)
Централизованное создание и маршрутизация MCP-сервисов

### 2. Strategy Pattern (HistoryPersistenceService)
Абстракция хранилища позволяет легко заменить PostgreSQL на другую БД

### 3. Template Method (BaseMCPService)
Базовая реализация для всех MCP-сервисов

### 4. Dependency Injection (Spring)
Все зависимости внедряются через конструктор

### 5. Repository Pattern (MemoryRepository)
Абстракция доступа к данным через JPA

---

## 🎓 Best Practices

### 1. Single Responsibility Principle
- Каждый сервис имеет одну ответственность
- ChatWithToolsService - tool execution
- ConversationHistoryService - cache management
- HistoryPersistenceService - DB operations

### 2. Open/Closed Principle
- MCPFactory легко расширяется новыми MCP-сервисами
- Не требует изменения существующего кода

### 3. Dependency Inversion
- Все зависимости через интерфейсы (MCPService, JpaRepository)
- Легко тестировать и мокать

### 4. Logging
- Структурированное логирование с Slf4j + Lombok
- Разные уровни: DEBUG, INFO, WARN, ERROR
- Emoji для визуального выделения важных событий

### 5. Error Handling
- Try-catch с graceful degradation
- Retry механизмы для нестабильных сервисов
- Информативные сообщения об ошибках

---

## 🚀 Deployment

### Локальный запуск

```bash
# 1. Убедиться что PostgreSQL запущен
# 2. Установить переменные окружения
export OPENROUTER_API_KEY='your-key'

# 3. Запустить сервис
cd backend/openrouter-service
mvn spring-boot:run

# Сервис доступен на: http://localhost:8084
```

### Docker (будущее)
```bash
docker build -t openrouter-service .
docker run -p 8084:8084 \
  -e OPENROUTER_API_KEY='your-key' \
  openrouter-service
```

---

## 📖 Связанная документация

### Architecture
- [MCP Multi-Provider Architecture](./MCP_MULTI_PROVIDER_ARCHITECTURE.md)
- [Conversation History Implementation](./CONVERSATION_HISTORY_IMPLEMENTATION.md)
- [RAG MCP Integration](./RAG_MCP_INTEGRATION.md)

### Features
- [OpenRouter Provider Feature](../features/OPENROUTER_PROVIDER_FEATURE.md)
- [System Prompt Feature](../features/SYSTEM_PROMPT_FEATURE.md)
- [Temperature Feature](../features/TEMPERATURE_FEATURE.md)

### Setup
- [Chatbot Deployment Guide](../setup/CHATBOT_DEPLOYMENT_GUIDE.md)
- [PostgreSQL Memory Setup](../setup/POSTGRESQL_MEMORY_SETUP.md)

---

## 📝 Changelog

### v1.0.0 (2026-01-12)
- ✅ Полная интеграция с OpenRouter API
- ✅ MCP Tool execution loop
- ✅ Двухуровневое кеширование истории (L1+L2)
- ✅ Context intelligence через LLM
- ✅ Source attribution для RAG results
- ✅ Swagger UI documentation
- ✅ PostgreSQL persistence

---

## 🤝 Contributing

При добавлении нового MCP-сервиса:

1. Создать класс, реализующий `MCPService`
2. Унаследоваться от `BaseMCPService` для WebClient логики
3. Зарегистрировать как Spring `@Service`
4. MCPFactory автоматически подхватит через `@Autowired Optional<List<MCPService>>`

---

## 📧 Контакты

- **Проект**: AI Advent Challenge
- **Service**: OpenRouter Service
- **Port**: 8084
- **Swagger UI**: http://localhost:8084/swagger-ui.html
- **API Docs**: http://localhost:8084/api-docs

---

**Версия документации**: 1.0.0  
**Дата обновления**: 2026-01-12

