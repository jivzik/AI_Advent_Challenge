# 🤖 AI Agent Service

**Port:** 8087 | **Spring Boot:** 4.0.1 | **Java:** 21

Сервис автоматических AI-агентов для DevOps и Code Review с интеграцией OpenRouter LLM, MCP tools и GitHub API.

## 🎯 Основные возможности

### 1. 🔍 Автоматический Code Review Agent

Непрерывный мониторинг Pull Requests с автоматическим review через AI:

- **Автоматическое обнаружение новых PR** (каждые 2 минуты)
- **Глубокий анализ кода** через OpenRouter API (Claude 3.5 Sonnet)
- **Интеграция с MCP tools** (git:get_pr_info, git:get_pr_diff, rag:search)
- **Structured review output** с категоризацией issue (CRITICAL/MAJOR/MINOR)
- **Автоматические решения**: APPROVE / REQUEST_CHANGES / COMMENT
- **Сохранение reviews** в PostgreSQL + Markdown файлы
- **Поддержка distributed locking** (ShedLock) для multi-instance deployment

**Пример автоматического review:**
```
✅ CodeReviewAgent completed in 12543ms with 3 issues (DECISION: REQUEST_CHANGES)
📁 Review saved: reviews/PR-123-20260125-143022.md
```

### 2. 💬 AI DevOps CLI

Интерактивный CLI для управления инфраструктурой через естественный язык:

**Поддерживаемые команды:**
- `status` - статус всех Docker контейнеров
- `deploy <service>` - деплой сервиса через GitHub Actions
- `logs <service>` - просмотр логов контейнера
- `health <service>` - проверка health endpoint
- `rollback <service>` - откат к предыдущей версии
- `commit <message>` - git commit с AI-оптимизацией сообщения
- `push [branch]` - git push в удаленный репозиторий
- `git status` - состояние git репозитория
- `generate release notes` - AI-генерация release notes
- `create release` - создание GitHub release

**Многоязычная поддержка:**
```bash
# English
> deploy team-service
> show status

# Deutsch
> задеплой support-service
> покажи статус
> закоммить изменения добавлен новый feature
```

**AI-powered parsing:**
- Использует OpenRouter LLM для понимания естественного языка
- Fallback на pattern matching для простых команд (1ms vs ~200ms AI call)
- Автоматическое определение языка и intent extraction

## 🚀 Быстрый старт

### Prerequisite: Настройка окружения

```bash
# Required credentials
export OPENROUTER_API_KEY="sk-or-v1-YOUR-KEY-HERE"
export PERSONAL_GITHUB_TOKEN="ghp_YOUR-TOKEN-HERE"
export PERSONAL_GITHUB_REPOSITORY="owner/repository"

# Database (PostgreSQL with pgvector)
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/ai_challenge_db"
export SPRING_DATASOURCE_USERNAME="local_user"
export SPRING_DATASOURCE_PASSWORD="local_password"
```

### Режим 1: Code Review Agent (Server Mode)

Запуск как HTTP сервис с автоматическим PR мониторингом:

```bash
cd backend/agent-service

# Build
./mvnw clean install -DskipTests

# Run as server
./mvnw spring-boot:run

# Проверка работы
curl http://localhost:8087/actuator/health
```

**Конфигурация PR мониторинга:**
```properties
# application.properties
code-review.scheduler.enabled=true          # Включить автомониторинг
code-review.scheduler.interval=120000       # Интервал проверки (2 мин)
code-review.repository=${GITHUB_REPOSITORY} # Репозиторий для мониторинга
code-review.reports-dir=reviews             # Директория для сохранения reviews
```

**Логи агента:**
```
🔍 PR Monitor: Starting scan at 2026-01-25T14:30:00
✅ Found new PR #123: Add feature X
🔍 CodeReviewAgent starting review for PR #123
✅ Got PR info: 8 files changed
🔧 Got 42 MCP tools
💾 Saving review...
🎉 CodeReviewAgent completed in 12543ms with 3 issues (DECISION: REQUEST_CHANGES)
```

### Режим 2: Interactive CLI Mode

Запуск интерактивного CLI для DevOps операций:

```bash
cd backend/agent-service

# Build (если еще не собрано)
./mvnw clean install -DskipTests

# Run CLI mode
./mvnw spring-boot:run -Dspring-boot.run.profiles=cli -Dcli.enabled=true

# Или через JAR
java -jar target/agent-service-0.0.1-SNAPSHOT.jar --cli.enabled=true
```

**CLI Session пример:**
```
╔══════════════════════════════════════╗
║   AI DevOps Agent CLI v1.0          ║
║   Type 'help' for available commands ║
╚══════════════════════════════════════╝

> status
✅ team-service: RUNNING (healthy)
✅ support-service: RUNNING (healthy)
⚠️  rag-service: STOPPED

> deploy support-service
🚀 Triggering GitHub Actions workflow...
✅ Deployment started: run_id=123456789
📊 View at: https://github.com/owner/repo/actions/runs/123456789

> generate release notes
🤖 Generating release notes with AI...
✨ Release Notes:

## 🎉 Version 1.2.0

### New Features
- Added Code Review Agent with automatic PR monitoring
- Multi-language CLI support (EN/DE/RU)
...

> exit
👋 Goodbye!
```

