# Git Tools Provider für MCP Server

## Übersicht

Der GitToolProvider stellt 10 Werkzeuge für die Arbeit mit Git-Repositories, Projektdateien und GitHub Issues bereit:
- 6 Tools für lokale Git-Operationen und Dateiverwaltung
- 4 Tools für GitHub Issue-Management (Erstellen, Auflisten, Bearbeiten, Löschen)

## Verfügbare Tools

### 1. `list_open_prs`
**Beschreibung:** Ruft die Liste offener Pull Requests aus einem GitHub-Repository ab

**Parameter:**
- `repository` (string, optional): GitHub Repository im Format 'owner/repo' (z.B. 'octocat/Hello-World')
  - Optional wenn `github.repository` in der Konfiguration gesetzt ist
- `state` (string, optional): Status der PRs: 'open', 'closed' oder 'all' (Standard: 'open')
- `limit` (integer, optional): Maximale Anzahl der zurückzugebenden PRs (Standard: 30, Maximum: 100)

**Rückgabe:**
```json
[
  {
    "number": 123,
    "title": "Add new feature",
    "description": "This PR adds...",
    "author": "username",
    "baseBranch": "main",
    "headBranch": "feature/new-feature",
    "baseSha": "abc123...",
    "headSha": "def456...",
    "repository": "owner/repo",
    "state": "open",
    "createdAt": "2026-01-10T10:00:00Z",
    "updatedAt": "2026-01-13T15:30:00Z",
    "url": "https://github.com/owner/repo/pull/123",
    "draft": false,
    "merged": false
  }
]
```

**Konfiguration:**

In `application.properties`:
```properties
# GitHub Personal Access Token (empfohlen für höhere Rate Limits)
# Token erstellen unter: https://github.com/settings/tokens
# Benötigte Scopes: repo (für private Repos) oder public_repo (nur öffentliche Repos)
github.token=${GITHUB_TOKEN:}

# Standard GitHub Repository im Format 'owner/repo' (optional)
github.repository=${GITHUB_REPOSITORY:}
```

Oder als Umgebungsvariablen:
```bash
export GITHUB_TOKEN=ghp_your_token_here
export GITHUB_REPOSITORY=owner/repo
```

**Beispiele:**

```bash
# PRs aus Standard-Repository abrufen (wenn konfiguriert)
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{"name": "list_open_prs", "arguments": {}}'

# PRs aus spezifischem Repository abrufen
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "list_open_prs",
    "arguments": {
      "repository": "octocat/Hello-World",
      "state": "open",
      "limit": 10
    }
  }'

# Alle PRs (offen und geschlossen) abrufen
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "list_open_prs",
    "arguments": {
      "repository": "owner/repo",
      "state": "all",
      "limit": 50
    }
  }'
```

**Hinweise:**
- Ohne Token: GitHub API Rate Limit beträgt 60 Anfragen/Stunde
- Mit Token: GitHub API Rate Limit beträgt 5000 Anfragen/Stunde
- Das Tool verwendet die GitHub REST API v3

---

### 2. `get_current_branch`
**Beschreibung:** Gibt den Namen des aktuellen Git-Branches zurück

**Parameter:** Keine

**Rückgabe:**
```json
{
  "branch": "main"
}
```

---

### 2. `get_current_branch`
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

### 3. `get_git_status`
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

### 3. `get_git_status`
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

### 4. `read_project_file`
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

### 4. `read_project_file`
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

### 5. `list_project_files`
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

### 5. `list_project_files`
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

### 6. `get_git_log`
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

## GitHub Issue Management Tools

### 7. `list_github_issues`
**Beschreibung:** Ruft die Liste von Issues aus einem GitHub-Repository ab

**Parameter:**
- `repository` (string, optional): GitHub Repository im Format 'owner/repo'
- `state` (string, optional): Status der Issues: 'open', 'closed' oder 'all' (Standard: 'open')
- `labels` (array, optional): Filter nach Label-Namen
- `assignee` (string, optional): Filter nach Assignee-Username
- `creator` (string, optional): Filter nach Creator-Username
- `limit` (integer, optional): Maximale Anzahl (Standard: 30, Maximum: 100)

**Rückgabe:**
```json
[
  {
    "number": 42,
    "title": "Bug in feature X",
    "body": "Description...",
    "state": "open",
    "author": "username",
    "url": "https://github.com/owner/repo/issues/42",
    "createdAt": "2026-01-10T10:00:00Z",
    "updatedAt": "2026-01-13T15:30:00Z",
    "commentsCount": 5,
    "labels": ["bug", "priority-high"],
    "assignees": ["developer1"],
    "milestone": "v1.0"
  }
]
```

**Beispiel:**
```bash
# Alle offenen Issues mit Label "bug" abrufen
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "list_github_issues",
    "arguments": {
      "repository": "owner/repo",
      "state": "open",
      "labels": ["bug"],
      "limit": 20
    }
  }'
```

---

### 8. `create_github_issue`
**Beschreibung:** Erstellt ein neues Issue in einem GitHub-Repository

