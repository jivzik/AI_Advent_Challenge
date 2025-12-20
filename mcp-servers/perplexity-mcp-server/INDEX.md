# 📚 Perplexity MCP Server - Dokumentations-Index

## 🎯 Schnelleinstieg

### Du fragst dich: "Wie bekomme ich alle Tasks/Tools?"

**Antwort:** 3 Wege:

#### 1. Einfachste Variante (Bash)
```bash
./query-tools.sh
```
→ Zeigt einfache Liste mit Beschreibungen

#### 2. Detaillierte Info (Node.js)
```bash
node list-tools.js
```
→ Zeigt vollständige Schema mit allen Parametern

#### 3. JSON Export
```bash
node export-tools.js > tools.json
cat tools.json | jq .
```
→ Exportiert als strukturiertes JSON

---

## 📖 Dokumentation

| Datei | Beschreibung | Für wen? |
|-------|-------------|---------|
| **TOOLS_OVERVIEW.md** | Schnelle Übersicht der 2 Tools | Alle |
| **QUERY_TOOLS_GUIDE.md** | Ausführliche Query-Anleitung | Entwickler |
| **index.js** | Der MCP Server selbst | Backend-Dev |

---

## 🔧 Verfügbare Tools

```
┌────────────────────────────────────┐
│ PERPLEXITY MCP SERVER TOOLS        │
├────────────────────────────────────┤
│                                    │
│  1. perplexity_ask                │
│     └─ Fragen beantworten         │
│                                    │
│  2. perplexity_search             │
│     └─ Mit Internet recherchieren  │
│                                    │
└────────────────────────────────────┘
```

---

## 📋 Tool-Details

### perplexity_ask
```
✅ Allgemeine Fragen beantworten
✅ Mit Kreativitäts-Kontrolle
✅ Begrenzbare Antwort-Länge

Parameter:
  • prompt (required): Die Frage
  • model: Modell auswählen (default: sonar)
  • temperature: Kreativität 0-1 (default: 0.7)
  • max_tokens: Max Länge (default: 1000)
```

### perplexity_search
```
✅ Mit Internet-Zugang recherchieren
✅ Aktuelle Informationen
✅ Mit Citations

Parameter:
  • query (required): Was suchen?
```

---

## 🚀 Workflow: Task-Erstellung

```
Benutzer-Input
    ↓
"Erstelle Task für Spring Conference 2025"
    ↓
System ruft perplexity_search auf mit:
  query = "Spring Conference 2025 dates location"
    ↓
Ergebnis: Event-Details
    ↓
Task wird mit allen Infos erstellt
```

---

## 💾 Dateien in diesem Verzeichnis

```
perplexity-mcp-server/
│
├── 📄 index.js
│   └─ Haupt-Server (aktuell laufend)
│
├── 🔧 Neue Query-Tools:
│   ├── list-tools.js          ← Detaillierte Liste
│   ├── export-tools.js        ← JSON Export
│   └── query-tools.sh         ← Bash-Wrapper
│
├── 📚 Dokumentation:
│   ├── TOOLS_OVERVIEW.md      ← Diese Überblick-Datei
│   ├── QUERY_TOOLS_GUIDE.md   ← Ausführlicher Guide
│   ├── INDEX.md              ← Dieses Dokument
│   └── README.md             ← Original README
│
└── ⚙️ Config:
    ├── .env                   ← API Keys
    ├── package.json
    └── package-lock.json
```

---

## 🎓 Cheat Sheet

### Commands

```bash
# Alles anzeigen
./query-tools.sh

# Nur Tool-Namen
node export-tools.js | jq '.tools[].name'

# Mit Beschreibungen
node export-tools.js | jq '.tools[] | {name, description}'

# Pretty-Print
node list-tools.js

# Als Variable speichern
TOOLS_JSON=$(node export-tools.js)
echo $TOOLS_JSON | jq '.tools | length'
```

### In Anwendungen nutzen

```javascript
// JavaScript/Node.js
const tools = await client.listTools();
// Returns: {tools: [{name, description, inputSchema}, ...]}

// curl/HTTP (wenn HTTP-Unterstützung)
curl http://localhost:3000/tools
```

```java
// Java/Spring
List<McpTool> tools = mcpToolClient.getAllTools();
// tools.get(0).getName()  → "perplexity_ask"
// tools.get(1).getName()  → "perplexity_search"
```

---

## 🔍 Debugging

### Problem: Tools werden nicht angezeigt
```bash
# 1. Server läuft?
ps aux | grep "node index.js"

# 2. Port korrekt?
netstat -tuln | grep 3000

# 3. Script läuft?
node list-tools.js 2>&1
```

### Problem: JSON Export fehlerhaft
```bash
# JSON validieren
node export-tools.js | jq empty && echo "Valid"

# Mit Error-Output
node export-tools.js 2>&1
```

---

## 📊 Integration

### Mit Backend

```
┌─────────────────────────────┐
│  TaskCreationService        │
│  (Spring Boot Backend)      │
└──────────┬──────────────────┘
           │
           ↓ mcpToolClient
┌─────────────────────────────┐
│  Perplexity MCP Server      │
│  (Node.js index.js)         │
│  - perplexity_ask           │
│  - perplexity_search        │
└──────────┬──────────────────┘
           │
           ↓
┌─────────────────────────────┐
│  Perplexity API             │
│  (Cloud)                    │
└─────────────────────────────┘
```

---

## ✨ Use Cases

### 1. Event-Recherche
```bash
# Automatisch vom LLM aufgerufen
query: "AWS Summit 2025 dates location registration link"
Result: Detaillierte Event-Infos → Task erstellt
```

### 2. News/Updates
```bash
query: "Latest news about AI in December 2025"
Result: Aktuelle Informationen → In Task eingebunden
```

### 3. Allgemeine Fragen
```bash
prompt: "Wie funktioniert Kubernetes?"
Result: Detaillierte Erklärung
```

---

## 🎯 Nächste Schritte

### Für Entwickler
1. [ ] `./query-tools.sh` ausführen
2. [ ] `node list-tools.js` testen
3. [ ] `node export-tools.js > tools.json` speichern
4. [ ] JSON-Struktur verstehen
5. [ ] Im Backend integrieren

### Für Production
1. [ ] Server stabil laufen lassen
2. [ ] Logs monitoren
3. [ ] API Key sicher speichern
4. [ ] Rate Limits beachten
5. [ ] Error Handling implementieren

---

## 📞 Support

### Häufige Fragen

**Q: Wo sehe ich die Tools?**
A: `./query-tools.sh` oder `node list-tools.js`

**Q: Wie nutze ich die Tools?**
A: Via `mcpToolClient.executeTool(name, args)` im Backend

**Q: Kann ich die Tools exportieren?**
A: Ja, `node export-tools.js > tools.json`

**Q: Sind nur diese 2 Tools verfügbar?**
A: Ja, derzeit perplexity_ask und perplexity_search

**Q: Kann ich weitere Tools hinzufügen?**
A: Ja, in index.js im ListToolsRequestSchema array

---

## 🔄 Version History

| Version | Datum | Änderung |
|---------|-------|---------|
| 1.0.0 | 2025-12-18 | Initial Release |
| - | - | list-tools.js hinzugefügt |
| - | - | export-tools.js hinzugefügt |
| - | - | query-tools.sh hinzugefügt |
| - | - | Dokumentation erweitert |

---

## 📝 Lizenzen

- Perplexity MCP Server: [Original License]
- Query Tools: MIT
- Dokumentation: CC-BY-4.0

---

**Last Updated:** 2025-12-18  
**Maintained by:** Development Team  
**Status:** ✅ Active

