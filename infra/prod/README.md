# Production Deployment Guide

## 🚀 Quick Start

### Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.x
- At least 8GB RAM
- 20GB free disk space

### 1. Setup Environment

Copy the example environment file and configure it:

```bash
cd infra/prod
cp .env.example .env
```

Edit `.env` and set:
- `POSTGRES_PASSWORD` (required)
- `OPENROUTER_API_KEY` (required)
- Other optional settings

### 2. Start Services

Start all services:

```bash
docker-compose up -d
```

Start specific services:

```bash
docker-compose up -d postgres ollama llm-chat-service
```

### 3. Initialize Ollama Model

Pull the default model (phi3:mini):

```bash
docker exec ai-ollama ollama pull phi3:mini
```

Or pull a different model:

```bash
docker exec ai-ollama ollama pull llama2
```

### 4. Verify Deployment

Check all services are healthy:

```bash
docker-compose ps
```

Check health endpoint:

```bash
curl http://localhost:8080/api/health
```

## 📊 Services Overview

| Service | Port | URL | Description |
|---------|------|-----|-------------|
| Frontend | 3300 | http://localhost:3300 | Vue3 SPA |
| Nginx | 8080 | http://localhost:8080 | Reverse Proxy |
| MCP Server | 8081 | http://localhost:8081 | MCP Server |
| LLM Chat Service | 8090 | http://localhost:8090 | Ollama Chat API |
| OpenRouter Service | 8084 | http://localhost:8084 | OpenRouter API |
| RAG MCP Server | 8086 | http://localhost:8086 | RAG API |
| Support Service | 8088 | http://localhost:8088 | Support Chat |
| Team Assistant | 8089 | http://localhost:8089 | Team Assistant |
| PostgreSQL | 5433 | localhost:5433 | Database |
| Ollama | 11434 | http://localhost:11434 | Ollama API |

## 🔗 API Endpoints via Nginx

All backend APIs are accessible through the Nginx reverse proxy:

- **MCP Server**: http://localhost:8080/api/mcp/
- **LLM Chat**: http://localhost:8080/api/llm-chat/
- **OpenRouter**: http://localhost:8080/api/openrouter/
- **RAG**: http://localhost:8080/api/rag/
- **Support**: http://localhost:8080/api/support/
- **Team Assistant**: http://localhost:8080/api/team/

## 🧪 Testing LLM Chat Service

Test directly:

```bash
curl -X POST http://localhost:8090/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Hello, how are you?",
    "stream": false
  }'
```

Test via Nginx proxy:

```bash
curl -X POST http://localhost:8080/api/llm-chat/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Hello, how are you?",
    "stream": false
  }'
```

## 📋 Common Commands

### View Logs

All services:
```bash
docker-compose logs -f
```

Specific service:
```bash
docker-compose logs -f llm-chat-service
```

### Restart Services

All services:
```bash
docker-compose restart
```

Specific service:
```bash
docker-compose restart llm-chat-service
```

### Stop Services

```bash
docker-compose down
```

Stop and remove volumes (⚠️ deletes data):
```bash
docker-compose down -v
```

### Update Services

Rebuild and restart:
```bash
docker-compose up -d --build
```

Rebuild specific service:
```bash
docker-compose up -d --build llm-chat-service
```

### Scale Services

Scale a service (if stateless):
```bash
docker-compose up -d --scale llm-chat-service=3
```

## 🔍 Monitoring

### Check Service Health

All services:
```bash
for port in 8081 8084 8086 8088 8089 8090; do
  echo "Port $port:"
  curl -s http://localhost:$port/actuator/health | jq
done
```

Ollama:
```bash
curl http://localhost:11434/api/version
```

PostgreSQL:
```bash
docker exec ai-postgres pg_isready -U ai_user
```

### Container Stats

```bash
docker stats
```

### Disk Usage

```bash
docker system df
```

## 🐛 Troubleshooting

### LLM Chat Service Issues

**Service won't start:**
```bash
# Check logs
docker-compose logs llm-chat-service

# Check Ollama is healthy
docker-compose ps ollama
```

**Ollama connection fails:**
```bash
# Verify Ollama is accessible
docker exec ai-llm-chat-service wget -O- http://ollama:11434/api/version

# Check network
docker network inspect ai-backend-network
```

**Model not found:**
```bash
# List available models
docker exec ai-ollama ollama list

# Pull the model
docker exec ai-ollama ollama pull phi3:mini
```

### Database Issues

**Connection refused:**
```bash
# Check if postgres is running
docker-compose ps postgres

# Check logs
docker-compose logs postgres
```

**Reset database:**
```bash
docker-compose down
docker volume rm ai-postgres-data
docker-compose up -d postgres
```

### Nginx Issues

**502 Bad Gateway:**
```bash
# Check backend services are running
docker-compose ps

# Check nginx logs
docker-compose logs nginx
```

## 🔐 Security Considerations

### Production Checklist

- [ ] Change default `POSTGRES_PASSWORD`
- [ ] Set strong `OPENROUTER_API_KEY`
- [ ] Use HTTPS in production (configure SSL certificates)
- [ ] Enable firewall rules
- [ ] Regular security updates
- [ ] Monitor logs for suspicious activity
- [ ] Backup database regularly
- [ ] Use secrets management (e.g., Docker Secrets)

### Database Backup

Backup:
```bash
docker exec ai-postgres pg_dump -U ai_user ai_challenge_db > backup.sql
```

Restore:
```bash
cat backup.sql | docker exec -i ai-postgres psql -U ai_user ai_challenge_db
```

## 📦 Volume Management

### List Volumes

```bash
docker volume ls | grep ai-
```

### Backup Volumes

Ollama data:
```bash
docker run --rm -v ai-ollama-data:/data -v $(pwd):/backup alpine \
  tar czf /backup/ollama-backup.tar.gz /data
```

PostgreSQL data:
```bash
docker run --rm -v ai-postgres-data:/data -v $(pwd):/backup alpine \
  tar czf /backup/postgres-backup.tar.gz /data
```

### Cleanup Unused Resources

```bash
docker system prune -a --volumes
```

## 🌐 Network Configuration

The deployment uses two networks:

- **ai-frontend-network**: Frontend and Nginx
- **ai-backend-network**: All backend services and database

Inspect networks:
```bash
docker network inspect ai-frontend-network
docker network inspect ai-backend-network
```

## 📈 Performance Tuning

### Ollama Performance

For better performance, increase Ollama resources:

```yaml
ollama:
  deploy:
    resources:
      limits:
        cpus: '4'
        memory: 8G
      reservations:
        cpus: '2'
        memory: 4G
```

### PostgreSQL Performance

Tune PostgreSQL settings in environment:

```yaml
POSTGRES_SHARED_BUFFERS=256MB
POSTGRES_MAX_CONNECTIONS=200
```

## 🆘 Getting Help

Check service logs:
```bash
docker-compose logs -f [service-name]
```

Check service health:
```bash
curl http://localhost:[port]/actuator/health
```

Full system status:
```bash
docker-compose ps
docker stats
```

