# Git Tools Provider

## 📋 Quick Summary
Der Git Tools Provider erweitert den MCP-Server um 6 Git- und Dateisystem-Tools für sichere Projekt-Operationen. Diese Tools ermöglichen LLM-Anwendungen den Zugriff auf Git-Repository-Informationen, Dateiinhalte und Verzeichnisstrukturen mit umfassenden Sicherheitsmechanismen.

## 🎯 Use Cases
- **Use Case 1**: LLM-gestützte Code-Reviews durch automatisches Lesen von Projektdateien
- **Use Case 2**: Kontextbewusste Entwickler-Assistenten mit Zugriff auf Git-Status
- **Use Case 3**: Automatisierte Dokumentationsgenerierung basierend auf Commit-Historie
- **Use Case 4**: Intelligente Projektanalyse durch strukturiertes Durchsuchen von Verzeichnissen

## 🏗️ Architecture Overview

### High-Level Diagram (ASCII)
```
LLM Application (OpenRouter)
         │
         ↓
  MCP Server (port 8081)
         │
    ┌────┴────┐
    │         │
[GitToolBase] [File System]
    │         │
    ↓         ↓
┌─────────────────────────────┐
│ Git Operations:              │
│ - get_current_branch         │
│ - get_git_status             │
│ - get_git_log                │
│ - compare_branches           │
├─────────────────────────────┤
│ File Operations:             │
│ - read_project_file          │
│ - list_project_files         │
└─────────────────────────────┘
         │
         ↓
   [JGit Library]
   [Java NIO]
```

### Key Components

1. **GitToolBase** (`backend/mcp-server/src/main/java/de/jivz/mcp/tools/git/GitToolBase.java`)
   - Purpose: Base class providing common Git operations and security
   - Methods: `openRepository()`, `validatePath()`, `isPathAllowed()`
   - Used by: All Git tool implementations

2. **GetCurrentBranchTool** (`backend/mcp-server/src/main/java/de/jivz/mcp/tools/git/GetCurrentBranchTool.java`)
   - Purpose: Returns current Git branch name
   - Dependencies: GitToolBase, JGit
   - Used by: LLM applications for context awareness

3. **ReadProjectFileTool** (`backend/mcp-server/src/main/java/de/jivz/mcp/tools/git/ReadProjectFileTool.java`)
   - Purpose: Safely reads project files with security validation
   - Dependencies: GitToolBase, Java NIO
   - Security: Path traversal protection, size limits, file type whitelist

4. **ListProjectFilesTool** (`backend/mcp-server/src/main/java/de/jivz/mcp/tools/git/ListProjectFilesTool.java`)
   - Purpose: Lists files in directories with filtering
   - Dependencies: GitToolBase, Java NIO
   - Features: Recursive search, extension filtering, sensitive dir exclusion

## 📊 Implemented Tools

### Tool 1: get_current_branch
Returns the name of the current Git branch.

**Parameters:** None

**Response:**
```json
{
  "branch": "main"
}
```

### Tool 2: get_git_status
Shows repository status with modified, added, untracked, and deleted files.

**Parameters:** None

**Response:**
```json
{
  "modified": ["src/main/java/Example.java"],
  "added": ["new-file.txt"],
  "untracked": ["temp.log"],
  "deleted": []
}
```

### Tool 3: read_project_file
Reads file contents from the project with comprehensive security checks.

**Parameters:**
- `filePath` (required): Relative path from project root

**Response:**
```json
{
  "content": "file contents here...",
  "path": "README.md",
  "size": 1234
}
```

**Security Features:**
- Path traversal protection (no "..")
- Maximum file size: 1 MB
- File type whitelist
- Audit logging

### Tool 4: list_project_files
Lists files in a project directory with optional filtering.

**Parameters:**
- `directory` (optional): Directory path (default: project root)
- `recursive` (optional): Recursive search (default: false)
- `extensions` (optional): Filter by file extensions (e.g., ["java", "ts"])

