# Full-Text Search (FTS) Feature

## 📋 Quick Summary
Full-Text Search (FTS) ist eine PostgreSQL-basierte Volltextsuche-Implementation mit tsvector/tsquery für intelligente Dokumentensuche. Das Feature bietet russischsprachige Unterstützung, Ranking-Algorithmen (ts_rank_cd), GIN-Indizierung und hybride Suche in Kombination mit semantischer Vektorsuche.

## 🎯 Use Cases
- **Use Case 1**: Schnelle Keyword-basierte Suche in großen Dokumentensammlungen (>10.000 Chunks)
- **Use Case 2**: Hybride Suche: Kombination aus semantischer (pgvector) und lexikalischer (FTS) Suche
- **Use Case 3**: Russischsprachige Dokumentensuche mit Stemming und Normalisierung
- **Use Case 4**: Erweiterte Suchabfragen mit Boolean-Operatoren (AND, OR, NOT, NEAR)

## 🏗️ Architecture Overview

### High-Level Diagram (ASCII)
```
Search Request
      │
      ├─── Semantic Search (pgvector)  ──> 70% weight
      │    - embedding <=> query_embedding
      │
      └─── Keyword Search (FTS)  ──> 30% weight
           │
           ├── text_vector (tsvector)
           ├── plainto_tsquery('russian', query)
           ├── ts_rank_cd() ranking
           └── GIN index acceleration
                │
                ↓
           Hybrid Results
           (sorted by combined score)
```

### Key Components

1. **Database Migration V2** (`backend/rag-mcp-server/src/main/resources/db/migration/V2__add_fts_support.sql`)
   - Purpose: Adds FTS infrastructure to existing schema
   - Features: tsvector column, GIN index, Russian language support
   - Used by: All search queries

2. **KeywordSearchService** (`backend/rag-mcp-server/src/main/java/de/jivz/rag/service/KeywordSearchService.java`)
   - Purpose: Implements keyword search logic
   - Methods: `keywordSearch()`, `advancedSearch()`, `hybridSearch()`
   - Dependencies: DocumentChunkRepository

3. **DocumentChunkRepository** (`backend/rag-mcp-server/src/main/java/de/jivz/rag/repository/DocumentChunkRepository.java`)
   - Purpose: Native SQL queries for FTS operations
   - Methods: `searchByKeywords()`, `searchByAdvancedQuery()`, `hybridSearch()`
   - Technology: JPA with native PostgreSQL FTS queries

4. **SearchController** (`backend/rag-mcp-server/src/main/java/de/jivz/rag/controller/SearchController.java`)
   - Purpose: REST API for all search types
   - Endpoints: `/api/search/keywords`, `/api/search/advanced`, `/api/search/hybrid`
   - Dependencies: KeywordSearchService, RagService

## 💻 Complete Code Examples

### Example 1: Database Schema - tsvector Column

```sql
-- File: backend/rag-mcp-server/src/main/resources/db/migration/V2__add_fts_support.sql

-- Enable unaccent extension for accent handling
CREATE EXTENSION IF NOT EXISTS unaccent;

-- Add tsvector column (auto-generated from chunk_text)
ALTER TABLE document_chunks
ADD COLUMN text_vector tsvector
  GENERATED ALWAYS AS (
    to_tsvector('russian', COALESCE(chunk_text, ''))
  ) STORED;

-- Create GIN index for fast FTS queries
CREATE INDEX idx_document_chunks_text_vector 
  ON document_chunks 
  USING GIN (text_vector);

-- Create composite index for document-scoped searches
CREATE INDEX idx_document_chunks_doc_id_text_vector 
  ON document_chunks (document_id, text_vector);

-- Example of tsvector content:
-- chunk_text: "Вернуться, вернулись, возвращение"
-- text_vector: 'верн':1,2 'возвращ':3
-- (Words are stemmed to their root forms)
```

**Explanation:**
- **GENERATED ALWAYS AS STORED**: Column auto-updates when chunk_text changes
- **to_tsvector('russian', ...)**: Applies Russian language stemming
- **GIN index**: Generalized Inverted Index for efficient text search
- **Composite index**: Optimizes document-specific searches

### Example 2: Keyword Search Implementation

