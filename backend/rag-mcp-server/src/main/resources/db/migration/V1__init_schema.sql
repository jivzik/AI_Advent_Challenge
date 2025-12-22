-- Create pgvector extension first
CREATE EXTENSION IF NOT EXISTS vector;

-- Table for storing documents
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

-- Table for storing document chunks with embeddings
-- Using 4096 dimensions for qwen3-embedding-8b model
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
-- With 4096-dimensional embeddings, similarity search uses sequential scans
-- For production with large datasets, consider using smaller embedding model or external search

-- Index for searching by document_id
CREATE INDEX IF NOT EXISTS idx_document_chunks_document_id
ON document_chunks(document_id);

-- Index for searching by document name
CREATE INDEX IF NOT EXISTS idx_document_chunks_document_name
ON document_chunks(document_name);

-- Function for updating updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger for auto-updating updated_at
DROP TRIGGER IF EXISTS update_documents_updated_at ON documents;
CREATE TRIGGER update_documents_updated_at
    BEFORE UPDATE ON documents
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