**Response:**
```json
[
  "src/main/java/de/jivz/mcp/McpServiceApplication.java",
  "src/main/java/de/jivz/mcp/controller/McpController.java"
]
```

### Tool 5: get_git_log
Shows recent Git commit history.

**Parameters:**
- `limit` (optional): Number of commits (default: 10, max: 50)

**Response:**
```json
{
  "commits": [
    {
      "hash": "abc123def456",
      "author": "Developer Name",
      "date": "2026-01-12T10:30:00Z",
      "message": "Add Git Tools Provider"
    }
  ]
}
```

### Tool 6: compare_branches
Compares two Git branches and shows the differences in commits.

**Parameters:**
- `base` (required): Base branch for comparison (e.g., 'main' or 'develop')
- `compare` (required): Branch to compare with base (e.g., 'feature/new-feature')

**Response:**
```json
{
  "base": "main",
  "compare": "feature/new-feature",
  "ahead": 3,
  "behind": 1,
  "aheadCommits": [
    {
      "hash": "abc123def456",
      "shortHash": "abc123d",
      "author": "Developer Name",
      "date": "2026-01-12T10:30:00Z",
      "message": "Add new feature"
    }
  ],
  "behindCommits": [
    {
      "hash": "def456abc123",
      "shortHash": "def456a",
      "author": "Another Developer",
      "date": "2026-01-11T15:20:00Z",
      "message": "Fix bug in main"
    }
  ]
}
```

**Use Cases:**
- Check if feature branch is up-to-date with main
- Review commits before merging
- Identify conflicts before pull request

## 💻 Complete Code Examples

### Example 1: GitToolBase - Template Method Pattern

```java
// File: backend/mcp-server/src/main/java/de/jivz/mcp/tools/git/GitToolBase.java
@Slf4j
public abstract class GitToolBase implements Tool {
    
    @Value("${git.project.root:${user.dir}}")
    protected String projectRoot;
    
    /**
     * Opens Git repository from configured project root
     */
    protected Repository openRepository() throws IOException {
        Path gitDir = Paths.get(projectRoot, ".git");
        
        if (!Files.exists(gitDir)) {
            throw new ToolExecutionException(
                "Git-репозиторий не найден в директории: " + projectRoot
            );
        }
        
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        return builder.setGitDir(gitDir.toFile())
                     .readEnvironment()
                     .findGitDir()
                     .build();
    }
    
    /**
     * Validates path for security (prevents path traversal)
     */
    protected Path validatePath(String relativePath) {
        if (relativePath.contains("..")) {
            throw new ToolExecutionException(
                "Путь содержит запрещенные символы (..)"
            );
        }
        
        Path projectPath = Paths.get(projectRoot);
        Path fullPath = projectPath.resolve(relativePath).normalize();
        
        if (!fullPath.startsWith(projectPath)) {
            throw new ToolExecutionException(
                "Файл должен находиться внутри проекта"
            );
        }
        
        return fullPath;
    }
    
    /**
     * Checks if path is in allowed directory (not sensitive)
     */
    protected boolean isPathAllowed(Path path) {
        String pathStr = path.toString();
        String[] excludedDirs = {".git/", "node_modules/", "target/", 
                                 "build/", "dist/", ".idea/", ".vscode/"};
        
        for (String excluded : excludedDirs) {
            if (pathStr.contains(excluded)) {
                return false;
            }
        }
        return true;
    }
}
```

**Explanation:**
- Template Method Pattern: Provides reusable methods for all Git tools
- Security: Multiple validation layers (path traversal, directory exclusion)
- Configuration: Injection via `@Value` with default fallback
- Error Handling: Custom `ToolExecutionException` for consistent errors

### Example 2: ReadProjectFileTool - Security Implementation

