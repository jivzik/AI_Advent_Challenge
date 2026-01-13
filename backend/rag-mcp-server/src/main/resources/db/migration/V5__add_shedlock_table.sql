-- Flyway Migration: V5__add_shedlock_table
-- Purpose: Add shedlock table for distributed scheduler locks
-- Date: 2026-01-13

CREATE TABLE IF NOT EXISTS shedlock (
                                        name VARCHAR(64) NOT NULL PRIMARY KEY,
                                        lock_until TIMESTAMP NOT NULL,
                                        locked_at TIMESTAMP NOT NULL,
                                        locked_by VARCHAR(255) NOT NULL
);

-- Log migration
DO $$
    BEGIN
        RAISE NOTICE '✅ Created shedlock table for distributed locks';
    END $$;