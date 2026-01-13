# MCP Server - Tool-basierte Architektur

Ein Spring Boot Service, der Model Context Protocol (MCP) mit Strategy Pattern und SOLID Principles implementiert.

## Übersicht

Der MCP Server folgt einer **Tool-basierten Architektur**, bei der jedes Tool eine eigenständige `@Component`-Klasse ist, die das `Tool`-Interface implementiert. Alle Tools werden automatisch registriert und sind über eine einheitliche API verfügbar.

## Architektur

### Core Interface

```java
public interface Tool {
    String getName();
    McpTool getDefinition();
    Object execute(Map<String, Object> arguments);
}
```

### Verfügbare Tools

#### Native Tools (`native_tools/`)
- **add_numbers** - Addiert zwei Zahlen
- **get_current_weather** - Mock-Wetterdaten
- **calculate_fibonacci** - Fibonacci-Berechnung
- **reverse_string** - String umkehren
- **count_words** - Wortstatistiken

#### Google Tools (`google/`)
- **google_tasks_list** - Alle Google Tasks Listen abrufen
- **google_tasks_get** - Alle Aufgaben aus einer Liste abrufen
- **google_tasks_create** - Neue Aufgabe erstellen
- **google_tasks_update** - Existierende Aufgabe aktualisieren
- **google_tasks_complete** - Aufgabe als erledigt markieren
- **google_tasks_delete** - Aufgabe löschen

## Design Patterns

- **Strategy Pattern**: Jedes Tool ist eine austauschbare Strategie
- **Template Method**: `AbstractGoogleTool` für gemeinsame Google API Logik
- **Facade Pattern**: `McpServerService` als vereinfachte Schnittstelle
- **Registry Pattern**: `ToolRegistry` für automatische Tool-Verwaltung

## API Endpoints

```bash
# Alle Tools auflisten
GET /api/tools

# Tool ausführen
POST /api/tools/execute
{
  "toolName": "add_numbers",
  "arguments": {"a": 5, "b": 3}
}

# Server Status
GET /api/status
```

## Neues Tool hinzufügen

1. Erstelle eine neue Klasse in `tools/`:
```java
@Component
public class MyNewTool implements Tool {
    @Override
    public String getName() {
        return "my_new_tool";
    }
    
    @Override
    public McpTool getDefinition() {
        // Tool-Definition
    }
    
    @Override
    public Object execute(Map<String, Object> arguments) {
        // Tool-Implementierung
    }
}
```

2. Fertig! Spring registriert es automatisch.

## Weitere Dokumentation

- **Refactoring Guide**: `REFACTORING_GUIDE.md`
- **Refactoring Summary**: `REFACTORING_SUMMARY.md`
- **Architektur**: `docs/architecture/MCP_MULTI_PROVIDER_ARCHITECTURE.md`
