# MCP Multi-Provider Service

## 📋 Quick Summary
Der MCP (Model Context Protocol) Service ist ein Multi-Provider Tool-Execution-System, das verschiedene Tool-Anbieter (Git, Google, Native Tools) über eine einheitliche REST API integriert. Das System nutzt das Strategy Pattern für modulare Tool-Implementierungen mit automatischer Spring-basierter Registrierung.

## 🎯 Use Cases
- **Use Case 1**: Ausführen von Git-Operationen (Status, Branch, Log) aus LLM-Anwendungen
- **Use Case 2**: Integration mit Google Tasks API für Task-Management
- **Use Case 3**: Bereitstellung einfacher Native Tools (Berechnungen, String-Operationen)
- **Use Case 4**: Erweiterung durch eigene Tool-Implementierungen via `@Component implements Tool`

## 🏗️ Architecture Overview

### High-Level Diagram (ASCII)
```
OpenRouter Service ──HTTP──> MCP Service (port 8081)
                                    │
        ┌───────────────────────────┼──────────────────────┐
        │                           │                      │
   [Native Tools]            [Google Tools]         [Git Tools]
   - add_numbers             - tasks_list           - get_status
   - reverse_string          - tasks_create         - read_file
   - count_words             - tasks_update         - list_files
```

### Key Components

1. **Tool Interface** (`backend/mcp-server/src/main/java/de/jivz/mcp/tools/Tool.java`)
   - Purpose: Defines contract for all tool implementations
   - Methods: `getName()`, `getDefinition()`, `execute(Map<String, Object>)`
   - Used by: ToolRegistry, ToolExecutorService

2. **ToolRegistry** (`backend/mcp-server/src/main/java/de/jivz/mcp/service/ToolRegistry.java`)
   - Purpose: Automatically registers all Spring `@Component` tools
   - Dependencies: Spring ApplicationContext
   - Used by: McpController, ToolExecutorService

3. **ToolExecutorService** (`backend/mcp-server/src/main/java/de/jivz/mcp/service/ToolExecutorService.java`)
   - Purpose: Executes tools with error handling and validation
   - Dependencies: ToolRegistry
   - Used by: McpController

4. **McpController** (`backend/mcp-server/src/main/java/de/jivz/mcp/controller/McpController.java`)
   - Purpose: REST endpoints for tool discovery and execution
   - Endpoints: `/api/tools`, `/api/tools/execute`, `/api/status`
   - Dependencies: ToolRegistry, ToolExecutorService

## 💻 Complete Code Examples

### Example 1: Core Tool Interface

```java
// File: backend/mcp-server/src/main/java/de/jivz/mcp/tools/Tool.java
public interface Tool {
    String getName();
    McpTool getDefinition();
    Object execute(Map<String, Object> arguments);
}
```

**Explanation:**
- `getName()`: Returns unique tool identifier (e.g., "add_numbers")
- `getDefinition()`: Returns tool metadata for MCP protocol
- `execute()`: Performs tool logic with validated arguments

### Example 2: Implementing a Custom Tool

```java
// File: backend/mcp-server/src/main/java/de/jivz/mcp/tools/native_tools/AddNumbersTool.java
@Component
@Slf4j
public class AddNumbersTool implements Tool {
    
    @Override
    public String getName() {
        return "add_numbers";
    }
    
    @Override
    public McpTool getDefinition() {
        return McpTool.builder()
                .name("add_numbers")
                .description("Add two numbers together")
                .inputSchema(InputSchema.builder()
                    .type("object")
                    .properties(Map.of(
                        "a", Property.builder()
                            .type("number")
                            .description("First number")
                            .build(),
                        "b", Property.builder()
                            .type("number")
                            .description("Second number")
                            .build()
                    ))
                    .required(List.of("a", "b"))
                    .build())
                .build();
    }
    
    @Override
    public Object execute(Map<String, Object> arguments) {
        ToolArguments args = ToolArguments.of(arguments);
        double a = args.getDouble("a");
        double b = args.getDouble("b");
        
        log.info("Adding {} + {}", a, b);
        
        return Map.of(
            "result", a + b,
            "operation", "addition"
        );
    }
}
```

