# PostgreSQL Long-Term Memory System - Setup Guide

## 📋 Überblick

Dieses System implementiert eine **dauerhafte Speicherlösung** für AI Agent Konversationen mit PostgreSQL.

### Architektur

```
┌─────────────────┐
│   Frontend      │
│   (Vue.js)      │
└────────┬────────┘
         │ HTTP Requests
         ▼
┌─────────────────────────────────────────┐
│         AgentService                    │
│  ┌──────────────────────────────────┐   │
│  │  1. Load History from DB/RAM     │   │
│  │  2. Send to LLM                  │   │
│  │  3. Save Response to DB          │   │
│  └──────────────────────────────────┘   │
└──────────┬──────────────┬───────────────┘
           │              │
           ▼              ▼
┌──────────────────┐  ┌────────────────────┐
│ MemoryService    │  │ ConversationHistory│
│ (PostgreSQL)     │  │ Service (RAM Cache)│
│                  │  │                    │
│ - saveMessage()  │  │ - Fast access      │
│ - getHistory()   │  │ - Session cache    │
│ - getStats()     │  │ - Fallback         │
└────────┬─────────┘  └────────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│      PostgreSQL Database                │
│                                         │
│  Table: memory_entries                  │
│  - Full conversation history            │
│  - Metrics (tokens, cost)               │
│  - Timestamps                           │
│  - Indexed for fast queries             │
└─────────────────────────────────────────┘
```

### Schlüsselkonzepte

1. **PostgreSQL = Source of Truth**
   - Alle Nachrichten werden dauerhaft gespeichert
   - Überlebt Server-Neustarts
   - Ermöglicht Analyse und Export

2. **RAM Cache für Performance**
   - ConcurrentHashMap für aktive Gespräche
   - Wird aus DB beim ersten Zugriff geladen
   - Schneller Zugriff während Session

3. **Kompression für LLM Context**
   - DialogCompressionService komprimiert Geschichte
   - DB enthält IMMER vollständige Historie
   - Kompression beeinflusst nur LLM-Anfragen

## 🚀 Installation

### 1. PostgreSQL installieren

#### Ubuntu/Debian
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

#### macOS
```bash
brew install postgresql@15
brew services start postgresql@15
```

#### Windows
Download von: https://www.postgresql.org/download/windows/

### 2. Datenbank erstellen

```bash
# Als postgres User einloggen
sudo -u postgres psql

# In psql:
CREATE DATABASE ai_agent_memory;
CREATE USER ai_agent WITH ENCRYPTED PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE ai_agent_memory TO ai_agent;

# Bei PostgreSQL 15+:
\c ai_agent_memory
GRANT ALL ON SCHEMA public TO ai_agent;

# Prüfen
\l
\q
```

### 3. Environment Variablen setzen

Erstelle `.env` Datei im Backend-Verzeichnis:

```bash
# PostgreSQL Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/ai_agent_memory
DATABASE_USERNAME=ai_agent
DATABASE_PASSWORD=your_secure_password

# API Keys (bereits vorhanden)
OPENROUTER_API_KEY=your_openrouter_key
PERPLEXITY_API_KEY=your_perplexity_key
```

### 4. Dependencies installieren

```bash
cd backend/perplexity-service
mvn clean install
```

### 5. Anwendung starten

```bash
# Mit Maven
mvn spring-boot:run

# Oder mit dem Script
cd ../..
./start-backend.sh
```

### 6. Verifizierung

**Überprüfe Logs:**
```
INFO  de.jivz.ai_challenge - Started Application in X.XXX seconds
INFO  org.hibernate.dialect - HHH000400: Using dialect: org.hibernate.dialect.PostgreSQLDialect
INFO  Liquibase - Database is up to date
```

**Teste Health Endpoint:**
```bash
curl http://localhost:8080/api/memory/health
```

Erwartete Antwort:
```json
{
  "status": "healthy",
  "database": "connected",
  "timestamp": "2024-12-12T...",
  "totalConversations": 0,
  "totalMessages": 0
}
```

## 📊 Database Schema

Die Tabelle `memory_entries` wird automatisch von Hibernate erstellt:

