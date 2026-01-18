# 🤖 AI DevOps Agent CLI

Ein intelligentes CLI-Tool für DevOps-Operationen, das natürliche Sprache (Deutsch & Englisch) versteht und mit AI-Unterstützung komplexe Infrastruktur-Aufgaben automatisiert.

## 🎯 Features

- **🚀 Deployment Management**: Trigger GitHub Actions Workflows
- **📊 Container Monitoring**: Status-Überwachung aller Docker-Container
- **📜 Log Viewing**: Container-Logs in Echtzeit anzeigen
- **🏥 Health Checks**: Service-Health-Status prüfen
- **📝 AI Release Notes**: Automatische Release-Notes-Generierung mit Claude
- **🔄 Rollback**: Schnelle Rollbacks zu vorherigen Versionen
- **🌍 Mehrsprachig**: Versteht Deutsch und Englisch
- **🎨 Schöne Ausgabe**: Emoji und Farben für bessere UX

## 📋 Voraussetzungen

- Java 21+
- Maven 3.9+
- Docker (für Container-Management)
- GitHub Personal Access Token
- OpenRouter API Key (für AI-Features)

## 🔧 Installation

### 1. Repository klonen

```bash
git clone <repository-url>
cd AI_Advent_Challenge/backend/agent-service
```

### 2. Umgebungsvariablen setzen

```bash
export OPENROUTER_API_KEY="your-openrouter-api-key"
export PERSONAL_GITHUB_TOKEN="your-github-token"
export PERSONAL_GITHUB_REPOSITORY="owner/repo"
export POSTGRES_PASSWORD="your-db-password"
```

Oder erstelle eine `.env` Datei:

```bash
OPENROUTER_API_KEY=sk-or-...
PERSONAL_GITHUB_TOKEN=ghp_...
PERSONAL_GITHUB_REPOSITORY=jivz/AI_Advent_Challenge
POSTGRES_PASSWORD=secret
```

### 3. Build

```bash
mvn clean package -DskipTests
```

### 4. CLI starten

```bash
java -jar target/agent-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=cli
```

## 💬 Verwendung

### Kommandos

#### 🚀 Deployment

```bash
> deploy team-service
🔄 Deploying team-service...
✅ Tests: 45/45 passed
✅ Building Docker image...
✅ Deployed team-service v1.2.3 in 2m 15s
```

Natürliche Sprache (Deutsch):
```bash
> задеплой support service
🔄 Деплою support-service...
✅ Задеплоено за 1м 45с
```

#### 📊 Status anzeigen

```bash
> status

📊 Services Status:
──────────────────────────────────────────────────────────────────────
✅ support-service        │ running      │ uptime: 3d 5h
   └─ Memory: 384MB
✅ team-service           │ running      │ uptime: 12m
   └─ Memory: 512MB
⚠️  rag-service           │ running      │ uptime: 5h
   └─ Memory: 850MB (High!)
✅ postgres               │ running      │ uptime: 5d
──────────────────────────────────────────────────────────────────────
```

Alternativ:
```bash
> покажи статус
> show status
> что там с сервисами?
```

#### 📜 Logs anzeigen

```bash
> logs team-service

📜 Logs for team-service (last 20 lines):
──────────────────────────────────────────────────────────────────────
2026-01-18 21:15:32 INFO  Starting TeamAssistantService
2026-01-18 21:15:35 INFO  Connected to PostgreSQL
2026-01-18 21:15:36 INFO  Loaded 13 prompts
2026-01-18 21:15:38 INFO  Service ready on port 8089
──────────────────────────────────────────────────────────────────────
```

#### 🏥 Health Check

```bash
> health support-service
✅ support-service is healthy
   All systems operational
```

#### 📝 Release Notes generieren

```bash
> generate release notes

📝 Analyzing commits...
📝 Generating release notes...

═══════════════════════════════════════════════════════════════════════

# Release v1.2.3 - January 18, 2026

## Team Service
### Features
- Added RAG priority system for better context ranking
- Implemented Russian language source display
- Enhanced confidence scoring algorithm

### Bug Fixes
- Fixed empty sources array handling
- Corrected UTF-8 encoding for Cyrillic text

## Support Service
### Bug Fixes
- Translation improvements for German/Russian
- Fixed empty array handling in responses

═══════════════════════════════════════════════════════════════════════

💡 To create a GitHub release, use: create release
```