**Explanation:**
- `@Component`: Spring auto-detection and registration
- `@Slf4j`: Lombok logging support
- `ToolArguments`: Type-safe argument extraction utility
- Return format: Map with results for JSON serialization

### Example 3: Tool Execution via REST API

```bash
# Execute add_numbers tool
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "add_numbers",
    "arguments": {
      "a": 42,
      "b": 8
    }
  }'
```

**Response:**
```json
{
  "success": true,
  "result": {
    "result": 50.0,
    "operation": "addition"
  }
}
```

## 📂 File Structure

Complete list of all files with descriptions:

```
backend/mcp-server/
├── src/main/java/de/jivz/mcp/
│   ├── McpServiceApplication.java              # Spring Boot main class
│   ├── controller/
│   │   └── McpController.java                  # REST API endpoints
│   ├── service/
│   │   ├── ToolRegistry.java                   # Auto-registration of tools
│   │   ├── ToolExecutorService.java            # Tool execution logic
│   │   └── ToolsDefinitionService.java         # Tool metadata provider
│   ├── tools/
│   │   ├── Tool.java                           # Core tool interface
│   │   ├── ToolArguments.java                  # Type-safe argument parser
│   │   ├── native_tools/
│   │   │   ├── AddNumbersTool.java             # Addition tool
│   │   │   ├── ReverseStringTool.java          # String reversal
│   │   │   ├── CountWordsTool.java             # Word statistics
│   │   │   ├── CalculateFibonacciTool.java     # Fibonacci sequence
│   │   │   └── GetCurrentWeatherTool.java      # Mock weather data
│   │   ├── google/
│   │   │   ├── AbstractGoogleTool.java         # Base class for Google tools
│   │   │   ├── GoogleTasksListTool.java        # List all task lists
│   │   │   ├── GoogleTasksGetTool.java         # Get tasks from list
│   │   │   ├── GoogleTasksCreateTool.java      # Create new task
│   │   │   ├── GoogleTasksUpdateTool.java      # Update existing task
│   │   │   ├── GoogleTasksCompleteTool.java    # Mark task as complete
│   │   │   └── GoogleTasksDeleteTool.java      # Delete task
│   │   └── git/
│   │       ├── GitToolBase.java                # Base class for Git tools
│   │       ├── GetCurrentBranchTool.java       # Get current Git branch
│   │       ├── GetGitStatusTool.java           # Get repository status
│   │       ├── ReadProjectFileTool.java        # Read file contents
│   │       ├── ListProjectFilesTool.java       # List directory files
│   │       └── GetGitLogTool.java              # Get commit history
│   └── model/
│       ├── McpTool.java                        # Tool definition model
│       ├── InputSchema.java                    # JSON Schema for inputs
│       └── Property.java                       # Property definition
└── src/main/resources/
    ├── application.properties                  # Main configuration
    └── application-dev.properties              # Development overrides
```

## 🔌 API Reference

### GET /api/tools
List all available tools with their definitions.

**Request:**
```bash
curl http://localhost:8081/api/tools
```

**Response:**
```json
{
  "tools": [
    {
      "name": "add_numbers",
      "description": "Add two numbers together",
      "inputSchema": {
        "type": "object",
        "properties": {
          "a": {"type": "number", "description": "First number"},
          "b": {"type": "number", "description": "Second number"}
        },
        "required": ["a", "b"]
      }
    }
  ]
}
```

**Status Codes:**
- 200: Success
- 500: Server error

### POST /api/tools/execute
Execute a specific tool with arguments.

**Request:**
```bash
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{
    "toolName": "add_numbers",
    "arguments": {
      "a": 42,
      "b": 8
    }
  }'
```

**Response (Success):**
```json
{
  "success": true,
  "result": {
    "result": 50.0,
    "operation": "addition"
  }
}
```

**Response (Error):**
```json
{
  "success": false,
  "error": "Tool 'invalid_tool' not found"
}
```

**Status Codes:**
- 200: Success (check "success" field for execution result)
- 400: Invalid request format
- 500: Server error

### GET /api/status
Get server status and statistics.

