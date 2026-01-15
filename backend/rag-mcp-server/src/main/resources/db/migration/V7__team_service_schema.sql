-- V7__team_service_schema.sql
-- Team Service: AI Assistant for Development Team

-- ============================================================================
-- Team Members Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS team_members (
                                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                            email VARCHAR(255) NOT NULL UNIQUE,
                                            full_name VARCHAR(255) NOT NULL,
                                            role VARCHAR(100), -- developer, qa, pm, designer, devops
                                            team VARCHAR(100), -- backend, frontend, mobile, etc
                                            is_active BOOLEAN DEFAULT true,
                                            skills TEXT, -- JSON array or comma-separated
                                            preferred_language VARCHAR(10) DEFAULT 'en',
                                            ai_enabled BOOLEAN DEFAULT true,
                                            total_tasks_completed INTEGER DEFAULT 0,
                                            total_tasks_active INTEGER DEFAULT 0,
                                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                            last_active_at TIMESTAMP
);

CREATE INDEX idx_team_members_email ON team_members(email);
CREATE INDEX idx_team_members_active ON team_members(is_active);
CREATE INDEX idx_team_members_team ON team_members(team);

-- ============================================================================
-- Query Logs Table
-- ============================================================================

CREATE TABLE IF NOT EXISTS query_logs (
                                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                          team_member_id UUID REFERENCES team_members(id) ON DELETE SET NULL,
                                          query TEXT NOT NULL,
                                          answer TEXT,
                                          query_type VARCHAR(50), -- SHOW_TASKS, CREATE_TASK, ANALYZE_PRIORITY, etc
                                          tools_used TEXT[], -- Array of tool names
                                          rag_sources TEXT[], -- Array of document names
                                          actions_performed TEXT[], -- Array of actions (task_created, etc)
                                          response_time_ms INTEGER,
                                          token_count INTEGER,
                                          confidence_score NUMERIC(3,2),
                                          user_feedback BOOLEAN, -- true = helpful, false = not helpful
                                          feedback_comment TEXT,
                                          session_id VARCHAR(255),
                                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_query_logs_team_member ON query_logs(team_member_id);
CREATE INDEX idx_query_logs_created_at ON query_logs(created_at DESC);
CREATE INDEX idx_query_logs_session_id ON query_logs(session_id);
CREATE INDEX idx_query_logs_query_type ON query_logs(query_type);

-- ============================================================================
-- Insert Test Data
-- ============================================================================

-- Test team members
INSERT INTO team_members (email, full_name, role, team, skills) VALUES
                                                                    ('john.dev@company.com', 'John Developer', 'developer', 'backend', 'Java,Spring Boot,PostgreSQL'),
                                                                    ('alice.qa@company.com', 'Alice QA', 'qa', 'qa', 'Testing,Automation,Selenium'),
                                                                    ('bob.pm@company.com', 'Bob PM', 'pm', 'management', 'Agile,Scrum,Project Management')
ON CONFLICT (email) DO NOTHING;

-- ============================================================================
-- Views for Analytics
-- ============================================================================

-- Active queries by team member
CREATE OR REPLACE VIEW v_queries_by_member AS
SELECT
    tm.id,
    tm.full_name,
    tm.email,
    tm.team,
    COUNT(ql.id) as total_queries,
    COUNT(CASE WHEN ql.user_feedback = true THEN 1 END) as helpful_queries,
    AVG(ql.response_time_ms) as avg_response_time_ms,
    AVG(ql.confidence_score) as avg_confidence_score,
    MAX(ql.created_at) as last_query_at
FROM team_members tm
         LEFT JOIN query_logs ql ON ql.team_member_id = tm.id
GROUP BY tm.id, tm.full_name, tm.email, tm.team;

-- Query types distribution
CREATE OR REPLACE VIEW v_query_types_stats AS
SELECT
    query_type,
    COUNT(*) as count,
    AVG(response_time_ms) as avg_response_time_ms,
    AVG(confidence_score) as avg_confidence_score,
    COUNT(CASE WHEN user_feedback = true THEN 1 END)::FLOAT / NULLIF(COUNT(CASE WHEN user_feedback IS NOT NULL THEN 1 END), 0) as helpful_ratio
FROM query_logs
WHERE created_at > CURRENT_DATE - INTERVAL '30 days'
GROUP BY query_type
ORDER BY count DESC;

-- Tools usage stats
CREATE OR REPLACE VIEW v_tools_usage_stats AS
SELECT
    unnest(tools_used) as tool_name,
    COUNT(*) as usage_count,
    AVG(response_time_ms) as avg_response_time_ms
FROM query_logs
WHERE tools_used IS NOT NULL
  AND array_length(tools_used, 1) > 0
  AND created_at > CURRENT_DATE - INTERVAL '30 days'
GROUP BY tool_name
ORDER BY usage_count DESC;

-- ============================================================================
-- Trigger to update team member's last_active_at
-- ============================================================================

CREATE OR REPLACE FUNCTION update_team_member_last_active()
    RETURNS TRIGGER AS $$
BEGIN
    UPDATE team_members
    SET last_active_at = NEW.created_at
    WHERE id = NEW.team_member_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_last_active
    AFTER INSERT ON query_logs
    FOR EACH ROW
EXECUTE FUNCTION update_team_member_last_active();

COMMENT ON TABLE team_members IS 'Development team members who use the AI assistant';
COMMENT ON TABLE query_logs IS 'Log of all queries to the Team Assistant AI';
COMMENT ON VIEW v_queries_by_member IS 'Query statistics grouped by team member';
COMMENT ON VIEW v_query_types_stats IS 'Statistics for different query types';
COMMENT ON VIEW v_tools_usage_stats IS 'MCP tools usage statistics';