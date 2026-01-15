# B2B WebShop - Database Documentation

> Database: PostgreSQL 15.4  
> Last Updated: 2026-01-15

## Table of Contents
1. [Database Schema](#database-schema)
2. [Optimization Guidelines](#optimization-guidelines)
3. [Common Queries](#common-queries)
4. [Backup & Recovery](#backup--recovery)

---

## Database Schema

### Core Tables

#### 1. users
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL, -- CUSTOMER_ADMIN, CUSTOMER_BUYER, etc
    company_id UUID REFERENCES companies(id),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_company ON users(company_id);
CREATE INDEX idx_users_active ON users(is_active) WHERE is_active = true;
```

#### 2. companies
```sql
CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    tax_id VARCHAR(50) NOT NULL UNIQUE,
    tier VARCHAR(20) NOT NULL DEFAULT 'BRONZE', -- BRONZE, SILVER, GOLD, PLATINUM
    credit_limit DECIMAL(12,2) NOT NULL DEFAULT 0,
    credit_used DECIMAL(12,2) NOT NULL DEFAULT 0,
    discount_percent INTEGER NOT NULL DEFAULT 0,
    address_street VARCHAR(255),
    address_city VARCHAR(100),
    address_postal_code VARCHAR(20),
    address_country VARCHAR(2),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_companies_tax_id ON companies(tax_id);
CREATE INDEX idx_companies_tier ON companies(tier);
```

#### 3. products
```sql
CREATE TABLE products (
    id VARCHAR(50) PRIMARY KEY, -- P001, P002, etc
    sku VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(500) NOT NULL,
    description TEXT,
    category_id UUID REFERENCES categories(id),
    base_price DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',
    min_order_quantity INTEGER DEFAULT 1,
    stock_quantity INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    image_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_active ON products(is_active) WHERE is_active = true;
CREATE INDEX idx_products_name_search ON products USING gin(to_tsvector('english', name));
```

#### 4. categories
```sql
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    parent_id UUID REFERENCES categories(id),
    sort_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_categories_parent ON categories(parent_id);
CREATE INDEX idx_categories_slug ON categories(slug);
```

#### 5. orders
```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number VARCHAR(50) NOT NULL UNIQUE, -- ORD-2026-0001
    user_id UUID NOT NULL REFERENCES users(id),
    company_id UUID NOT NULL REFERENCES companies(id),
    status VARCHAR(20) NOT NULL DEFAULT 'NEW', -- NEW, CONFIRMED, PAID, PACKED, SHIPPED, DELIVERED, CANCELLED
    subtotal DECIMAL(12,2) NOT NULL,
    discount DECIMAL(12,2) DEFAULT 0,
    tax DECIMAL(12,2) NOT NULL,
    total DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',
    delivery_address_street VARCHAR(255),
    delivery_address_city VARCHAR(100),
    delivery_address_postal_code VARCHAR(20),
    delivery_address_country VARCHAR(2),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP,
    paid_at TIMESTAMP,
    shipped_at TIMESTAMP,
    delivered_at TIMESTAMP
);

CREATE INDEX idx_orders_number ON orders(order_number);
CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_company ON orders(company_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);

-- Partition by date for performance
CREATE TABLE orders_2026 PARTITION OF orders
FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
```

#### 6. order_items
```sql
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id VARCHAR(50) NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(12,2) NOT NULL,
    discount_percent INTEGER DEFAULT 0,
    total_price DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);
```

#### 7. invoices
```sql
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number VARCHAR(50) NOT NULL UNIQUE, -- INV-2026-0001
    order_id UUID NOT NULL REFERENCES orders(id),
    company_id UUID NOT NULL REFERENCES companies(id),
    status VARCHAR(20) NOT NULL DEFAULT 'UNPAID', -- UNPAID, PAID, OVERDUE, CANCELLED
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'EUR',
    due_date DATE NOT NULL,
    paid_date DATE,
    pdf_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_invoices_number ON invoices(invoice_number);
CREATE INDEX idx_invoices_order ON invoices(order_id);
CREATE INDEX idx_invoices_company ON invoices(company_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_due_date ON invoices(due_date);
```

---

## Optimization Guidelines

### 1. Index Strategy

**When to add indexes:**
- Foreign key columns (ALWAYS)
- Columns used in WHERE clauses frequently
- Columns used in JOIN conditions
- Columns used for sorting (ORDER BY)

**When NOT to add indexes:**
- Rarely queried columns
- Columns with low cardinality (few distinct values)
- Tables with heavy write workload

### 2. Query Optimization

#### Avoid N+1 Queries
❌ **Bad:**
```java
// This executes 1 query for orders + N queries for items
List<Order> orders = orderRepository.findAll();
for (Order order : orders) {
    order.getItems().size(); // Triggers lazy loading
}
```

✅ **Good:**
```java
// This executes 1 query with JOIN
@Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE ...")
List<Order> findAllWithItems();
```

#### Use Pagination
❌ **Bad:**
```sql
SELECT * FROM orders ORDER BY created_at DESC;
-- Returns all orders (millions of rows)
```

✅ **Good:**
```sql
SELECT * FROM orders 
ORDER BY created_at DESC 
LIMIT 20 OFFSET 0;
-- Returns only 20 rows
```

#### Use Covering Indexes
```sql
-- Query: SELECT id, name, price FROM products WHERE category_id = 'CAT001'
-- Create index covering all columns
CREATE INDEX idx_products_category_covering 
ON products(category_id, id, name, price);
```

### 3. Connection Pooling

**HikariCP Configuration:**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

**Pool Size Formula:**
```
connections = ((core_count * 2) + effective_spindle_count)
Example: (4 cores * 2) + 1 SSD = 9 connections per service
```

### 4. Slow Query Log

Enable slow query logging in PostgreSQL:
```sql
ALTER SYSTEM SET log_min_duration_statement = 1000; -- Log queries > 1s
SELECT pg_reload_conf();
```

View slow queries:
```sql
SELECT 
    query,
    mean_exec_time,
    calls
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;
```

---

## Common Queries

### 1. Product Search with Inventory
```sql
SELECT 
    p.id,
    p.name,
    p.base_price,
    p.stock_quantity,
    c.name as category_name
FROM products p
JOIN categories c ON p.category_id = c.id
WHERE 
    p.is_active = true
    AND p.stock_quantity > 0
    AND to_tsvector('english', p.name) @@ plainto_tsquery('laptop')
ORDER BY p.name
LIMIT 20;
```

### 2. Customer Order History with Totals
```sql
SELECT 
    o.order_number,
    o.created_at,
    o.status,
    o.total,
    COUNT(oi.id) as item_count
FROM orders o
LEFT JOIN order_items oi ON o.id = oi.order_id
WHERE o.company_id = 'uuid-here'
GROUP BY o.id
ORDER BY o.created_at DESC
LIMIT 50;
```

### 3. Top Selling Products
```sql
SELECT 
    p.name,
    SUM(oi.quantity) as total_sold,
    SUM(oi.total_price) as revenue
FROM order_items oi
JOIN products p ON oi.product_id = p.id
JOIN orders o ON oi.order_id = o.id
WHERE 
    o.status = 'DELIVERED'
    AND o.created_at >= NOW() - INTERVAL '30 days'
GROUP BY p.id, p.name
ORDER BY revenue DESC
LIMIT 10;
```

### 4. Credit Limit Check
```sql
SELECT 
    c.name,
    c.credit_limit,
    c.credit_used,
    (c.credit_limit - c.credit_used) as available_credit,
    CASE 
        WHEN c.credit_used >= c.credit_limit THEN 'EXCEEDED'
        WHEN c.credit_used >= c.credit_limit * 0.9 THEN 'WARNING'
        ELSE 'OK'
    END as credit_status
FROM companies c
WHERE c.id = 'uuid-here';
```

### 5. Overdue Invoices
```sql
SELECT 
    i.invoice_number,
    i.due_date,
    i.amount,
    c.name as company_name,
    CURRENT_DATE - i.due_date as days_overdue
FROM invoices i
JOIN companies c ON i.company_id = c.id
WHERE 
    i.status = 'UNPAID'
    AND i.due_date < CURRENT_DATE
ORDER BY i.due_date ASC;
```

---

## Database Maintenance

### 1. Vacuum & Analyze

Regular maintenance tasks:
```sql
-- Manual vacuum (run weekly)
VACUUM ANALYZE;

-- Vacuum specific table
VACUUM ANALYZE orders;

-- Check table bloat
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

### 2. Reindex

Rebuild indexes (run monthly):
```sql
-- Reindex specific table
REINDEX TABLE orders;

-- Reindex all tables (during maintenance window)
REINDEX DATABASE webshop;
```

---

## Backup & Recovery

### 1. Backup Strategy

**Daily Backups:**
```bash
# Full database backup
pg_dump -U ai_user -d webshop > backup_$(date +%Y%m%d).sql

# Compressed backup
pg_dump -U ai_user -d webshop | gzip > backup_$(date +%Y%m%d).sql.gz
```

**Retention Policy:**
- Daily backups: 7 days
- Weekly backups: 4 weeks
- Monthly backups: 12 months

### 2. Point-in-Time Recovery

Enable WAL archiving:
```sql
ALTER SYSTEM SET archive_mode = on;
ALTER SYSTEM SET archive_command = 'cp %p /backup/wal/%f';
```

Restore to specific time:
```bash
pg_restore -U ai_user -d webshop --clean backup_20260115.sql
```

### 3. Testing Backups

Monthly backup test:
1. Restore to test environment
2. Verify data integrity
3. Run smoke tests
4. Document any issues

---

## Performance Monitoring

### Key Metrics to Monitor:

1. **Connection Usage**
```sql
SELECT 
    count(*) as total_connections,
    count(*) FILTER (WHERE state = 'active') as active_connections,
    count(*) FILTER (WHERE state = 'idle') as idle_connections
FROM pg_stat_activity;
```

2. **Cache Hit Ratio** (should be > 95%)
```sql
SELECT 
    sum(heap_blks_hit) / nullif(sum(heap_blks_hit) + sum(heap_blks_read), 0) as cache_hit_ratio
FROM pg_statio_user_tables;
```

3. **Table Sizes**
```sql
SELECT 
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

---

## Troubleshooting

### Slow Queries

1. Enable `pg_stat_statements`
```sql
CREATE EXTENSION pg_stat_statements;
```

2. Find slow queries
```sql
SELECT 
    query,
    mean_exec_time,
    calls,
    total_exec_time
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;
```

3. Analyze query plan
```sql
EXPLAIN ANALYZE
SELECT * FROM orders WHERE company_id = 'uuid';
```

### Lock Contention

Check for locks:
```sql
SELECT 
    blocked_locks.pid AS blocked_pid,
    blocking_locks.pid AS blocking_pid,
    blocked_activity.query AS blocked_query
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_locks blocking_locks 
    ON blocking_locks.locktype = blocked_locks.locktype
WHERE NOT blocked_locks.granted;
```

---

## Contact

**Database Team:**
- DBA: dba@webshop.example.com
- Questions: #database on Slack

**Last Updated:** 2026-01-15