**Request:**
```bash
curl http://localhost:8081/api/status
```

**Response:**
```json
{
  "status": "running",
  "type": "MCP Tool Server",
  "version": "3.0.0",
  "total_tools": 16,
  "tools": {
    "native": 5,
    "google": 6,
    "git": 5
  }
}
```

**Status Codes:**
- 200: Success
- 500: Server error

## ⚙️ Configuration

### Required Properties

File: `backend/mcp-server/src/main/resources/application.properties`

```properties
# Server Configuration
server.port=8081
server.servlet.context-path=/

# Git Tools Configuration
git.project.root=${user.dir}

# Logging
logging.level.de.jivz.mcp=INFO
```

### Optional Properties

```properties
# Google Tools Configuration (if using Google Tasks)
google.credentials.path=backend/google-service/credentials.json

# Debug Logging
logging.level.de.jivz.mcp.tools=DEBUG
logging.level.de.jivz.mcp.service=DEBUG

# Tool Execution Settings
tool.execution.timeout=30000
tool.execution.retry.enabled=false
```

### Environment Variables

```bash
# Optional - Override Google credentials location
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json

# Optional - Override project root for Git tools
export GIT_PROJECT_ROOT=/path/to/project

# Optional - Set log level
export LOG_LEVEL=DEBUG
```

## 🚀 Quick Start Guide

### Step 1: Build the Project

```bash
# Navigate to MCP server directory
cd backend/mcp-server

# Clean and build
mvn clean install -DskipTests

# Expected output:
# [INFO] BUILD SUCCESS
```

### Step 2: Configure (Optional)

```bash
# Edit application.properties if needed
nano src/main/resources/application.properties

# Key settings:
# - server.port (default: 8081)
# - git.project.root (default: current directory)
```

### Step 3: Run the Service

```bash
# Start the MCP server
mvn spring-boot:run

# Expected output:
# Started McpServiceApplication in X.XXX seconds
# Server listening on port 8081
```

### Step 4: Verify Installation

```bash
# Check server status
curl http://localhost:8081/api/status

# List available tools
curl http://localhost:8081/api/tools | jq '.tools[].name'

# Test a simple tool
curl -X POST http://localhost:8081/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{"toolName":"add_numbers","arguments":{"a":5,"b":3}}' | jq
```

## 🧪 Testing

### Unit Tests

```java
// File: backend/mcp-server/src/test/java/de/jivz/mcp/tools/ToolExecutionTest.java
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ToolExecutionTest {
    
    @Autowired
    private ToolRegistry toolRegistry;
    
    @Autowired
    private ToolExecutorService executorService;
    
    @Test
    @Order(1)
    void testToolRegistryLoadsAllTools() {
        // When
        List<Tool> tools = toolRegistry.getAllTools();
        
        // Then
        assertThat(tools).hasSizeGreaterThan(10);
        assertThat(tools.stream().map(Tool::getName))
            .contains("add_numbers", "reverse_string", 
                     "get_current_branch", "google_tasks_list");
    }
    
    @Test
    @Order(2)
    void testAddNumbersExecution() {
        // Given
        Map<String, Object> arguments = Map.of("a", 10, "b", 20);
        
        // When
        Object result = executorService.execute("add_numbers", arguments);
        
        // Then
        assertThat(result).isInstanceOf(Map.class);
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertThat(resultMap.get("result")).isEqualTo(30.0);
    }
    
    @Test
    @Order(3)
    void testReverseStringExecution() {
        // Given
        Map<String, Object> arguments = Map.of("text", "hello");
        
        // When
        Object result = executorService.execute("reverse_string", arguments);
        
        // Then
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertThat(resultMap.get("reversed")).isEqualTo("olleh");
    }
}
```

### Integration Tests

```bash
# Run all tests
cd backend/mcp-server
mvn test

# Run specific test class
mvn test -Dtest=ToolExecutionTest

# Run with coverage report
mvn test jacoco:report
open target/site/jacoco/index.html
```

### Manual Testing Script

