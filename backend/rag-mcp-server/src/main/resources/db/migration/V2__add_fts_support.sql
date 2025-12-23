-- Fix FTS to support multilingual documents (English, Russian, German)

-- 1. Drop the old text_vector column
ALTER TABLE document_chunks DROP COLUMN IF EXISTS text_vector;

-- 2. Recreate with 'simple' configuration for multilingual support
-- 'simple' works with ANY language without morphology issues
ALTER TABLE document_chunks
    ADD COLUMN text_vector tsvector GENERATED ALWAYS AS (
        to_tsvector('simple', COALESCE(chunk_text, ''))
        ) STORED;

-- 3. Recreate GIN index
DROP INDEX IF EXISTS idx_document_chunks_text_vector;
CREATE INDEX idx_document_chunks_text_vector
    ON document_chunks USING gin(text_vector);

-- 4. Recreate composite index
DROP INDEX IF EXISTS idx_document_chunks_doc_id_text_vector;
CREATE INDEX idx_document_chunks_doc_id_text_vector
    ON document_chunks(document_id) INCLUDE (text_vector);

-- 5. Log the change
DO $$
    BEGIN
        RAISE NOTICE '✅ Fixed FTS to support multilingual documents (simple config)';
    END $$;