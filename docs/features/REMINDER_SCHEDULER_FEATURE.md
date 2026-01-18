# Reminder Scheduler Feature

## Übersicht

Der Reminder Scheduler ist ein Cron-Job basiertes System, das automatisch:
1. MCP Tools vom Backend abruft
2. Dynamische System-Prompts mit den Tool-Definitionen erstellt
3. Den Tool-Loop ausführt um Aufgaben-Zusammenfassungen zu generieren
4. Summaries in PostgreSQL speichert
5. Optional Benachrichtigungen sendet

## Architektur

```
┌─────────────────────────────────────────────────────────────────┐
│                    ReminderSchedulerService                      │
│                    (@Scheduled Cron Job)                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. fetchCurrentTools()  ───────────► McpToolClient              │
│          ↓                             (GET /mcp/tools)          │
│                                                                  │
│  2. buildDynamicSystemPrompt() ────► ReminderToolsPromptStrategy │
│          ↓                                                       │
│                                                                  │
│  3. executeToolLoop()  ─────────────► PerplexityToolClient       │
│          ↓                             (Sonar API)               │
│                                                                  │
│  4. saveReminderSummary() ──────────► ReminderSummaryRepository  │
│          ↓                             (PostgreSQL)              │
│                                                                  │
│  5. triggerNotification() ──────────► (Email/Push/Webhook)       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Dateien

### Entity
- `entity/ReminderSummary.java` - JPA Entity für Zusammenfassungen

### Repository
- `repository/ReminderSummaryRepository.java` - JPA Repository mit vielen Query-Methoden

### Services
- `service/ReminderSchedulerService.java` - Haupt-Service mit Cron Job
- `service/strategy/ReminderToolsPromptStrategy.java` - Dynamischer Prompt Builder

### Controller
- `controller/ReminderController.java` - REST API für manuelle Trigger und Abfragen

## Konfiguration

In `application.properties`:

```properties
# Scheduler aktivieren/deaktivieren
reminder.scheduler.enabled=true

# Cron Expression: Sekunden Minuten Stunden TagDesMonats Monat Wochentag
# Täglich um 9:00 Uhr:
reminder.scheduler.cron=0 0 9 * * ?

# Zum Testen: Alle 5 Minuten:
reminder.scheduler.cron=0 */5 * * * ?

# Default User ID
reminder.scheduler.user-id=system

# LLM Temperature (niedriger = deterministischer)
reminder.scheduler.temperature=0.3
```

## REST API

### Manueller Trigger
```bash
POST /api/reminder/trigger?userId=my-user
```

### Alle Summaries eines Users
```bash
GET /api/reminder/summaries?userId=my-user
```

### Neueste Summary
```bash
GET /api/reminder/latest
```

### Ausstehende Benachrichtigungen
```bash
GET /api/reminder/pending
```

### Status
```bash
GET /api/reminder/status
```

## Datenbank Schema

Die Tabelle `reminder_summaries` wird automatisch erstellt:

```sql
CREATE TABLE reminder_summaries (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(255),
    summary_type VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    raw_data TEXT,
    created_at TIMESTAMP NOT NULL,
    notified_at TIMESTAMP,
    notified BOOLEAN NOT NULL DEFAULT FALSE,
    items_count INTEGER,
    priority VARCHAR(20),
    next_reminder_at TIMESTAMP
);

-- Indexes
CREATE INDEX idx_reminder_user_id ON reminder_summaries(user_id);
CREATE INDEX idx_reminder_created_at ON reminder_summaries(created_at);
CREATE INDEX idx_reminder_type ON reminder_summaries(summary_type);
CREATE INDEX idx_reminder_notified ON reminder_summaries(notified);
```

## Dynamischer System-Prompt

Der `ReminderToolsPromptStrategy` generiert System-Prompts mit:

1. **Dynamisch geladenen MCP Tools** (name, description, inputSchema)
2. **Strukturiertem JSON Output Format**
3. **Spezifischen Anweisungen** für Summary-Generierung

Beispiel generierter Prompt:
```
Du bist ein intelligenter Reminder-Assistent...

## Verfügbare MCP Tools:

1. **google_tasks_list**
   - Beschreibung: List all task lists
   - Schema: { "type": "object", "properties": {} }

2. **google_tasks_get**
   - Beschreibung: Get tasks from a list
   - Schema: { "type": "object", "properties": { "taskListId": {...} } }
...

## OUTPUT FORMAT (nur JSON):
{
  "step": "tool|final",
  "tool_calls": [...],
  "answer": "...",
  "summary": {
    "title": "...",
    "total_items": N,
    "priority": "HIGH|MEDIUM|LOW",
    ...
  }
}
```

## Tool-Loop Workflow

```
┌──────────────────────────────────────────────────┐
│           User Message + System Prompt            │
└────────────────────────┬─────────────────────────┘
                         ↓
┌──────────────────────────────────────────────────┐
│              Sonar API Call                       │
└────────────────────────┬─────────────────────────┘
                         ↓
            ┌────────────┴────────────┐
            │     Parse Response      │
            └────────────┬────────────┘
                         ↓
           ┌─────────────┴─────────────┐
           │                           │
     step == "tool"              step == "final"
           │                           │
           ↓                           ↓
  ┌────────────────┐         ┌─────────────────────┐
  │ Execute MCP    │         │ Return Summary &    │
  │ Tools          │         │ Save to DB          │
  └───────┬────────┘         └─────────────────────┘
          │
          ↓
  ┌────────────────┐
  │ Add Results    │
  │ to Messages    │
  └───────┬────────┘
          │
          └──────► (zurück zu Sonar API Call)
```

## Erweiterungen

### Benachrichtigungen implementieren

In `ReminderSchedulerService.triggerNotification()`:

```java
private void triggerNotification(ReminderSummary summary) {
    // Email senden
    emailService.sendSummary(summary.getUserId(), summary.getTitle(), summary.getContent());
    
    // Push Notification
    pushService.sendNotification(summary.getUserId(), summary.getTitle());
    
    // Webhook aufrufen
    webhookService.post(summary);
    
    // WebSocket Event
    webSocketHandler.broadcast("reminder", summary);
}
```

### Weitere Summary-Typen

Erweitere `SummaryType` Enum:
- `CALENDAR` - Kalender-Events
- `EMAIL` - Email-Zusammenfassungen
- `NEWS` - Nachrichten-Digest
- `CUSTOM` - Benutzerdefiniert

### Scheduler-Profile

Verschiedene Cron-Jobs für verschiedene Benutzer/Typen:

```java
@Scheduled(cron = "${reminder.scheduler.daily.cron:0 0 9 * * ?}")
public void dailyDigest() { ... }

@Scheduled(cron = "${reminder.scheduler.weekly.cron:0 0 9 * * MON}")
public void weeklyDigest() { ... }
```

## Test

Manueller Test via curl:

```bash
# Trigger ausführen
curl -X POST "http://localhost:8080/api/reminder/trigger?userId=test-user"

# Ergebnis abrufen
curl "http://localhost:8080/api/reminder/latest"

# Status prüfen
curl "http://localhost:8080/api/reminder/status"
```