Natürliche Sprache:
```bash
> сгенерируй release notes
> was hat sich geändert?
> что изменилось с последнего релиза?
```

#### 🔄 Rollback

```bash
> rollback support-service

🔄 Rolling back support-service...
✅ Container restarted
✅ Health check: OK
✅ Rollback completed
```

#### ❓ Hilfe

```bash
> help

📚 Available Commands:

🚀 Deployment:
  deploy <service>        Deploy a specific service
  deploy all             Deploy all services
  rollback <service>     Rollback to previous version

📊 Monitoring:
  status                 Show all container statuses
  logs <service>         Show last 20 log lines
  health <service>       Check service health

📝 Release Management:
  release notes          Generate AI release notes
  create release         Create GitHub release
  commits                Show recent commits

🛠️  Utility:
  help                   Show this help
  exit / quit            Exit the agent

💡 Examples:
  > deploy team-service
  > показать статус
  > задеплой support-service
  > generate release notes
```

## 🏗️ Architektur

Das Projekt folgt **SOLID-Prinzipien** und **Clean Code**:

```
cli/
├── domain/          # Domain Models (Value Objects)
│   ├── Command.java
│   ├── CommandResult.java
│   ├── ContainerStatus.java
│   └── DeploymentInfo.java
│
├── parser/          # Command Parsing mit AI
│   └── CommandParser.java
│
├── executor/        # Strategy Pattern für Commands
│   ├── CommandExecutor.java (Interface)
│   ├── DeployExecutor.java
│   ├── StatusExecutor.java
│   ├── LogsExecutor.java
│   ├── ReleaseNotesExecutor.java
│   ├── RollbackExecutor.java
│   └── HealthExecutor.java
│
├── service/         # Business Logic
│   └── CommandService.java
│
├── formatter/       # Output Formatting
│   └── CLIOutputFormatter.java
│
└── CLIApplication.java  # Main CLI Entry Point

client/              # External API Clients
├── GitHubActionsClient.java
├── DockerClient.java
└── OpenRouterApiClient.java (existing)
```

### Design Patterns

- **Strategy Pattern**: `CommandExecutor` Interface mit verschiedenen Implementierungen
- **Dependency Injection**: Alle Dependencies via Constructor Injection
- **Single Responsibility**: Jede Klasse hat genau eine Verantwortung
- **Open/Closed**: Neue Commands können ohne Änderung bestehenden Codes hinzugefügt werden

## 🔐 Sicherheit

- API Keys werden über Umgebungsvariablen geladen
- GitHub Token hat minimale Permissions (actions:write, contents:read)
- Keine Secrets im Code oder Logs

## 🧪 Tests

```bash
# Unit Tests
mvn test

# Integration Tests
mvn verify
```

## 🐛 Troubleshooting

### CLI startet nicht

```bash
# Prüfe Java Version
java -version  # Muss Java 21+ sein

# Prüfe Umgebungsvariablen
echo $OPENROUTER_API_KEY
echo $PERSONAL_GITHUB_TOKEN
```

### Deployment schlägt fehl

```bash
# Prüfe GitHub Token Permissions
# Token muss 'workflow' scope haben

# Prüfe Workflow-Datei
# deploy.yml muss in .github/workflows/ existieren
```

### Container Status zeigt nichts

```bash
# Prüfe MCP Docker Service
curl http://localhost:8081/actuator/health

# Starte MCP Docker Service falls nötig
docker-compose up -d mcp-docker-monitor
```

## 📦 Verfügbare Services

- `team-service` (Port 8089)
- `support-service` (Port 8088)
- `rag-service` / `rag-mcp-server` (Port 8086)
- `openrouter-service` (Port 8084)
- `mcp-server` (Port 8081)
- `agent-service` (Port 8087)

## 🚀 Deployment

### Als Standalone CLI

```bash
java -jar agent-service.jar --spring.profiles.active=cli
```

### Im Docker Container

```dockerfile
FROM eclipse-temurin:21-jre-alpine
COPY target/agent-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar", "--spring.profiles.active=cli"]
```

## 📄 Lizenz

MIT License

## 🤝 Beitragen

Pull Requests sind willkommen! Bitte beachte die SOLID-Prinzipien und Clean Code Guidelines.

## 📞 Support

Bei Fragen oder Problemen erstelle ein Issue im GitHub Repository.

---

**Made with ❤️ and 🤖 AI**

