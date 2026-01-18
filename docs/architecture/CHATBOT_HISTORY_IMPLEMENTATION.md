# 🗂️ Чат-бот с Персистентной Историей Диалогов

## 📋 Обзор Имплементации

Реализован простой чат-бот с сохранением истории диалогов в PostgreSQL. Архитектура следует принципам **SOLID** и **Spring Pattern Strategy**.

---

## 🏗️ Архитектура

```
┌─────────────────────────────────────────────────────────────┐
│                     ChatWithToolsController                  │
│                (REST API + Tool Orchestration)               │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                   ChatWithToolsService                       │
│ - buildMessages() - загрузка истории + tools                │
│ - executeToolLoop() - вызов LLM и MCP tools                 │
│ - saveToHistory() - сохранение в БД                         │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│            ConversationHistoryService                        │
│           (Гибридный L1 RAM + L2 БД кеш)                    │
│ - getHistory() - загрузка из cache/БД                       │
│ - addMessage() - сохранение в оба уровня                    │
│ - saveMessages() - батч операция                            │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│          HistoryPersistenceService                           │
│        (Single Responsibility - только БД)                  │
│ - loadHistory() - загрузка из PostgreSQL                     │
│ - saveMessage() - сохранение одного сообщения               │
│ - saveMessages() - батч сохранение                          │
│ - deleteHistory() - удаление истории                        │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│               MemoryRepository (JPA)                         │
│          (Spring Data JPA Interface)                         │
│ - findByConversationIdOrderByTimestampAsc()                 │
│ - deleteByConversationId()                                  │
│ - countByConversationId()                                   │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              PostgreSQL Database                             │
│         (Таблица: memory_entries)                            │
│ - conversation_id | user_id | role | content                │
│ - timestamp | model | tokens | cost                         │
│ - is_compressed | response_time_ms                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 Компоненты

### 1. **MemoryEntry** (Entity)
- JPA сущность для хранения сообщений
- Поля: conversationId, userId, role, content, timestamp, model, токены, стоимость
- Индексы на: conversation_id, user_id, timestamp

### 2. **MemoryRepository** (Repository)
- Spring Data JPA интерфейс
- Методы: findByConversationId, deleteByConversationId, countByConversationId, getConversationStats

### 3. **HistoryPersistenceService** (Service Layer 2)
- Абстрактный слой для работы с БД
- Single Responsibility Principle
- Методы: loadHistory, saveMessage, saveMessages, deleteHistory

### 4. **ConversationHistoryService** (Service Layer 1)
- Управление двухуровневым кешем (L1 RAM + L2 PostgreSQL)
- Методы: getHistory, addMessage, saveMessages, clearHistory
- L1 кеш: ConcurrentHashMap для активных конверсаций
- L2 кеш: PostgreSQL для персистентности

### 5. **ChatWithToolsService** (Business Logic)
- Главный сервис чата
- Методы:
  - `chatWithTools(ChatRequest)` - основной метод
  - `buildMessages()` - загрузка истории и формирование контекста
  - `executeToolLoop()` - вызов LLM и MCP tools
  - `saveToHistory()` - сохранение в БД

### 6. **ChatWithToolsController** (REST API)
- Входная точка API
- Endpoints:
  - `POST /api/v1/openrouter/tools/chat` - основной чат
  - `POST /api/v1/openrouter/tools/chat/simple` - простой чат
  - `GET /api/v1/openrouter/tools/available` - список доступных tools

---

## 🗄️ База Данных

### Flyway Миграция (V1__init_memory_entries_schema.sql)

Таблица `memory_entries`:

```sql
CREATE TABLE memory_entries (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255),
    role VARCHAR(50) NOT NULL,           -- 'user', 'assistant', 'system'
    content TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    model VARCHAR(255),
    input_tokens INTEGER,
    output_tokens INTEGER,
    total_tokens INTEGER,
    cost DECIMAL(10, 6),
    is_compressed BOOLEAN DEFAULT FALSE,
    response_time_ms BIGINT,
    compressed_messages_count INTEGER,
    compression_timestamp TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Индексы
