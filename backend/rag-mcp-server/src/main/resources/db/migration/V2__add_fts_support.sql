-- Add Full-Text Search (FTS) support to document_chunks table

-- 1. Create unaccent extension (for handling Russian accents and special characters)
CREATE EXTENSION IF NOT EXISTS unaccent;

-- 2. Add text_vector column to document_chunks table for FTS
ALTER TABLE IF EXISTS document_chunks
ADD COLUMN IF NOT EXISTS text_vector tsvector GENERATED ALWAYS AS (
    -- Use Russian language configuration for proper morphological analysis
    -- tsvector converts text to normalized tokens (lemmas)
    to_tsvector('russian', COALESCE(chunk_text, ''))
) STORED;

-- 3. Create GIN index on text_vector for fast keyword search
-- GIN (Generalized Inverted Index) is optimal for tsvector
CREATE INDEX IF NOT EXISTS idx_document_chunks_text_vector
ON document_chunks USING gin(text_vector);

-- 4. Create composite index for searching by document and text
CREATE INDEX IF NOT EXISTS idx_document_chunks_doc_id_text_vector
ON document_chunks(document_id) INCLUDE (text_vector);

-- 5. Log that FTS was added
DO $$
BEGIN
    RAISE NOTICE '✅ Full-Text Search (FTS) support added to document_chunks table';
END $$;

