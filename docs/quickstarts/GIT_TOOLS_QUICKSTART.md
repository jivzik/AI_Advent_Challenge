# Git Tools Provider - Quick Start Guide

## Schnellstart

Dieses Quick Start Guide hilft Ihnen, die neuen Git-Tools im MCP-Server in wenigen Minuten zu nutzen.

## Voraussetzungen

- ✅ Java 21
- ✅ Maven
- ✅ Git-Repository (das Projekt selbst)
- ✅ Port 8081 verfügbar

## Schritt 1: Server starten

```bash
# Im Projektverzeichnis
cd /home/jivz/IdeaProjects/AI_Advent_Challenge/backend/mcp-server

# Server starten
mvn spring-boot:run
```

Der Server startet auf `http://localhost:8081`

## Schritt 2: Tools überprüfen

Öffnen Sie einen neuen Terminal und listen Sie alle verfügbaren Tools auf:

```bash
curl http://localhost:8081/api/tools | jq '.'
```

Sie sollten die 5 neuen Git-Tools sehen:
- ✅ `get_current_branch`
- ✅ `get_git_status`
- ✅ `read_project_file`
- ✅ `list_project_files`
- ✅ `get_git_log`

## Schritt 3: Tools testen

### Test 1: Aktuellen Branch abrufen

```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{"name": "get_current_branch", "arguments": {}}' | jq '.'
```

**Erwartete Ausgabe:**
```json
{
  "success": true,
  "result": {
    "branch": "main"
  }
}
```

### Test 2: Git-Status abrufen

```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{"name": "get_git_status", "arguments": {}}' | jq '.'
```

**Erwartete Ausgabe:**
```json
{
  "success": true,
  "result": {
    "modified": ["src/main/java/Example.java"],
    "added": [],
    "untracked": [],
    "deleted": []
  }
}
```

### Test 3: Datei lesen

```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "read_project_file",
    "arguments": {
      "filePath": "README.md"
    }
  }' | jq '.'
```

**Erwartete Ausgabe:**
```json
{
  "success": true,
  "result": {
    "content": "# AI Advent Challenge\n\n...",
    "path": "README.md",
    "size": 1234
  }
}
```

### Test 4: Dateien auflisten

```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "list_project_files",
    "arguments": {
      "directory": "backend/mcp-server/src/main/java",
      "recursive": true,
      "extensions": ["java"]
    }
  }' | jq '.'
```

**Erwartete Ausgabe:**
```json
{
  "success": true,
  "result": [
    "backend/mcp-server/src/main/java/de/jivz/mcp/McpServiceApplication.java",
    "backend/mcp-server/src/main/java/de/jivz/mcp/controller/McpToolsController.java",
    "..."
  ]
}
```

### Test 5: Git-Log abrufen

```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "get_git_log",
    "arguments": {
      "limit": 5
    }
  }' | jq '.'
```

**Erwartete Ausgabe:**
```json
{
  "success": true,
  "result": {
    "commits": [
      {
        "hash": "abc123...",
        "author": "Developer",
        "date": "2026-01-12T10:30:00Z",
        "message": "Add Git Tools"
      }
    ]
  }
}
```

## Schritt 4: Automatisiertes Testen

Nutzen Sie das bereitgestellte Test-Script:

```bash
# Script ausführbar machen (falls noch nicht geschehen)
chmod +x test-git-tools.sh

# Tests ausführen
./test-git-tools.sh
```

Das Script testet alle 5 Tools und führt auch Sicherheitstests durch.

## Schritt 5: Sicherheitstests

### Test: Path Traversal (sollte fehlschlagen)

```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "read_project_file",
    "arguments": {
      "filePath": "../../../etc/passwd"
    }
  }' | jq '.'
```

**Erwartete Ausgabe:**
```json
{
  "success": false,
  "error": "Путь содержит запрещенные символы (..)"
}
```

### Test: Absolute Pfade (sollte fehlschlagen)

```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "read_project_file",
    "arguments": {
      "filePath": "/etc/passwd"
    }
  }' | jq '.'
```

**Erwartete Ausgabe:**
```json
{
  "success": false,
  "error": "Абсолютные пути запрещены"
}
```

## Häufige Probleme & Lösungen

### Problem: Server startet nicht

**Lösung:**
```bash
# Prüfen, ob Port 8081 belegt ist
lsof -i :8081

# Falls ja, anderen Port verwenden
# In application.properties: server.port=8082
```

### Problem: Git-Repository nicht gefunden

**Lösung:**
```bash
# Prüfen, ob .git existiert
ls -la .git

# Falls nicht im richtigen Verzeichnis:
# In application.properties:
# git.project.root=/pfad/zum/projekt
```

### Problem: Tools erscheinen nicht in der Liste

**Lösung:**
```bash
# Log-Level erhöhen
# In application.properties:
# logging.level.de.jivz.mcp.tools.git=DEBUG

# Server neu starten
mvn spring-boot:run
```

### Problem: "Datei nicht gefunden"

**Lösung:**
- Pfad ist relativ zum Projektroot
- Prüfen Sie `git.project.root` in application.properties
- Beispiel: `"filePath": "backend/mcp-server/README.md"`

## Nützliche Befehle

### Alle Tools auflisten
```bash
curl http://localhost:8081/api/tools | jq -r '.[] | .name'
```

### Nur Git-Tools anzeigen
```bash
curl http://localhost:8081/api/tools | jq -r '.[] | select(.name | contains("git") or contains("project")) | .name'
```

### Tool-Definition anzeigen
```bash
curl http://localhost:8081/api/tools | jq '.[] | select(.name == "read_project_file")'
```

### Server-Logs verfolgen
```bash
# In einem separaten Terminal
tail -f backend/mcp-server/target/logs/spring.log
```

## Integration mit LLM (z.B. Claude, GPT)

Die Git-Tools können direkt in LLM-Workflows integriert werden:

```bash
# Beispiel: Projekt-Übersicht erstellen
curl -X POST http://localhost:8081/api/tools/execute \
  -d '{"name": "list_project_files", "arguments": {"recursive": true, "extensions": ["java", "md"]}}' \
  | jq -r '.result[]' \
  | head -20
```

## Nächste Schritte

1. ✅ **Dokumentation lesen**: `docs/features/GIT_TOOLS_PROVIDER_FEATURE.md`
2. ✅ **API-Beispiele**: `backend/mcp-server/src/main/java/de/jivz/mcp/tools/git/README.md`
3. ✅ **Tests schreiben**: Eigene Integration-Tests erstellen
4. ✅ **Frontend-Integration**: Git-Tools im Frontend nutzen

## Support

Bei Fragen oder Problemen:
1. Prüfen Sie die Logs: `logging.level.de.jivz.mcp.tools.git=DEBUG`
2. Lesen Sie die Feature-Dokumentation
3. Führen Sie das Test-Script aus: `./test-git-tools.sh`

## Zusammenfassung

✅ 5 funktionsfähige Git-Tools  
✅ Sichere Dateioperationen  
✅ Einfache REST-API  
✅ Umfassende Dokumentation  
✅ Test-Script bereitgestellt  

**Sie sind bereit, die Git-Tools zu nutzen! 🚀**

