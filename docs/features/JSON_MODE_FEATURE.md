# JSON Response Mode Feature

## Übersicht

Die Chatbox unterstützt jetzt einen **JSON-Antwort-Modus**, der sicherstellt, dass alle Antworten vom LLM in einem validen JSON-Format zurückgegeben werden.

## Funktionalität

### Frontend
- ✅ Neue Checkbox "JSON-Antworten" im Chat-Header
- ✅ Checkbox-Status wird an Backend übermittelt
- ✅ Visuelles Feedback im UI

### Backend
- ✅ Neues Feld `jsonMode` in `ChatRequest`
- ✅ System-Prompt für JSON-Format wird automatisch hinzugefügt
- ✅ Validierung und Parsing der JSON-Antworten
- ✅ Fehlerbehandlung bei ungültigen JSON-Responses

## Technische Details

### Request Format
```json
{
  "message": "Deine Frage hier",
  "userId": "user-123",
  "conversationId": "conv-456",
  "jsonMode": true
}
```

### Response Format (JSON-Modus aktiviert)
Das LLM antwortet immer in diesem Format:
```json
{
  "response": "Die eigentliche Antwort hier"
}
```

Die API gibt dann zurück:
```json
{
  "reply": "Die eigentliche Antwort hier",
  "toolName": "PerplexityToolClient",
  "timestamp": "2025-12-02T08:34:16.162950138Z"
}
```

## System-Prompt

Wenn JSON-Modus aktiviert ist, wird automatisch folgender System-Prompt eingefügt:

```
You MUST respond ONLY with valid JSON in this exact format:
{"response": "your answer here"}

STRICT RULES:
- Always include the "response" field
- No additional fields allowed
- No markdown formatting
- No code blocks (```)
- Just pure, valid JSON
- The entire response must be parseable JSON

EXAMPLES:
Correct: {"response": "Spring Boot ist ein Framework für Java-Anwendungen."}
Correct: {"response": "Die Hauptstadt von Deutschland ist Berlin."}
WRONG: ```json{"response": "..."}```
WRONG: {"response": "...", "extra": "field"}
```

## Implementierte Features

### 1. Format-Vorgabe für LLM
- System-Prompt definiert exaktes JSON-Format
- Klare Beispiele und Regeln
- Verhindert unerwünschte Formate (z.B. Markdown Code-Blocks)

### 2. JSON-Parsing & Validierung
- Backend parst JSON-Response automatisch
- Extrahiert das `response`-Feld
- Bereinigt potentielle Markdown-Formatierung
- Fehlerbehandlung bei ungültigem JSON

### 3. UI-Integration
- Toggle-Checkbox im Chat-Header
- Deaktiviert während Anfragen
- Konsistentes Design mit bestehendem UI

## Testing

### Manueller Test (mit curl)

**JSON-Modus aktiviert:**
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Was ist die Hauptstadt von Deutschland?",
    "userId": "test-user",
    "conversationId": "test-conv-123",
    "jsonMode": true
  }'
```

**Normaler Modus:**
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Nenne mir 3 Programmiersprachen",
    "userId": "test-user",
    "conversationId": "test-conv-456",
    "jsonMode": false
  }'
```

## Dateien geändert

### Frontend
1. `/frontend/src/components/ChatInterface.vue`
   - Checkbox für JSON-Modus hinzugefügt
   - `jsonResponseMode` Ref erstellt
   - CSS-Styles für Toggle

2. `/frontend/src/services/chatService.ts`
   - `jsonMode` Parameter zu `sendMessage()` hinzugefügt

3. `/frontend/src/types/types.ts`
   - `jsonMode?: boolean` zu `ChatRequest` Interface

### Backend
1. `/backend/perplexity-service/src/main/java/de/jivz/ai_challenge/dto/ChatRequest.java`
   - `jsonMode` Feld hinzugefügt
   - Getter/Setter implementiert

2. `/backend/perplexity-service/src/main/java/de/jivz/ai_challenge/service/AgentService.java`
   - System-Prompt für JSON-Modus
   - JSON-Parsing Logik
   - Fehlerbehandlung

3. `/backend/perplexity-service/src/main/java/de/jivz/ai_challenge/config/JacksonConfig.java` *(NEU)*
   - ObjectMapper Bean konfiguriert

4. `/backend/perplexity-service/pom.xml`
   - Jackson-Databind Dependency hinzugefügt

## Best Practices

✅ **Strikte System-Prompts**: Klare Anweisungen für JSON-Format  
✅ **Validierung**: JSON wird immer geparst und validiert  
✅ **Fehlerbehandlung**: Fallback wenn JSON ungültig ist  
✅ **Logging**: JSON-Parsing-Fehler werden protokolliert  
✅ **User-Feedback**: Fehlermeldungen werden angezeigt  
✅ **Flexibilität**: Beide Modi (JSON/Normal) funktionieren parallel

## Ergebnis

✅ Ответ от LLM можно распарсить  
✅ Checkbox "JSON-Antworten" im UI  
✅ Alle Antworten im JSON-Modus sind valide: `{"response": "..."}`  
✅ Das Feld `response` ist immer vorhanden  
✅ Keine zusätzlichen Felder  
✅ Robuste Fehlerbehandlung

