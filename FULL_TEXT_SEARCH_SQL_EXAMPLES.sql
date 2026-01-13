-- Full-Text Search (FTS) - SQL Query Examples
-- Используйте эти запросы для тестирования и отладки

-- ============================================================================
-- 1. ПРОВЕРКА УСТАНОВКИ
-- ============================================================================

-- Проверить наличие расширения unaccent
SELECT extname FROM pg_extension WHERE extname = 'unaccent';
-- Результат: должно вывести "unaccent"

-- Проверить наличие расширения pgvector
SELECT extname FROM pg_extension WHERE extname = 'vector';
-- Результат: должно вывести "vector"

-- Проверить наличие колонки text_vector
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'document_chunks'
AND column_name = 'text_vector';
-- Результат: text_vector | text

-- ============================================================================
-- 2. ПРОВЕРКА ИНДЕКСОВ
-- ============================================================================

-- Список всех индексов для document_chunks
SELECT schemaname, tablename, indexname, indexdef
FROM pg_indexes
WHERE tablename = 'document_chunks'
ORDER BY indexname;

-- Размер GIN индекса
SELECT
  pg_size_pretty(pg_relation_size('idx_document_chunks_text_vector')) as index_size,
  pg_size_pretty(pg_total_relation_size('document_chunks')) as table_size;

-- Статистика индекса
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
WHERE indexname = 'idx_document_chunks_text_vector';

-- ============================================================================
-- 3. ПРОСМОТР ДАННЫХ
-- ============================================================================

-- Посмотреть tsvector для документов
SELECT
  id,
  document_name,
  chunk_text,
  text_vector
FROM document_chunks
LIMIT 5;

-- Просмотр конкретного tsvector в удобном формате
SELECT id, chunk_text, array_agg(lexeme ORDER BY lexeme) as words
FROM (
  SELECT
    id,
    chunk_text,
    (each(text_vector)).key as lexeme
  FROM document_chunks
  LIMIT 1
) t
GROUP BY id, chunk_text;

-- ============================================================================
-- 4. ПРОСТОЙ ПОЛНОТЕКСТОВЫЙ ПОИСК
-- ============================================================================

-- Базовый поиск (используется в /api/search/keywords)
SELECT
  id,
  document_name,
  chunk_index,
  chunk_text,
  ts_rank(text_vector, query) as relevance
FROM document_chunks,
     plainto_tsquery('russian', 'машинное обучение') query
WHERE text_vector @@ query
ORDER BY relevance DESC
LIMIT 10;

-- Поиск в конкретном документе (используется в /api/search/keywords/document/{id})
SELECT
  id,
  chunk_text,
  ts_rank(text_vector, query) as relevance
FROM document_chunks,
     plainto_tsquery('russian', 'нейрон') query
WHERE text_vector @@ query
  AND document_id = 1  -- Замените на реальный ID документа
ORDER BY relevance DESC
LIMIT 5;

-- ============================================================================
-- 5. РАСШИРЕННЫЙ ПОИСК С ОПЕРАТОРАМИ
-- ============================================================================

-- Поиск с AND оператором (оба слова должны присутствовать)
SELECT
  id,
  chunk_text,
  ts_rank(text_vector, query) as relevance
FROM document_chunks,
     to_tsquery('russian', 'python & machine') query
WHERE text_vector @@ query
ORDER BY relevance DESC
LIMIT 10;

-- Поиск с OR оператором (одно из слов)
SELECT
  id,
  chunk_text,
  ts_rank(text_vector, query) as relevance
FROM document_chunks,
     to_tsquery('russian', 'python | java') query
WHERE text_vector @@ query
ORDER BY relevance DESC
LIMIT 10;

-- Поиск с NOT оператором (исключить слово)
SELECT
  id,
  chunk_text,
  ts_rank(text_vector, query) as relevance
FROM document_chunks,
     to_tsquery('russian', 'machine & !learning') query
WHERE text_vector @@ query
ORDER BY relevance DESC
LIMIT 10;

-- Комплексный поиск
-- (Python или Java) И (web OR api) И НЕ deprecated
SELECT
  id,
  document_name,
  chunk_text,
  ts_rank(text_vector, query) as relevance
FROM document_chunks,
     to_tsquery('russian', '(python | java) & (web | api) & !deprecated') query