INDEX idx_memory_entries_conversation_id (conversation_id)
INDEX idx_memory_entries_user_id (user_id)
INDEX idx_memory_entries_timestamp (timestamp)
INDEX idx_memory_entries_conversation_timestamp (conversation_id, timestamp)
INDEX idx_memory_entries_conversation_is_compressed (conversation_id, is_compressed)
```

### Включение Flyway в application.properties

```properties
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baselineOnMigrate=true
spring.jpa.hibernate.ddl-auto=validate
```

---

## 🔄 Flow: Как Работает Чат

### 1️⃣ Пользователь Отправляет Сообщение

```
POST /api/v1/openrouter/tools/chat
{
  "message": "What are my tasks?",
  "conversationId": "conv-123",
  "temperature": 0.7
}
```

### 2️⃣ ChatWithToolsService Загружает Историю

```java
// buildMessages()
List<Message> history = historyService.getHistory(conversationId);
// ↓
// ConversationHistoryService.getHistory()
// Проверяет L1 кеш (ConcurrentHashMap)
// Если не найдено → загружает из БД через HistoryPersistenceService
// Кеширует в L1 для будущих запросов
```

### 3️⃣ LLM Обрабатывает Запрос с История

```
System Prompt: {tools + context}
Message 1 (user): "What are my tasks?" [from history]
Message 2 (assistant): "I'll check your tasks" [from history]
...
Current User Message: "What are my tasks?"
```

### 4️⃣ LLM Решает Вызвать Tool

```
LLM Response:
{
  "step": "tool",
  "tool_calls": [
    {"name": "google:list_tasks", "arguments": {...}}
  ]
}
```

### 5️⃣ ChatWithToolsService Вызывает MCP Tool

```java
// MCP Factory Router вызывает нужный инструмент
MCPToolResult result = mcpFactory.route("google:list_tasks", args);
```

### 6️⃣ LLM Обрабатывает Результат Tool

```
LLM получает результат и решает финальный ответ:
{
  "step": "final",
  "answer": "Your tasks are: ..."
}
```

### 7️⃣ Сохранение в История

```java
// saveToHistory()
historyService.addMessage(conversationId, "user", userMessage, null);
historyService.addMessage(conversationId, "assistant", finalAnswer, model);

// ↓ 
// ConversationHistoryService.addMessage()
// 1. Добавляет в L1 кеш (ConcurrentHashMap)
// 2. Сохраняет в L2 кеш (PostgreSQL через HistoryPersistenceService)
```

### 8️⃣ Response Отправляется Пользователю

```json
{
  "reply": "Your tasks are: ...",
  "model": "anthropic/claude-3.5-sonnet",
  "finishReason": "stop"
}
```

---

## 📊 Стратегия Двухуровневого Кеша

### Level 1: RAM (ConcurrentHashMap)
- **Назначение**: Быстрый доступ к активным конверсациям
- **Время жизни**: Пока приложение работает
- **Использование**: getHistory() сначала проверяет L1
- **Преимущества**: O(1) поиск, не требует БД запроса

### Level 2: PostgreSQL
- **Назначение**: Персистентное хранилище истории
- **Время жизни**: Неограничено
- **Использование**: Если нет в L1, загружается из L2
- **Преимущества**: Восстановление после перезагрузки, многопроцессная поддержка

### Алгоритм getHistory()

```
if conversationId is blank
  return empty list

if conversationId in L1 cache
  log "L1 cache HIT"
  return copy from L1

log "L1 cache MISS"
messages = load from PostgreSQL (L2)

if messages not empty
  cache in L1
  log "L2 cache HIT"
else
  log "L2 cache MISS"

return messages
```

---

## 💾 Сохранение Сообщений

### Одно Сообщение

```java
// Базовое сохранение (без метрик)
historyService.addMessage(conversationId, "user", "Hello", null);

// или через HistoryPersistenceService напрямую
persistenceService.saveMessage(conversationId, "user", "Hello", null);
```

### С Метриками (токены, время ответа)

```java
historyService.addMessageWithMetrics(
  conversationId, 
  "assistant", 
  "Hi there", 
  "gpt-3.5", 
  inputTokens,      // 50
  outputTokens,     // 25
  responseTimeMs    // 523
);
```

### Батч Операция

```java
List<Message> messages = List.of(
  new Message("user", "First question"),
  new Message("assistant", "First answer"),
  new Message("user", "Second question"),
  new Message("assistant", "Second answer")
);

historyService.saveMessages(conversationId, messages, model);
```

---

## 🔍 Примеры Использования

### Пример 1: Многотуровой Диалог

```
Запрос 1: User -> "Show my calendar for today"
  ↓ LLM вызывает Google Calendar tool
  ↓ Возвращает события дня
Response 1: "Your have 3 meetings today: ..."

Запрос 2: User -> "Add a new task for tomorrow"
  ↓ Контекст включает Запрос 1 и Response 1 из истории!
  ↓ LLM знает о событиях дня и добавляет задачу
Response 2: "Task added for tomorrow"
```

### Пример 2: Получение Статистики

```java
// Количество сообщений
long count = historyService.getMessageCount(conversationId);

