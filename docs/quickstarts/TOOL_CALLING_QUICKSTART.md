# 🚀 Tool-Calling - Schnellstart

## ✅ Was wurde implementiert?

Die korrekte Tool-Calling-Architektur:
1. **LLM bekommt Tools** → Liste vom MCP Server
2. **LLM entscheidet** → Welche Tools zu nutzen
3. **LLM gibt Tool-Calls zurück** → Strukturiert mit Argumenten
4. **AgentService führt aus** → Via MCP Client
5. **Ergebnisse an LLM** → Zur Verarbeitung
6. **LLM generiert Antwort** → Finale Response

## 📝 API-Endpunkte

### POST /api/chat/with-tools
Chat mit LLM-basiertem Tool-Calling

**Request:**
```json
{
  "userId": "user123",
  "conversationId": "conv-001",
  "message": "Zeige meine Google Tasks und rechne 5 + 3",
  "provider": "openrouter",
  "model": "openai/gpt-4-turbo",
  "temperature": 0.7
}
```

**Response:**
```json
{
  "reply": "Hier sind deine Google Tasks:\n1. Einkaufen\n2. Projekt fertigstellen\n\nUnd 5 + 3 = 8",
  "toolName": "OpenRouterToolClient",
  "timestamp": "2025-12-16T15:00:00.000+00:00",
  "metrics": {
    "inputTokens": 523,
    "outputTokens": 89,
    "totalTokens": 612,
    "cost": 0.00234,
    "responseTimeMs": 1432,
    "model": "openai/gpt-4-turbo",
    "provider": "openrouter"
  }
}
```

### GET /api/chat/available-tools
Liste verfügbarer Tools

**Response:**
```json
{
  "count": 5,
  "tools": [
    {
      "name": "google_tasks_list",
      "description": "Lists all Google Tasks",
      "inputSchema": {
        "type": "object",
        "properties": {},
        "required": []
      }
    },
    {
      "name": "google_tasks_create",
      "description": "Creates a new Google Task",
      "inputSchema": {
        "type": "object",
        "properties": {
          "title": { "type": "string" },
          "notes": { "type": "string" }
        },
        "required": ["title"]
      }
    },
    {
      "name": "add_numbers",
      "description": "Adds two numbers",
      "inputSchema": {
        "type": "object",
        "properties": {
          "a": { "type": "number" },
          "b": { "type": "number" }
        },
        "required": ["a", "b"]
      }
    }
  ],
  "timestamp": "2025-12-16T15:00:00.000+00:00"
}
```

## 🧪 Test-Beispiele

### Curl-Befehle

**1. Verfügbare Tools anzeigen:**
```bash
curl -X GET http://localhost:8081/api/chat/available-tools
```

**2. Chat mit Tool-Calling (einzelnes Tool):**
```bash
curl -X POST http://localhost:8081/api/chat/with-tools \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user",
    "conversationId": "test-conv-1",
    "message": "Zeige meine Google Tasks",
    "provider": "openrouter",
    "model": "openai/gpt-4-turbo"
  }'
```

**3. Chat mit Tool-Calling (mehrere Tools):**
```bash
curl -X POST http://localhost:8081/api/chat/with-tools \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user",
    "conversationId": "test-conv-2",
    "message": "Zeige meine Google Tasks und rechne 5 + 3",
    "provider": "openrouter",
    "model": "openai/gpt-4-turbo"
  }'
```

**4. Chat mit Tool-Calling (komplexe Anfrage):**
```bash
curl -X POST http://localhost:8081/api/chat/with-tools \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user",
    "conversationId": "test-conv-3",
    "message": "Erstelle eine Task mit dem Titel \"Meeting vorbereiten\" und zeige dann alle meine Tasks",
    "provider": "openrouter",
    "model": "openai/gpt-4-turbo"
  }'
```

## 🔍 Logging-Ausgabe

Bei Tool-Nutzung sieht das Log so aus:

```
🔧 5 MCP tools available for LLM
📝 Converted 5 tools to OpenRouter format
🔍 Raw reply from OpenRouter with model openai/gpt-4-turbo (first 200 chars): 
🔧 Response contains 2 tool calls
🔧 LLM requested 2 tool calls
🔧 Executing tool: google_tasks_list with args: {}
✅ Tool 'google_tasks_list' executed successfully
🔧 Executing tool: add_numbers with args: {a=5.0, b=3.0}
✅ Tool 'add_numbers' executed successfully
🔍 Raw reply from OpenRouter with model openai/gpt-4-turbo (first 200 chars): Hier sind deine Google Tasks:
1. Einkaufen
2. Projekt fertigstellen

Und 5 + 3 = 8
⏱️ Total request processing time: 1843 ms
```

