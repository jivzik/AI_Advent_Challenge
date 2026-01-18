# System Prompt Feature

## Übersicht

Dieses Feature ermöglicht es, den System-Prompt (die "Persönlichkeit" des AI-Agenten) dynamisch während eines laufenden Dialogs zu ändern. Der Benutzer kann beobachten, wie sich die Antworten des Agenten je nach System-Prompt ändern (Ton, Stil, Format usw.).

## Funktionsweise

### UI-Komponenten

- **System-Prompt-Textfeld**: Ein mehrzeiliges Textfeld oben im Chat-Interface
- **Standard-Text**: "Ты дружелюбный ассистент, отвечай кратко и по делу."
- **Live-Änderung**: Der System-Prompt kann jederzeit geändert werden - bei der nächsten Nachricht wird der neue Prompt verwendet

### API-Struktur

Request an `/api/chat`:
```json
{
  "message": "Привет, кто ты?",
  "conversationId": "abc123",
  "systemPrompt": "Ты дружелюбный ассистент...",
  "userId": "user-123",
  "jsonMode": false,
  "autoSchema": false
}
```

### Backend-Logik

1. **System-Prompt wird bei jedem Request übergeben**
2. **Der System-Prompt wird als erste Nachricht in der Historie gesetzt**
3. **Bei Änderung des System-Prompts wird die bestehende System-Nachricht ersetzt**
4. **Die Konversationshistorie bleibt erhalten** - nur die "Persönlichkeit" ändert sich

### Nachrichtenformat für LLM

```
[
  { "role": "system", "content": "Aktueller System-Prompt" },
  { "role": "user", "content": "Erste Nachricht" },
  { "role": "assistant", "content": "Erste Antwort" },
  { "role": "user", "content": "Zweite Nachricht" },
  // ...
]
```

## Beispiel-Szenarien

### Szenario 1: Wechsel von freundlich zu formell

**Start-Prompt**: "Ты дружелюбный ассистент, используй эмодзи и неформальный стиль"

User: "Привет! Как дела?"
AI: "Привет! 😊 Всё отлично! Чем могу помочь? 🎉"

**Geänderter Prompt**: "Ты строгий профессиональный консультант, используй формальный стиль"

User: "Расскажи про JavaScript"
AI: "JavaScript является высокоуровневым языком программирования, широко применяемым для веб-разработки..."

### Szenario 2: Wechsel der Sprache

**Start-Prompt**: "Отвечай только на русском языке"

**Geänderter Prompt**: "Отвечай только на английском языке"

## Technische Details

### Geänderte Dateien

#### Backend
- `ChatRequest.java` - Neues Feld `systemPrompt`
- `MessageHistoryManager.java` - Neue Methode `updateSystemPrompt()`

#### Frontend
- `types.ts` - Interface erweitert um `systemPrompt`
- `chatService.ts` - `systemPrompt` in SendMessageOptions
- `ChatInterface.vue` - System-Prompt-Textfeld und -Logik

## Verwendung

1. Starte Backend und Frontend
2. Öffne den Chat
3. Ändere den System-Prompt im Textfeld oben
4. Führe einen Dialog
5. Ändere den System-Prompt erneut und beobachte die Verhaltensänderung