// Статистика
Object[] stats = historyService.getConversationStats(conversationId);
// [totalTokens, totalCost, messageCount]

// Проверка существования
boolean exists = historyService.historyExists(conversationId);
```

### Пример 3: Удаление Истории

```java
// Очистить всю историю
historyService.clearHistory(conversationId);

// Или очистить только L1 кеш (данные остаются в БД)
historyService.clearL1Cache();
```

---

## 🎯 SOLID Принципы

### ✅ Single Responsibility Principle
- **ConversationHistoryService**: управление кешем
- **HistoryPersistenceService**: работа с БД
- **ChatWithToolsService**: бизнес-логика чата

### ✅ Open/Closed Principle
- Легко заменить HistoryPersistenceService другой реализацией (Redis, MongoDB)
- Новые источники данных добавляются без изменения существующего кода

### ✅ Liskov Substitution Principle
- HistoryPersistenceService может быть заменён на другую реализацию без нарушения контракта

### ✅ Interface Segregation Principle
- Каждый сервис имеет узкий интерфейс
- Клиенты зависят только от нужных методов

### ✅ Dependency Inversion Principle
- ConversationHistoryService зависит от HistoryPersistenceService (абстракция)
- А не от конкретной реализации БД

---

## 🚀 Spring Patterns

### ✅ Service Layer Pattern
- **Controller** → **Service** → **Repository** → **Entity** → **Database**

### ✅ Strategy Pattern
- Двухуровневая стратегия кеширования (L1 RAM + L2 БД)

### ✅ Repository Pattern
- MemoryRepository изолирует БД логику

### ✅ Dependency Injection
- Spring автоматически внедряет зависимости через конструктор

### ✅ Transactional Operations
- @Transactional аннотация на методах БД операций

---

## 📝 Конфигурация

### application.properties

```properties
# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/ai_challenge_db
spring.datasource.username=local_user
spring.datasource.password=local_password

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baselineOnMigrate=true

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

---

## 🧪 Тестирование

### Unit Test для ConversationHistoryService

```java
@Test
public void testGetHistoryFromL1Cache() {
    String conversationId = "test-conv-1";
    List<Message> history = service.getHistory(conversationId);
    
    // First call loads from DB
    assertEquals(0, history.size());
    
    // Add message
    service.addMessage(conversationId, "user", "Hello", null);
    
    // Second call hits L1 cache
    history = service.getHistory(conversationId);
    assertEquals(1, history.size());
}
```

### Integration Test для ChatWithToolsService

```java
@Test
@Transactional
public void testChatWithHistoryPersistence() {
    ChatRequest request = ChatRequest.builder()
        .message("What are my tasks?")
        .conversationId("conv-123")
        .temperature(0.7)
        .build();
    
    ChatResponse response = chatService.chatWithTools(request);
    
    // Verify response
    assertNotNull(response.getReply());
    
    // Verify history saved
    long count = historyService.getMessageCount("conv-123");
    assertTrue(count > 0);
}
```

---

## 📚 Файлы Реализации

```
backend/openrouter-service/
├── src/main/java/de/jivz/ai_challenge/openrouterservice/
│   ├── persistence/
│   │   ├── MemoryRepository.java          ← JPA Repository
│   │   └── entity/
│   │       └── MemoryEntry.java           ← JPA Entity
│   ├── service/
│   │   ├── ChatWithToolsService.java      ← Бизнес-логика
│   │   ├── ConversationHistoryService.java ← Управление кешем (L1+L2)
│   │   └── HistoryPersistenceService.java ← Абстракция БД
│   └── controller/
│       └── ChatWithToolsController.java   ← REST API
└── src/main/resources/
    ├── db/migration/
    │   └── V1__init_memory_entries_schema.sql ← Flyway миграция
    └── application.properties               ← Конфигурация
```

---

## ⚡ Производительность

- **L1 Cache Hit**: O(1) - почти мгновенно
- **L2 Cache Hit**: O(n) где n = количество сообщений (индексировано)
- **Concurrent Access**: ConcurrentHashMap безопасен для многопоточности
- **Transaction Isolation**: ACID гарантии для БД

---

## 🎓 Заключение

Реализован **простой и эффективный** чат-бот с:
- ✅ Персистентной историей диалогов в PostgreSQL
- ✅ Двухуровневым кешем для производительности
- ✅ Поддержкой MCP tools/инструментов
- ✅ SOLID принципами и Spring patterns
- ✅ Clean Code и хорошей документацией

---

**Author**: GitHub Copilot  
**Date**: 2026-01-10  
**Version**: 1.0

