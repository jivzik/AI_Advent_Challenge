# MCP Server - Quick Start Guide

## 🚀 Schnellstart

### Service starten

```bash
cd backend/mcp-server
./mvnw spring-boot:run
```

Der Service läuft auf Port **8081**.

## ✅ Service testen

### 1. Status prüfen

```bash
curl http://localhost:8081/api/status
```

**Erwartete Antwort:**
```json
{
  "status": "running",
  "type": "MCP Tool Server",
  "version": "3.0.0",
  "total_tools": 11,
  "tools": {
    "native": 5,
    "google": 6
  }
}
```

### 2. Alle Tools anzeigen

```bash
curl http://localhost:8081/api/tools
```

**Erwartete Antwort:**
```json
{
  "tools": [
    {
      "name": "add_numbers",
      "description": "Addiert zwei Zahlen",
      "inputSchema": { "..." }
    },
    {
      "name": "google_tasks_list",
      "description": "Ruft alle Google Tasks Listen ab",
      "inputSchema": { ... }
    },
    ...
  ]
}
```

## 🧪 Tools testen

### Native Tools

#### Add Numbers
```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "add_numbers",
    "arguments": {"a": 42, "b": 8}
  }'
```

**Erwartete Antwort:**
```json
{
  "success": true,
  "result": 50,
  "toolName": "add_numbers"
}
```

#### Get Weather
```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "get_current_weather",
    "arguments": {
      "location": "Berlin",
      "unit": "celsius"
    }
  }'
```

#### Calculate Fibonacci
```bash
curl -X POST http://localhost:8081/mcp/execute \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "calculate_fibonacci",
    "arguments": {"n": 10}
  }'
```

#### Reverse String
```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "reverse_string",
    "arguments": {"text": "Hello MCP!"}
  }'
```

#### Count Words
```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "count_words",
    "arguments": {"text": "The quick brown fox jumps over the lazy dog"}
  }'
```

### Google Tasks Tools

#### Liste alle Task-Listen
```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "google_tasks_list",
    "arguments": {}
  }'
```

#### Erstelle neue Task
```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "google_tasks_create",
    "arguments": {
      "title": "Meeting vorbereiten",
      "notes": "Agenda erstellen und Präsentation vorbereiten",
      "due": "2026-01-15T10:00:00Z"
    }
  }'
```

#### Markiere Task als erledigt
```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "google_tasks_complete",
    "arguments": {
      "taskId": "your-task-id"
    }
  }'
```

## 📊 Tool-Kategorien

### Native Tools (5)
- **add_numbers** - Mathematische Operation
- **calculate_fibonacci** - Fibonacci-Berechnung
- **reverse_string** - String-Manipulation
- **count_words** - Text-Analyse
- **Typ:** Java-basiert, keine externen Abhängigkeiten
- **Status:** ✅ Voll funktionsfähig

### Google Tools (6)
- **google_tasks_list** - Alle Task-Listen abrufen
- **google_tasks_get** - Tasks einer Liste abrufen
- **google_tasks_create** - Neue Task erstellen
- **google_tasks_update** - Task aktualisieren
- **google_tasks_complete** - Task als erledigt markieren
- **google_tasks_delete** - Task löschen
- **Typ:** Google Tasks API Integration
- **Status:** ✅ Voll funktionsfähig

## 🏗️ Architektur

### Strategy Pattern
Jedes Tool ist eine eigenständige `@Component`-Klasse:
```java
@Component
public class AddNumbersTool implements Tool {
    // Implementierung
}
```

### Automatische Registrierung
- Spring findet alle `@Component`-Klassen mit `Tool`-Interface
- `ToolRegistry` registriert sie automatisch
- Keine manuelle Konfiguration nötig

### SOLID Principles
- **Single Responsibility**: Jedes Tool hat eine klare Aufgabe
- **Open/Closed**: Neue Tools ohne Änderungen hinzufügen
- **Dependency Inversion**: Abhängigkeiten über Interfaces

## 📝 API Endpoints Übersicht

| Methode | Endpoint | Beschreibung |
|---------|----------|--------------|
| GET | `/api/status` | Server-Status und Statistiken |
| GET | `/api/tools` | Alle verfügbaren Tools |
| POST | `/api/tools/execute` | Tool ausführen |

## 🎯 Nächste Schritte

1. ✅ Service starten und testen
2. ✅ Native Tools ausprobieren
3. ✅ Google Tools nutzen
4. ⏳ Eigene Tools hinzufügen (siehe REFACTORING_GUIDE.md)

## 🐛 Troubleshooting

### Service startet nicht
```bash
# Port-Konflikt prüfen
netstat -an | grep 8081

# Anderen Port verwenden
SERVER_PORT=8082 ./mvnw spring-boot:run
```

### Tool nicht gefunden
```bash
# Logs prüfen
tail -f logs/mcp-server.log

# Alle Tools überprüfen
curl http://localhost:8081/api/tools
```

### Tool-Ausführung schlägt fehl
```bash
# Tool-Definition überprüfen
curl http://localhost:8081/api/tools

# Log-Ausgabe beachten für Details
tail -f logs/spring.log
```

## 📚 Weitere Dokumentation

- **Architektur**: `docs/architecture/MCP_MULTI_PROVIDER_ARCHITECTURE.md`
- **Refactoring Guide**: `backend/mcp-server/REFACTORING_GUIDE.md`
- **Refactoring Summary**: `backend/mcp-server/REFACTORING_SUMMARY.md`

