# Perplexity MCP Server - Tools Query Guide

## 🎯 Überblick

Der Perplexity MCP Server stellt folgende Tools zur Verfügung:

| Tool | Beschreibung | Verwendung |
|------|-------------|-----------|
| **perplexity_ask** | Stelle Fragen an Perplexity Sonar AI | Allgemeine Q&A |
| **perplexity_search** | Suche mit Internet-Zugang | Event-Recherche, aktuelle Info |

## 📋 Available Tools

### 1. perplexity_ask
```
Name: perplexity_ask
Description: Ask a question to Perplexity Sonar AI model
```

**Parameter:**
- `prompt` (required, string): Die Frage oder Anfrage
- `model` (optional, string): Modell-Auswahl (default: "sonar")
- `temperature` (optional, number): Kreativität der Antwort (0.0-1.0, default: 0.7)
- `max_tokens` (optional, number): Maximale Antwort-Länge (default: 1000)

**Beispiel:**
```json
{
  "name": "perplexity_ask",
  "arguments": {
    "prompt": "Wann findet die AWS Summit 2025 statt?",
    "temperature": 0.5,
    "max_tokens": 500
  }
}
```

### 2. perplexity_search
```
Name: perplexity_search
Description: Search for information using Perplexity with internet access
```

**Parameter:**
- `query` (required, string): Die Suchanfrage

**Beispiel:**
```json
{
  "name": "perplexity_search",
  "arguments": {
    "query": "Spring Conference 2025 dates location agenda"
  }
}
```

---

## 🚀 Tools Query - 3 Wege

### Weg 1: Einfache Liste (Bash)
```bash
./query-tools.sh
```

**Output:**
```
Available Tools in Perplexity MCP Server:
==========================================

1. perplexity_ask
   • Description: Ask a question to Perplexity Sonar AI
   • Parameters: prompt, model, temperature, max_tokens

2. perplexity_search
   • Description: Search for information with internet access
   • Parameters: query

==========================================
```

### Weg 2: Detaillierte Info (Node.js)
```bash
node list-tools.js
```

**Output:**
```
✅ Available Tools:

============================================================

[1] PERPLEXITY_ASK
----------------------------------------
Description: Ask a question to Perplexity Sonar AI model...

Input Schema:
  • prompt
    - Type: string
    - Description: The question or prompt to send to Perplexity Sonar
    - Required
  • model
    - Type: string
    - Default: sonar
  • temperature
    - Type: number
    - Default: 0.7
  • max_tokens
    - Type: number
    - Default: 1000

[2] PERPLEXITY_SEARCH
...
```

### Weg 3: JSON Export
```bash
node export-tools.js > tools.json
```

**Output (tools.json):**
```json
{
  "server": {
    "name": "Perplexity MCP Server",
    "version": "1.0.0",
    "description": "MCP Server for Perplexity AI Integration",
    "timestamp": "2025-12-18T10:30:00.000Z"
  },
  "tools": [
    {
      "name": "perplexity_ask",
      "description": "Ask a question to Perplexity Sonar AI model...",
      "inputSchema": { ... },
      "usage": { ... }
    },
    {
      "name": "perplexity_search",
      "description": "Search for information using Perplexity...",
      "inputSchema": { ... },
      "usage": { ... }
    }
  ]
}
```

---

## 📊 Tools Details

### perplexity_ask

**Wofür?** Allgemeine Fragen an KI beantworten

**Beispiele:**
```bash
# Einfache Frage
{
  "prompt": "Was ist Machine Learning?",
  "temperature": 0.7
}

# Technische Frage mit niedrigem Temperature
{
  "prompt": "Erklär die Differentialgleichung y' = 2x",
  "temperature": 0.2,
  "max_tokens": 300
}

# Kreative Anfrage
{
  "prompt": "Schreib einen Pitch für mein SaaS Produkt",
  "temperature": 0.9,
  "max_tokens": 1500
}
```

### perplexity_search

**Wofür?** Aktuelle Informationen recherchieren

**Beispiele:**
```bash
# Event-Recherche
{
  "query": "Google I/O 2025 dates location registration link"
}

# News-Recherche
{
  "query": "Latest developments in generative AI December 2025"
}

# Produkt-Information
{
  "query": "Spring Boot 4.0 release date features"
}

# Lokale Events
{
  "query": "Tech conferences Berlin 2025"
}
```

---

## 🔧 Integration mit Backend

### Aus Java aufrufen

