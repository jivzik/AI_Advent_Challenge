# RAG MCP Server

MCP Server для RAG (Retrieval-Augmented Generation) системы с PostgreSQL + pgvector.

## 🎯 Функциональность

- **Загрузка документов**: PDF, EPUB, TXT, MD, DOCX, код
- **Chunking**: Recursive Character Splitting с overlap
- **Embeddings**: OpenRouter API (qwen/qwen3-embedding-8b)
- **Хранение**: PostgreSQL + pgvector (vector(768))
- **Поиск**: Семантический поиск по косинусному сходству
- **MCP Tools**: Интеграция с perplexity-service

## 🚀 Быстрый старт

### 1. Настройка PostgreSQL

```bash
# Установка pgvector extension
docker run -d \
  --name rag-postgres \
  -e POSTGRES_DB=rag_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  pgvector/pgvector:pg16
```

Или добавь расширение в существующую БД:
```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### 2. Создание таблиц

Запусти SQL из `src/main/resources/db/migration/V1__init_schema.sql`

### 3. Конфигурация

Установи переменные окружения или редактируй `application.yml`:

```bash
export OPENROUTER_API_KEY=your-api-key
```

### 4. Запуск

```bash
cd backend/rag-mcp-server
mvn spring-boot:run
```

Сервер запустится на `http://localhost:8086`

## 📡 API Endpoints

### Документы

| Метод | Endpoint | Описание |
|-------|----------|----------|
| POST | `/api/documents/upload` | Загрузка документа (multipart) |
| GET | `/api/documents` | Список всех документов |
| GET | `/api/documents/{id}` | Информация о документе |
| DELETE | `/api/documents/{id}` | Удаление документа |
| POST | `/api/documents/search` | Семантический поиск |

### MCP Tools

| Метод | Endpoint | Описание |
|-------|----------|----------|
| GET | `/api/tools` | Список MCP инструментов |
| POST | `/api/tools/execute` | Выполнение инструмента |
| GET | `/api/tools/health` | Health check |

## 🔧 MCP Инструменты

### search_documents
Семантический поиск по документам.

```json
{
  "name": "search_documents",
  "arguments": {
    "query": "Как работает машинное обучение?",
    "topK": 5,
    "threshold": 0.5,
    "documentId": null
  }
}
```

### list_documents
Список загруженных документов.

```json
{
  "name": "list_documents",
  "arguments": {
    "status": "READY"
  }
}
```

### get_document_info
Информация о конкретном документе.

```json
{
  "name": "get_document_info",
  "arguments": {
    "documentId": 1
  }
}
```

## 📤 Примеры использования

### Загрузка документа

```bash
curl -X POST http://localhost:8086/api/documents/upload \
  -F "file=@my-document.pdf"
```

### Поиск

```bash
curl -X POST http://localhost:8086/api/documents/search \
  -H "Content-Type: application/json" \
  -d '{"query": "машинное обучение", "topK": 5}'
```

### Вызов MCP tool

```bash
curl -X POST http://localhost:8086/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "search_documents",
    "arguments": {"query": "нейронные сети", "topK": 3}
  }'
```

## 🏗 Архитектура

```
User Request
     ↓
Perplexity-Service (ChatWithToolsService)
     ↓
MCPFactory.route("search_documents", args)
     ↓
RAG-MCP-Server → EmbeddingService → OpenRouter API
     ↓                                    ↓
DocumentChunkRepository ←← pgvector search
     ↓
Search Results → back to Perplexity-Service
```

## ⚙️ Конфигурация

### application.yml

```yaml
server:
  port: 8086

openrouter:
  api:
    key: ${OPENROUTER_API_KEY}
    embedding-model: qwen/qwen3-embedding-8b

rag:
  chunking:
    chunk-size: 500      # Размер чанка в символах
    chunk-overlap: 100   # Overlap между чанками
  
  embedding:
    batch-size: 20       # Batch размер для embeddings
    dimension: 768       # Размерность вектора
```

## 📊 Таблица document_chunks

```sql
CREATE TABLE document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT REFERENCES documents(id),
    document_name VARCHAR(500),
    chunk_index INTEGER,
    chunk_text TEXT NOT NULL,
    embedding vector(768),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX ON document_chunks 
USING ivfflat (embedding vector_cosine_ops);
```

## 🔗 Интеграция с perplexity-service

Интеграция уже настроена:

1. **RagMcpService.java** - сервис в perplexity-service
2. **McpServerConfig.java** - WebClient bean `ragMcpWebClient`
3. **application.properties** - `rag.mcp.url=http://localhost:8086`

RAG tools автоматически доступны через MCPFactory с префиксом `rag:`:
- `rag:search_documents`
- `rag:list_documents`
- `rag:get_document_info`

## 📝 TODO

- [ ] Async document processing с очередью
- [ ] Кэширование embeddings
- [ ] Rate limiting для OpenRouter API
- [ ] Поддержка нескольких embedding моделей
- [ ] UI для управления документами