## 🎨 Frontend-Integration

### JavaScript/TypeScript Beispiel

```typescript
async function chatWithTools(message: string) {
  const response = await fetch('http://localhost:8081/api/chat/with-tools', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      userId: 'user123',
      conversationId: getCurrentConversationId(),
      message: message,
      provider: 'openrouter',
      model: 'openai/gpt-4-turbo',
      temperature: 0.7
    })
  });
  
  const data = await response.json();
  console.log('LLM Reply:', data.reply);
  console.log('Metrics:', data.metrics);
  
  return data;
}

// Verwendung
chatWithTools("Zeige meine Google Tasks und rechne 5 + 3");
```

### Available Tools anzeigen

```typescript
async function getAvailableTools() {
  const response = await fetch('http://localhost:8081/api/chat/available-tools');
  const data = await response.json();
  
  console.log(`${data.count} Tools verfügbar:`);
  data.tools.forEach(tool => {
    console.log(`- ${tool.name}: ${tool.description}`);
  });
  
  return data.tools;
}
```

## 🔧 Unterstützte Modelle

### OpenRouter (empfohlen)
- ✅ `openai/gpt-4-turbo` - Beste Tool-Calling Performance
- ✅ `openai/gpt-4` - Sehr gut
- ✅ `openai/gpt-3.5-turbo` - Gut, günstiger
- ✅ `anthropic/claude-3-opus` - Exzellent
- ✅ `anthropic/claude-3-sonnet` - Sehr gut
- ✅ `google/gemini-pro` - Gut

### Perplexity
- ⚠️ In Entwicklung (noch nicht implementiert)

## 📊 Performance

### Typical Response Times
- **Ohne Tool-Calls:** 800-1200ms
- **Mit 1 Tool-Call:** 1500-2000ms
- **Mit 2+ Tool-Calls:** 2000-3000ms

### Token-Kosten
- **Tool-Definition:** ~50-100 Tokens pro Tool
- **Tool-Call:** ~20-40 Tokens
- **Tool-Result:** Variabel (10-500 Tokens)

**Beispiel-Berechnung:**
```
5 Tools (Definitionen): 5 × 75 = 375 Tokens
User Message: 15 Tokens
Tool-Call (2x): 2 × 30 = 60 Tokens
Tool-Results: 150 Tokens
LLM Response: 50 Tokens
---
Total Input: 375 + 15 + 60 + 150 = 600 Tokens
Total Output: 50 Tokens
---
Cost (GPT-4-turbo): ~$0.006 + $0.0015 = $0.0075
```

## 🐛 Troubleshooting

### Problem: "MCP Server not available"
```bash
# Prüfe ob MCP Service läuft
curl http://localhost:8080/mcp/status

# Starte MCP Service
cd backend/mcp-service
./mvnw spring-boot:run
```

### Problem: "No tools available"
```bash
# Prüfe registrierte Provider
curl http://localhost:8080/mcp/providers

# Prüfe Tools
curl http://localhost:8080/mcp/tools
```

### Problem: "Tool execution failed"
```bash
# Prüfe MCP Logs
tail -f backend/mcp-server/logs/spring.log

# Teste Tool direkt
curl -X POST http://localhost:8080/mcp/execute \
  -H "Content-Type: application/json" \
  -d '{"toolName": "add_numbers", "arguments": {"a": 5, "b": 3}}'
```

## 📚 Weitere Dokumentation

- [TOOL_CALLING_IMPLEMENTATION.md](./TOOL_CALLING_IMPLEMENTATION.md) - Detaillierte Architektur
- [MCP_SERVICE_QUICKSTART.md](./MCP_SERVICE_QUICKSTART.md) - MCP Server Setup
- [OPENROUTER_QUICKSTART.md](./OPENROUTER_QUICKSTART.md) - OpenRouter Integration

## ✨ Zusammenfassung

✅ **Korrekte Implementierung** - LLM entscheidet selbst welche Tools
✅ **Multi-Tool Support** - Mehrere Tools in einer Anfrage
✅ **Erweiterbar** - Neue Tools ohne Code-Änderung
✅ **Standard-kompatibel** - OpenAI Tool-Calling Format
✅ **Production-Ready** - Error Handling, Logging, Metrics

Die neue Implementierung ersetzt die fehlerhafte `detectAndExecuteTool()` Methode durch echtes LLM-basiertes Tool-Calling! 🚀

