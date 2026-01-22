# AgentService Integration für OpenRouter-Service

## Zusammenfassung

Der AgentService aus dem perplexity-service wurde erfolgreich in den openrouter-service integriert. Diese Integration bringt erweiterte Konversationsmanagement-Funktionen, automatische Dialogkompression und PostgreSQL-Persistierung in den openrouter-service.

## Neu erstellte Services

### 1. **AgentService** 
`/backend/openrouter-service/src/main/java/de/jivz/ai_challenge/openrouterservice/service/AgentService.java`

Hauptorchestrator für Chat-Anfragen mit folgenden Verantwortlichkeiten:
- Validierung von Anfragen
- Orchestrierung des Konversationsflusses
- Koordination zwischen History-, Parsing- und LLM-Services
- Automatische History-Kompression bei Schwellwertüberschreitung
- Speicherung von Nachrichten in PostgreSQL

**Wichtige Methoden:**
- `handle(ChatRequest)` - Verarbeitet Chat-Anfragen mit vollständiger Orchestrierung
- `handleWithMcpTools(ChatRequest)` - Verarbeitet Chat mit MCP-Tool-Integration

### 2. **MemoryService**
`/backend/openrouter-service/src/main/java/de/jivz/ai_challenge/openrouterservice/service/MemoryService.java`

Verantwortlich für PostgreSQL-Persistierung:
- Speichert alle Nachrichten in der Datenbank (vollständiger Verlauf)
- Lädt Konversationshistorie
- Unterstützt automatische Summary-Wiederverwendung
- Fehlerbehandlung mit Fallback auf RAM

**Wichtige Methoden:**
- `saveMessage(...)` - Speichert Nachrichten mit Metriken
- `getFullHistory(conversationId)` - Lädt vollständigen Verlauf
- `loadHistoryForLLM(conversationId)` - Lädt optimierten Verlauf mit Summary-Unterstützung
- `saveSummary(...)` - Speichert Zusammenfassungen

### 3. **MessageHistoryManager**
`/backend/openrouter-service/src/main/java/de/jivz/ai_challenge/openrouterservice/service/MessageHistoryManager.java`

Verwaltet Nachrichtenverlauf und JSON-Instruktionen:
- Aktualisiert System-Prompts
- Fügt Nachrichten mit JSON-Instruktionen hinzu
- Unterstützt normale Nachrichten ohne JSON-Modus

**Wichtige Methoden:**
- `prepareHistory(history, request)` - Bereitet History mit System-Prompt und User-Nachricht vor
- `addAssistantResponse(history, response)` - Fügt Assistant-Antwort hinzu

### 4. **JsonResponseParser**
`/backend/openrouter-service/src/main/java/de/jivz/ai_challenge/openrouterservice/service/JsonResponseParser.java`

Parst und bereinigt JSON-Antworten vom LLM:
- Entfernt Markdown-Code-Block-Syntax
- Validiert JSON
- Extrahiert relevante Felder basierend auf Request-Modus

**Wichtige Methoden:**
- `parse(rawResponse, request)` - Parst und bereinigt JSON-Antworten

### 5. **DialogCompressionService**
`/backend/openrouter-service/src/main/java/de/jivz/ai_challenge/openrouterservice/service/DialogCompressionService.java`

Komprimiert Dialoghistorie mittels Zusammenfassungen:
- Automatische Kompression nach N Nachrichten (Schwellwert: 5)
- Erstellt Zusammenfassungen mit LLM
- Speichert Summaries in PostgreSQL
- Behält System-Prompts + Summary + letzte Nachrichten

**Wichtige Methoden:**
- `checkAndCompress(conversationId)` - Prüft und komprimiert wenn nötig
- `getCompressedHistory(conversationId)` - Lädt komprimierte Version
- `hasCompressedVersion(conversationId)` - Prüft ob komprimierte Version existiert

## Erweiterte DTOs

### **ChatRequest**
Erweitert um folgende Felder:
- `userId` - User-Identifikator für Tracking
- `jsonMode` - JSON-Modus aktivieren
- `jsonSchema` - Benutzerdefiniertes JSON-Schema
- `autoSchema` - Automatisches JSON-Schema generieren
- `systemPrompt` - System-Prompt für Agent-Persönlichkeit
- `provider` - AI-Provider (openrouter, perplexity)

## Repository-Erweiterungen

