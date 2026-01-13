-- Flyway Migration: V4__add_pr_reviews_table
-- Purpose: Add pr_reviews table for tracking code review status
-- Date: 2026-01-13
-- Author: AI Code Review System

-- Create pr_reviews table for deduplication and tracking
CREATE TABLE pr_reviews (
                            id BIGSERIAL PRIMARY KEY,

    -- PR identification
                            pr_number INTEGER NOT NULL,
                            repository VARCHAR(500) NOT NULL,

    -- Git commits (for deduplication)
                            base_sha VARCHAR(40) NOT NULL,
                            head_sha VARCHAR(40) NOT NULL,

    -- PR metadata
                            pr_title VARCHAR(1000),
                            pr_author VARCHAR(255),
                            base_branch VARCHAR(255),
                            head_branch VARCHAR(255),

    -- Review metadata
                            agent_name VARCHAR(100) DEFAULT 'CodeReviewAgent',
                            agent_version VARCHAR(50) DEFAULT '1.0.0',

    -- Review timing
                            reviewed_at TIMESTAMP NOT NULL DEFAULT NOW(),
                            review_time_ms BIGINT,

    -- Review results
                            decision VARCHAR(50), -- APPROVE, REQUEST_CHANGES, COMMENT
                            summary TEXT,
                            total_issues INTEGER DEFAULT 0,
                            critical_issues INTEGER DEFAULT 0,
                            high_issues INTEGER DEFAULT 0,
                            medium_issues INTEGER DEFAULT 0,
                            low_issues INTEGER DEFAULT 0,

    -- File reference
                            report_file_path VARCHAR(500),

    -- GitHub integration
                            posted_to_github BOOLEAN DEFAULT FALSE,
                            github_review_url VARCHAR(1000),

    -- Status tracking
                            status VARCHAR(50) DEFAULT 'COMPLETED', -- PENDING, PROCESSING, COMPLETED, FAILED
                            error_message TEXT,

    -- Timestamps
                            created_at TIMESTAMP DEFAULT NOW(),
                            updated_at TIMESTAMP DEFAULT NOW(),

    -- Unique constraint: same PR + same commit = same review
                            CONSTRAINT unique_pr_review UNIQUE (pr_number, head_sha, agent_name)
);

-- Indexes for fast queries
CREATE INDEX idx_pr_reviews_pr_number
    ON pr_reviews(pr_number);

CREATE INDEX idx_pr_reviews_repository
    ON pr_reviews(repository);

CREATE INDEX idx_pr_reviews_head_sha
    ON pr_reviews(head_sha);

CREATE INDEX idx_pr_reviews_reviewed_at
    ON pr_reviews(reviewed_at DESC);

CREATE INDEX idx_pr_reviews_status
    ON pr_reviews(status);

CREATE INDEX idx_pr_reviews_agent
    ON pr_reviews(agent_name);

-- Composite index for checking if already reviewed
CREATE INDEX idx_pr_reviews_dedup
    ON pr_reviews(pr_number, head_sha, agent_name);

-- Trigger for auto-updating updated_at
CREATE TRIGGER trigger_update_pr_reviews_updated_at
    BEFORE UPDATE ON pr_reviews
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Log migration
DO $$
    BEGIN
        RAISE NOTICE '✅ Created pr_reviews table for code review tracking';
    END $$;