```java
// File: backend/rag-mcp-server/src/main/java/de/jivz/rag/service/KeywordSearchService.java
@Service
@Slf4j
public class KeywordSearchService {
    
    private final DocumentChunkRepository chunkRepository;
    
    /**
     * Simple keyword search with Russian language support
     */
    public List<SearchResultDto> keywordSearch(String query, int topK) {
        log.info("Keyword search for: {}", query);
        
        // Normalize query (remove special characters)
        String normalizedQuery = normalizeQuery(query);
        
        // Execute FTS query
        List<DocumentChunk> chunks = chunkRepository.searchByKeywords(
            normalizedQuery, 
            topK
        );
        
        // Convert to DTOs with relevance scores
        return chunks.stream()
            .map(this::toSearchResult)
            .collect(Collectors.toList());
    }
    
    /**
     * Advanced search with Boolean operators
     * Examples:
     * - "микросервисы & Spring"  (AND)
     * - "Java | Kotlin"          (OR)
     * - "Spring & !Boot"         (AND NOT)
     * - "микро <-> сервисы"      (NEAR)
     */
    public List<SearchResultDto> advancedSearch(String query, int topK) {
        log.info("Advanced search for: {}", query);
        
        // Convert to tsquery format
        String tsQuery = formatAsQuery(query);
        
        // Execute with ts_rank_cd for relevance scoring
        List<DocumentChunk> chunks = chunkRepository.searchByAdvancedQuery(
            tsQuery,
            topK
        );
        
        return chunks.stream()
            .map(this::toSearchResult)
            .collect(Collectors.toList());
    }
    
    /**
     * Normalizes query: removes punctuation, extra spaces
     */
    private String normalizeQuery(String query) {
        return query
            .replaceAll("[^а-яА-Яa-zA-Z0-9\\s]", " ")  // Remove special chars
            .replaceAll("\\s+", " ")                    // Collapse whitespace
            .trim();
    }
    
    /**
     * Formats query for tsquery
     * Converts user-friendly syntax to PostgreSQL tsquery
     */
    private String formatAsQuery(String query) {
        return query
            .replace(" AND ", " & ")
            .replace(" OR ", " | ")
            .replace(" NOT ", " ! ");
    }
}
```

**Explanation:**
- **normalizeQuery()**: Cleans input for reliable matching
- **formatAsQuery()**: Converts natural language to tsquery syntax
- **Russian language**: Automatic stemming (e.g., "вернулись" → "верн")
- **Boolean operators**: Support for complex search expressions

### Example 3: Hybrid Search (Semantic + Keyword)

```java
// File: backend/rag-mcp-server/src/main/java/de/jivz/rag/repository/DocumentChunkRepository.java
@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    
    /**
     * Hybrid search: 70% semantic + 30% keyword
     * Best of both worlds: meaning + exact terms
     */
    @Query(value = """
        SELECT 
            dc.*,
            (0.7 * (1 - (dc.embedding <=> CAST(:queryEmbedding AS vector))) +
             0.3 * ts_rank_cd(dc.text_vector, plainto_tsquery('russian', :keywords))) 
            AS combined_score
        FROM document_chunks dc
        WHERE 
            -- Match either semantic OR keyword
            dc.text_vector @@ plainto_tsquery('russian', :keywords)
            OR (1 - (dc.embedding <=> CAST(:queryEmbedding AS vector))) > 0.7
        ORDER BY combined_score DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> hybridSearch(
        @Param("queryEmbedding") String queryEmbedding,
        @Param("keywords") String keywords,
        @Param("limit") int limit
    );
}
```

**Explanation:**
- **70/30 weighting**: Prioritizes semantic understanding with keyword backup
- **ts_rank_cd()**: PostgreSQL's relevance ranking algorithm
- **@@ operator**: Full-text match operator
- **Fallback logic**: Returns results if either method matches

## 📂 File Structure

```
backend/rag-mcp-server/
├── src/main/resources/db/migration/
│   ├── V1__init_schema.sql                    # Initial schema
│   └── V2__add_fts_support.sql                # FTS infrastructure ✨
├── src/main/java/de/jivz/rag/
│   ├── controller/
│   │   └── SearchController.java              # Search API endpoints ✨
│   ├── service/
│   │   ├── KeywordSearchService.java          # FTS logic ✨
│   │   └── RagService.java                    # Hybrid search integration
│   ├── repository/
│   │   └── DocumentChunkRepository.java       # FTS queries ✨
│   ├── entity/
│   │   └── DocumentChunk.java                 # Updated with @Transient textVector
│   └── dto/
│       ├── KeywordSearchRequest.java          # FTS request DTO ✨
│       └── KeywordSearchResponse.java         # FTS response DTO ✨
```

**Особенности:**
- Использует `plainto_tsquery` - автоматическая нормализация
- Поддерживает морфологию русского языка
- Быстрый поиск благодаря GIN индексу
- Ранжирование по `ts_rank`

