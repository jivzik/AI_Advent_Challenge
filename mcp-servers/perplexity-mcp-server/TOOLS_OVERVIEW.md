# Perplexity MCP Server - Tasks/Tools Übersicht

## 🎯 Kurzantwort

Der Perplexity MCP Server hat **2 verfügbare Tools/Tasks**:

### 1️⃣ **perplexity_ask**
- Stelle Fragen an Perplexity Sonar AI
- Parameter: `prompt`, `model`, `temperature`, `max_tokens`
- Use Case: Allgemeine Q&A

### 2️⃣ **perplexity_search**
- Suche mit Internet-Zugang
- Parameter: `query`
- Use Case: Event-Recherche, aktuelle Informationen

---

## 🚀 Schnelle Commands

```bash
# 1. Einfache Liste (Bash)
./query-tools.sh

# 2. Detaillierte Info (Node.js)
node list-tools.js

# 3. Als JSON exportieren
node export-tools.js > tools.json

# 4. JSON anzeigen
cat tools.json | jq .
```

---

## 📋 Tools Details

### perplexity_ask
```
Ask a question to Perplexity Sonar AI model. This tool uses Perplexity API 
to get answers with real-time internet search capabilities.

Input Schema:
  prompt (string, required): The question or prompt to send
  model (string, optional): Perplexity model to use (default: sonar)
  temperature (number, optional): 0.0-1.0 (default: 0.7)
  max_tokens (number, optional): Max response tokens (default: 1000)
```

### perplexity_search
```
Search for information using Perplexity with internet access. 
Returns detailed answers with citations.

Input Schema:
  query (string, required): Search query
```

---

## 📊 Integration im System

```
TaskCreationService
  ↓
  └─ mcpToolClient.executeTool("perplexity_search", ...)
       ↓
       └─ Perplexity MCP Server (index.js)
            ├─ perplexity_ask
            └─ perplexity_search
                 ↓
                 └─ Perplexity API
                      ↓
                      └─ Sonar Model + Web Search
```

---

## ✨ Use Cases

### Event-Recherche
```json
{
  "name": "perplexity_search",
  "arguments": {
    "query": "AWS Summit 2025 dates location registration"
  }
}
```

### Allgemeine Frage
```json
{
  "name": "perplexity_ask",
  "arguments": {
    "prompt": "Wann ist die beste Zeit um Aktien zu kaufen?"
  }
}
```

### Mit Temperatuerkontrolle
```json
{
  "name": "perplexity_ask",
  "arguments": {
    "prompt": "Erkläre Machine Learning",
    "temperature": 0.2,
    "max_tokens": 500
  }
}
```

---

## 🎓 Beispiel: Task-Erstellung Workflow

```
1. Benutzer: "Erstelle einen Task für Spring Conference 2025"
   ↓
2. System: Baut Prompt mit verfügbaren Tools
   ↓
3. Sonar LLM: Entscheidet, perplexity_search zu nutzen
   ↓
4. Tool-Call: {
     "name": "perplexity_search",
     "arguments": {"query": "Spring Conference 2025 dates location"}
   }
   ↓
5. Result: "Spring Conference 2025 findet 22.-24. Mai in San Francisco statt"
   ↓
6. Sonar LLM: Erstellt Task mit reminder_create_task
   ↓
7. Output: Task gespeichert ✅
```

---

## 🛠️ Ordnerstruktur

```
perplexity-mcp-server/
├── index.js                  # Main Server
├── list-tools.js            # Zeigt detaillierte Tool-Info
├── export-tools.js          # Exportiert als JSON
├── query-tools.sh           # Bash-Script für einfache Queries
├── QUERY_TOOLS_GUIDE.md     # Ausführliche Dokumentation
└── TOOLS_OVERVIEW.md        # Diese Datei
```

---

## 📌 Remember

- **perplexity_ask**: Für allgemeine Fragen
- **perplexity_search**: Für aktuelle Infos + Internet
- **Temperature**: 0 = präzise, 1 = kreativ
- **max_tokens**: 1000-2000 für Detailfragen
- **query**: Sehr spezifisch für bessere Results

---

## ✅ Fertig!

Du kannst jetzt:
1. ✅ Alle Tasks/Tools abrufen
2. ✅ Detaillierte Informationen ansehen
3. ✅ Als JSON exportieren
4. ✅ In Backend-Services nutzen

Viel Erfolg! 🚀