```bash
#!/bin/bash
# File: test-mcp-tools.sh

BASE_URL="http://localhost:8081/api"

echo "=== MCP Service Test Suite ==="
echo ""

# Test 1: Server Status
echo "1. Testing server status..."
STATUS=$(curl -s "$BASE_URL/status")
echo "$STATUS" | jq
echo ""

# Test 2: List Tools
echo "2. Listing all tools..."
curl -s "$BASE_URL/tools" | jq '.tools[] | .name'
echo ""

# Test 3: Native Tool - Add Numbers
echo "3. Testing add_numbers..."
curl -s -X POST "$BASE_URL/tools/execute" \
  -H "Content-Type: application/json" \
  -d '{"toolName":"add_numbers","arguments":{"a":42,"b":8}}' | jq
echo ""

# Test 4: Native Tool - Reverse String
echo "4. Testing reverse_string..."
curl -s -X POST "$BASE_URL/tools/execute" \
  -H "Content-Type: application/json" \
  -d '{"toolName":"reverse_string","arguments":{"text":"hello"}}' | jq
echo ""

# Test 5: Git Tool - Current Branch
echo "5. Testing get_current_branch..."
curl -s -X POST "$BASE_URL/tools/execute" \
  -H "Content-Type: application/json" \
  -d '{"toolName":"get_current_branch","arguments":{}}' | jq
echo ""

# Test 6: Git Tool - Git Status
echo "6. Testing get_git_status..."
curl -s -X POST "$BASE_URL/tools/execute" \
  -H "Content-Type: application/json" \
  -d '{"toolName":"get_git_status","arguments":{}}' | jq
echo ""

echo "=== Tests Completed ==="
```

## 🔧 Troubleshooting

### Problem: Server startet nicht auf Port 8081

**Symptom:**
```
Port 8081 was already in use
```

**Solution:**
```bash
# Check what's using the port
lsof -i :8081

# Kill the process or change port in application.properties
server.port=8082
```

### Problem: Git tools nicht verfügbar

**Symptom:**
```json
{
  "success": false,
  "error": "Git repository not found"
}
```

**Solution:**
```bash
# Ensure you're in a Git repository
git status

# Or configure git.project.root in application.properties
git.project.root=/path/to/your/git/project
```

### Problem: Google tools schlagen fehl

**Symptom:**
```json
{
  "success": false,
  "error": "Google credentials not found"
}
```

**Solution:**
```bash
# Set credentials path
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json

# Or configure in application.properties
google.credentials.path=/path/to/credentials.json
```

### Problem: Tool wird nicht registriert

**Symptom:**
Tool erscheint nicht in `/api/tools` Liste.

**Solution:**
1. Verify `@Component` annotation is present
2. Check package is scanned by Spring Boot:
```java
@SpringBootApplication
@ComponentScan(basePackages = "de.jivz.mcp")
public class McpServiceApplication {
    // ...
}
```
3. Enable debug logging:
```properties
logging.level.de.jivz.mcp.service.ToolRegistry=DEBUG
```

## 💡 Best Practices

### 1. Tool Implementation

✅ **DO:**
- Use `@Component` for automatic registration
- Implement comprehensive error handling
- Add detailed descriptions in `getDefinition()`
- Use `ToolArguments` for type-safe parameter extraction
- Log important operations with `@Slf4j`

❌ **DON'T:**
- Don't throw unchecked exceptions from `execute()`
- Don't use blocking I/O without timeout
- Don't store state in tool instances
- Don't access external resources without validation

### 2. Adding New Tools

```java
@Component
@Slf4j
public class MyTool implements Tool {
    
    // 1. Descriptive name (snake_case)
    @Override
    public String getName() {
        return "my_descriptive_tool_name";
    }
    
    // 2. Complete definition with all parameters
    @Override
    public McpTool getDefinition() {
        return McpTool.builder()
            .name(getName())
            .description("Clear description of what the tool does")
            .inputSchema(InputSchema.builder()
                .type("object")
                .properties(Map.of(
                    "param1", Property.builder()
                        .type("string")
                        .description("Clear parameter description")
                        .build()
                ))
                .required(List.of("param1"))
                .build())
            .build();
    }
    
    // 3. Robust execution with error handling
    @Override
    public Object execute(Map<String, Object> arguments) {
        try {
            ToolArguments args = ToolArguments.of(arguments);
            String param1 = args.getString("param1");
            
            log.debug("Executing {} with param1={}", getName(), param1);
            
            // Tool logic here
            
            return Map.of("result", "success");
        } catch (Exception e) {
            log.error("Error executing {}: {}", getName(), e.getMessage());
            throw new ToolExecutionException("Execution failed: " + e.getMessage(), e);
        }
    }
}
```

