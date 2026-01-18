# 🚀 QuickStart: MCP Server + Google Service Integration

## Was wurde implementiert?

Ein vollständiges System mit **Tool-basierter Architektur** (Strategy Pattern):
1. **MCP Server** mit automatischer Tool-Registrierung
2. **Google Service** als MCP-Tools integriert (6 Tools)
3. **Native Tools** für grundlegende Funktionen (5 Tools)
4. **Strategy Pattern** - jedes Tool ist eine austauschbare Komponente
5. **SOLID Principles** - saubere, erweiterbare Architektur

## ⚡ Schnellstart (3 Schritte)

### Schritt 1: Google Service starten

```bash
cd backend/google-service
./mvnw spring-boot:run
```

Überprüfen:
```bash
curl http://localhost:8082/api/tasks/lists
```

### Schritt 2: MCP Service starten

```bash
cd backend/mcp-service
./mvnw spring-boot:run
```

Überprüfen:
```bash
curl http://localhost:8081/api/status
```

### Schritt 3: Test ausführen

```bash
# Alle Tools anzeigen (inkl. google-service)
curl http://localhost:8081/api/tools | jq

# Google Service direkt über MCP aufrufen
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "google_tasks_list",
    "arguments": {}
  }' | jq
```

## 📋 Verfügbare Endpoints

### 1. MCP Status & Tools

```bash
# Status prüfen
curl http://localhost:8081/api/status | jq

# Alle Tools auflisten
curl http://localhost:8081/api/tools | jq
```

### 2. Google Service Tools nutzen

**Task-Listen abrufen:**
```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "google_tasks_list",
    "arguments": {}
  }' | jq
```

**Tasks abrufen:**
```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "google_tasks_get",
    "arguments": {}
  }' | jq
```

**Task erstellen:**
```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "google_tasks_create",
    "arguments": {
      "title": "Test Task via MCP",
      "notes": "Erstellt über MCP Server"
    }
  }' | jq
```

**Task aktualisieren:**
```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "google_tasks_update",
    "arguments": {
      "taskId": "YOUR_TASK_ID",
      "status": "completed"
    }
  }' | jq
```

**Task löschen:**
```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "google_tasks_delete",
    "arguments": {
      "taskId": "YOUR_TASK_ID"
    }
  }' | jq
```

### 3. Perplexity mit MCP Tools

**Einfache Anfrage an Perplexity (ohne Tools):**
```bash
curl -X POST http://localhost:8080/mcp/perplexity/ask \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Was ist die Hauptstadt von Deutschland?",
    "useTools": false
  }' | jq
```

**Perplexity mit Google Service Tools:**
```bash
curl -X POST http://localhost:8080/mcp/perplexity/ask \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Zeige mir alle meine Google Tasks",
    "useTools": true
  }' | jq
```

**Response Beispiel:**
```json
{
  "answer": "Hier sind Ihre Google Tasks: ...",
  "toolsUsed": ["google_tasks_get"],
  "success": true
}
```

## 🔧 Konfiguration

### application.properties (mcp-server)

```properties
# Server Port
server.port=8081

# Google Service URL
google.service.url=http://localhost:8082

# Logging
logging.level.de.jivz.mcp=DEBUG
```

## 📊 Architektur-Übersicht

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────┐
│      MCP Server (Port 8081)         │
├─────────────────────────────────────┤
│  McpController                      │
│    ├─ /api/tools                    │
│    ├─ /api/tools/execute            │
│    └─ /api/status                   │
├─────────────────────────────────────┤
│  McpServerService (Facade)          │
│    ├─ ToolExecutorService           │
│    └─ ToolsDefinitionService        │
├─────────────────────────────────────┤
│  ToolRegistry                       │
│    └─ Auto-registers all Tools      │
├─────────────────────────────────────┤
│  Tools (@Component):                │
│    ├─ Native Tools (5)              │
│    │   ├─ AddNumbersTool            │
│    │   ├─ GetCurrentWeatherTool     │
│    │   └─ ...                        │
│    └─ Google Tools (6)              │
│        ├─ GoogleTasksListTool       │
│        ├─ GoogleTasksCreateTool     │
│        └─ ... ──────────────────────┼──────┐
└─────────────────────────────────────┘      │
                                              │
                                              ▼
                                    ┌──────────────────┐
                                    │  Google Service  │
                                    │  (Port 8082)     │
                                    │                  │
                                    │  Google Tasks    │
                                    │  API Integration │
                                    └──────────────────┘