```sql
CREATE TABLE memory_entries (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    model VARCHAR(255),
    input_tokens INTEGER,
    output_tokens INTEGER,
    total_tokens INTEGER,
    cost DECIMAL(10, 6),
    is_compressed BOOLEAN NOT NULL DEFAULT false,
    response_time_ms BIGINT
);

-- Indizes für Performance
CREATE INDEX idx_conversation_id ON memory_entries(conversation_id);
CREATE INDEX idx_user_id ON memory_entries(user_id);
CREATE INDEX idx_timestamp ON memory_entries(timestamp);
CREATE INDEX idx_conversation_timestamp ON memory_entries(conversation_id, timestamp);
```

## 🔧 Konfiguration

### application.properties

```properties
# PostgreSQL
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/ai_agent_memory}
spring.datasource.username=${DATABASE_USERNAME:postgres}
spring.datasource.password=${DATABASE_PASSWORD:postgres}

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update  # Automatische Schema-Updates
spring.jpa.show-sql=false              # SQL-Logging (true für Debugging)

# Connection Pool
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
```

### Wichtige Einstellungen

**Development (ddl-auto):**
- `update` - Empfohlen: Updates Schema automatisch
- `create-drop` - Löscht DB bei jedem Start (nur für Tests!)
- `validate` - Nur validieren, keine Änderungen
- `none` - Keine Schema-Verwaltung

**Production:**
```properties
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
logging.level.org.hibernate.SQL=WARN
```

## 📡 API Endpoints

### Memory Management

#### 1. Get User's Conversations
```bash
GET /api/memory/conversations/{userId}
```

Response:
```json
{
  "userId": "user123",
  "conversationCount": 5,
  "conversationIds": [
    "conv-2024-12-12-001",
    "conv-2024-12-11-042",
    ...
  ]
}
```

#### 2. Get Full Conversation History
```bash
GET /api/memory/conversation/{conversationId}
```

Response:
```json
{
  "conversationId": "conv-001",
  "messageCount": 12,
  "messages": [
    {
      "role": "user",
      "content": "Hello!"
    },
    {
      "role": "assistant",
      "content": "Hi! How can I help?"
    }
  ]
}
```

#### 3. Get Conversation Statistics
```bash
GET /api/memory/conversation/{conversationId}/stats
```

Response:
```json
{
  "conversationId": "conv-001",
  "messageCount": 12,
  "totalTokens": 4523,
  "totalCost": 0.0234,
  "averageTokensPerMessage": 377,
  "firstMessageAt": "2024-12-12T10:00:00Z",
  "lastMessageAt": "2024-12-12T11:30:00Z",
  "duration": "1 hours"
}
```

#### 4. Export Conversation
```bash
GET /api/memory/conversation/{conversationId}/export
```

Downloads JSON file with complete conversation.

#### 5. Delete Conversation
```bash
DELETE /api/memory/conversation/{conversationId}
```

Response:
```json
{
  "conversationId": "conv-001",
  "deletedMessages": 12,
  "status": "deleted"
}
```

#### 6. Global Statistics
```bash
GET /api/memory/stats
```

Response:
```json
{
  "totalConversations": 127,
  "totalMessages": 3456,
  "totalTokens": 892341,
  "totalCost": 12.45
}
```

## 🧪 Testing

### 1. Test Database Connection
```bash
curl http://localhost:8080/api/memory/health
```

### 2. Test Conversation Persistence

**Send a message:**
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Test message",
    "userId": "test-user",
    "conversationId": "test-conv-001",
    "provider": "openrouter"
  }'
```

**Restart server:**
```bash
# Ctrl+C to stop
mvn spring-boot:run
```

**Check if history persisted:**
```bash
curl http://localhost:8080/api/memory/conversation/test-conv-001
```

Should return the messages from before restart! ✅

### 3. Test Statistics
```bash
curl http://localhost:8080/api/memory/conversation/test-conv-001/stats
```

## 🔍 Monitoring & Debugging

### Enable SQL Logging
```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

### Check Database Directly
```bash
psql -U ai_agent -d ai_agent_memory

# Useful queries:
SELECT COUNT(*) FROM memory_entries;
SELECT conversation_id, COUNT(*) FROM memory_entries GROUP BY conversation_id;
SELECT * FROM memory_entries ORDER BY timestamp DESC LIMIT 10;
```

