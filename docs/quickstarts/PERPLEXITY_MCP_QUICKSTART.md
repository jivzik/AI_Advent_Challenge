# 🚀 Perplexity MCP - Schnellstart

## In 5 Schritten zur fertigen Integration

### Schritt 1: Node.js prüfen/installieren

```bash
# Prüfen ob Node.js installiert ist
node --version

# Falls nicht installiert:
# Ubuntu/Debian:
sudo apt install nodejs npm

# Fedora:
sudo dnf install nodejs npm

# macOS:
brew install node
```

### Schritt 2: MCP Server Setup

```bash
cd backend/perplexity-service
./setup-perplexity-mcp.sh
```

### Schritt 3: API Key konfigurieren

Hole deinen Perplexity API Key von: https://www.perplexity.ai/settings/api

```bash
# Bearbeite .env Datei
nano perplexity-mcp-server/.env

# Oder direkt erstellen:
echo "PERPLEXITY_API_KEY=pplx-DEIN-KEY-HIER" > perplexity-mcp-server/.env
```

### Schritt 4: Application starten

```bash
./start-with-mcp.sh
```

**Oder manuell:**
```bash
./mvnw spring-boot:run
```

### Schritt 5: Testen

```bash
# In einem neuen Terminal
./test-perplexity-mcp.sh
```

**Oder einzeln:**
```bash
# Status prüfen
curl http://localhost:8080/perplexity/status

# Tools auflisten
curl http://localhost:8080/perplexity/tools

# Frage stellen
curl -X POST http://localhost:8080/perplexity/ask \
  -H "Content-Type: application/json" \
  -d '{"prompt": "What is the Model Context Protocol?"}'
```

## 🎯 Verfügbare Endpoints

| Endpoint | Methode | Beschreibung |
|----------|---------|--------------|
| `/perplexity/tools` | GET | Liste alle MCP Tools |
| `/perplexity/ask` | POST | Frage an Perplexity |
| `/perplexity/search` | POST | Suche mit Perplexity |
| `/perplexity/status` | GET | Server Status |

## 📖 Weitere Dokumentation

- **Komplette Anleitung:** `PERPLEXITY_MCP_INTEGRATION_GUIDE.md`
- **Implementierung:** `PERPLEXITY_MCP_IMPLEMENTATION_SUMMARY.md`
- **MCP Server:** `backend/perplexity-service/PERPLEXITY_MCP_README.md`

## ❓ Probleme?

### "node: command not found"
→ Node.js ist nicht installiert. Siehe Schritt 1.

### "PERPLEXITY_API_KEY not set"
→ .env Datei fehlt oder ist leer. Siehe Schritt 3.

### IDE zeigt Fehler bei McpTool
→ Das ist ein Caching-Problem. Der Code kompiliert korrekt:
```bash
./mvnw clean compile -DskipTests
```

In IntelliJ: **File → Invalidate Caches / Restart**

---

**Das war's! Viel Erfolg! 🎉**

