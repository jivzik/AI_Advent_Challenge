/*-- ============================================
-- Инициализация PostgreSQL с pgvector для RAG
-- ============================================

-- Создаём расширение pgvector
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================
-- RAG Tables (для rag-mcp-server)
-- ============================================

-- Таблица для хранения документов
CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(500) NOT NULL,
    file_type VARCHAR(50),
    file_size BIGINT,
    chunk_count INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'PROCESSING',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Таблица для хранения чанков с эмбеддингами
CREATE TABLE IF NOT EXISTS document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT REFERENCES documents(id) ON DELETE CASCADE,
    document_name VARCHAR(500),
    chunk_index INTEGER,
    chunk_text TEXT NOT NULL,
    embedding vector(4096),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Note: No vector index because pgvector limits indexes to 2000 dimensions
-- Using 4096 dimensions for qwen3-embedding-8b model
-- Similarity search will use sequential scans

-- Индекс для поиска по document_id
CREATE INDEX IF NOT EXISTS idx_document_chunks_document_id
ON document_chunks(document_id);

-- Индекс для поиска по имени документа
CREATE INDEX IF NOT EXISTS idx_document_chunks_document_name
ON document_chunks(document_name);

-- Функция для обновления updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Триггер для автообновления updated_at
DROP TRIGGER IF EXISTS update_documents_updated_at ON documents;
CREATE TRIGGER update_documents_updated_at
    BEFORE UPDATE ON documents
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Проверка что pgvector установлен
DO $$
BEGIN
    RAISE NOTICE '✅ pgvector extension installed successfully';
    RAISE NOTICE '✅ RAG tables created: documents, document_chunks';
END $$;

*/