# 🎯 AI DevOps Agent CLI - Финальная Оптимизированная Конфигурация

## ✅ Что было оптимизировано

### 1. **Архитектура микросервисов - правильное использование**

#### **MCP Server (port 8081)** 
✅ GitHub Tools Integration
- `trigger_workflow` - Триггер GitHub Actions workflow
- `list_workflow_runs` - Список workflow runs
- `get_workflow_run` - Получить конкретный run
- `list_commits` - Список коммитов

**GitHubActionsClient** → использует MCP Server на 8081

#### **MCP Docker Monitor (port 8083)**
✅ Docker Container Management через SSH
- `list_containers` - Список всех контейнеров
- `get_container_logs` - Логи контейнера
- `restart_container` - Перезапуск контейнера
- Container stats и monitoring

**DockerClient** → использует MCP Docker Monitor на 8083

---

## 🏗️ Правильная Архит��ктура

```
AI DevOps CLI (agent-service)
         │
         ├─→ GitHubActionsClient ─���→ MCP Server (8081)
         │                               │
         │                               └─→ GitHub API
         │
         └─→ DockerClient ──→ MCP Docker Monitor (8083)
                                         │
                                         └─→ Docker (SSH)
```

### Преимущества такого подхода:

1. ✅ **Переиспользование существующей инфраструктуры**
   - MCP Server уже умеет работать с GitHub
   - MCP Docker Monitor уже умеет работать с Docker через SSH

2. ✅ **Единая точка конфигурации**
   - GitHub token настраивается только в MCP Server
   - SSH credentials только в MCP Docker Monitor
   - Agent-service не нужны прямые credentials

3. ✅ **Масштабируемость**
   - MCP Server может использоваться другими сервисами
   - MCP Docker Monitor может мониторить удаленные серверы

4. ✅ **Безопасность**
   - Agent-service не имеет прямого доступа к GitHub API token
   - Agent-service не имеет SSH credentials к серверам
   - Все через MCP абстракцию

---

## 📝 Конфигурация

### application.properties
```properties
# MCP Server Configuration (GitHub Tools)
mcp.server.url=http://localhost:8081

# Docker MCP Configuration
mcp.docker.url=http://localhost:8083

# GitHub Configuration (только для metadata, не для прямого доступа)
github.repository=${PERSONAL_GITHUB_REPOSITORY:}
```

### application-cli.properties
```properties
# MCP Server Configuration (GitHub Tools on port 8081)
mcp.server.url=${MCP_SERVER_URL:http://localhost:8081}

# Docker MCP Configuration (for container monitoring on port 8083)
mcp.docker.url=${MCP_DOCKER_URL:http://localhost:8083}

# GitHub Configuration
github.repository=${PERSONAL_GITHUB_REPOSITORY}
```

---

## 🚀 Запуск всей системы

### 1. Запусти все микросервисы

```bash
cd infra/prod
docker-compose up -d
```

Это запустит:
- **postgres** (5433)
- **mcp-server** (8081) - с GitHub tools
- **openrouter-service** (8084)
- **rag-mcp-server** (8086)
- **support-service** (8088)
- **team-assistant-service** (8089)

### 2. Запусти MCP Docker Monitor отдельно

```bash
cd backend/mcp-docker-monitor
mvn spring-boot:run
```

Или если он уже в docker-compose - убедись что работает на **8083**.

### 3. Запусти Agent CLI

```bash
cd backend/agent-service
./start-cli.sh
```

---

## 🎯 Теперь все команды работают через MCP!

### Deploy через MCP Server → GitHub Actions
```bash
> deploy team-service
```
**Поток:**
1. CLI → DeployExecutor
2. DeployExecutor → GitHubActionsClient
3. GitHubActionsClient → MCP Server (8081)
4. MCP Server → GitHub Actions API
5. Workflow запущен!

### Status через MCP Docker Monitor
```bash
> status
```
**Поток:**
1. CLI → StatusExecutor
2. StatusExecutor → DockerClient
3. DockerClient → MCP Docker Monitor (8083)
4. MCP Docker Monitor → Docker via SSH
5. Статус контейнеров показан!

### Logs через MCP Docker Monitor
```bash
> logs team-service
```
**Поток:**
1. CLI → LogsExecutor
2. LogsExecutor → DockerClient
3. DockerClient → MCP Docker Monitor (8083)
4. MCP Docker Monitor → Docker logs via SSH
5. Логи показаны!

### Release Notes через MCP Server → GitHub
```bash
> generate release notes
```
**Поток:**
1. CLI → ReleaseNotesExecutor
2. ReleaseNotesExecutor → GitHubActionsClient.getCommits()
3. GitHubActionsClient → MCP Server (8081)
4. MCP Server → GitHub API (list commits)
5. Коммиты → OpenRouter AI
6. AI генерирует release notes!

---

## ✨ Что получилось

### ✅ **Чистая ��рхитектура**
- Agent CLI не знает о GitHub API
- Agent CLI не знает о Docker/SSH
- Все через MCP абстракции

### ✅ **Переиспользование кода**
- MCP Server tools используются из CLI
- Docker Monitor используется для мониторинга
- Нет дублирования логики

### ✅ **SOLID принципы**
- **Single Responsibility**: Каждый клиент делает одно дело
- **Dependency Inversion**: Зависимость от абстракций (WebClient)
- **Open/Closed**: Легко добавить новые MCP tools

### ✅ **Безопасность**
- Credentials в одном месте
- Agent CLI работает только с localhost MCP
- Нет прямого доступа к внешним API

---

## 🎬 Полный сценарий работы

```bash
# 1. Запусти все сервисы
cd infra/prod && docker-compose up -d

# 2. Проверь что MCP Docker Monitor работает
curl http://localhost:8083/actuator/health

# 3. Проверь что MCP Server работает
curl http://localhost:8081/actuator/health

# 4. Запусти CLI
cd backend/agent-service
./start-cli.sh

# 5. Используй команды
> status                    # Docker Monitor → SSH → Docker
> logs team-service         # Docker Monitor → SSH → Logs
> deploy team-service       # MCP Server → GitHub Actions
> generate release notes    # MCP Server → GitHub → AI
> exit
```

---

## 📊 Порты и сервисы

| Порт | Сервис | Назначение |
|------|--------|------------|
| 8081 | mcp-server | GitHub Tools MCP |
| 8083 | mcp-docker-monitor | Docker SSH Monitor |
| 8084 | openrouter-service | AI API |
| 8085 | agent-service | (web mode) |
| 8086 | rag-mcp-server | RAG MCP |
| 8088 | support-service | Support Bot |
| 8089 | team-assistant-service | Team Bot |
| 5433 | postgres | Database |

---

## 🎉 Результат

Теперь у тебя **правильная микросервисная архитектура**:

1. **Agent CLI** - UI layer
2. **MCP Server** - GitHub integration layer
3. **MCP Docker Monitor** - Infrastructure layer
4. **External APIs** - GitHub, Docker

Каждый слой делает свою работу, все чисто, масштабируемо и безопасно! 🚀

---

**Обновлено:** 2026-01-18  
**Статус:** ✅ Оптимизировано и готово к использованию