### 2. Поиск в конкретном документе

```bash
POST /api/search/keywords/document/42
Content-Type: application/json

{
  "query": "нейронные сети",
  "topK": 5
}
```

**Параметры:**
- `documentId` (path parameter): ID документа для поиска
- `query`: текст для поиска
- `topK`: максимальное количество результатов

### 3. Расширенный поиск с операторами

```bash
POST /api/search/advanced
Content-Type: application/json

{
  "query": "python & machine & !deep",
  "topK": 10
}
```

**Поддерживаемые операторы:**

| Оператор | Описание | Пример |
|----------|---------|--------|
| `&` | AND - оба слова | `python & java` |
| `\|` | OR - одно из слов | `python \| java` |
| `!` | NOT - исключить | `ai & !robot` |
| `<->` | Близость слов | `python <-> machine` |

**Примеры:**
```
"python & machine & learning"      // Все три слова
"neural | deep | machine"           // Одно из трех
"tensorflow & !keras"               // TensorFlow, но не Keras
"machine <-> learning"              // Слова рядом друг с другом
```

### 4. Поиск с расширенным ранжированием

```bash
POST /api/search/ranked
Content-Type: application/json

{
  "query": "искусственный интеллект",
  "topK": 5
}
```

**Отличия от простого поиска:**
- Использует `ts_rank_cd` вместо `ts_rank`
- Более точное вычисление релевантности
- Учитывает:
  - TF (частота слов в документе)
  - IDF (редкость слов в коллекции)
  - Длину документа
  - Близость слов друг к другу

## Примеры использования

### Пример 1: Поиск по документации

```bash
# Найти все упоминания "API"
curl -X POST http://localhost:8080/api/search/keywords \
  -H "Content-Type: application/json" \
  -d '{
    "query": "API",
    "topK": 20
  }'
```

### Пример 2: Поиск с исключением

```bash
# Найти про Python, но не про Django
curl -X POST http://localhost:8080/api/search/advanced \
  -H "Content-Type: application/json" \
  -d '{
    "query": "python & !django",
    "topK": 10
  }'
```

### Пример 3: Поиск фразы

```bash
# Найти "machine learning" в этом порядке и рядом
curl -X POST http://localhost:8080/api/search/advanced \
  -H "Content-Type: application/json" \
  -d '{
    "query": "machine <-> learning",
    "topK": 5
  }'
```

### Пример 4: Комплексный поиск

```bash
# (Python или Java) И (машинное обучение) И (НЕ GPU)
curl -X POST http://localhost:8080/api/search/advanced \
  -H "Content-Type: application/json" \
  -d '{
    "query": "(python | java) & machine & !gpu",
    "topK": 15
  }'
```

## Производительность

### Индексирование

Когда был загружен документ, PostgreSQL:
1. Разбивает текст на токены (слова)
2. Нормализует слова по русской морфологии
3. Создает tsvector
4. Индексирует через GIN индекс

**Время первого поиска:** ~100-200ms (зависит от размера)
**Последующие поиски:** ~5-20ms (благодаря GIN индексу)

### Сравнение с семантическим поиском

| Параметр | FTS | Semantic Search |
|----------|-----|-----------------|
| Скорость | ⚡⚡⚡ Очень быстро | ⚡⚡ Медленно |
| Морфология | ✅ Да (Russian) | ⚠️ Плохо |
| Точность | ⚡ Точные совпадения | ⚡⚡⚡ Контекст |
| Размер индекса | 📦 Маленький | 📦📦📦 Большой |
| Операторы | ✅ AND, OR, NOT | ❌ Нет |

## Интеграция с фронтендом

### React пример

```javascript
// Полнотекстовый поиск
async function performKeywordSearch(query) {
  const response = await fetch('/api/search/keywords', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      query: query,
      topK: 10
    })
  });
  
  return response.json();
}

// Расширенный поиск
async function advancedSearch(query) {
  const response = await fetch('/api/search/advanced', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      query: query,
      topK: 10
    })
  });
  
  return response.json();
}

// Использование
const results = await performKeywordSearch('машинное обучение');
results.results.forEach(r => {
  console.log(`${r.documentName}: ${r.chunkText.substring(0, 50)}...`);
});
```

## Миграция данных

Если вы загружали документы до добавления FTS:

1. **Автоматически обновится**: Поскольку `text_vector` - это GENERATED ALWAYS AS STORED, PostgreSQL автоматически создаст индексы для всех существующих записей.