```java
// Via McpToolClient
String result = mcpToolClient.executeTool("perplexity_search", 
  Map.of("query", "AWS Summit 2025 dates"));

// Ergebnis verarbeiten
String answer = parseResult(result).get("answer");
```

### Mit TaskCreationService

```java
// Der Service nutzt perplexity_search automatisch
ReminderSummary task = taskCreationService.createTaskWithEventResearch(
  userId, 
  "Erstelle einen Task für die Java Conference 2025"
);
```

---

## 📊 Workflow: Wie Tools verwendet werden

```
Benutzer-Input
    ↓
ReminderController
    ↓
TaskCreationService
    ├─ fetches Tools from McpToolClient
    ├─ builds System Prompt with Tools
    ├─ calls Sonar LLM
    │   ├─ LLM entscheidet: perplexity_search nötig?
    │   └─ LLM constructs tool call
    ├─ executes Tool (perplexity_ask oder perplexity_search)
    ├─ gets Result
    └─ sends to LLM für Task-Erstellung
        ↓
    Task wird erstellt und gespeichert
```

---

## 🎓 Beispiel-Workflow: Event-Task

### Schritt 1: Benutzer fragt
```
"Erstelle einen Task für die Spring Conference 2025"
```

### Schritt 2: System-Prompt wird generiert
```
Available Tools:
1. perplexity_ask - Ask Perplexity questions
2. perplexity_search - Search the web

Task: Create a task based on user request
If it's about an event, use perplexity_search to gather info
```

### Schritt 3: Sonar LLM antwortet
```json
{
  "step": "tool",
  "tool_calls": [
    {
      "name": "perplexity_search",
      "arguments": {
        "query": "Spring Conference 2025 dates location registration link agenda"
      }
    }
  ]
}
```

### Schritt 4: Tool wird ausgeführt
```
perplexity_search wird aufgerufen mit der Query
Ergebnis: 
{
  "answer": "Spring Conference 2025 findet vom 22.-24. Mai in San Francisco statt...",
  "citations": [...],
  ...
}
```

### Schritt 5: LLM erstellt Task
```json
{
  "step": "tool",
  "tool_calls": [
    {
      "name": "reminder_create_task",
      "arguments": {
        "title": "Spring Conference 2025",
        "description": "Attend Spring Conference 2025\nDate: May 22-24, 2025\nLocation: San Francisco\nRegistration: ...",
        "priority": "HIGH",
        "date": "2025-05-22"
      }
    }
  ]
}
```

### Schritt 6: Task wird gespeichert
```
✅ Task erstellt und in DB gespeichert
```

---

## 🛠️ Debugging

### Logs prüfen
```bash
# Terminal 1: Server starten mit Logs
npm run start

# Terminal 2: Tools query
./query-tools.sh --details
```

### JSON validieren
```bash
# JSON exportieren
node export-tools.js > tools.json

# JSON validieren
jq . tools.json
```

### Tool-Ausführung testen
```bash
# Mit TaskCreationService testen
curl -X POST http://localhost:8080/api/reminder/task/create?userId=test \
  -H "Content-Type: application/json" \
  -d '{"taskRequest":"Erstelle einen Task für die Java Conference 2025"}'

# Logs prüfen
tail -f logs/application.log | grep -i "perplexity_search"
```

---

## 📝 Cheat Sheet

```bash
# Tools anzeigen
./query-tools.sh

# Detaillierte Info
node list-tools.js

# Als JSON exportieren
node export-tools.js

# Mit jq filtern
node export-tools.js | jq '.tools[] | .name'

# Nur Tool-Namen
node export-tools.js | jq '.tools[].name'

# Mit Description
node export-tools.js | jq '.tools[] | {name, description}'
```

---

## ✅ Checkliste

- [ ] Perplexity MCP Server läuft (`npm run start`)
- [ ] Node.js ist installiert (`node --version`)
- [ ] Scripts sind ausführbar
- [ ] Backend läuft (für TaskCreationService Tests)
- [ ] Perplexity API Key ist gesetzt (`.env`)
- [ ] Tools können abgerufen werden (`./query-tools.sh`)
- [ ] JSON Export funktioniert (`node export-tools.js`)

---

## 🚀 Nächste Schritte

1. **Tools abrufen**: `./query-tools.sh`
2. **Details ansehen**: `node list-tools.js`
3. **Als JSON speichern**: `node export-tools.js > tools.json`
4. **Mit Backend integrieren**: TaskCreationService nutzt automatisch die Tools
5. **Tasks erstellen**: REST API aufrufen mit Event-Anfrage

