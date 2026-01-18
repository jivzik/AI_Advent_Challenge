# Perplexity MCP Integration - Komplette Anleitung

## 📖 Überblick

Diese Integration ermöglicht es Ihrem Spring Boot Projekt, mit Perplexity AI über das Model Context Protocol (MCP) zu kommunizieren. Der MCP-Server läuft in Node.js und kommuniziert über STDIO mit dem Java-Client.

## 🎯 Features

✅ **GET /perplexity/tools** - Liste alle verfügbaren MCP Tools  
✅ **POST /perplexity/ask** - Frage an Perplexity Sonar stellen  
✅ **POST /perplexity/search** - Internet-Suche mit Perplexity  
✅ **GET /perplexity/status** - MCP Server Status prüfen  
✅ **Automatischer Start** - MCP Server startet mit Spring Boot  
✅ **STDIO Kommunikation** - Zuverlässige Process-basierte Integration  

---

## 🚀 Schnellstart

### 1. Voraussetzungen installieren

**Node.js & npm:**
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install nodejs npm

# Fedora
sudo dnf install nodejs npm

# macOS
brew install node

# Überprüfen
node --version  # sollte v18+ sein
npm --version
```

**Perplexity API Key:**
1. Gehe zu https://www.perplexity.ai/settings/api
2. Erstelle einen API Key
3. Exportiere als Umgebungsvariable:
   ```bash
   export PERPLEXITY_API_KEY=pplx-xxxxxxxxxxxxxxxx
   ```

### 2. Installation

```bash
cd backend/perplexity-service

# MCP Server Setup
./setup-perplexity-mcp.sh

# .env Datei konfigurieren
nano perplexity-mcp-server/.env
# Füge hinzu: PERPLEXITY_API_KEY=pplx-xxxxxxxxxxxxxxxx
```

### 3. Starten

```bash
# Kompletter Start (empfohlen)
./start-with-mcp.sh

# Oder manuell
./mvnw spring-boot:run
```

### 4. Testen

```bash
# Alle Tests ausführen
./test-perplexity-mcp.sh

# Oder einzeln:
curl http://localhost:8080/perplexity/status
```

---

## 📋 API Dokumentation

### 1. GET /perplexity/tools

**Beschreibung:** Liste alle verfügbaren MCP Tools vom Perplexity MCP Server.

**Request:**
```bash
curl http://localhost:8080/perplexity/tools
```

**Response:**
```json
[
  {
    "name": "perplexity_ask",
    "description": "Ask a question to Perplexity Sonar AI model. This tool uses Perplexity API to get answers with real-time internet search capabilities.",
    "inputSchema": {
      "type": "object",
      "properties": {
        "prompt": {
          "type": "string",
          "description": "The question or prompt to send to Perplexity Sonar"
        },
        "model": {
          "type": "string",
          "description": "Perplexity model to use (default: sonar)",
          "default": "sonar"
        },
        "temperature": {
          "type": "number",
          "description": "Temperature for response generation (0.0-1.0, default: 0.7)",
          "default": 0.7
        },
        "max_tokens": {
          "type": "number",
          "description": "Maximum tokens in response (default: 1000)",
          "default": 1000
        }
      },
      "required": ["prompt"]
    }
  },
  {
    "name": "perplexity_search",
    "description": "Search for information using Perplexity with internet access. Returns detailed answers with citations.",
    "inputSchema": {
      "type": "object",
      "properties": {
        "query": {
          "type": "string",
          "description": "Search query"
        }
      },
      "required": ["query"]
    }
  }
]
```

---

### 2. POST /perplexity/ask

**Beschreibung:** Stelle eine Frage an Perplexity Sonar AI. Das MCP Tool `perplexity_ask` wird aufgerufen, welches intern die Perplexity API `/chat/completions` verwendet.

**Request:**
```bash
curl -X POST http://localhost:8080/perplexity/ask \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "What is the Model Context Protocol?"
  }'
```

**Request mit Parametern:**
```bash
curl -X POST http://localhost:8080/perplexity/ask \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Explain quantum entanglement",
    "model": "sonar",
    "temperature": 0.3,
    "max_tokens": 1500
  }'