2. **Проверьте индекс**: 
   ```sql
   SELECT schemaname, tablename, indexname 
   FROM pg_indexes 
   WHERE tablename = 'document_chunks' 
   AND indexname LIKE 'idx_%';
   ```

3. **Переиндексируйте вручную** (если нужно):
   ```sql
   REINDEX INDEX idx_document_chunks_text_vector;
   ```

## Отладка

### Просмотр tsvector

```sql
SELECT 
  id, 
  chunk_text,
  text_vector
FROM document_chunks 
LIMIT 5;
```

### Тест поиска

```sql
-- Простой поиск
SELECT id, chunk_text, ts_rank(text_vector, query) as rank
FROM document_chunks,
     plainto_tsquery('russian', 'машинное обучение') query
WHERE text_vector @@ query
ORDER BY rank DESC
LIMIT 10;

-- Расширенный поиск
SELECT id, chunk_text, ts_rank(text_vector, query) as rank
FROM document_chunks,
     to_tsquery('russian', 'python & machine') query
WHERE text_vector @@ query
ORDER BY rank DESC
LIMIT 10;
```

## Настройки

### Изменить язык морфологии

В файле `V2__add_fts_support.sql` измените:
```sql
-- Было:
to_tsvector('russian', COALESCE(chunk_text, ''))

-- Будет (например, для английского):
to_tsvector('english', COALESCE(chunk_text, ''))
```

Поддерживаемые языки: `'simple'`, `'danish'`, `'dutch'`, `'english'`, `'finnish'`, `'french'`, `'german'`, `'hungarian'`, `'italian'`, `'norwegian'`, `'portuguese'`, `'romanian'`, `'russian'`, `'spanish'`, `'swedish'`, `'turkish'`.

### Изменить флаги ранжирования

В `KeywordSearchService.searchByKeywordsAdvanced()`:
```java
// Было: ts_rank_cd(c.text_vector, query, 32)
// 32 = 1 (log TF) + 2 (IDF) + 4 (length norm) + 8 (cover density) + 16 (cover density)

// Вы можете использовать:
// 1  = log frequency weighting
// 2  = inverse document frequency
// 4  = length normalization
// 8  = extended cover density ranking
// 16 = cover density ranking
```

## Производственные рекомендации

1. **Мониторинг индекса**:
   ```sql
   -- Размер индекса
   SELECT pg_size_pretty(pg_relation_size('idx_document_chunks_text_vector'));
   ```

2. **Vacuum и Analyze**:
   ```sql
   VACUUM ANALYZE document_chunks;
   ```

3. **Backup индекса**: Включен в обычный backup PostgreSQL

4. **Масштабирование**: Для очень больших данных рассмотрите:
   - Партицирование таблицы по document_id
   - Использование Elasticsearch для еще большей скорости
   - Кэширование популярных запросов

## Дальнейшее развитие

### 1. Синонимы
```sql
CREATE TEXT SEARCH DICTIONARY my_synonyms (
  TEMPLATE = synonym,
  SYNONYMS = my_synonyms
);
```

### 2. Кастомные стоп-слова
```sql
ALTER TEXT SEARCH DICTIONARY russian_stop (STOPWORDS = my_stop_words);
```

### 3. Фильтр по датам
```sql
WHERE text_vector @@ query 
  AND created_at > NOW() - INTERVAL '30 days'
```

### 4. Весовые коэффициенты
```sql
-- Давать больший вес заголовкам
ts_rank(text_vector, query, 1)  -- Заголовок
vs
ts_rank(text_vector, query, 8)  -- Текст
```

## Тестирование

### Unit тесты

```java
@SpringBootTest
class KeywordSearchServiceTest {

    @Autowired
    private KeywordSearchService keywordSearchService;

    @Test
    void testKeywordSearch() {
        List<SearchResultDto> results = keywordSearchService.keywordSearch("тест", 10);
        assertNotNull(results);
    }

    @Test
    void testAdvancedSearch() {
        List<SearchResultDto> results = keywordSearchService.advancedKeywordSearch("test & java", 10);
        assertNotNull(results);
    }

    @Test
    void testQueryNormalization() {
        String normalized = KeywordSearchService.normalizeQuery("  тест    AND  java  ");
        assertEquals("тест AND java", normalized);
    }
}
```

## Заключение

Full-Text Search на PostgreSQL обеспечивает:
- ✅ Быстрый полнотекстовый поиск
- ✅ Поддержку русского языка
- ✅ Мощные операторы поиска
- ✅ Эффективное использование индексов
- ✅ Низкую нагрузку на базу данных

Идеально для документ-ориентированных приложений!