```java
// File: backend/mcp-server/src/main/java/de/jivz/mcp/tools/git/ReadProjectFileTool.java
@Component
@Slf4j
public class ReadProjectFileTool extends GitToolBase {
    
    private static final long MAX_FILE_SIZE = 1_048_576; // 1 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        ".java", ".kt", ".ts", ".vue", ".js", ".jsx", ".tsx",
        ".md", ".txt", ".json", ".yml", ".yaml",
        ".properties", ".xml", ".sql", ".sh", ".gradle"
    );
    
    @Override
    public String getName() {
        return "read_project_file";
    }
    
    @Override
    public McpTool getDefinition() {
        return McpTool.builder()
            .name("read_project_file")
            .description("Читает содержимое файла из проекта с проверками безопасности")
            .inputSchema(InputSchema.builder()
                .type("object")
                .properties(Map.of(
                    "filePath", Property.builder()
                        .type("string")
                        .description("Относительный путь к файлу от корня проекта")
                        .build()
                ))
                .required(List.of("filePath"))
                .build())
            .build();
    }
    
    @Override
    public Object execute(Map<String, Object> arguments) {
        ToolArguments args = ToolArguments.of(arguments);
        String relativePath = args.getString("filePath");
        
        log.debug("Reading file: {}", relativePath);
        
        // 1. Validate path (path traversal protection)
        Path validatedPath = validatePath(relativePath);
        
        // 2. Check file exists
        if (!Files.exists(validatedPath)) {
            throw new ToolExecutionException(
                "Файл не найден: " + validatedPath
            );
        }
        
        // 3. Check file size
        try {
            long fileSize = Files.size(validatedPath);
            if (fileSize > MAX_FILE_SIZE) {
                throw new ToolExecutionException(
                    String.format("Файл слишком большой: %d байт (максимум: %d байт)", 
                                  fileSize, MAX_FILE_SIZE)
                );
            }
        } catch (IOException e) {
            throw new ToolExecutionException("Ошибка при чтении размера файла", e);
        }
        
        // 4. Check file extension
        String fileName = validatedPath.getFileName().toString();
        boolean isAllowed = ALLOWED_EXTENSIONS.stream()
            .anyMatch(fileName::endsWith);
        
        if (!isAllowed) {
            throw new ToolExecutionException(
                "Тип файла не поддерживается. Разрешенные расширения: " + 
                ALLOWED_EXTENSIONS
            );
        }
        
        // 5. Read file content
        try {
            String content = Files.readString(validatedPath, StandardCharsets.UTF_8);
            
            // Audit log
            log.debug("Аудит: Чтение файла {} пользователем system", validatedPath);
            
            return Map.of(
                "content", content,
                "path", relativePath,
                "size", Files.size(validatedPath)
            );
        } catch (IOException e) {
            log.error("Ошибка при чтении файла {}: {}", relativePath, e.getMessage());
            throw new ToolExecutionException("Не удалось прочитать файл", e);
        }
    }
}
```

**Explanation:**
- **Security Layers:**
  1. Path traversal protection via `validatePath()`
  2. File existence check
  3. File size limit (1 MB)
  4. File extension whitelist
- **Audit Logging**: All file reads are logged
- **Error Handling**: Specific error messages for each validation failure
- **UTF-8 Encoding**: Consistent character encoding

### Example 3: REST API Usage

```bash
# 1. Execute get_current_branch
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "get_current_branch",
    "arguments": {}
  }'

# Response:
# {
#   "success": true,
#   "result": {
#     "branch": "feature/git-tools"
#   }
# }

# 2. Execute read_project_file
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "read_project_file",
    "arguments": {
      "filePath": "README.md"
    }
  }'

# Response:
# {
#   "success": true,
#   "result": {
#     "content": "# Project Title\n\nDescription...",
#     "path": "README.md",
#     "size": 1234
#   }
# }

# 3. Execute list_project_files with filtering
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "list_project_files",
    "arguments": {
      "directory": "src/main/java",
      "recursive": true,
      "extensions": ["java"]
    }
  }'

# Response:
# {
#   "success": true,
#   "result": [
#     "src/main/java/de/jivz/mcp/McpServiceApplication.java",
#     "src/main/java/de/jivz/mcp/controller/McpController.java"
#   ]
# }
```

