# OpenRouter Service - Быстрый старт

> **📖 Полная документация**: [OPENROUTER_SERVICE_ARCHITECTURE.md](../architecture/OPENROUTER_SERVICE_ARCHITECTURE.md)

## 🚀 Quick Start Guide

### 1. API-Key настроить

```bash
export OPENROUTER_API_KEY='sk-or-v1-your-actual-key-here'
```

**Совет:** Добавь в `~/.bashrc` для постоянного использования:

```bash
echo 'export OPENROUTER_API_KEY="sk-or-v1-your-actual-key-here"' >> ~/.bashrc
source ~/.bashrc
```

### 2. Запустить сервис

```bash
cd backend/openrouter-service
mvn spring-boot:run
```

Жди пока увидишь:
```
✅ ChatWithToolsService initialized
✅ MCPFactory initialized with servers: [google, rag, docker]
```

**Сервис доступен**: http://localhost:8084

### 3. Первый тест

```bash
# Простой чат
curl -X POST "http://localhost:8084/api/v1/openrouter/chat/simple?message=Hello"

# Чат с инструментами
curl -X POST "http://localhost:8084/api/v1/openrouter/tools/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "List my Google Tasks",
    "conversationId": "test-conv-1",
    "temperature": 0.7
  }'
```

### 4. Swagger UI

Открой в браузере: **http://localhost:8084/swagger-ui.html**

Здесь можно интерактивно тестировать все endpoints.

---

## 📝 Примеры использования

### 1. Простой чат без истории

```bash
curl -X POST "http://localhost:8084/api/v1/openrouter/chat/simple?message=Объясни%20что%20такое%20квантовая%20физика"
```

**Response**:
```json
{
  "reply": "Квантовая физика - это раздел физики...",
  "model": "anthropic/claude-3.5-sonnet",
  "responseTimeMs": 1234,
  "finishReason": "stop"
}
```

### 2. Чат с историей диалога

```bash
# Первое сообщение
curl -X POST "http://localhost:8084/api/v1/openrouter/chat/full" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Меня зовут Макс",
    "conversationId": "conv-123",
    "temperature": 0.7
  }'

# Второе сообщение (с историей)
curl -X POST "http://localhost:8084/api/v1/openrouter/chat/full" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Как меня зовут?",
    "conversationId": "conv-123",
    "temperature": 0.7
  }'
```

**Response второго сообщения**:
```json
{
  "reply": "Вас зовут Макс.",
  "model": "anthropic/claude-3.5-sonnet",
  ...
}
```

### 3. Чат с автоматическим использованием инструментов

```bash
curl -X POST "http://localhost:8084/api/v1/openrouter/tools/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Search for information about microservices in my documents",
    "conversationId": "conv-456",
    "temperature": 0.7
  }'
```

**OpenRouter автоматически**:
1. Определит что нужен инструмент `rag:search_documents`
2. Выполнит поиск в документах
3. Сформирует ответ на основе найденных документов
4. Добавит список источников в ответ

**Response**:
```json
{
  "reply": "Based on the documents, microservices architecture is...\n\n---\n\n**📚 Источники информации:**\n1. `microservices_guide.pdf`\n2. `architecture_patterns.md`",
  "model": "anthropic/claude-3.5-sonnet",
  "responseTimeMs": 3456,
  "finishReason": "stop"
}
```

### 4. Получить историю диалога

```bash
curl "http://localhost:8084/api/v1/openrouter/chat/history/conv-123"
```

**Response**:
```json
{
  "conversationId": "conv-123",
  "messages": [
    {
      "role": "user",
      "content": "Меня зовут Макс",
      "timestamp": "2026-01-12T10:00:00"
    },
    {
      "role": "assistant",
      "content": "Приятно познакомиться, Макс!",
      "timestamp": "2026-01-12T10:00:02"
    }
  ]
}
```

### 5. Список всех диалогов

```bash
curl "http://localhost:8084/api/v1/openrouter/chat/conversations"
```

### 6. Список доступных MCP инструментов

```bash
curl "http://localhost:8084/api/v1/openrouter/tools/available"
```

**Response**:
```json
[
  {
    "name": "rag:search_documents",
    "description": "Search documents in the knowledge base",
    "inputSchema": {
      "type": "object",
      "properties": {
        "query": {"type": "string"},
        "topK": {"type": "integer"}
      }
    }
  },
  {
    "name": "google:tasks_list",
    "description": "List Google Tasks",
    "inputSchema": {...}
  }
]
```

---

## ⚙️ Конфигурация

### Изменить модель

Редактируй `backend/openrouter-service/src/main/resources/application.properties`:

```properties
# Текущая модель
spring.ai.openrouter.default-model=anthropic/claude-3.5-sonnet

# Альтернативы:
# spring.ai.openrouter.default-model=openai/gpt-4-turbo
# spring.ai.openrouter.default-model=openai/gpt-4o
# spring.ai.openrouter.default-model=meta-llama/llama-3.1-405b-instruct
# spring.ai.openrouter.default-model=google/gemini-pro-1.5
```