## 🔧 Архитектура

### MCP Integration

Агент использует **Model Context Protocol** для взаимодействия с внешними сервисами:

```
CodeReviewAgent
    ├─> MCPFactory.getAllToolDefinitions()
    │   ├─ GitMCPService (git:get_pr_info, git:get_pr_diff)
    │   ├─ GitHubMCPService (github:create_comment, github:approve_pr)
    │   ├─ DockerMCPService (docker:list, docker:logs, docker:inspect)
    │   └─ RagMcpService (rag:search for documentation context)
    │
    └─> ToolExecutionOrchestrator.executeToolLoop()
        ├─ OpenRouterApiClient (Claude 3.5 Sonnet)
        ├─ Tool calling loop (max 10 iterations)
        └─ Result parsing & aggregation
```

### Agent Execution Flow

```
1. PRMonitorScheduler (@Scheduled every 2 min)
   ↓
2. PRDetectorService.detectAndProcessNewPRs()
   ↓
3. AgentOrchestratorService.processTask()
   ↓
4. CodeReviewAgent.execute()
   ├─ getPRInfo() via git:get_pr_info
   ├─ buildReviewPrompt() with structured format
   ├─ ToolExecutionOrchestrator.executeToolLoop()
   │   ├─ LLM calls git:get_pr_diff
   │   ├─ LLM analyzes code
   │   └─ LLM returns structured review
   ├─ parseReviewResult() extracts DECISION BLOCK
   └─ ReviewStorageService.saveReview() to DB + file
```

### CLI Architecture

```
CLIApplication (JLine3 terminal)
    ↓
CommandParser (AI-powered or pattern-based)
    ↓
CommandService.execute()
    ↓
CommandExecutor (strategy pattern)
    ├─ DeployExecutor → GitHubActionsClient
    ├─ StatusExecutor → DockerClient
    ├─ LogsExecutor → DockerClient
    ├─ CommitExecutor → GitMCPService
    ├─ PushExecutor → GitMCPService
    └─ ReleaseNotesExecutor → OpenRouterApiClient
```

## 📊 API Endpoints

Сервис предоставляет REST API для ручного управления:

```bash
# Health check
GET http://localhost:8087/actuator/health

# Trigger manual review
POST http://localhost:8087/api/agent/review
{
  "prNumber": 123,
  "repository": "owner/repo"
}

# Get review history
GET http://localhost:8087/api/agent/reviews?prNumber=123

# List all agents
GET http://localhost:8087/api/agent/list
```

## 🔐 Required Credentials

### 1. OpenRouter API Key

```bash
# Получить на https://openrouter.ai/
# Settings → Keys → Create new key

export OPENROUTER_API_KEY="sk-or-v1-..."
```

**Используемые модели:**
- `anthropic/claude-3.5-sonnet` - для code review (temperature: 0.2)
- `anthropic/claude-3.5-sonnet` - для CLI parsing (temperature: 0.7)

### 2. GitHub Personal Access Token

```bash
# GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
# Scopes:
#   ✅ repo (full control)
#   ✅ workflow (GitHub Actions)

export PERSONAL_GITHUB_TOKEN="ghp_..."
export PERSONAL_GITHUB_REPOSITORY="owner/repository"
```

### 3. PostgreSQL Database

```bash
# PostgreSQL 15+ with pgvector extension
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/ai_challenge_db"
export SPRING_DATASOURCE_USERNAME="local_user"
export SPRING_DATASOURCE_PASSWORD="local_password"
```

**Database Schema:**
```sql
-- PR Reviews storage
CREATE TABLE pr_reviews (
    id BIGSERIAL PRIMARY KEY,
    pr_number INTEGER NOT NULL,
    repository VARCHAR(255),
    decision VARCHAR(50),
    total_issues INTEGER,
    critical_issues INTEGER,
    major_issues INTEGER,
    minor_issues INTEGER,
    review_text TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- ShedLock for distributed scheduling
CREATE TABLE shedlock (
    name VARCHAR(64) PRIMARY KEY,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);
```

## 🧪 Тестирование

```bash
# Unit tests
./mvnw test

# Integration test (requires running MCP servers)
./mvnw test -Dtest=CodeReviewIntegrationTest

# Test specific agent
./mvnw test -Dtest=CodeReviewAgentTest

# Test CLI parsing
./mvnw test -Dtest=CommandParserTest
```

**Test coverage:**
- ✅ CodeReviewAgent.execute() with mock MCP responses
- ✅ CommandParser AI parsing vs pattern matching
- ✅ ReviewStorageService DB persistence
- ✅ PRDetectorService new PR detection

## 🐛 Troubleshooting

### "Java 21 required"
```bash
# Ubuntu/Debian
sudo apt install openjdk-21-jdk

# macOS
brew install openjdk@21

# Verify
java -version  # должно быть 21.x
```