## 📂 File Structure

```
backend/mcp-server/src/main/java/de/jivz/mcp/tools/git/
├── GitToolBase.java                    # Abstract base with common Git operations
├── GetCurrentBranchTool.java           # Returns current Git branch
├── GetGitStatusTool.java               # Returns repository status
├── ReadProjectFileTool.java            # Reads file with security checks
├── ListProjectFilesTool.java           # Lists files with filtering
└── GetGitLogTool.java                  # Returns commit history

backend/mcp-server/pom.xml               # Maven dependencies (JGit)
backend/mcp-server/src/main/resources/
└── application.properties               # Configuration (git.project.root)
```

```xml
<dependency>
    <groupId>org.eclipse.jgit</groupId>
    <artifactId>org.eclipse.jgit</artifactId>
    <version>6.8.0.202311291450-r</version>
</dependency>
```

### Application Properties

```properties
# Git Configuration
# Project root directory (defaults to user.dir if not specified)
git.project.root=/path/to/project
```

Standard: `System.getProperty("user.dir")`

## API-Beispiele

### get_current_branch

**Request:**
```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{"name": "get_current_branch", "arguments": {}}'
```

**Response:**
```json
{
  "success": true,
  "result": {
    "branch": "main"
  }
}
```

### get_git_status

**Request:**
```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{"name": "get_git_status", "arguments": {}}'
```

**Response:**
```json
{
  "success": true,
  "result": {
    "modified": ["src/main/java/Example.java"],
    "added": ["new-file.txt"],
    "untracked": ["temp.log"],
    "deleted": []
  }
}
```

### read_project_file

**Request:**
```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "read_project_file",
    "arguments": {
      "filePath": "README.md"
    }
  }'
```

**Response:**
```json
{
  "success": true,
  "result": {
    "content": "# Project Title\n\n...",
    "path": "README.md",
    "size": 1234
  }
}
```

### list_project_files

**Request:**
```bash
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

**Response:**
```json
{
  "success": true,
  "result": [
    "src/main/java/de/jivz/mcp/McpServiceApplication.java",
    "src/main/java/de/jivz/mcp/controller/McpToolsController.java",
    "..."
  ]
}
```

### get_git_log

**Request:**
```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "name": "get_git_log",
    "arguments": {
      "limit": 5
    }
  }'
```

**Response:**
```json
{
  "success": true,
  "result": {
    "commits": [
      {
        "hash": "abc123def456789",
        "author": "Developer Name",
        "date": "2026-01-12T10:30:00Z",
        "message": "Add Git Tools Provider"
      }
    ]
  }
}
```

## Fehlerbehandlung

### Typische Fehlerszenarien

1. **Git-Repository nicht gefunden**
```json
{
  "success": false,
  "error": "Git-репозиторий не найден в директории: /path/to/project"
}
```

2. **Datei nicht gefunden**
```json
{
  "success": false,
  "error": "Файл не найден: /path/to/project/missing-file.txt"
}
```

3. **Path Traversal Versuch**
```json
{
  "success": false,
  "error": "Путь содержит запрещенные символы (..)"
}
```

4. **Datei zu groß**
```json
{
  "success": false,
  "error": "Файл слишком большой: 2097152 байт (максимум: 1048576 байт)"
}
```

5. **Ungültiger Dateityp**
```json
{
  "success": false,
  "error": "Тип файла не поддерживается. Разрешенные расширения: [.java, .kt, ...]"
}
```

## Testing

### Manuelle Tests

1. **Server starten:**
```bash
cd backend/mcp-server
mvn spring-boot:run
```

2. **Tools auflisten:**
```bash
curl http://localhost:8081/api/tools
```

3. **Tool ausführen:**
```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{"name": "get_current_branch", "arguments": {}}'
```

### Unit-Tests (Beispiel)

```java
@SpringBootTest
class GitToolsIntegrationTest {
    
    @Autowired
    private List<Tool> tools;
    
