# Developer Assistant API - Dokumentation

## Übersicht

Der Developer Assistant ist ein intelligenter REST API Endpoint, der Entwicklern bei ihrer Arbeit hilft durch:

- **RAG (Retrieval Augmented Generation)**: Suche in der Projekt-Dokumentation
- **Git Context**: Integration des aktuellen Git-Status
- **Spezialisierte AI Prompts**: Optimierte Prompts für Code-Generierung, Debugging und Refactoring
- **Parallele MCP-Aufrufe**: Effiziente Datenabfrage von mehreren MCP Services

## Architektur

### 3-Schritt-Prozess

```
User Query
    ↓
[1. Query Optimierung]
    → AI optimiert die Suchanfrage
    ↓
[2. Parallele MCP Aufrufe]
    ├─ RAG Search (Dokumentation)
    └─ Git Context (Branch, Status, Log)
    ↓
[3. AI Antwort Generierung]
    → Kontextbewusste Antwort mit Quellen
    ↓
DevAssistantResponse (JSON)
```

## API Endpoints

### POST `/api/dev/help`

Hauptendpoint für Developer-Anfragen.

**Request Body:**
```json
{
  "query": "Wie erstelle ich einen neuen MCP Provider?",
  "userId": "user123",
  "includeGitContext": true,
  "maxDocuments": 5,
  "model": "anthropic/claude-3.5-sonnet",
  "temperature": 0.3
}
```

**Response:**
```json
{
  "answer": "# MCP Provider erstellen\n\n...",
  "sources": [
    {
      "documentId": "doc-123",
      "filePath": "docs/features/GIT_TOOLS_PROVIDER_FEATURE.md",
      "title": "Git Tools Provider Feature",
      "relevanceScore": 0.95,
      "excerpt": "Um einen neuen MCP Provider zu erstellen..."
    }
  ],
  "gitContext": {
    "currentBranch": "feature/dev-assistant",
    "gitStatus": "modified: 2 files",
    "recentCommits": [
      "commit abc123: Added DevAssistantService",
      "commit def456: Created DTOs"
    ],
    "hasUncommittedChanges": true
  },
  "suggestedFiles": [
    "src/main/java/.../.../mcp/BaseMCPService.java"
  ],
  "codeExamples": [],
  "responseTimeMs": 8542,
  "model": "anthropic/claude-3.5-sonnet"
}
```

### GET `/api/dev/help/health`

Health Check Endpoint.

**Response:**
```
Developer Assistant Service is up and running
```

### GET `/api/dev/help/info`

Service Informationen und verfügbare Features.

**Response:**
```json
{
  "serviceName": "Developer Assistant",
  "version": "1.0.0",
  "features": [
    "RAG-basierte Dokumentationssuche",
    "Git Context Integration",
    "AI-optimierte Query-Verarbeitung",
    "Spezialisierte Code-Prompts",
    "Parallele MCP-Aufrufe",
    "Strukturierte Antworten mit Quellen"
  ],
  "supportedContexts": [
    "developer",
    "docker",
    "tasks",
    "calendar"
  ]
}
```

## Konfiguration

### application.properties

```properties
# Developer Assistant Configuration
dev.assistant.ai.model=anthropic/claude-3.5-sonnet
dev.assistant.ai.temperature=0.3
dev.assistant.ai.search-temperature=0.2
dev.assistant.rag.max-documents=5
dev.assistant.mcp.timeout-ms=5000

# MCP Service URLs
mcp.rag.base-url=http://localhost:8086
mcp.git.base-url=http://localhost:8083
```

## Verwendete Prompts

Der Service nutzt spezialisierte Prompts aus `resources/prompts/`:

1. **developer-search.md**: Optimiert Suchanfragen für bessere RAG-Ergebnisse
2. **context-developer.md**: Hauptprompt für Entwickler-Kontext
3. **developer-code-style.md**: Code-Style Guidelines
4. **tool-results.md**: Formatierung von Tool-Ergebnissen

## Integration mit MCP Services

### RAG MCP Server (Port 8086)

**Tool:** `search_documents`

```json
{
  "toolName": "search_documents",
  "arguments": {
    "query": "optimized search query",
    "limit": 5
  }
}
```

### Git MCP Server (Port 8083)

**Tools:**
- `get_current_branch`: Aktueller Git Branch
- `get_git_status`: Git Status (modified files, etc.)
- `get_git_log`: Letzte Commits (limit: 5)

```json
{
  "toolName": "get_current_branch",
  "arguments": {}
}
```

## Implementierte Komponenten

### 1. DTOs

- **DevAssistantRequest.java**: Request-Modell mit Validierung
- **DevAssistantResponse.java**: Response-Modell mit nested DTOs
  - `SourceReference`: Dokumentationsquelle
  - `GitContextInfo`: Git-Informationen
  - `CodeExample`: Code-Beispiele

