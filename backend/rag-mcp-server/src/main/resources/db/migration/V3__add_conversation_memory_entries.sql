-- Flyway Migration: V3__add_conversation_memory_entries
-- Purpose: Add memory_entries table for OpenRouter conversation history persistence
-- Date: 2026-01-10
-- Author: GitHub Copilot

-- Create memory_entries table for storing conversation messages
CREATE TABLE memory_entries (
    id BIGSERIAL PRIMARY KEY,

    -- Identifiers (indexed for fast lookups)
    conversation_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255),

    -- Message content
    role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,

    -- Timestamps
    timestamp TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Model information
    model VARCHAR(255),

    -- Token metrics
    input_tokens INTEGER,
    output_tokens INTEGER,
    total_tokens INTEGER,

    -- Cost tracking
    cost DECIMAL(10, 6),

    -- Compression flags
    is_compressed BOOLEAN NOT NULL DEFAULT FALSE,
    response_time_ms BIGINT,
    compressed_messages_count INTEGER,
    compression_timestamp TIMESTAMP,

    -- Metadata
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Create indexes for fast queries
CREATE INDEX idx_memory_entries_conversation_id
    ON memory_entries(conversation_id);

CREATE INDEX idx_memory_entries_user_id
    ON memory_entries(user_id);

CREATE INDEX idx_memory_entries_timestamp
    ON memory_entries(timestamp);

CREATE INDEX idx_memory_entries_conversation_timestamp
    ON memory_entries(conversation_id, timestamp);

CREATE INDEX idx_memory_entries_conversation_is_compressed
    ON memory_entries(conversation_id, is_compressed);

-- Create trigger for auto-updating updated_at
CREATE TRIGGER trigger_update_memory_entries_updated_at
    BEFORE UPDATE ON memory_entries
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