Полный список: https://openrouter.ai/models

### Изменить температуру по умолчанию

```properties
spring.ai.openrouter.default-temperature=0.7  # 0.0 = детерминированный, 2.0 = креативный
spring.ai.openrouter.default-max-tokens=1000
```

### Настроить MCP сервисы

```properties
# RAG Service
mcp.rag.base-url=http://localhost:8086

# Google Service
mcp.google.enabled=true
mcp.google.base-url=http://localhost:8081

# Docker Monitor Service
mcp.docker.monitor.base-url=http://localhost:8083
```

---

## 🔍 Troubleshooting

### ❌ "OPENROUTER_API_KEY is not set"

```bash
export OPENROUTER_API_KEY='your-key-here'
# Перезапусти сервис
```

### ❌ "Connection refused"

Сервис не запущен:
```bash
cd backend/openrouter-service
mvn spring-boot:run
```

### ❌ "MCP server not found: google"

MCP сервис не запущен. Проверь:
```bash
curl http://localhost:8081/health  # Google
curl http://localhost:8086/health  # RAG
curl http://localhost:8083/health  # Docker
```

Запусти нужные сервисы:
```bash
cd backend/google-service && mvn spring-boot:run &
cd backend/rag-mcp-server && mvn spring-boot:run &
cd backend/mcp-docker-monitor && mvn spring-boot:run &
```

### ❌ PostgreSQL ошибка

Проверь что БД запущена:
```bash
psql -h localhost -U local_user -d ai_challenge_db
```

Если нет, создай БД:
```bash
createdb -h localhost -U local_user ai_challenge_db
```

### 📋 Проверка логов

```bash
# В другом терминале
tail -f logs/openrouter-service.log

# Или в реальном времени:
cd backend/openrouter-service
mvn spring-boot:run
```

Ищи эти эмодзи:
- `🚀` - Старт запроса
- `📤` - Отправка в OpenRouter
- `📥` - Получение ответа
- `🔧` - Выполнение MCP tool
- `✅` - Успех
- `❌` - Ошибка
- `📚` - Найдены источники

---

## 📊 Response структура

### ChatResponse

```json
{
  "reply": "Ответ ассистента",
  "model": "anthropic/claude-3.5-sonnet",
  "responseTimeMs": 1234,
  "finishReason": "stop"
}
```

### С источниками (RAG)

```json
{
  "reply": "Ответ на основе документов...\n\n---\n\n**📚 Источники информации:**\n1. `doc1.pdf`\n2. `doc2.md`",
  "model": "anthropic/claude-3.5-sonnet",
  "responseTimeMs": 2345,
  "finishReason": "stop"
}
```

---

## 🎯 Основные фичи

| Feature | Status | Описание |
|---------|--------|----------|
| ✅ Multi-model Support | Ready | Все модели OpenRouter |
| ✅ MCP Tools | Ready | Автоматическое использование |
| ✅ Conversation History | Ready | L1+L2 кеширование |
| ✅ Context Intelligence | Ready | Авто-определение контекста |
| ✅ Source Attribution | Ready | Источники из RAG |
| ✅ Swagger UI | Ready | Интерактивная документация |
| ✅ PostgreSQL Persistence | Ready | Сохранение истории |

---

## 📚 Дополнительная документация

### Архитектура
- 📖 **[OpenRouter Service Architecture](../architecture/OPENROUTER_SERVICE_ARCHITECTURE.md)** - Полная архитектурная документация
- [MCP Multi-Provider Architecture](../architecture/MCP_MULTI_PROVIDER_ARCHITECTURE.md)
- [Conversation History Implementation](../architecture/CONVERSATION_HISTORY_IMPLEMENTATION.md)

### Features
- [OpenRouter Provider Feature](../features/OPENROUTER_PROVIDER_FEATURE.md) - Краткий обзор
- [System Prompt Feature](../features/SYSTEM_PROMPT_FEATURE.md)
- [Temperature Feature](../features/TEMPERATURE_FEATURE.md)

### Setup
- [Chatbot Deployment Guide](../setup/CHATBOT_DEPLOYMENT_GUIDE.md)
- [PostgreSQL Memory Setup](../setup/POSTGRESQL_MEMORY_SETUP.md)

### API Reference
- **Swagger UI**: http://localhost:8084/swagger-ui.html
- **API Docs**: http://localhost:8084/api-docs

---

## 🚀 Следующие шаги

1. **Попробуй разные модели** - измени `default-model` в application.properties
2. **Протестируй MCP tools** - используй `/tools/chat` endpoint
3. **Изучи Swagger UI** - http://localhost:8084/swagger-ui.html
4. **Просмотри логи** - изучи tool execution loop в реальном времени
5. **Прочитай архитектуру** - [OPENROUTER_SERVICE_ARCHITECTURE.md](../architecture/OPENROUTER_SERVICE_ARCHITECTURE.md)

---

**Версия**: 1.0.0  
**Дата обновления**: 2026-01-12  
**Статус**: ✅ Production Ready

