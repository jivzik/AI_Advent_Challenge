-- ================================================
-- Flyway Migration V2: Support Service Schema
-- Description: Tables for customer support system with tickets and RAG integration
-- ================================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ================================================
-- Table: support_users
-- Stores B2B WebShop customers
-- ================================================
CREATE TABLE IF NOT EXISTS support_users (
                                             id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),

    -- Company information
                                             company_name VARCHAR(255) NOT NULL,
                                             company_inn VARCHAR(12) NOT NULL UNIQUE,
                                             company_ogrn VARCHAR(13),

    -- Contact information
                                             email VARCHAR(255) NOT NULL UNIQUE,
                                             phone VARCHAR(20),

    -- User details
                                             full_name VARCHAR(255) NOT NULL,
                                             role VARCHAR(50) DEFAULT 'user', -- admin, manager, user, viewer

    -- Account status
                                             is_verified BOOLEAN DEFAULT FALSE,
                                             is_active BOOLEAN DEFAULT TRUE,
                                             verification_date TIMESTAMP,

    -- Business metrics
                                             total_orders INTEGER DEFAULT 0,
                                             total_spent DECIMAL(15, 2) DEFAULT 0.00,
                                             loyalty_tier VARCHAR(20) DEFAULT 'bronze', -- bronze, silver, gold, platinum

    -- Support metrics
                                             total_tickets INTEGER DEFAULT 0,
                                             open_tickets INTEGER DEFAULT 0,

    -- Timestamps
                                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                             updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                             last_login TIMESTAMP
);

-- ================================================
-- Table: support_tickets
-- Stores customer support requests
-- ================================================
CREATE TABLE IF NOT EXISTS support_tickets (
                                               id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                               ticket_number VARCHAR(20) UNIQUE NOT NULL, -- e.g., "TICK-2026-0001"

    -- User reference
                                               user_id UUID NOT NULL REFERENCES support_users(id) ON DELETE CASCADE,

    -- Ticket details
                                               subject VARCHAR(500) NOT NULL,
                                               description TEXT NOT NULL,
                                               category VARCHAR(100) NOT NULL, -- auth, catalog, order, payment, delivery, billing, api, other
                                               priority VARCHAR(20) DEFAULT 'medium', -- low, medium, high, critical
                                               status VARCHAR(50) DEFAULT 'open', -- open, in_progress, waiting_customer, resolved, closed

    -- Assignment
                                               assigned_to VARCHAR(100), -- support agent name/email
                                               assigned_at TIMESTAMP,

    -- Context (для RAG)
                                               order_id VARCHAR(50), -- related order number
                                               product_id VARCHAR(50), -- related product ID
                                               error_code VARCHAR(50), -- if technical issue

    -- Resolution
                                               resolution TEXT,
                                               resolved_at TIMESTAMP,
                                               resolved_by VARCHAR(100),

    -- Satisfaction
                                               satisfaction_rating INTEGER CHECK (satisfaction_rating >= 1 AND satisfaction_rating <= 5),
                                               satisfaction_comment TEXT,

    -- SLA tracking
                                               first_response_at TIMESTAMP,
                                               first_response_time_minutes INTEGER,
                                               resolution_time_minutes INTEGER,
                                               sla_breached BOOLEAN DEFAULT FALSE,

    -- Timestamps
                                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                               updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                               closed_at TIMESTAMP
);