### "OPENROUTER_API_KEY not set"
```bash
# Проверка
echo $OPENROUTER_API_KEY

# Permanent (добавить в ~/.bashrc или ~/.zshrc)
export OPENROUTER_API_KEY="sk-or-v1-..."
source ~/.bashrc
```

### "Cannot connect to MCP server"
```bash
# Проверить что MCP серверы запущены
curl http://localhost:8081/actuator/health  # mcp-server
curl http://localhost:8083/actuator/health  # mcp-docker-monitor

# Запустить через docker-compose
cd ../../infra/localdev
docker-compose up -d mcp-server mcp-docker-monitor
```

### "Database connection failed"
```bash
# Проверить PostgreSQL
psql -U local_user -d ai_challenge_db -c "SELECT version();"

# Создать pgvector extension
psql -U local_user -d ai_challenge_db -c "CREATE EXTENSION IF NOT EXISTS vector;"

# Проверить таблицы
psql -U local_user -d ai_challenge_db -c "\dt"
```

### "PR Monitor не запускается"
```bash
# Проверить конфигурацию
grep "code-review" backend/agent-service/src/main/resources/application.properties

# Проверить что GITHUB_REPOSITORY установлена
echo $PERSONAL_GITHUB_REPOSITORY

# Включить debug логи
java -jar target/agent-service-0.0.1-SNAPSHOT.jar --logging.level.de.jivz.agentservice=DEBUG
```

## 📁 Структура проекта

```
agent-service/
├── src/main/java/de/jivz/agentservice/
│   ├── agent/
│   │   ├── Agent.java                    # Interface для всех агентов
│   │   ├── AgentRegistry.java            # Registry pattern для агентов
│   │   ├── CodeReviewAgent.java          # ⭐ Main code review logic
│   │   └── model/
│   │       ├── AgentTask.java            # Task definition
│   │       └── AgentResult.java          # Execution result
│   │
│   ├── cli/
│   │   ├── CLIApplication.java           # ⭐ Interactive CLI entry point
│   │   ├── parser/CommandParser.java     # AI-powered command parsing
│   │   ├── executor/                     # Command executors (strategy)
│   │   │   ├── DeployExecutor.java
│   │   │   ├── StatusExecutor.java
│   │   │   ├── CommitExecutor.java
│   │   │   └── ...
│   │   └── formatter/CLIOutputFormatter.java
│   │
│   ├── scheduler/
│   │   └── PRMonitorScheduler.java       # ⭐ @Scheduled PR monitoring
│   │
│   ├── service/
│   │   ├── PRDetectorService.java        # New PR detection
│   │   ├── ReviewStorageService.java     # DB + file persistence
│   │   ├── PromptLoaderService.java      # Load prompts from resources
│   │   └── orchestrator/
│   │       ├── AgentOrchestratorService.java
│   │       └── ToolExecutionOrchestrator.java # MCP tool calling loop
│   │
│   ├── mcp/
│   │   ├── MCPFactory.java               # MCP provider registry
│   │   ├── BaseMCPService.java           # Abstract MCP client
│   │   ├── GitMCPService.java            # Git operations
│   │   ├── GitHubMCPService.java         # GitHub API
│   │   ├── DockerMCPService.java         # Docker management
│   │   └── RagMcpService.java            # RAG search
│   │
│   ├── client/
│   │   ├── OpenRouterApiClient.java      # OpenRouter LLM API
│   │   ├── GitHubActionsClient.java      # GitHub Actions API
│   │   └── DockerClient.java             # Docker daemon API
│   │
│   └── persistence/
│       ├── PRReviewEntity.java           # JPA entity
│       └── PRReviewRepository.java       # Spring Data JPA
│
└── src/main/resources/
    ├── application.properties             # Main configuration
    ├── application-cli.properties         # CLI-specific config
    └── prompts/
        ├── code-reviewer.md               # Code review prompt
        ├── system-tools.md                # Tool calling instructions
        └── ...
```

## 🔗 Интеграции

### Внутренние MCP сервисы
- `mcp-server:8081` - Git operations
- `mcp-docker-monitor:8083` - Docker monitoring
- `rag-mcp-server:8086` - Document search

### Внешние API
- **OpenRouter API** - LLM inference (Claude 3.5 Sonnet)
- **GitHub REST API** - PR info, Actions workflows
- **GitHub GraphQL API** - Advanced PR queries
- **Docker Engine API** - Container management

## 📚 Дополнительная документация

- Основная документация проекта: `../../CLAUDE.md`
- Prompts для агентов: `src/main/resources/prompts/`
- Review examples: `reviews/` (создается автоматически)

## 🎯 Roadmap

- [ ] Telegram bot для уведомлений о reviews
- [ ] Multi-agent collaboration (CodeReviewAgent + SecurityAgent)
- [ ] Custom review rules per repository
- [ ] Web UI для просмотра review history
- [ ] GitHub App integration (webhooks вместо polling)

---

**💡 Tip:** Для быстрого тестирования используйте CLI mode с командой `help` для списка всех доступных команд.

