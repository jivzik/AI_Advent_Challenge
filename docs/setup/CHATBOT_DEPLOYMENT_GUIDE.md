# 🚀 Развёртывание Чат-Бота с Историей Диалогов

## ⚠️ Важно: Архитектура БД

### Структура Микросервисов

```
┌─────────────────────────────────────────┐
│        rag-mcp-server (Port 8086)       │
│  ✅ Управляет Flyway миграциями         │
│  ✅ Создаёт таблицы в PostgreSQL        │
│  ✅ Хранит документы и embeddings       │
│  ✅ Хранит conversation memory          │
└─────────────────────┬───────────────────┘
                      │
        Shared Database (ai_challenge_db)
                      │
┌─────────────────────▼───────────────────┐
│    openrouter-service (Port 8084)       │
│  ❌ НЕ управляет Flyway                 │
│  ✅ Читает/пишет данные в таблицы       │
│  ✅ Использует MemoryRepository         │
└─────────────────────────────────────────┘
```

### Таблицы в БД (управляются rag-mcp-server)

1. **documents** - хранение файлов/документов (Flyway V1)
2. **document_chunks** - чанки документов с embeddings (Flyway V1)
3. **memory_entries** - история диалогов (Flyway V3) ← **НОВОЕ**

---

## 📋 Процедура Развёртывания

### 1️⃣ Стартуем rag-mcp-server ПЕРВЫМ

```bash
cd /home/jivz/IdeaProjects/AI_Advent_Challenge/backend/rag-mcp-server
mvn spring-boot:run
```

**Что происходит:**
- ✅ Flyway проверяет таблицу `flyway_schema_history`
- ✅ Применяет миграции V1 (documents, document_chunks)
- ✅ Применяет миграции V2 (FTS поддержка)
- ✅ Применяет миграции V3 (memory_entries) ← **НОВОЕ**
- ✅ Создаёт таблицу `memory_entries` для OpenRouter

**Логи которые ты должен увидеть:**
```
INFO o.f.core.internal.command.migrate.Migrate : DB: PostgreSQL 16.1
INFO o.f.c.i.c.validate.ValidateResult : Successfully validated 3 migrations
INFO o.f.c.i.c.migrate.MigrateResultImpl : +-----+-----+-----+-----+-----+
INFO o.f.c.i.c.migrate.MigrateResultImpl : | V1  | V2  | V3  | ... |
INFO o.f.c.i.c.migrate.MigrateResultImpl : +-----+-----+-----+-----+-----+
```

### 2️⃣ Стартуем openrouter-service ВТОРЫМ

```bash
cd /home/jivz/IdeaProjects/AI_Advent_Challenge/backend/openrouter-service
mvn spring-boot:run
```

**Что происходит:**
- ✅ Подключается к той же БД
- ✅ Не управляет Flyway (конфигурация удалена)
- ✅ JPA валидирует существующие таблицы (ddl-auto=validate)
- ✅ MemoryRepository готов к использованию
- ✅ ConversationHistoryService готов к использованию

---

## ✅ Проверка Статуса

### Проверить таблицу в PostgreSQL

```bash
psql -h localhost -U local_user -d ai_challenge_db -c "\dt memory_entries"
```

**Ожидаемый результат:**
```
               Table "public.memory_entries"
         Column         |            Type             | Collation | Nullable
------------------------+-----------------------------+-----------+----------
 id                     | bigint                      |           | not null
 conversation_id        | character varying(255)      |           | not null
 user_id                | character varying(255)      |           |
 role                   | character varying(50)       |           | not null
 content                | text                        |           | not null
 timestamp              | timestamp without time zone |           | not null
 model                  | character varying(255)      |           |
 input_tokens           | integer                     |           |
 output_tokens          | integer                     |           |
 total_tokens           | integer                     |           |
 cost                   | numeric(10,6)               |           |
 is_compressed          | boolean                     |           | not false
 response_time_ms       | bigint                      |           |
 compressed_messages_count | integer                  |           |
 compression_timestamp  | timestamp without time zone |           |
 created_at             | timestamp without time zone |           |
 updated_at             | timestamp without time zone |           |
Indexes:
    "memory_entries_pkey" PRIMARY KEY, btree (id)
    "idx_memory_entries_conversation_id" btree (conversation_id)
    "idx_memory_entries_user_id" btree (user_id)
    "idx_memory_entries_timestamp" btree ("timestamp")
    "idx_memory_entries_conversation_timestamp" btree (conversation_id, "timestamp")
    "idx_memory_entries_conversation_is_compressed" btree (conversation_id, is_compressed)
Triggers:
    trigger_update_memory_entries_updated_at BEFORE UPDATE ON memory_entries FOR EACH ROW EXECUTE FUNCTION update_updated_at_column()
```

### Проверить индексы

```bash
psql -h localhost -U local_user -d ai_challenge_db -c "\di memory_entries*"
```

### Проверить Flyway историю

```bash
psql -h localhost -U local_user -d ai_challenge_db -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank;"
```

**Ожидаемый результат:**
```
 installed_rank | version |                      description                      | type | script | installed_by | installed_on | execution_time | success
----------------+---------+------------------------------------------------------+------+--------+--------------+--------------+----------------+---------
              1 | 1       | init schema                                          | SQL  | ...    | local_user   | ...          |           1234 | t
              2 | 2       | add fts support                                      | SQL  | ...    | local_user   | ...          |           5678 | t
              3 | 3       | add conversation memory entries                      | SQL  | ...    | local_user   | ...          |           9012 | t
```

---

## 🧪 Тестирование

### Тест 1: Отправить сообщение с историей