**Parameter:**
- `repository` (string, optional): GitHub Repository im Format 'owner/repo'
- `title` (string, **required**): Titel des Issues
- `body` (string, optional): Beschreibung des Issues
- `labels` (array, optional): Array von Label-Namen
- `assignees` (array, optional): Array von GitHub-Usernames zum Zuweisen
- `milestone` (integer, optional): Milestone-Nummer

**Rückgabe:**
```json
{
  "number": 43,
  "title": "New feature request",
  "body": "We need...",
  "state": "open",
  "author": "current-user",
  "url": "https://github.com/owner/repo/issues/43",
  "createdAt": "2026-01-15T10:00:00Z",
  "updatedAt": "2026-01-15T10:00:00Z",
  "labels": ["enhancement"],
  "assignees": ["developer1", "developer2"],
  "milestone": "v1.1"
}
```

**Beispiel:**
```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "create_github_issue",
    "arguments": {
      "repository": "owner/repo",
      "title": "Add new feature",
      "body": "We need to implement feature X because...",
      "labels": ["enhancement", "priority-high"],
      "assignees": ["developer1"]
    }
  }'
```

---

### 9. `update_github_issue`
**Beschreibung:** Aktualisiert ein existierendes GitHub Issue

**Parameter:**
- `repository` (string, optional): GitHub Repository im Format 'owner/repo'
- `issueNumber` (integer, **required**): Issue-Nummer zum Aktualisieren
- `title` (string, optional): Neuer Titel
- `body` (string, optional): Neue Beschreibung
- `state` (string, optional): Neuer Status: 'open' oder 'closed'
- `labels` (array, optional): Neue Labels (ersetzt existierende)
- `assignees` (array, optional): Neue Assignees (ersetzt existierende)
- `milestone` (integer, optional): Milestone-Nummer (-1 um Milestone zu entfernen)

**Rückgabe:**
```json
{
  "number": 43,
  "title": "Updated title",
  "body": "Updated description...",
  "state": "open",
  "author": "original-author",
  "url": "https://github.com/owner/repo/issues/43",
  "createdAt": "2026-01-15T10:00:00Z",
  "updatedAt": "2026-01-15T11:00:00Z",
  "labels": ["enhancement", "in-progress"],
  "assignees": ["developer2"],
  "milestone": "v1.2"
}
```

**Beispiel:**
```bash
# Issue-Status auf "closed" setzen und Label hinzufügen
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "update_github_issue",
    "arguments": {
      "repository": "owner/repo",
      "issueNumber": 43,
      "state": "closed",
      "labels": ["enhancement", "completed"]
    }
  }'
```

---

### 10. `delete_github_issue`
**Beschreibung:** Schließt ein GitHub Issue (echtes Löschen ist aus Audit-Gründen nicht möglich)

**Parameter:**
- `repository` (string, optional): GitHub Repository im Format 'owner/repo'
- `issueNumber` (integer, **required**): Issue-Nummer zum Schließen
- `reason` (string, optional): Grund für Schließung: 'completed' oder 'not_planned'
- `comment` (string, optional): Kommentar vor dem Schließen hinzufügen

**Rückgabe:**
```json
{
  "success": true,
  "message": "Issue successfully closed",
  "issueNumber": 43,
  "title": "Old issue",
  "state": "closed",
  "url": "https://github.com/owner/repo/issues/43",
  "closedAt": "2026-01-15T12:00:00Z"
}
```

**Beispiel:**
```bash
# Issue schließen mit Kommentar
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "delete_github_issue",
    "arguments": {
      "repository": "owner/repo",
      "issueNumber": 43,
      "reason": "completed",
      "comment": "This has been implemented in PR #50"
    }
  }'
```

---

## Installation

### 1. Abhängigkeiten hinzufügen (bereits erledigt)

Die folgenden Abhängigkeiten wurden bereits zur `pom.xml` hinzugefügt:

```xml
<!-- JGit für Git-Operationen -->
<dependency>
    <groupId>org.eclipse.jgit</groupId>
    <artifactId>org.eclipse.jgit</artifactId>
    <version>7.2.1.202505142326-r</version>
</dependency>

<!-- GitHub API für Pull Request Operationen -->
<dependency>
    <groupId>org.kohsuke</groupId>
    <artifactId>github-api</artifactId>
    <version>1.321</version>
</dependency>
```

### 2. Konfiguration

In `application.properties`:

```properties
# Git Configuration
# Project root directory (defaults to user.dir if not specified)
git.project.root=${user.dir}

# GitHub Configuration (für list_open_prs Tool)
# GitHub Personal Access Token (empfohlen für höhere Rate Limits)
github.token=${GITHUB_TOKEN:}

# Standard GitHub Repository im Format 'owner/repo' (optional)
github.repository=${GITHUB_REPOSITORY:}
```

Wenn nicht konfiguriert, wird für lokale Git-Operationen automatisch `System.getProperty("user.dir")` verwendet.

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
├── GetGitLogTool.java            # Tool für Git-Log
├── ListOpenPRsTool.java          # Tool für GitHub Pull Requests
├── ListGitHubIssuesTool.java    # Tool zum Auflisten von Issues
├── CreateGitHubIssueTool.java   # Tool zum Erstellen von Issues
├── UpdateGitHubIssueTool.java   # Tool zum Bearbeiten von Issues
└── DeleteGitHubIssueTool.java   # Tool zum Schließen von Issues
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