```

**Response:**
```json
{
  "success": true,
  "answer": "The Model Context Protocol (MCP) is an open protocol that standardizes how applications provide context to Large Language Models (LLMs). It was introduced by Anthropic in November 2024...",
  "model": "sonar",
  "usage": {
    "prompt_tokens": 12,
    "completion_tokens": 156,
    "total_tokens": 168
  },
  "citations": []
}
```

**Parameter:**
- `prompt` (string, **required**) - Die Frage oder der Prompt
- `model` (string, optional) - Perplexity Modell (default: "sonar")
- `temperature` (number, optional) - 0.0-1.0 (default: 0.7)
- `max_tokens` (number, optional) - Max Tokens (default: 1000)

---

### 3. POST /perplexity/search

**Beschreibung:** Suche nach aktuellen Informationen mit Perplexity's Internet-Zugang. Verwendet das MCP Tool `perplexity_search`.

**Request:**
```bash
curl -X POST http://localhost:8080/perplexity/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "latest developments in AI 2024"
  }'
```

**Response:**
```json
{
  "success": true,
  "answer": "In 2024, significant AI developments include OpenAI's GPT-4 Turbo release, Google's Gemini model family, Anthropic's Claude 3 series, and major advancements in open-source models like Llama 3...",
  "model": "sonar",
  "usage": {
    "prompt_tokens": 8,
    "completion_tokens": 245,
    "total_tokens": 253
  },
  "citations": [
    "https://openai.com/blog/gpt-4-turbo",
    "https://blog.google/technology/ai/google-gemini-ai/"
  ]
}
```

**Parameter:**
- `query` (string, **required**) - Suchanfrage

---

### 4. GET /perplexity/status

**Beschreibung:** Überprüfe den Status des MCP Servers.

**Request:**
```bash
curl http://localhost:8080/perplexity/status
```

**Response (initialized):**
```json
{
  "initialized": true,
  "type": "Perplexity MCP Client (Node.js)",
  "version": "1.0.0",
  "toolCount": 2,
  "status": "running"
}
```

**Response (not initialized):**
```json
{
  "initialized": false,
  "type": "Perplexity MCP Client (Node.js)",
  "version": "1.0.0",
  "status": "not_initialized"
}
```

---

## 🔧 Konfiguration

### application.properties

```properties
# MCP Perplexity Configuration
mcp.perplexity.node.executable=node
mcp.perplexity.server.path=${user.dir}/perplexity-mcp-server/index.js
```

### .env Datei (perplexity-mcp-server/.env)

```env
PERPLEXITY_API_KEY=pplx-xxxxxxxxxxxxxxxx
```

---

## 🏗️ Architektur

```
┌─────────────────────────────────────────────────────────┐
│            Spring Boot Application (Java)                │
│                                                           │
│  ┌─────────────────────────────────────────────────┐   │
│  │     PerplexityMcpController                      │   │
│  │     - GET  /perplexity/tools                     │   │
│  │     - POST /perplexity/ask                       │   │
│  │     - POST /perplexity/search                    │   │
│  │     - GET  /perplexity/status                    │   │
│  └─────────────────────────────────────────────────┘   │
│                         │                                │
│                         ↓                                │
│  ┌─────────────────────────────────────────────────┐   │
│  │     PerplexityMcpClientService                   │   │
│  │     - MCP JSON-RPC über STDIO                    │   │
│  │     - Process Management                         │   │
│  │     - initialize(), listTools(), executeTool()   │   │
│  └─────────────────────────────────────────────────┘   │
│                         │                                │
└─────────────────────────┼────────────────────────────────┘
                          │ STDIO (stdin/stdout)
                          ↓
              ┌───────────────────────┐
              │   Node.js Process     │
              │   (MCP Server)        │
              │   - index.js          │
              │   - @modelcontext     │
              │     protocol/sdk      │
              └───────────────────────┘
                          │
                          ↓ HTTPS
              ┌───────────────────────┐
              │   Perplexity API      │
              │   /chat/completions   │
              └───────────────────────┘
```

### Komponenten

1. **PerplexityMcpController** - REST API Endpoints
2. **PerplexityMcpClientService** - MCP Client Logik
3. **Node.js MCP Server** - Implementiert MCP Protocol
4. **Perplexity API** - Externe AI Service

### Kommunikation

- **Spring Boot ↔ Node.js**: STDIO (JSON-RPC 2.0)
- **Node.js ↔ Perplexity**: HTTPS REST API
- **Protokoll**: Model Context Protocol (MCP)

---

## 🧪 Beispiele

### Beispiel 1: Einfache Frage

```bash
curl -X POST http://localhost:8080/perplexity/ask \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "What is Spring Boot?"
  }'
