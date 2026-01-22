# 🚀 Quick Start Guide - AI DevOps Agent CLI

Eine 5-Minuten-Anleitung zum Starten des AI DevOps Agent CLI.

## ⚡ Schnellstart

### 1. Umgebungsvariablen setzen

```bash
export OPENROUTER_API_KEY="sk-or-v1-YOUR-KEY-HERE"
export PERSONAL_GITHUB_TOKEN="ghp_YOUR-TOKEN-HERE"
export PERSONAL_GITHUB_REPOSITORY="username/repository"
export POSTGRES_PASSWORD="your-password"
```

### 2. CLI starten

```bash
cd backend/agent-service
./start-cli.sh
```

Das Script prüft automatisch:
- ✅ Java Version (21+)
- ✅ Umgebungsvariablen
- ✅ Baut die Anwendung
- ✅ Startet den CLI

### 3. Erste Schritte

```bash
# Hilfe anzeigen
> help

# Container Status prüfen
> status

# Logs anschauen
> logs team-service

# Service deployen
> deploy support-service
```

## 🔐 Benötigte Credentials

### OpenRouter API Key
1. Gehe zu https://openrouter.ai/
2. Registriere dich / Login
3. Erstelle einen API Key unter "Keys"
4. Key Format: `sk-or-v1-...`

### GitHub Personal Access Token
1. GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. "Generate new token (classic)"
3. Wähle diese Scopes:
   - ✅ `repo` (Full control of private repositories)
   - ✅ `workflow` (Update GitHub Action workflows)
4. Token Format: `ghp_...`

### Repository Name
Format: `owner/repository`
Beispiel: `jivz/AI_Advent_Challenge`

## 🧪 Test ohne Build

Wenn schon gebaut:

```bash
./start-cli.sh --skip-build
```

## 🐛 Troubleshooting

### "Java 21 required"
```bash
# Ubuntu/Debian
sudo apt install openjdk-21-jdk

# macOS
brew install openjdk@21
```

### "OPENROUTER_API_KEY not set"
```bash
# Prüfen ob gesetzt
echo $OPENROUTER_API_KEY

# Dauerhaft setzen (in ~/.bashrc oder ~/.zshrc)
export OPENROUTER_API_KEY="sk-or-..."
```

### "Cannot connect to Docker"
```bash
# Prüfe ob MCP Docker Service läuft
curl http://localhost:8081/actuator/health

# Falls nicht, starte Docker Compose
cd ../../infra/prod
docker-compose up -d mcp-server
```

## 📖 Mehr Details

Siehe [CLI_README.md](CLI_README.md) für vollständige Dokumentation.

## 🎬 Demo-Befehle

Probiere diese Befehle aus:

```bash
# Status Check
> status

# Deploy
> deploy team-service

# Logs
> logs support-service

# Health
> health rag-service

# Release Notes (AI)
> generate release notes

# Rollback
> rollback support-service

# Auf Deutsch
> покажи статус
> задеплой team-service
```

## 🆘 Support

Bei Problemen:
1. Prüfe die Logs: `tail -f logs/agent-service.log`
2. Prüfe Environment: `env | grep -E 'OPENROUTER|GITHUB|POSTGRES'`
3. Erstelle ein GitHub Issue

---

**Viel Erfolg! 🚀**

