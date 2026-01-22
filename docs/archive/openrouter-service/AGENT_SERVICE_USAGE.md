# AgentService Nutzungsanleitung

## Übersicht

Der AgentService bietet erweiterte Chat-Funktionen für den openrouter-service mit automatischer Dialogkompression, PostgreSQL-Persistierung und JSON-Modus-Unterstützung.

## API Endpoints

### 1. Standard Chat mit AgentService

**Endpoint:** `POST /api/v1/openrouter/agent/chat`

**Beschreibung:** Verarbeitet Chat-Anfragen mit automatischer History-Kompression und PostgreSQL-Persistierung.

**Request Body:**
```json
{
  "message": "Erkläre mir die Quantenmechanik",
  "conversationId": "conv-12345",
  "userId": "user-123",
  "temperature": 0.7,
  "model": "openrouter/auto"
}
```

**Response:**
```json
{
  "reply": "Die Quantenmechanik ist...",
  "model": "anthropic/claude-3-sonnet",
  "inputTokens": 120,
  "outputTokens": 450,
  "totalTokens": 570,
  "cost": 0.0012,
  "responseTimeMs": 2500,
  "finishReason": "stop"
}
```

**cURL Beispiel:**
```bash
curl -X POST http://localhost:8084/api/v1/openrouter/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Was ist KI?",
    "conversationId": "test-conv-1",
    "userId": "user-1"
  }'
```

---

### 2. Chat mit MCP Tools

**Endpoint:** `POST /api/v1/openrouter/agent/chat/mcp`

**Beschreibung:** Chat mit MCP Tools Integration für erweiterte Fähigkeiten (Docker, RAG, etc.).

**Request Body:**
```json
{
  "message": "Welche Docker Container laufen gerade?",
  "conversationId": "conv-mcp-1",
  "userId": "user-123"
}
```

**cURL Beispiel:**
```bash
curl -X POST http://localhost:8084/api/v1/openrouter/agent/chat/mcp \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Liste alle laufenden Docker Container",
    "conversationId": "mcp-conv-1"
  }'
```

---

### 3. Chat mit JSON-Modus

**Endpoint:** `POST /api/v1/openrouter/agent/json`

**Beschreibung:** Chat mit aktiviertem JSON-Modus für strukturierte Antworten.

**Query Parameters:**
- `message` (required): Die Nachricht
- `conversationId` (optional): Konversations-ID
- `userId` (optional): User-ID
- `autoSchema` (optional, default: false): Auto-Schema generieren
- `jsonSchema` (optional): Benutzerdefiniertes JSON-Schema

**cURL Beispiel (Einfaches JSON):**
```bash
curl -X POST "http://localhost:8084/api/v1/openrouter/agent/json?message=Gib%20mir%20Infos%20zu%20Berlin&userId=user-1" \
  -H "Content-Type: application/json"
```

**Antwort:**
```json
{
  "reply": "{\"response\": \"Berlin ist die Hauptstadt...\", \"status\": \"success\"}",
  "model": "anthropic/claude-3-sonnet",
  ...
}
```

**cURL Beispiel (Auto-Schema):**
```bash
curl -X POST "http://localhost:8084/api/v1/openrouter/agent/json?message=Liste%20die%205%20größten%20Städte%20Deutschlands&autoSchema=true" \
  -H "Content-Type: application/json"
```

**cURL Beispiel (Custom Schema):**
```bash
curl -X POST "http://localhost:8084/api/v1/openrouter/agent/json" \
  -H "Content-Type: application/json" \
  -G \
  --data-urlencode 'message=Liste deutsche Städte' \
  --data-urlencode 'jsonSchema={"type":"object","properties":{"cities":{"type":"array","items":{"type":"object","properties":{"name":{"type":"string"},"population":{"type":"number"}}}}}}'
```

---

## Features

### 1. Automatische Dialogkompression

Nach 5+ Nachrichten wird automatisch eine Zusammenfassung erstellt:

```
Nachricht 1: User: "Hallo"
Nachricht 2: Assistant: "Hallo!"
Nachricht 3: User: "Was ist AI?"
Nachricht 4: Assistant: "AI ist..."
Nachricht 5: User: "Erkläre mehr"
Nachricht 6: Assistant: "..." 
→ KOMPRESSION AUSGELÖST
```

**Nach Kompression:**
```
[SUMMARY]: "Benutzer fragte nach AI, ich erklärte Grundlagen..."
Nachricht 5: User: "Erkläre mehr"
Nachricht 6: Assistant: "..."
[Neue Nachrichten...]
```

**Vorteile:**
- ✅ Spart Tokens (weniger Kontext zu senden)
- ✅ Erhält wichtigen Kontext
- ✅ Verbessert Performance
- ✅ Senkt Kosten

### 2. PostgreSQL-Persistierung

Alle Nachrichten werden automatisch in PostgreSQL gespeichert:

```java
// Automatisch beim Chat
memoryService.saveMessage(
    conversationId,  // "conv-12345"
    userId,          // "user-123"
    "assistant",     // role
    response,        // Antworttext
    "claude-3",      // Modell
    metrics          // Token, Kosten, Zeit
);
```

**Gespeicherte Daten:**
- Vollständiger Nachrichtenverlauf
- Token-Nutzung (Input/Output)
- Kosten pro Nachricht
- Antwortzeit
- Verwendetes Modell
- Zeitstempel