### **MemoryRepository**
Erweitert um:
- `findLastSummary(conversationId)` - Findet letzte Zusammenfassung
- `findByConversationIdAndTimestampAfterOrderByTimestampAsc(...)` - Findet Nachrichten nach Zeitstempel

## Hauptfunktionen

### 1. **Automatische Dialogkompression**
```java
// Beispiel: Nach 5+ Nachrichten wird automatisch komprimiert
List<Message> history = loadHistoryWithCompression(conversationId);
```

Workflow:
1. Prüft ob Schwellwert erreicht (5+ Nachrichten)
2. Erstellt Zusammenfassung mit LLM
3. Speichert Summary in PostgreSQL
4. Behält: System + Summary + letzte 2 Nachrichten

### 2. **Summary-Wiederverwendung**
```java
// Lädt optimierte History mit gespeichertem Summary
List<Message> optimizedHistory = memoryService.loadHistoryForLLM(conversationId);
```

Vorteile:
- Summary wird einmal erstellt, für immer wiederverwendet (0 Tokens)
- Nur neue Nachrichten nach Summary werden gesendet
- LLM versteht vollständigen Kontext mit weniger Tokens

### 3. **PostgreSQL-Persistierung**
```java
// Speichert Nachricht mit Metriken
memoryService.saveMessage(
    conversationId, 
    userId, 
    "assistant", 
    parsedReply, 
    modelName, 
    metrics
);
```

Alle Nachrichten werden in PostgreSQL gespeichert:
- Vollständiger Verlauf bleibt erhalten
- Unterstützt Metriken (Tokens, Kosten, Antwortzeit)
- Fehlerbehandlung mit Fallback

### 4. **JSON-Modus**
```java
ChatRequest request = ChatRequest.builder()
    .message("Gib mir strukturierte Daten")
    .jsonMode(true)
    .autoSchema(true)
    .build();
```

Unterstützt:
- Einfaches JSON-Format (`{"response": "...", "status": "success"}`)
- Benutzerdefiniertes JSON-Schema
- Automatische Schema-Generierung

## Integration in Controller

Der AgentService kann in OpenRouterChatController verwendet werden:

```java
@PostMapping("/agent")
public ResponseEntity<ChatResponse> agentChat(@RequestBody ChatRequest request) {
    ChatResponse response = agentService.handle(request);
    return ResponseEntity.ok(response);
}

@PostMapping("/agent/mcp")
public ResponseEntity<ChatResponse> agentChatWithMcp(@RequestBody ChatRequest request) {
    ChatResponse response = agentService.handleWithMcpTools(request);
    return ResponseEntity.ok(response);
}
```

## Technische Details

### Dependencies
- Spring Boot 3.x
- Spring Data JPA
- PostgreSQL
- Lombok
- Jackson (JSON)
- WebClient (reactive HTTP)

### Architektur-Prinzipien
- **Single Responsibility Principle**: Jeder Service hat eine klare Verantwortung
- **Dependency Injection**: Über Constructor Injection mit @Qualifier
- **Error Handling**: Graceful Degradation mit Fallback auf RAM
- **Logging**: Strukturierte Logs mit Emojis für bessere Lesbarkeit

## Build-Status

✅ **BUILD SUCCESS**
```
[INFO] Building openrouter-service 0.0.1-SNAPSHOT
[INFO] Compiling 40 source files with javac [debug parameters release 17]
[INFO] BUILD SUCCESS
```

## Nächste Schritte

1. **Controller-Integration**: AgentService in OpenRouterChatController einbinden
2. **Testing**: Unit- und Integrationstests erstellen
3. **Documentation**: API-Dokumentation mit Swagger erweitern
4. **Monitoring**: Metriken für Performance-Tracking hinzufügen

## Vorteile der Integration

1. **Token-Einsparungen**: Automatische Summary-Wiederverwendung spart Tokens
2. **Bessere Historie**: Vollständiger Verlauf in PostgreSQL
3. **Flexibilität**: JSON-Modus für strukturierte Antworten
4. **Skalierbarkeit**: Automatische Kompression bei langen Gesprächen
5. **Robustheit**: Fehlerbehandlung und Fallback-Mechanismen

## Wartung

- Services sind gut dokumentiert mit JavaDoc
- Logging ermöglicht einfaches Debugging
- Modulare Architektur erleichtert Wartung und Erweiterung