WHERE text_vector @@ query
ORDER BY relevance DESC
LIMIT 10;

-- ============================================================================
-- 6. ПОИСК С РАСШИРЕННЫМ РАНЖИРОВАНИЕМ (ts_rank_cd)
-- ============================================================================

-- Использует более сложный алгоритм ранжирования
-- Флаг 32 = 1 (log TF) + 2 (IDF) + 4 (length) + 8 (cover density) + 16 (cover density)
SELECT
  id,
  document_name,
  chunk_text,
  ts_rank_cd(text_vector, query, 32) as relevance
FROM document_chunks,
     plainto_tsquery('russian', 'машинное обучение') query
WHERE text_vector @@ query
ORDER BY relevance DESC
LIMIT 10;

-- С разными флагами
SELECT
  id,
  chunk_text,
  ts_rank_cd(text_vector, query, 1) as rank_tfidf,
  ts_rank_cd(text_vector, query, 2) as rank_idf,
  ts_rank_cd(text_vector, query, 4) as rank_length,
  ts_rank_cd(text_vector, query, 32) as rank_full
FROM document_chunks,
     plainto_tsquery('russian', 'test') query
WHERE text_vector @@ query
LIMIT 5;

-- ============================================================================
-- 7. АНАЛИЗ МОРФОЛОГИИ
-- ============================================================================

-- Как PostgreSQL преобразует текст в tsvector
SELECT to_tsvector('russian', 'Машинное обучение');
-- Результат: 'маш':1 'обучен':2

-- Проверить, что разные формы одного слова преобразуются одинаково
SELECT
  to_tsvector('russian', 'обучение'),
  to_tsvector('russian', 'обучении'),
  to_tsvector('russian', 'обучением'),
  to_tsvector('russian', 'обучаются');
-- Все дают 'обучен':1

-- Проверить стоп-слова (они не индексируются)
SELECT to_tsvector('russian', 'это является и как в на');
-- Результат: пусто (все стоп-слова)

-- ============================================================================
-- 8. ТЕСТИРОВАНИЕ ПРОИЗВОДИТЕЛЬНОСТИ
-- ============================================================================

-- Включить timing
\timing

-- Тест: простой поиск
SELECT COUNT(*) FROM document_chunks
WHERE text_vector @@ plainto_tsquery('russian', 'test');

-- Тест: расширенный поиск
SELECT COUNT(*) FROM document_chunks
WHERE text_vector @@ to_tsquery('russian', 'test & (python | java)');

-- Тест: поиск без индекса (медленно!)
EXPLAIN ANALYZE
SELECT * FROM document_chunks
WHERE chunk_text ILIKE '%test%';

-- Тест: поиск с индексом (быстро!)
EXPLAIN ANALYZE
SELECT * FROM document_chunks
WHERE text_vector @@ plainto_tsquery('russian', 'test');

-- ============================================================================
-- 9. СТАТИСТИКА
-- ============================================================================

-- Количество документов
SELECT COUNT(*) as total_documents FROM documents;

-- Количество чанков
SELECT COUNT(*) as total_chunks FROM document_chunks;

-- Количество индексированных чанков
SELECT COUNT(*) as indexed_chunks FROM document_chunks WHERE text_vector IS NOT NULL;

-- Размер данных
SELECT
  pg_size_pretty(pg_total_relation_size('documents')) as documents_size,
  pg_size_pretty(pg_total_relation_size('document_chunks')) as chunks_size,
  pg_size_pretty(pg_relation_size('idx_document_chunks_text_vector')) as fts_index_size;

-- Распределение по документам
SELECT
  document_name,
  COUNT(*) as chunk_count,
  SUM(LENGTH(chunk_text)) as total_bytes,
  AVG(LENGTH(chunk_text)) as avg_chunk_size
FROM document_chunks
GROUP BY document_name
ORDER BY chunk_count DESC;

-- ============================================================================
-- 10. ОТЛАДКА ПРОБЛЕМ
-- ============================================================================

-- Проблема: не найдены результаты

-- Проверить, что документы загружены
SELECT COUNT(*) FROM document_chunks;

-- Проверить, что индекс создан
SELECT COUNT(*) FROM document_chunks WHERE text_vector IS NOT NULL;