### 2. Service Layer

- **DevAssistantService.java**: Hauptlogik mit 3-Schritt-Prozess
  - Query-Optimierung durch AI
  - Parallele MCP-Aufrufe mit Reactor
  - Kontextbewusste Antwort-Generierung

### 3. Controller

- **DevAssistantController.java**: REST API mit Swagger-Dokumentation
  - `/api/dev/help` - Hauptendpoint
  - `/api/dev/help/health` - Health Check
  - `/api/dev/help/info` - Service Info

### 4. MCP Services

- **GitMcpService.java**: Git Operations
- Integration mit bestehendem `RagMcpService`

## Error Handling

Der Service implementiert Graceful Degradation:

| Fehler | Verhalten |
|--------|-----------|
| Query Optimization failed | Nutzt Original-Query |
| RAG MCP timeout | Fährt ohne Dokumentation fort |
| Git MCP timeout | Fährt ohne Git-Context fort |
| AI Response failed | Gibt 500 Error mit Fehlermeldung zurück |

**Prinzip:** Besser eine teilweise Antwort als gar keine Antwort.

## Performance

- **Parallele Execution**: RAG und Git Calls laufen parallel
- **Timeouts**: 5 Sekunden pro MCP Call (konfigurierbar)
- **Durchschnittliche Response Time**: 8-15 Sekunden
  - Query Optimization: 2-3 Sekunden
  - MCP Calls (parallel): 2-5 Sekunden
  - AI Response: 5-10 Sekunden

## Beispiel-Nutzung

### cURL

```bash
curl -X POST http://localhost:8084/api/dev/help \
  -H "Content-Type: application/json" \
  -d '{
    "query": "Wie implementiere ich einen neuen Git Provider?",
    "userId": "dev-user-123",
    "includeGitContext": true,
    "maxDocuments": 5
  }'
```

### Java (RestTemplate)

```java
DevAssistantRequest request = DevAssistantRequest.builder()
    .query("Wie erstelle ich einen MCP Provider?")
    .userId("user123")
    .includeGitContext(true)
    .maxDocuments(5)
    .build();

DevAssistantResponse response = restTemplate.postForObject(
    "http://localhost:8084/api/dev/help",
    request,
    DevAssistantResponse.class
);
```

### JavaScript (Fetch)

```javascript
const response = await fetch('http://localhost:8084/api/dev/help', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    query: 'Wie debugge ich MCP Tools?',
    userId: 'user123',
    includeGitContext: true,
    maxDocuments: 5
  })
});

const data = await response.json();
console.log(data.answer);
console.log('Sources:', data.sources);
```

## Testing

### Unit Tests

```bash
cd backend/openrouter-service
mvn test
```

### Integration Test

```bash
# Starte alle Services
docker-compose up -d

# Test Developer Assistant
curl http://localhost:8084/api/dev/help/health

# Vollständiger Test
./test-dev-assistant.sh
```

## Swagger UI

Öffne im Browser: http://localhost:8084/swagger-ui.html

Dort kannst du:
- Alle Endpoints testen
- Request/Response Schemas sehen
- Beispiel-Requests ausführen

## Troubleshooting

### Problem: RAG MCP nicht erreichbar

```
WARN: RAG search failed: Connection refused
```

**Lösung:** Starte den RAG MCP Server:
```bash
cd backend/rag-mcp-server
mvn spring-boot:run
```

### Problem: Git MCP nicht erreichbar

```
WARN: Git context fetch failed: 404 Not Found
```

**Lösung:** Prüfe, ob Git Tools im MCP Server registriert sind:
```bash
curl http://localhost:8083/api/tools
```

### Problem: Prompts nicht gefunden

```
ERROR: Failed to load prompts
```

**Lösung:** Prüfe, ob alle Prompt-Dateien existieren:
```bash
ls -la backend/openrouter-service/src/main/resources/prompts/
```

## Roadmap

### Geplante Features

- [ ] **Tool Calling Support**: AI kann weitere Tools dynamisch aufrufen
- [ ] **Caching**: Cache für häufige Queries
- [ ] **Conversation History**: Multi-Turn Conversations
- [ ] **Code Analysis**: Direkte Code-Analyse aus Projektdateien
- [ ] **Metrics & Analytics**: Tracking von Anfragen und Performance

### Version 2.0

- WebSocket Support für Streaming-Antworten
- Multi-Language Support (English, Deutsch)
- Custom Prompt Templates pro User
- Integration mit IDE Plugins

## Support

Bei Fragen oder Problemen:
- Siehe Swagger UI: http://localhost:8084/swagger-ui.html
- Logs: `backend/openrouter-service/logs/`
- GitHub Issues: [Project Repository]

## Lizenz

[Deine Lizenz hier]