### 3. Tool Categories Organization

Organize tools by functionality:
```
tools/
├── native_tools/      # Pure Java tools
├── google/            # Google API integration
├── git/               # Git operations
├── filesystem/        # File system operations (future)
└── database/          # Database operations (future)
```

### 4. Testing Strategy

- **Unit Tests**: Test each tool in isolation
- **Integration Tests**: Test tool registration and execution
- **Manual Tests**: Use curl scripts for quick validation

## 🎯 Design Patterns

### Strategy Pattern
Each tool is an interchangeable strategy implementing the `Tool` interface.

```java
public interface Tool {  // Strategy interface
    String getName();
    McpTool getDefinition();
    Object execute(Map<String, Object> arguments);
}

// Concrete strategies
public class AddNumbersTool implements Tool { ... }
public class ReverseStringTool implements Tool { ... }
```

### Template Method Pattern
Base classes provide common functionality for tool groups.

```java
public abstract class GitToolBase implements Tool {
    protected Repository openRepository() { ... }
    protected void validatePath(Path path) { ... }
}

public class GetCurrentBranchTool extends GitToolBase {
    // Uses template methods from base class
}
```

### Registry Pattern
`ToolRegistry` maintains a collection of all available tools.

```java
@Service
public class ToolRegistry {
    private final Map<String, Tool> tools = new ConcurrentHashMap<>();
    
    public void register(Tool tool) {
        tools.put(tool.getName(), tool);
    }
}
```

### Facade Pattern
`McpController` provides a simplified interface to the complex tool system.

```java
@RestController
public class McpController {
    // Simplified API hiding complexity
    @GetMapping("/api/tools")
    public List<McpTool> getTools() { ... }
}
```

## 📚 Related Documentation

- **[Git Tools Provider Feature](../features/GIT_TOOLS_PROVIDER_FEATURE.md)** - Detailed Git tools documentation
- **[OpenRouter Service Architecture](OPENROUTER_SERVICE_ARCHITECTURE.md)** - Integration with LLM services
- **[RAG MCP Integration](RAG_MCP_INTEGRATION.md)** - RAG service integration
- **[MCP Server Refactoring Guide](../../backend/mcp-server/REFACTORING_GUIDE.md)** - Internal architecture details

## 🔄 Version History

### v3.0.0 (2026-01-13)
- ✅ Complete refactoring to tool-based architecture
- ✅ Strategy pattern implementation
- ✅ Automatic tool registration via Spring
- ✅ Git tools integration (5 tools)
- ✅ Google Tasks integration (6 tools)
- ✅ Native tools (5 tools)

### v2.0.0 (Previous)
- Provider-based architecture
- Manual tool registration

### v1.0.0 (Initial)
- Basic MCP protocol support

## 📞 Support & Contact

For issues or questions:
- Check [Troubleshooting](#-troubleshooting) section
- Review [Best Practices](#-best-practices)
- Consult related documentation
- Check server logs: `tail -f logs/mcp-server.log`

## 🎓 Summary

The MCP Multi-Provider Service provides:

✅ **Unified Tool Interface**: Single API for all tool types  
✅ **Automatic Registration**: Spring-based component scanning  
✅ **Strategy Pattern**: Flexible, extensible architecture  
✅ **Multiple Providers**: Native, Google, Git tools  
✅ **Production Ready**: Error handling, logging, testing  
✅ **Easy Extension**: Add new tools with `@Component implements Tool`  

**Quick Reference:**
- Port: **8081**
- Base URL: **http://localhost:8081/api**
- Total Tools: **16** (5 Native + 6 Google + 5 Git)
- Architecture: **Strategy Pattern + Spring Boot**

