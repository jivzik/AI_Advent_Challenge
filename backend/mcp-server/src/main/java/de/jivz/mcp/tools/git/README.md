# Git Tools Provider für MCP Server

## Übersicht

Der GitToolProvider stellt 5 Werkzeuge für die Arbeit mit Git-Repositories und Projektdateien bereit:

## Verfügbare Tools

### 1. `get_current_branch`
**Beschreibung:** Gibt den Namen des aktuellen Git-Branches zurück

**Parameter:** Keine

**Rückgabe:**
```json
{
  "branch": "main"
}
```

**Beispiel:**
```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{"name": "get_current_branch", "arguments": {}}'
```

---

### 2. `get_git_status`
**Beschreibung:** Gibt den Status des Repositories zurück (geänderte, hinzugefügte, nicht verfolgte Dateien)

**Parameter:** Keine

**Rückgabe:**
```json
{
  "modified": ["src/main/java/Example.java"],
  "added": ["new-file.txt"],
  "untracked": ["temp.log"],
  "deleted": ["old-file.java"]
}
```

**Beispiel:**
```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{"name": "get_git_status", "arguments": {}}'
```

---

### 3. `read_project_file`
**Beschreibung:** Liest den Inhalt einer Datei aus dem Projekt

**Parameter:**
- `filePath` (string, required): Relativer Pfad zur Datei vom Projektstamm

**Rückgabe:**
```json
{
  "content": "...",
  "path": "src/main/java/Example.java",
  "size": 1234
}
```

**Sicherheit:**
- Path Traversal (..) ist verboten
- Absolute Pfade sind verboten
- Maximale Dateigröße: 1MB
- Nur Textdateien: .java, .kt, .ts, .vue, .js, .md, .txt, .json, .yml, .properties, .xml

**Beispiel:**
```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "read_project_file",
    "arguments": {
      "filePath": "src/main/java/de/jivz/mcp/controller/McpToolsController.java"
    }
  }'
```

---

### 4. `list_project_files`
**Beschreibung:** Gibt eine Liste von Dateien in einem Projektverzeichnis zurück

**Parameter:**
- `directory` (string, optional): Pfad zum Verzeichnis (Standard: ".")
- `recursive` (boolean, optional): Rekursiv durch Unterverzeichnisse (Standard: false)
- `extensions` (array, optional): Filter nach Dateiendungen, z.B. ["java", "md"]

**Rückgabe:**
```json
[
  "src/main/java/Example.java",
  "src/main/resources/application.properties",
  "README.md"
]
```

**Ausschlüsse:** .git/, node_modules/, target/, dist/, .idea/, .vscode/

**Beispiel:**
```bash
# Alle Java-Dateien rekursiv auflisten
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "list_project_files",
    "arguments": {
      "directory": "src/main/java",
      "recursive": true,
      "extensions": ["java"]
    }
  }'
```

---

### 5. `get_git_log`
**Beschreibung:** Gibt die letzten Commits aus der Git-Historie zurück

**Parameter:**
- `limit` (integer, optional): Anzahl der Commits (Standard: 10, Maximum: 50)

**Rückgabe:**
```json
{
  "commits": [
    {
      "hash": "abc123def456",
      "author": "Developer Name",
      "date": "2026-01-12T10:30:00Z",
      "message": "Add feature"
    }
  ]
}
```

**Beispiel:**
```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "get_git_log",
    "arguments": {
      "limit": 20
    }
  }'
```

---

## Installation

### 1. Abhängigkeit hinzufügen (bereits erledigt)

Die JGit-Abhängigkeit wurde bereits zur `pom.xml` hinzugefügt:

```xml
<dependency>
    <groupId>org.eclipse.jgit</groupId>
    <artifactId>org.eclipse.jgit</artifactId>
    <version>6.8.0.202311291450-r</version>
</dependency>
```

### 2. Konfiguration

In `application.properties` (optional):

```properties
# Git Configuration
# Project root directory (defaults to user.dir if not specified)
git.project.root=/pfad/zum/projekt
```

Wenn nicht konfiguriert, wird automatisch `System.getProperty("user.dir")` verwendet.

### 3. Server starten

```bash
cd backend/mcp-server
mvn spring-boot:run
```

## Architektur

### Klassenstruktur

```
tools/git/
├── GitToolBase.java              # Basisklasse mit gemeinsamer Logik
├── GetCurrentBranchTool.java     # Tool für aktuellen Branch
├── GetGitStatusTool.java         # Tool für Git-Status
├── ReadProjectFileTool.java      # Tool zum Dateilesen
├── ListProjectFilesTool.java     # Tool zum Dateiauflisten
└── GetGitLogTool.java            # Tool für Git-Log
```

### Design Pattern

- **Strategy Pattern**: Jedes Tool implementiert das `Tool`-Interface
- **Template Method**: `GitToolBase` enthält gemeinsame Logik
- **Dependency Injection**: Alle Tools sind Spring `@Component`s

### Sicherheitsmaßnahmen

1. **Path Traversal Schutz:**
   - Prüfung auf `..` in Pfaden
   - Normalisierung mit `Path.normalize()`
   - Validierung, dass Dateien innerhalb des Projekts liegen

2. **Dateigröße-Beschränkung:**
   - Maximum: 1MB (1_048_576 Bytes)

3. **Dateitype-Whitelist:**
   - Nur erlaubte Erweiterungen: .java, .kt, .ts, .vue, .js, .md, .txt, .json, .yml, .properties, .xml

4. **Zugriffskontrolle:**
   - Prüfung auf Leseberechtigung
   - Ausschluss sensibler Verzeichnisse (.git, node_modules, etc.)

5. **Audit-Logging:**
   - Alle Dateioperationen werden protokolliert
   - Sicherheitswarnungen bei verdächtigen Zugriffen

## Fehlerbehandlung

Alle Tools werfen `ToolExecutionException` mit aussagekräftigen Fehlermeldungen:

- Git-Repository nicht gefunden
- Datei nicht gefunden
- Keine Leseberechtigung
- Path Traversal Versuch
- Datei zu groß
- Ungültiger Dateityp

## Logging

Alle Git-Tools verwenden strukturiertes Logging:

```
🔧 Ausführung: Tool-Name mit Parametern
✅ Erfolg: Ergebnis
❌ Fehler: Fehlermeldung
```

Logging-Level in `application.properties` konfigurieren:
```properties
logging.level.de.jivz.mcp.tools.git=DEBUG
```

## Tests

Beispiel-Testfälle für die Tools:

```java
@SpringBootTest
class GitToolsTest {
    
    @Autowired
    private GetCurrentBranchTool branchTool;
    
    @Test
    void testGetCurrentBranch() {
        Map<String, Object> result = branchTool.execute(Map.of());
        assertNotNull(result.get("branch"));
    }
}
```

## Bekannte Einschränkungen

1. Nur lokale Git-Repositories werden unterstützt
2. Keine Schreiboperationen (nur Lesen)
3. Maximale Dateigröße: 1MB
4. Nur Textdateien werden unterstützt

## Zukünftige Erweiterungen

- Git Diff-Tool
- Git Blame-Tool
- Commit-Erstellung
- Branch-Wechsel
- Remote-Repository-Operationen