```

### Beispiel 2: Technische Erklärung

```bash
curl -X POST http://localhost:8080/perplexity/ask \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Explain how the Model Context Protocol works",
    "temperature": 0.2,
    "max_tokens": 2000
  }'
```

### Beispiel 3: Aktuelle Nachrichten

```bash
curl -X POST http://localhost:8080/perplexity/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "latest news about quantum computing breakthroughs"
  }'
```

### Beispiel 4: Java Integration

```java
@RestController
public class MyController {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @GetMapping("/ask-perplexity")
    public Map<String, Object> askPerplexity(@RequestParam String question) {
        String url = "http://localhost:8080/perplexity/ask";
        
        Map<String, Object> request = Map.of("prompt", question);
        
        return restTemplate.postForObject(url, request, Map.class);
    }
}
```

---

## 🐛 Troubleshooting

### Problem: "MCP client not initialized"

**Lösung:**
```bash
# Logs prüfen
tail -f logs/spring.log

# MCP Server manuell testen
cd perplexity-mcp-server
node index.js

# Node.js Version prüfen (mind. v18)
node --version
```

### Problem: "PERPLEXITY_API_KEY not set"

**Lösung:**
```bash
# .env Datei prüfen
cat perplexity-mcp-server/.env

# Sollte enthalten:
# PERPLEXITY_API_KEY=pplx-xxxxxxxxxxxxxxxx

# Falls nicht, erstellen:
echo "PERPLEXITY_API_KEY=pplx-your-key-here" > perplexity-mcp-server/.env
```

### Problem: Node.js nicht gefunden

**Lösung:**
```bash
# Node.js installieren
sudo apt install nodejs npm  # Ubuntu/Debian
sudo dnf install nodejs npm  # Fedora

# Oder Node.js Version Manager (nvm)
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
nvm install --lts
```

### Problem: MCP Server antwortet nicht

**Lösung:**
```bash
# Dependencies neu installieren
cd perplexity-mcp-server
rm -rf node_modules package-lock.json
npm install

# Server standalone testen
./test-mcp-server.sh
```

---

## 📊 Logging

### MCP Communication Logs aktivieren

In `application.properties`:
```properties
logging.level.de.jivz.ai_challenge.mcp=DEBUG
```

### Log-Ausgabe Beispiel

```
2024-12-15 10:30:15.123  INFO --- Initializing Perplexity MCP Client
2024-12-15 10:30:15.456  INFO --- Node executable: node
2024-12-15 10:30:15.789  INFO --- MCP Server script: /path/to/index.js
2024-12-15 10:30:16.012  INFO --- MCP connection initialized successfully
2024-12-15 10:30:16.234 DEBUG --- Sending request: {"jsonrpc":"2.0","id":1,"method":"tools/list"}
2024-12-15 10:30:16.456 DEBUG --- Received response: {"jsonrpc":"2.0","id":1,"result":{"tools":[...]}}
2024-12-15 10:30:16.678  INFO --- Received 2 tools from Perplexity MCP server
```

---

## 🔒 Sicherheit

### API Key Schutz

- ❌ **Niemals** den API Key in Git committen
- ✅ Verwende `.env` Dateien (sind in `.gitignore`)
- ✅ Verwende Umgebungsvariablen in Production
- ✅ Rotiere API Keys regelmäßig

### Production Setup

```bash
# Umgebungsvariable setzen
export PERPLEXITY_API_KEY=pplx-production-key

# Application starten
java -jar app.jar
```

---

## 📚 Weiterführende Links

- [Model Context Protocol Specification](https://modelcontextprotocol.io/)
- [Perplexity API Documentation](https://docs.perplexity.ai/)
- [MCP TypeScript SDK](https://github.com/modelcontextprotocol/typescript-sdk)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)

---

## ✅ Zusammenfassung

Sie haben jetzt eine vollständige Perplexity MCP Integration mit:

- ✅ Node.js MCP Server (`perplexity_ask`, `perplexity_search`)
- ✅ Java Spring Boot Client (STDIO Communication)
- ✅ REST API Endpoints (`/perplexity/*`)
- ✅ Automatischer Prozess-Management
- ✅ Comprehensive Error Handling
- ✅ Logging & Debugging Support

**Nächste Schritte:**
1. Installiere Node.js falls noch nicht vorhanden
2. Führe `./setup-perplexity-mcp.sh` aus
3. Konfiguriere `.env` mit deinem API Key
4. Starte mit `./start-with-mcp.sh`
5. Teste mit `./test-perplexity-mcp.sh`

Viel Erfolg! 🚀