### 3. JSON-Modus

**Einfacher JSON-Modus:**
```json
Request: {"message": "Info zu Berlin", "jsonMode": true}
Response: {
  "response": "Berlin ist die Hauptstadt von Deutschland...",
  "status": "success"
}
```

**Auto-Schema:**
LLM analysiert Frage und erstellt passendes Schema automatisch.

```json
Request: {"message": "Liste Top 3 Städte", "jsonMode": true, "autoSchema": true}
Response: {
  "cities": [
    {"name": "Berlin", "population": 3800000},
    {"name": "München", "population": 1500000},
    {"name": "Hamburg", "population": 1900000}
  ]
}
```

**Custom Schema:**
```json
Request: {
  "message": "Wetterdaten für Berlin",
  "jsonMode": true,
  "jsonSchema": "{\"type\":\"object\",\"properties\":{\"temperature\":{\"type\":\"number\"},\"condition\":{\"type\":\"string\"}}}"
}
Response: {
  "temperature": 18,
  "condition": "cloudy"
}
```

### 4. System Prompts

Definiere die Persönlichkeit des Assistenten:

```json
{
  "message": "Erkläre Physik",
  "systemPrompt": "Du bist ein freundlicher Physiklehrer, der komplexe Themen einfach erklärt.",
  "conversationId": "physics-conv-1"
}
```

---

## Konversationsmanagement

### Neue Konversation starten
```bash
curl -X POST http://localhost:8084/api/v1/openrouter/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Hallo, ich bin neu hier",
    "userId": "user-1"
  }'
# conversationId wird automatisch generiert
```

### Bestehende Konversation fortsetzen
```bash
curl -X POST http://localhost:8084/api/v1/openrouter/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Erzähl mir mehr",
    "conversationId": "conv-12345",
    "userId": "user-1"
  }'
# Historie wird automatisch geladen
```

---

## Integration mit Frontend

### JavaScript/TypeScript Beispiel

```typescript
async function sendAgentMessage(message: string, conversationId?: string) {
  const response = await fetch('http://localhost:8084/api/v1/openrouter/agent/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      message: message,
      conversationId: conversationId,
      userId: 'current-user-id',
      temperature: 0.7
    })
  });

  const data = await response.json();
  console.log('Reply:', data.reply);
  console.log('Tokens:', data.totalTokens);
  console.log('Cost:', data.cost);
  
  return data;
}

// Verwendung
sendAgentMessage('Was ist KI?', 'my-conv-1');
```

### React Hook Beispiel

```typescript
function useAgentChat() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const sendMessage = async (message: string, conversationId?: string) => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch('/api/v1/openrouter/agent/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message,
          conversationId,
          userId: 'user-123'
        })
      });

      const data = await response.json();
      return data;
    } catch (err) {
      setError(err.message);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  return { sendMessage, loading, error };
}
```

---

## Best Practices

### 1. ConversationId Management
```typescript
// Generiere eindeutige IDs
const conversationId = `conv-${userId}-${Date.now()}`;

// Speichere sie im Frontend
localStorage.setItem('currentConversationId', conversationId);

// Wiederverwendung
const savedConvId = localStorage.getItem('currentConversationId');
```

### 2. Error Handling
```typescript
try {
  const response = await sendAgentMessage(message, convId);
  // Erfolg
} catch (error) {
  if (error.status === 400) {
    // Validierungsfehler
  } else if (error.status === 500) {
    // Server-Fehler
    console.error('Server error:', error.message);
  }
}
```

### 3. Token-Überwachung
```typescript
function trackTokenUsage(response) {
  const usage = {
    tokens: response.totalTokens,
    cost: response.cost,
    timestamp: new Date()
  };
  
  // Speichere für Analytics
  analytics.track('token_usage', usage);
}
```

---

## Monitoring und Debugging

### Logs prüfen
```bash
# Tail logs
tail -f /path/to/logs/openrouter-service.log

# Filter nach conversationId
grep "conv-12345" /path/to/logs/openrouter-service.log

# Filter nach Errors
grep "ERROR" /path/to/logs/openrouter-service.log
```

### Wichtige Log-Emojis
- 🚀 Request Start
- ✅ Success
- ❌ Error
- 💾 Database Save
- 📚 History Load
- 🗜️ Compression
- 📊 Metrics
- 🔧 MCP Tools

---

## Troubleshooting

### Problem: Keine Antwort
```bash
# Prüfe ob Service läuft
curl http://localhost:8084/actuator/health

# Prüfe Logs
tail -f logs/openrouter-service.log
```

### Problem: Fehlende Historie
```bash
# Prüfe DB-Verbindung
psql -h localhost -U user -d ai_challenge

# Prüfe Einträge
SELECT * FROM memory_entries WHERE conversation_id = 'conv-12345';
```

### Problem: Kompression funktioniert nicht
```bash
# Prüfe Anzahl Nachrichten
SELECT COUNT(*) FROM memory_entries 
WHERE conversation_id = 'conv-12345' 
AND is_compressed = false;

# Sollte >= 5 sein für Kompression
```

---

## Support

Bei Fragen oder Problemen:
1. Prüfe Logs: `/logs/openrouter-service.log`
2. Prüfe Dokumentation: `AGENT_SERVICE_INTEGRATION.md`
3. Prüfe API-Docs: http://localhost:8084/swagger-ui.html

