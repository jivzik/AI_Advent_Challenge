-- Таблица профилей пользователей
CREATE TABLE user_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100) UNIQUE NOT NULL,
    name VARCHAR(200),
    expertise_level VARCHAR(50),
    preferred_language VARCHAR(10),
    timezone VARCHAR(50),
    tech_stack JSONB,
    coding_style JSONB,
    communication_preferences JSONB,
    work_style JSONB,
    recent_projects JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_user_profiles_user_id ON user_profiles(user_id);
-- Таблица взаимодействий
CREATE TABLE user_interactions (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    query TEXT NOT NULL,
    query_type VARCHAR(50),
    response TEXT,
    feedback INTEGER DEFAULT 0,
    context JSONB,
    processing_time_ms INTEGER,
    tokens_used INTEGER,
    model VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_user_interactions_user_id ON user_interactions(user_id, created_at DESC);
CREATE INDEX idx_user_interactions_query_type ON user_interactions(query_type);
CREATE INDEX idx_user_interactions_feedback ON user_interactions(user_id, feedback);
-- Таблица памяти агента
CREATE TABLE agent_memory (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    memory_type VARCHAR(50) NOT NULL,
    key VARCHAR(200) NOT NULL,
    value TEXT,
    confidence DOUBLE PRECISION DEFAULT 0.5,
    usage_count INTEGER DEFAULT 1,
    last_used TIMESTAMP DEFAULT NOW(),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_agent_memory_user_id ON agent_memory(user_id, memory_type);
CREATE INDEX idx_agent_memory_key ON agent_memory(user_id, key);
CREATE INDEX idx_agent_memory_confidence ON agent_memory(confidence DESC);
-- Комментарии
COMMENT ON TABLE user_profiles IS 'Профили пользователей с предпочтениями';
COMMENT ON TABLE user_interactions IS 'История взаимодействий для обучения';
COMMENT ON TABLE agent_memory IS 'Долговременная память агента';