```bash
curl -X POST http://localhost:8084/api/v1/openrouter/tools/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Hello, what are my tasks?",
    "conversationId": "test-conv-001",
    "temperature": 0.7
  }'
```

**Ожидаемый результат:**
- ✅ 200 OK с response
- ✅ Сообщение сохранено в БД
- ✅ История загружается при следующем запросе

### Тест 2: Проверить историю в БД

```bash
psql -h localhost -U local_user -d ai_challenge_db -c "SELECT role, content FROM memory_entries WHERE conversation_id='test-conv-001' ORDER BY timestamp;"
```

**Ожидаемый результат:**
```
  role    |           content
----------+----------------------------------
 user     | Hello, what are my tasks?
 assistant| I'll help you with your tasks...
```

### Тест 3: Многотуровой диалог (история загружается)

```bash
# Второй запрос к той же конверсации
curl -X POST http://localhost:8084/api/v1/openrouter/tools/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Add a reminder for tomorrow",
    "conversationId": "test-conv-001",
    "temperature": 0.7
  }'
```

**В логах openrouter-service ты должен увидеть:**
```
📝 Loaded 2 messages from history for conversationId: test-conv-001
```

Это значит что история загружается из БД! ✅

---

## 📂 Файлы Которые Были Изменены

### ✅ Создано

- `backend/rag-mcp-server/src/main/resources/db/migration/V3__add_conversation_memory_entries.sql` ← **НОВОЕ**

### ✅ Обновлено

- `backend/openrouter-service/src/main/java/de/jivz/ai_challenge/openrouterservice/service/ConversationHistoryService.java`
  - Гибридный кеш (L1 RAM + L2 БД)
  
- `backend/openrouter-service/src/main/java/de/jivz/ai_challenge/openrouterservice/service/HistoryPersistenceService.java` ← **НОВОЕ**
  - Абстракция для работы с БД
  
- `backend/openrouter-service/src/main/java/de/jivz/ai_challenge/openrouterservice/service/ChatWithToolsService.java`
  - Сохранение истории в БД

### ❌ Удалено

- `backend/openrouter-service/src/main/resources/db/migration/` (миграция переместилась в rag-mcp-server)
- Flyway конфигурация из `backend/openrouter-service/src/main/resources/application.properties`

---

## 🎯 Порядок Запуска

### Рекомендуемый порядок

1. **Убедись что PostgreSQL запущен:**
   ```bash
   docker ps | grep postgres
   ```

2. **Запусти rag-mcp-server (управляет Flyway):**
   ```bash
   cd backend/rag-mcp-server && mvn spring-boot:run
   ```
   
3. **В другом терминале запусти openrouter-service:**
   ```bash
   cd backend/openrouter-service && mvn spring-boot:run
   ```

4. **Проверь что таблицы созданы:**
   ```bash
   psql -h localhost -U local_user -d ai_challenge_db -c "\dt memory_entries"
   ```

---

## 🔧 Решение Проблем

### Проблема: "Unable to build Hibernate SessionFactory; Schema-validation: missing table [memory_entries]"

**Причина:** rag-mcp-server не запущен или Flyway миграции не выполнились

**Решение:**
1. Убедись что rag-mcp-server запущен ПЕРВЫМ
2. Проверь логи rag-mcp-server на ошибки Flyway
3. Проверь что таблица создалась: `psql -c "\dt memory_entries"`
4. Если таблицы нет, вручную выполни V3 миграцию SQL файл

### Проблема: Flyway error "Schema has version X, but should be Y"

**Причина:** История миграций в БД не совпадает с файлами

**Решение:**
```bash
# Проверь flyway_schema_history
psql -c "SELECT * FROM flyway_schema_history;"

# Если проблема, удали и переустанови (только для dev!)
psql -c "DROP TABLE flyway_schema_history, memory_entries, document_chunks, documents CASCADE;"
```

### Проблема: "Parameter index out of range"

**Причина:** SQL синтаксис ошибка в миграции

**Решение:**
- Проверь SQL файл на правильность синтаксиса
- Убедись что все точки с запятой на месте
- Проверь логи rag-mcp-server

---

## 📊 Мониторинг

### Логирование истории

В `openrouter-service` логи показывают:
```
📦 L1 cache HIT: Retrieved 2 messages from RAM for: conv-123
📦 L1 cache MISS: Loading from database for: conv-123  
📦 L2 cache HIT: Loaded 2 messages from DB and cached in RAM
💾 Saved conversation to history for conversationId: conv-123
```

### Метрики

```java
// Количество активных конверсаций в RAM
int activeCount = historyService.getConversationCount();

// Количество сообщений в БД
long dbCount = historyService.getMessageCount("conv-123");

// Статистика (токены, стоимость)
Object[] stats = historyService.getConversationStats("conv-123");
```

---

## ✨ Итоговая Архитектура

```
PostgreSQL Database (ai_challenge_db)
├── documents (V1)
├── document_chunks (V1)
├── memory_entries (V3) ← OpenRouter history
└── flyway_schema_history

rag-mcp-server (8086)
└── Flyway migrations
    ├── V1__init_schema.sql
    ├── V2__add_fts_support.sql
    └── V3__add_conversation_memory_entries.sql ← НОВОЕ

openrouter-service (8084)
├── MemoryRepository (JPA)
├── MemoryEntry (Entity)
├── HistoryPersistenceService (L2 БД)
├── ConversationHistoryService (L1 RAM + L2 БД)
└── ChatWithToolsService (используёт историю)
```

---

**Status:** ✅ Ready to Deploy
**Author:** GitHub Copilot
**Date:** 2026-01-10