### Monitor Connection Pool
```bash
# Add to application.properties:
logging.level.com.zaxxer.hikari=DEBUG
```

## 🚨 Troubleshooting

### Problem: "Cannot connect to database"

**Prüfen ob PostgreSQL läuft:**
```bash
sudo systemctl status postgresql
# oder
pg_isready -h localhost -p 5432
```

**Firewall:**
```bash
sudo ufw allow 5432/tcp
```

**PostgreSQL Config (`postgresql.conf`):**
```
listen_addresses = 'localhost'
port = 5432
```

### Problem: "Permission denied for schema public"

```sql
\c ai_agent_memory
GRANT ALL ON SCHEMA public TO ai_agent;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ai_agent;
```

### Problem: "Relation 'memory_entries' does not exist"

- Stelle sicher `spring.jpa.hibernate.ddl-auto=update`
- Oder erstelle Tabelle manuell (siehe Schema oben)
- Prüfe Logs auf Hibernate-Errors

### Problem: "Application runs but doesn't save to DB"

- Check logs für SQLException
- Verifiziere `@Transactional` Annotationen
- Teste mit `curl` API direkt
- Prüfe ob MemoryService Exception wirft (wird geloggt)

## 🔄 Backup & Recovery

### Backup erstellen
```bash
pg_dump -U ai_agent -d ai_agent_memory > backup_$(date +%Y%m%d).sql
```

### Restore
```bash
psql -U ai_agent -d ai_agent_memory < backup_20241212.sql
```

### Automated Backup (Crontab)
```bash
crontab -e

# Daily backup at 2 AM
0 2 * * * pg_dump -U ai_agent ai_agent_memory > /backups/ai_memory_$(date +\%Y\%m\%d).sql
```

## 📈 Performance Optimization

### Indizes prüfen
```sql
SELECT tablename, indexname, indexdef 
FROM pg_indexes 
WHERE tablename = 'memory_entries';
```

### Slow Query Analysis
```sql
-- Enable query logging
ALTER DATABASE ai_agent_memory SET log_min_duration_statement = 100;

-- View slow queries
SELECT * FROM pg_stat_statements ORDER BY mean_exec_time DESC LIMIT 10;
```

### Vacuum & Analyze
```bash
psql -U ai_agent -d ai_agent_memory -c "VACUUM ANALYZE memory_entries;"
```

## 🌐 Production Deployment

### Docker Compose Example
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: ai_agent_memory
      POSTGRES_USER: ai_agent
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  backend:
    build: ./backend
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/ai_agent_memory
      DATABASE_USERNAME: ai_agent
      DATABASE_PASSWORD: ${DB_PASSWORD}
    depends_on:
      - postgres
    ports:
      - "8080:8080"

volumes:
  pgdata:
```

### Environment Variables (Production)
```bash
export DATABASE_URL=jdbc:postgresql://prod-db.example.com:5432/ai_agent_memory
export DATABASE_USERNAME=ai_agent_prod
export DATABASE_PASSWORD=$(cat /secrets/db_password)
```

## 📚 Weitere Informationen

- **Spring Data JPA Docs:** https://spring.io/projects/spring-data-jpa
- **PostgreSQL Docs:** https://www.postgresql.org/docs/
- **Hibernate Docs:** https://hibernate.org/orm/documentation/

## ✅ Checklist für Inbetriebnahme

- [ ] PostgreSQL installiert und läuft
- [ ] Datenbank `ai_agent_memory` erstellt
- [ ] User `ai_agent` mit Rechten konfiguriert
- [ ] `.env` Datei mit Credentials erstellt
- [ ] Dependencies installiert (`mvn clean install`)
- [ ] Backend startet ohne Errors
- [ ] Health Endpoint antwortet mit "healthy"
- [ ] Test-Nachricht senden
- [ ] Server neustarten
- [ ] Nachricht noch vorhanden (Persistenz verifiziert)
- [ ] Alle API Endpoints getestet

---

**Bei Fragen oder Problemen:**
- Prüfe die Logs: `tail -f backend/perplexity-service/logs/application.log`
- Enable DEBUG logging
- Check PostgreSQL logs: `sudo tail -f /var/log/postgresql/postgresql-15-main.log`