```

## 🧪 Vollständiger Test

```bash
# Test-Skript ausführen
chmod +x test-perplexity-google-integration.sh
./test-perplexity-google-integration.sh
```

## 🎯 Use Cases

### Use Case 1: Task-Verwaltung über Perplexity

```bash
# Perplexity erstellt automatisch eine Task
curl -X POST http://localhost:8080/mcp/perplexity/ask \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Erstelle eine Aufgabe '\''Meeting vorbereiten'\'' für morgen",
    "useTools": true
  }'
```

Perplexity wird:
1. Erkennen, dass eine Task erstellt werden soll
2. Das Tool `google_tasks_create` aufrufen
3. Die Task mit passendem Datum erstellen
4. Eine Bestätigung zurückgeben

### Use Case 2: Multi-Step Reasoning

```bash
# Perplexity führt mehrere Tool-Aufrufe durch
curl -X POST http://localhost:8080/mcp/perplexity/ask \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Zeige mir alle meine Tasks und markiere '\''Einkaufen'\'' als erledigt",
    "useTools": true
  }'
```

Perplexity wird:
1. `google_tasks_get` aufrufen
2. Die Task "Einkaufen" finden
3. `google_tasks_update` aufrufen mit `status: completed`
4. Bestätigung geben

### Use Case 3: Native Tools kombinieren

```bash
curl -X POST http://localhost:8080/mcp/perplexity/ask \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Berechne 15 + 27 und erstelle eine Task mit dem Ergebnis",
    "useTools": true
  }'
```

Perplexity nutzt:
1. `add_numbers` (Native Tool)
2. `google_tasks_create` (Google Service Tool)

## 🔍 Debugging

### Logs ansehen

```bash
# MCP Service Logs
cd backend/mcp-server
./mvnw spring-boot:run

# Zeigt:
# - Registrierte Provider
# - Tool-Aufrufe
# - Google Service Calls
```

### Probleme beheben

**Problem: Google Service nicht erreichbar**
```bash
# Prüfen ob Google Service läuft
curl http://localhost:8082/api/tasks/lists

# Falls nicht, starten:
cd backend/google-service
./mvnw spring-boot:run
```

**Problem: Tools werden nicht gefunden**
```bash
# Provider-Status prüfen
curl http://localhost:8081/api/tools | jq

# Sollte zeigen:
# 11 Tools: 5 native + 6 google
```

## ✅ Erfolgsmetriken

Nach erfolgreichem Setup sollten Sie sehen:

1. **11 Tools registriert**: 5 native + 6 google
2. **Google Service erreichbar**: Alle 6 CRUD-Operationen funktionieren
3. **Tool-basierte Architektur**: Jedes Tool ist eine eigenständige @Component
4. **Strategy Pattern**: Alle Tools implementieren das Tool-Interface

## 🎉 Zusammenfassung

Sie haben jetzt:
- ✅ MCP Server mit Tool-basierter Architektur (Strategy Pattern)
- ✅ Google Service als MCP-Tools integriert
- ✅ SOLID Principles implementiert
- ✅ Erweiterbar für weitere Tools

**Nächste Schritte:**
1. Weitere Tools als @Component hinzufügen
2. Authentifizierung für Service-Aufrufe implementieren
3. Error Handling und Retry-Logik verbessern
4. Frontend-Integration für visuelle Tool-Übersicht

**Dokumentation:**
- **Refactoring Guide**: `backend/mcp-server/REFACTORING_GUIDE.md`
- **Architektur**: `docs/architecture/MCP_MULTI_PROVIDER_ARCHITECTURE.md`