-- ================================================
-- Table: ticket_messages
-- Stores conversation history for each ticket
-- ================================================
CREATE TABLE IF NOT EXISTS ticket_messages (
                                               id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                               ticket_id UUID NOT NULL REFERENCES support_tickets(id) ON DELETE CASCADE,

    -- Message details
                                               sender_type VARCHAR(20) NOT NULL, -- customer, agent, system, ai
                                               sender_id VARCHAR(100) NOT NULL, -- user_id or agent email
                                               sender_name VARCHAR(255) NOT NULL,

                                               message TEXT NOT NULL,

    -- AI context (если ответ от AI)
                                               is_ai_generated BOOLEAN DEFAULT FALSE,
                                               rag_sources TEXT[], -- array of document IDs used
                                               confidence_score DECIMAL(3, 2), -- 0.00-1.00

    -- Attachments
                                               attachments JSONB, -- [{filename, url, size}]

    -- Metadata
                                               is_internal BOOLEAN DEFAULT FALSE, -- internal notes invisible to customer

                                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ================================================
-- Table: support_categories
-- Predefined ticket categories with SLA
-- ================================================
CREATE TABLE IF NOT EXISTS support_categories (
                                                  id SERIAL PRIMARY KEY,
                                                  name VARCHAR(100) NOT NULL UNIQUE,
                                                  description TEXT,

    -- SLA (Service Level Agreement) in minutes
                                                  sla_first_response INTEGER NOT NULL, -- minutes
                                                  sla_resolution INTEGER NOT NULL, -- minutes

    -- Auto-assignment rules
                                                  auto_assign_to VARCHAR(100), -- team or agent email
                                                  requires_escalation BOOLEAN DEFAULT FALSE,

    -- FAQ references
                                                  faq_document_ids TEXT[], -- related FAQ sections

                                                  is_active BOOLEAN DEFAULT TRUE,
                                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ================================================
-- Table: ticket_tags
-- Flexible tagging system for tickets
-- ================================================
CREATE TABLE IF NOT EXISTS ticket_tags (
                                           ticket_id UUID NOT NULL REFERENCES support_tickets(id) ON DELETE CASCADE,
                                           tag VARCHAR(50) NOT NULL,
                                           added_by VARCHAR(100),
                                           added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

                                           PRIMARY KEY (ticket_id, tag)
);

-- ================================================
-- Indexes for performance
-- ================================================

-- Support users indexes
CREATE INDEX idx_support_users_email ON support_users(email);
CREATE INDEX idx_support_users_inn ON support_users(company_inn);
CREATE INDEX idx_support_users_active ON support_users(is_active);
CREATE INDEX idx_support_users_open_tickets ON support_users(open_tickets);

-- Support tickets indexes
CREATE INDEX idx_support_tickets_user_id ON support_tickets(user_id);
CREATE INDEX idx_support_tickets_status ON support_tickets(status);
CREATE INDEX idx_support_tickets_priority ON support_tickets(priority);
CREATE INDEX idx_support_tickets_category ON support_tickets(category);
CREATE INDEX idx_support_tickets_created_at ON support_tickets(created_at DESC);
CREATE INDEX idx_support_tickets_assigned_to ON support_tickets(assigned_to);
CREATE INDEX idx_support_tickets_order_id ON support_tickets(order_id);

-- Ticket messages indexes
CREATE INDEX idx_ticket_messages_ticket_id ON ticket_messages(ticket_id);
CREATE INDEX idx_ticket_messages_created_at ON ticket_messages(created_at DESC);

-- Ticket tags indexes
CREATE INDEX idx_ticket_tags_tag ON ticket_tags(tag);

-- ================================================
-- Full-text search indexes (PostgreSQL FTS)
-- ================================================
ALTER TABLE support_tickets ADD COLUMN search_vector tsvector;

CREATE INDEX idx_support_tickets_search_vector
    ON support_tickets USING gin(search_vector);

-- Function to update search vector
CREATE OR REPLACE FUNCTION support_tickets_search_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
            setweight(to_tsvector('simple', COALESCE(NEW.subject, '')), 'A') ||
            setweight(to_tsvector('simple', COALESCE(NEW.description, '')), 'B') ||
            setweight(to_tsvector('simple', COALESCE(NEW.category, '')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to auto-update search vector
CREATE TRIGGER trigger_support_tickets_search_update
    BEFORE INSERT OR UPDATE ON support_tickets
    FOR EACH ROW EXECUTE FUNCTION support_tickets_search_update();

-- ================================================
-- Triggers for automatic updates
-- ================================================

-- Reuse existing update_updated_at_column function from V1
-- Only create if it doesn't exist
DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'update_updated_at_column') THEN
            CREATE FUNCTION update_updated_at_column()
                RETURNS TRIGGER AS $func$
            BEGIN
                NEW.updated_at = CURRENT_TIMESTAMP;
                RETURN NEW;
            END;
            $func$ LANGUAGE plpgsql;
        END IF;
    END $$;

CREATE TRIGGER update_support_users_updated_at
    BEFORE UPDATE ON support_users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_support_tickets_updated_at
    BEFORE UPDATE ON support_tickets
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Update user's open_tickets counter
CREATE OR REPLACE FUNCTION update_user_tickets_count()
    RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE support_users
        SET total_tickets = total_tickets + 1,
            open_tickets = open_tickets + 1
        WHERE id = NEW.user_id;
    ELSIF TG_OP = 'UPDATE' THEN
        IF OLD.status IN ('open', 'in_progress', 'waiting_customer')
            AND NEW.status IN ('resolved', 'closed') THEN
            UPDATE support_users
            SET open_tickets = open_tickets - 1
            WHERE id = NEW.user_id;
        ELSIF OLD.status IN ('resolved', 'closed')
            AND NEW.status IN ('open', 'in_progress', 'waiting_customer') THEN
            UPDATE support_users
            SET open_tickets = open_tickets + 1
            WHERE id = NEW.user_id;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_user_tickets_count
    AFTER INSERT OR UPDATE ON support_tickets
    FOR EACH ROW EXECUTE FUNCTION update_user_tickets_count();

-- Calculate first response time
CREATE OR REPLACE FUNCTION calculate_first_response_time()
    RETURNS TRIGGER AS $$
BEGIN
    IF NEW.sender_type IN ('agent', 'ai') THEN
        UPDATE support_tickets
        SET first_response_at = NEW.created_at,
            first_response_time_minutes = EXTRACT(EPOCH FROM (NEW.created_at - support_tickets.created_at)) / 60
        WHERE id = NEW.ticket_id
          AND first_response_at IS NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_calculate_first_response_time
    AFTER INSERT ON ticket_messages
    FOR EACH ROW EXECUTE FUNCTION calculate_first_response_time();

-- ================================================
-- Initial data: Support categories
-- ================================================
INSERT INTO support_categories (name, description, sla_first_response, sla_resolution, auto_assign_to, faq_document_ids) VALUES
                                                                                                                             ('auth', 'Проблемы с авторизацией и доступом', 15, 240, 'tech-team', ARRAY['auth-section']),
                                                                                                                             ('catalog', 'Вопросы по каталогу и товарам', 30, 480, 'product-team', ARRAY['catalog-section']),
                                                                                                                             ('order', 'Вопросы по заказам', 15, 240, 'order-team', ARRAY['order-section']),
                                                                                                                             ('payment', 'Проблемы с оплатой', 15, 240, 'billing-team', ARRAY['billing-section']),
                                                                                                                             ('delivery', 'Вопросы по доставке', 30, 480, 'logistics-team', ARRAY['delivery-section']),
                                                                                                                             ('billing', 'Биллинг и документы', 60, 1440, 'billing-team', ARRAY['billing-section']),
                                                                                                                             ('api', 'Вопросы по API', 60, 480, 'tech-team', ARRAY['api-section']),
                                                                                                                             ('return', 'Возвраты и обмен', 30, 720, 'return-team', ARRAY['return-section']),
                                                                                                                             ('other', 'Прочие вопросы', 60, 720, 'general-support', ARRAY[]::text[])
ON CONFLICT (name) DO NOTHING;

-- ================================================
-- Sample data for testing
-- ================================================

-- Insert test users
INSERT INTO support_users (
    company_name, company_inn, company_ogrn, email, phone, full_name, role,
    is_verified, is_active, total_orders, total_spent, loyalty_tier
) VALUES
      ('ООО "Рога и Копыта"', '7707123456', '1027700000001', 'ceo@rogaikopyta.ru', '+79161234567',
       'Иванов Иван Иванович', 'admin', TRUE, TRUE, 42, 1500000.00, 'gold'),
      ('ИП Петров', '773801001', NULL, 'petrov@business.ru', '+79169876543',
       'Петров Петр Петрович', 'user', TRUE, TRUE, 5, 75000.00, 'bronze'),
      ('ООО "МегаСтрой"', '7704567890', '1027704000002', 'zakaz@megastroy.ru', '+74951234567',
       'Сидорова Анна Сергеевна', 'manager', TRUE, TRUE, 120, 5000000.00, 'platinum')
ON CONFLICT (email) DO NOTHING;

-- Insert test tickets
DO $$
    DECLARE
        user1_id UUID;
        user2_id UUID;
        ticket1_id UUID;
        ticket2_id UUID;
        ticket3_id UUID;
    BEGIN
        -- Get user IDs
        SELECT id INTO user1_id FROM support_users WHERE email = 'ceo@rogaikopyta.ru';
        SELECT id INTO user2_id FROM support_users WHERE email = 'petrov@business.ru';

        -- Skip if users don't exist
        IF user1_id IS NULL OR user2_id IS NULL THEN
            RAISE NOTICE 'Test users not found, skipping ticket creation';
            RETURN;
        END IF;

        -- Insert ticket 1
        INSERT INTO support_tickets (
            ticket_number, user_id, subject, description, category, priority, status, order_id
        ) VALUES (
                     'TICK-2026-0001', user1_id, 'Не могу войти в личный кабинет',
                     'После обновления пароля система не пускает. Пишет "Неверный логин или пароль", хотя точно ввожу правильно. Пробовал в Chrome и Firefox.',
                     'auth', 'high', 'open', NULL
                 ) RETURNING id INTO ticket1_id;

        -- Insert ticket 2
        INSERT INTO support_tickets (
            ticket_number, user_id, subject, description, category, priority, status, order_id
        ) VALUES (
                     'TICK-2026-0002', user2_id, 'Не отображается цена на товар артикул БВ-12345',
                     'В каталоге у товара "Болт высокопрочный М12" не показана цена. Написано "Цена по запросу". Раньше была цена 15.50 руб.',
                     'catalog', 'medium', 'in_progress', NULL
                 ) RETURNING id INTO ticket2_id;

        -- Insert ticket 3
        INSERT INTO support_tickets (
            ticket_number, user_id, subject, description, category, priority, status, order_id
        ) VALUES (
                     'TICK-2026-0003', user1_id, 'Где счет на заказ №ORD-2026-1234?',
                     'Оформили заказ вчера, статус "Подтвержден", но счет на email не пришел. Проверял спам - нет.',
                     'order', 'high', 'open', 'ORD-2026-1234'
                 ) RETURNING id INTO ticket3_id;

        -- Insert messages for first ticket
        IF ticket1_id IS NOT NULL THEN
            INSERT INTO ticket_messages (ticket_id, sender_type, sender_id, sender_name, message) VALUES
                                                                                                      (ticket1_id, 'customer', user1_id::TEXT, 'Иванов Иван Иванович',
                                                                                                       'После обновления пароля система не пускает. Пишет "Неверный логин или пароль"'),
                                                                                                      (ticket1_id, 'ai', 'support-ai', 'AI Assistant',
                                                                                                       'Здравствуйте! Судя по вашему описанию, проблема может быть связана с кэшем браузера. Попробуйте: 1) Очистить кэш (Ctrl+Shift+Del), 2) Войти в режиме инкогнито, 3) Проверить раскладку клавиатуры. Если не поможет - сообщите, я передам тикет технической поддержке.');

            -- Add tags
            INSERT INTO ticket_tags (ticket_id, tag, added_by) VALUES
                                                                   (ticket1_id, 'login-issue', 'system'),
                                                                   (ticket1_id, 'urgent', 'system');
        END IF;
    END $$;

-- ================================================
-- Views for analytics
-- ================================================

-- Ticket statistics by category
CREATE OR REPLACE VIEW v_ticket_stats_by_category AS
SELECT
    category,
    COUNT(*) as total_tickets,
    COUNT(CASE WHEN status IN ('open', 'in_progress') THEN 1 END) as open_tickets,
    COUNT(CASE WHEN status = 'resolved' THEN 1 END) as resolved_tickets,
    AVG(resolution_time_minutes) as avg_resolution_time,
    AVG(first_response_time_minutes) as avg_first_response_time,
    COUNT(CASE WHEN sla_breached THEN 1 END) as sla_breaches
FROM support_tickets
GROUP BY category;

-- Top customers by ticket volume
CREATE OR REPLACE VIEW v_top_customers_by_tickets AS
SELECT
    u.id,
    u.company_name,
    u.email,
    u.total_tickets,
    u.open_tickets,
    u.loyalty_tier,
    COUNT(t.id) as recent_tickets_30d
FROM support_users u
         LEFT JOIN support_tickets t ON u.id = t.user_id AND t.created_at > NOW() - INTERVAL '30 days'
GROUP BY u.id, u.company_name, u.email, u.total_tickets, u.open_tickets, u.loyalty_tier
ORDER BY u.total_tickets DESC;

-- ================================================
-- Functions for support operations
-- ================================================

-- Function to get ticket context for RAG
CREATE OR REPLACE FUNCTION get_ticket_context(p_ticket_id UUID)
    RETURNS JSON AS $$
DECLARE
    result JSON;
BEGIN
    SELECT json_build_object(
                   'ticket', json_build_object(
                    'id', t.id,
                    'number', t.ticket_number,
                    'subject', t.subject,
                    'description', t.description,
                    'category', t.category,
                    'priority', t.priority,
                    'status', t.status,
                    'order_id', t.order_id,
                    'product_id', t.product_id,
                    'created_at', t.created_at
                             ),
                   'user', json_build_object(
                           'company_name', u.company_name,
                           'email', u.email,
                           'loyalty_tier', u.loyalty_tier,
                           'total_orders', u.total_orders,
                           'total_tickets', u.total_tickets
                           ),
                   'messages', (
                       SELECT json_agg(json_build_object(
                                               'sender_type', sender_type,
                                               'sender_name', sender_name,
                                               'message', message,
                                               'created_at', created_at
                                       ) ORDER BY created_at)
                       FROM ticket_messages
                       WHERE ticket_id = t.id
                   ),
                   'tags', (
                       SELECT array_agg(tag)
                       FROM ticket_tags
                       WHERE ticket_id = t.id
                   )
           ) INTO result
    FROM support_tickets t
             JOIN support_users u ON t.user_id = u.id
    WHERE t.id = p_ticket_id;

    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- ================================================
-- END OF MIGRATION V2
-- ================================================