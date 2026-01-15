-- Add metadata column to documents table
-- This allows storing custom metadata (tags, categories, source, etc.) with each document

ALTER TABLE documents
ADD COLUMN IF NOT EXISTS metadata JSONB;

-- Create GIN index for efficient JSONB queries
CREATE INDEX IF NOT EXISTS idx_documents_metadata
ON documents USING GIN (metadata);

-- Comment explaining the column
COMMENT ON COLUMN documents.metadata IS 'Custom metadata in JSONB format for flexible document categorization and tagging';