-- Проверить, как преобразуется запрос
SELECT plainto_tsquery('russian', 'ваш запрос');
SELECT to_tsquery('russian', 'ваш & запрос');

-- Проверить, что есть совпадения в tsvector
SELECT * FROM document_chunks
WHERE chunk_text ILIKE '%ваше_слово%'
LIMIT 1;

-- ============================================================================
-- 11. ОБСЛУЖИВАНИЕ ИНДЕКСА
-- ============================================================================

-- Переиндексировать индекс FTS
REINDEX INDEX idx_document_chunks_text_vector;

-- Vacuum и Analyze для оптимизации
VACUUM ANALYZE document_chunks;

-- Проверить fragmentation индекса
SELECT
  schemaname,
  tablename,
  indexname,
  idx_scan,
  idx_tup_read,
  idx_tup_fetch
FROM pg_stat_user_indexes
WHERE indexname LIKE 'idx_document_chunks%';

-- ============================================================================
-- 12. ИНТЕГРАЦИОННЫЕ ТЕСТЫ
-- ============================================================================

-- Сценарий 1: Загрузить документ и найти в нём
-- 1. Создать документ
INSERT INTO documents (file_name, file_type, file_size, chunk_count, status)
VALUES ('test.pdf', 'PDF', 1024, 1, 'READY')
RETURNING id;  -- Запомнить ID (например, 999)

-- 2. Создать чанк с текстом
INSERT INTO document_chunks (document_id, document_name, chunk_index, chunk_text)
VALUES (999, 'test.pdf', 0, 'Это тестовый текст про машинное обучение и нейронные сети');

-- 3. Проверить, что индекс создался
SELECT text_vector FROM document_chunks WHERE document_id = 999;

-- 4. Выполнить поиск
SELECT * FROM document_chunks
WHERE document_id = 999
  AND text_vector @@ plainto_tsquery('russian', 'нейрон');

-- ============================================================================
-- 13. ПОЛЕЗНЫЕ VIEW'Ы (опционально)
-- ============================================================================

-- Создать view для более удобного поиска
CREATE OR REPLACE VIEW fts_search AS
SELECT
  dc.id as chunk_id,
  d.id as doc_id,
  d.file_name,
  dc.chunk_index,
  dc.chunk_text,
  ts_rank(dc.text_vector, query) as relevance,
  dc.created_at
FROM document_chunks dc
JOIN documents d ON dc.document_id = d.id,
     plainto_tsquery('russian', 'search_query') query  -- Заменить на параметр
WHERE dc.text_vector @@ query
ORDER BY relevance DESC;

-- ============================================================================
-- 14. ПРИМЕРЫ РЕАЛЬНЫХ ЗАПРОСОВ
-- ============================================================================

-- Пример 1: Найти все про Python в документах
SELECT
  DISTINCT document_name,
  COUNT(*) as occurrences,
  SUM(LENGTH(chunk_text)) as total_text_size
FROM document_chunks
WHERE text_vector @@ plainto_tsquery('russian', 'python')
GROUP BY document_name
ORDER BY occurrences DESC;

-- Пример 2: Найти редкие слова (появляются в 1-2 документах)
SELECT
  (each(text_vector)).key as word,
  COUNT(DISTINCT document_id) as doc_count,
  COUNT(*) as total_occurrences
FROM document_chunks
GROUP BY (each(text_vector)).key
HAVING COUNT(DISTINCT document_id) <= 2
ORDER BY doc_count, total_occurrences DESC
LIMIT 20;

-- Пример 3: Найти самые "тяжелые" чанки по количеству слов
SELECT
  document_name,
  chunk_index,
  array_length(avals(text_vector), 1) as word_count,
  LENGTH(chunk_text) as char_count,
  chunk_text::text | ' ...}'::text as preview
FROM document_chunks
ORDER BY word_count DESC
LIMIT 10;

-- ============================================================================
-- КОНЕЦ
-- ============================================================================

-- Для отключения timing
\timing off

-- Примечания:
-- 1. Замените 'russian' на другой язык если нужно
-- 2. Используйте EXPLAIN ANALYZE для анализа производительности
-- 3. Проверьте статистику индекса регулярно
-- 4. Запускайте VACUUM ANALYZE периодически

