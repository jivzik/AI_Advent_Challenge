# JSON Schema Feature - Strukturierte Antworten

## Problem
Wenn du fragst: "дай список топ книг по жанрам макс 2 в макс 5 жанрах"

Bekommst du Text im JSON:
```json
{"response": "**Фэнтези:**\n- Книга 1\n- Книга 2\n\n**Фантастика:**..."}
```

## Lösung: Custom JSON Schema

Du kannst jetzt ein **JSON Schema** im Request mitgeben!

### Request-Beispiel

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "дай список топ книг по жанрам макс 2 в макс 5 жанрах",
    "userId": "test-user",
    "conversationId": "test-conv-123",
    "jsonMode": true,
    "jsonSchema": "{\"response\": [{\"genre\": \"string\", \"books\": [\"string\"]}]}"
  }'
```

### Besseres Schema-Beispiel (formatiert)

```json
{
  "message": "дай список топ книг по жанрам макс 2 в макс 5 жанрах",
  "userId": "test-user",
  "conversationId": "test-conv-123",
  "jsonMode": true,
  "jsonSchema": "{\n  \"response\": [\n    {\n      \"genre\": \"genre name\",\n      \"books\": [\"book 1\", \"book 2\"]\n    }\n  ]\n}"
}
```

## Erwartete Response vom Backend

```json
{
  "reply": "{\"response\":[{\"genre\":\"Фэнтези\",\"books\":[\"Книга 1\",\"Книга 2\"]},{\"genre\":\"Фантастика\",\"books\":[\"Книга 3\",\"Книга 4\"]}]}",
  "toolName": "PerplexityToolClient",
  "timestamp": "2025-12-02T..."
}
```

Der `reply` enthält dann das strukturierte JSON!

## Wie es funktioniert

### 1. Ohne Custom Schema (Standard)
```json
{
  "jsonMode": true
}
```
→ LLM gibt zurück: `{"response": "text hier"}`
→ Backend extrahiert: `"text hier"`

### 2. Mit Custom Schema
```json
{
  "jsonMode": true,
  "jsonSchema": "{\"response\": [{\"genre\": \"string\", \"books\": [\"string\"]}]}"
}
```
→ LLM gibt zurück: `{"response": [{"genre": "...", "books": [...]}]}`
→ Backend gibt **gesamtes JSON** zurück (nicht nur den response-Teil)

## Tipp: Schema-Generator

Für komplexe Schemas, erstelle erst das gewünschte JSON-Beispiel:

```json
{
  "response": [
    {
      "genre": "Фэнтези",
      "books": ["Книга 1", "Книга 2"]
    },
    {
      "genre": "Фантастика", 
      "books": ["Книга 3", "Книга 4"]
    }
  ]
}
```

Dann in Prompt einbauen:
```
"jsonSchema": "{\"response\": [{\"genre\": \"string\", \"books\": [\"string\"]}]}"
```

## Frontend Integration

Im ChatService:
```typescript
ChatService.sendMessage(
  message,
  userId,
  conversationId,
  true, // jsonMode
  '{"response": [{"genre": "string", "books": ["string"]}]}' // jsonSchema
);
```

## Testing

```bash
# Test mit Custom Schema
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "List top 3 programming languages with their use cases",
    "jsonMode": true,
    "jsonSchema": "{\"response\": [{\"language\": \"string\", \"useCases\": [\"string\"]}]}"
  }' | jq .
```

## Vorteile

✅ **Strukturierte Daten**: Arrays, Objekte statt nur Text
✅ **Vorhersagbar**: Immer die gleiche Struktur  
✅ **Einfach zu parsen**: Im Frontend direkt als JSON verwendbar  
✅ **Flexibel**: Jede Anfrage kann eigenes Schema haben