    @Test
    void testAllGitToolsRegistered() {
        List<String> gitToolNames = tools.stream()
            .filter(t -> t.getName().startsWith("get_") || 
                         t.getName().contains("project"))
            .map(Tool::getName)
            .toList();
            
        assertThat(gitToolNames).contains(
            "get_current_branch",
            "get_git_status",
            "read_project_file",
            "list_project_files",
            "get_git_log"
        );
    }
}
```

## Performance-Überlegungen

1. **Git-Repository wird für jeden Aufruf neu geöffnet** - Keine persistente Verbindung
2. **Dateigröße ist auf 1MB begrenzt** - Verhindert Out-of-Memory-Fehler
3. **Rekursive Dateisuche kann langsam sein** - Bei großen Projekten `recursive=false` verwenden
4. **Git-Log ist auf 50 Commits begrenzt** - Verhindert übermäßigen Speicherverbrauch

## Bekannte Einschränkungen

1. **Nur lokale Git-Repositories** - Remote-Operationen werden nicht unterstützt
2. **Read-Only** - Keine Schreiboperationen (keine Commits, keine Branch-Wechsel)
3. **Keine Git-Authentifizierung** - Funktioniert nur mit bereits geklonten Repositories
4. **Maximale Dateigröße: 1MB** - Größere Dateien können nicht gelesen werden
5. **Nur Textdateien** - Binärdateien werden nicht unterstützt

## Zukünftige Erweiterungen

### Geplante Features

1. **git_diff** - Unterschiede zwischen Commits anzeigen
2. **git_blame** - Autoren für jede Zeile anzeigen
3. **search_in_files** - Volltextsuche im Projekt
4. **get_file_history** - Historie einer einzelnen Datei
5. **compare_branches** - Vergleich zwischen zwei Branches

### Mögliche Verbesserungen

1. **Caching** - Repository-Instanzen cachen
2. **Streaming** - Große Dateien streamen statt komplett laden
3. **Asynchrone Operationen** - Lange Operationen im Hintergrund ausführen
4. **Berechtigungssystem** - Feinkörnige Zugriffskontrolle

## Integration mit anderen Tools

Die Git-Tools können mit anderen MCP-Tools kombiniert werden:

```bash
# 1. Liste Java-Dateien auf
curl -X POST http://localhost:8081/api/tools/execute \
  -d '{"name": "list_project_files", "arguments": {"extensions": ["java"]}}'

# 2. Lese eine spezifische Datei
curl -X POST http://localhost:8081/api/tools/execute \
  -d '{"name": "read_project_file", "arguments": {"filePath": "src/.../Example.java"}}'

# 3. Prüfe Git-Status
curl -X POST http://localhost:8081/api/tools/execute \
  -d '{"name": "get_git_status", "arguments": {}}'
```

## Troubleshooting

### Problem: "Git-Repository nicht gefunden"

**Lösung:**
1. Prüfen, dass `.git`-Verzeichnis existiert
2. `git.project.root` in `application.properties` konfigurieren
3. Server neu starten

### Problem: "Keine Leseberechtigung"

**Lösung:**
1. Dateirechte prüfen: `ls -la <file>`
2. Benutzerrechte des Server-Prozesses überprüfen

### Problem: Tools erscheinen nicht in der Liste

**Lösung:**
1. Prüfen, dass alle Tools mit `@Component` annotiert sind
2. Prüfen, dass Package-Scan konfiguriert ist
3. Log-Level auf DEBUG setzen: `logging.level.de.jivz.mcp.tools.git=DEBUG`

## Zusammenfassung

Der Git Tools Provider ist ein vollständiges, produktionsreifes Feature mit:

✅ 5 funktionsfähige Git-Tools  
✅ Umfassende Sicherheitsmaßnahmen  
✅ Audit-Logging  
✅ Fehlerbehandlung  
✅ Konfigurierbarkeit  
✅ Spring Boot Integration  
✅ RESTful API  

Das Feature ist bereit für den Produktionseinsatz und kann sofort verwendet werden.

