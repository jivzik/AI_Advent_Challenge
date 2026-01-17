# AI Advent Challenge

> 🤖 Multi-Service AI Platform with MCP Integration, RAG, and Advanced Conversational Agents

An enterprise-grade AI chat platform that integrates multiple AI providers (OpenRouter, Perplexity) with Model Context Protocol (MCP), Retrieval Augmented Generation (RAG), and specialized conversational agents.

## 🎯 Core Features

### 🔌 Model Context Protocol (MCP)
- **Multi-Provider Architecture**: Native tools + Perplexity integration
- **Dynamic Tool Discovery**: Auto-detect and execute MCP tools
- **Bidirectional Communication**: Spring Boot ↔ Node.js MCP servers
- 📖 [Quick Start](docs/quickstarts/MCP_SERVICE_QUICKSTART.md)

### 🧠 RAG (Retrieval Augmented Generation)
- **PostgreSQL + pgvector**: Vector similarity search
- **Full-Text Search**: Multi-language support (EN, DE, RU)
- **Document Upload**: PDF, TXT, FB2 with chunking
- **Hybrid Search**: Combines semantic + keyword search
- 📖 [Integration Guide](docs/architecture/RAG_MCP_INTEGRATION.md)

### 🤖 AI Provider Integration
- **OpenRouter**: 100+ LLM models (GPT-4, Claude, Gemini, etc.)
- **Perplexity**: Real-time web search with Sonar models
- **Multi-Turn Conversations**: Context-aware dialogue
- **Tool Calling**: Function execution for complex tasks
- 📖 [OpenRouter Guide](docs/quickstarts/OPENROUTER_QUICKSTART.md) | [Perplexity Guide](docs/quickstarts/PERPLEXITY_MCP_QUICKSTART.md)

### 🎯 Specialized Agents
- **Meta-Prompt Agent**: Universal AI that adapts to ANY goal
- **Nutritionist Agent**: Family meal planning with KBJU calculations
- **Auto-Schema Generation**: Dynamic JSON structure from goals
- 📖 [Meta-Prompting](docs/features/META_PROMPTING_FEATURE.md) | [Nutritionist](docs/features/NUTRITIONIST_AGENT_FEATURE.md)

### 💬 Conversation Management
- **PostgreSQL Storage**: Long-term memory persistence
- **Summary Reuse**: Token-efficient context management
- **Multi-User Support**: Isolated conversation threads
- **Reminder Scheduler**: Calendar integration for reminders
- 📖 [History Implementation](docs/architecture/CHATBOT_HISTORY_IMPLEMENTATION.md)

### 🎛️ Advanced Controls
- **Temperature Slider**: 0.0-2.0 creativity control
- **System Prompts**: Custom AI behavior
- **JSON Mode**: Structured output with schema validation
- **Model Pricing**: Real-time cost tracking
- 📖 [Temperature Control](docs/features/TEMPERATURE_FEATURE.md)

### 🔍 Search & Indexing
- **FB2 Indexing**: Russian literature format support
- **Relevance Filtering**: LLM-powered result reranking
- **Keyword Search**: PostgreSQL FTS with morphology
- 📖 [Search Guide](docs/features/FULL_TEXT_SEARCH_GUIDE.md)

## 🎨 Design Guidelines

📖 **[Design Guidelines](DESIGN_GUIDELINES.md)** - Comprehensive coding standards and design patterns  
⚡ **[Quick Reference](DESIGN_QUICK_REFERENCE.md)** - Fast lookup for colors, spacing, and conventions

- **Backend:** Java/Spring Boot best practices with Lombok
- **Frontend:** Vue 3 Composition API + TypeScript patterns
- **UI/UX:** SCSS variables, mixins, and component guidelines
- **Naming:** Consistent conventions across the stack

## 🏗️ Architecture

### Backend Services
```
backend/
├── openrouter-service/     # Main API & OpenRouter integration (Port 8080)
├── perplexity-service/     # Perplexity API wrapper (Port 8081)
├── mcp-server/             # MCP Multi-Provider Server (Port 8083)
├── google-service/         # Google Calendar integration (Port 8084)
├── mcp-docker-monitor/     # Docker container monitoring
└── rag-mcp-server/         # RAG with pgvector (MCP server)
```

### Frontend
```
frontend/                   # Vue 3 + TypeScript (Port 5173)
├── src/components/
│   ├── ChatInterface.vue        # Main chat UI
│   ├── MetaPromptChat.vue       # Meta-prompting mode
│   ├── OpenRouterToolsSidebar.vue
│   ├── ReminderDashboard.vue
│   └── RAG*.vue                 # RAG upload/search/library
```

### MCP Servers
```
mcp-servers/
└── perplexity-mcp-server/  # Node.js MCP server for Perplexity
```

## 🚀 Quick Start

### Prerequisites
- **Java 21** (OpenJDK or Oracle JDK)
- **Node.js 18+** (for frontend & MCP servers)
- **PostgreSQL 15+** with pgvector extension
- **Maven 3.8+**
- **Docker** (optional, for containerized setup)

### 1. Database Setup

```bash
# Install PostgreSQL with pgvector
./setup-postgres-memory.sh

# Or use Docker
docker run -d \
  --name ai-postgres \
  -e POSTGRES_USER=ai_user \
  -e POSTGRES_PASSWORD=ai_password \
  -e POSTGRES_DB=ai_memory \
  -p 5432:5432 \
  ankane/pgvector
```

### 2. Configure Environment Variables

Create `.env` in project root:

```bash
# AI Providers
OPENROUTER_API_KEY=sk-or-v1-your-key-here
PERPLEXITY_API_KEY=pplx-your-key-here

# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ai_memory
SPRING_DATASOURCE_USERNAME=ai_user
SPRING_DATASOURCE_PASSWORD=ai_password

# Google Calendar (optional)
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret
```

### 3. Start Backend Services

```bash
# Option A: Start all services
./start-backend.sh

# Option B: Start individual services
cd backend/openrouter-service
./mvnw spring-boot:run
```

**Available Ports:**
- OpenRouter Service: http://localhost:8080
- Perplexity Service: http://localhost:8081
- MCP Service: http://localhost:8083
- Google Service: http://localhost:8084

### 4. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend: **http://localhost:5173**

### 5. Index Project Documentation (Optional)

Automatically index all project documentation into RAG:

```bash
# Start RAG service first (port 8086)
cd backend/rag-mcp-server
./mvnw spring-boot:run &

# Index all documentation
./index-project-docs.sh

# Or dry-run to see what will be indexed
./index-project-docs.sh --dry-run

# Force reindex (delete existing docs first)
./index-project-docs.sh --force
```

### 6. Verify Installation

```bash
# Test OpenRouter
curl http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello!", "userId": "test", "provider": "openrouter"}'

# Test MCP Tools
curl http://localhost:8083/mcp/status

# Test RAG Search (after indexing docs)
curl http://localhost:8086/api/search \
  -H "Content-Type: application/json" \
  -d '{"query": "How to use OpenRouter?", "limit": 5}'
```

## 📡 API Overview

### Main Chat API (OpenRouter Service)

**POST** `/api/chat` - Send message to AI

```json
{
  "message": "Your question",
  "userId": "user-123",
  "conversationId": "conv-456",
  "provider": "openrouter",  // or "perplexity"
  "temperature": 0.7,
  "jsonMode": false,
  "jsonSchema": "auto",      // or "nutritionist_mode", "meta_prompt"
  "systemPrompt": "Custom instructions..."
}
```

### MCP Service

- `GET /mcp/status` - Service status & tool count
- `GET /mcp/providers` - List all MCP providers
- `GET /mcp/tools` - List all available tools
- `POST /mcp/execute` - Execute MCP tool

### RAG Service

- `POST /rag/upload` - Upload document for indexing
- `POST /rag/search` - Semantic + keyword search
- `GET /rag/documents` - List indexed documents
- `DELETE /rag/documents/{id}` - Delete document

### Perplexity Service

- `POST /perplexity/ask` - Ask Perplexity AI
- `POST /perplexity/search` - Web search
- `GET /perplexity/tools` - Available MCP tools

## 🧪 Testing

```bash
# Run all tests
./test-all.sh

# Individual feature tests
./test-openrouter.sh
./test-perplexity-google-integration.sh
./test-meta-prompt.sh
./test-nutritionist.sh
./test-temperature.sh
./test-summary-reuse.sh
```

## 📚 Documentation

### Quick Start Guides
- [OpenRouter Setup](docs/quickstarts/OPENROUTER_QUICKSTART.md)
- [Perplexity MCP Setup](docs/quickstarts/PERPLEXITY_MCP_QUICKSTART.md)
- [MCP Service Setup](docs/quickstarts/MCP_SERVICE_QUICKSTART.md)
- [Temperature Control](docs/quickstarts/TEMPERATURE_QUICKSTART.md)
- [Nutritionist Agent](docs/quickstarts/NUTRITIONIST_QUICKSTART.md)

### Architecture & Features
- [MCP Multi-Provider Architecture](docs/architecture/MCP_MULTI_PROVIDER_ARCHITECTURE.md)
- [RAG Integration](docs/architecture/RAG_MCP_INTEGRATION.md)
- [Conversation History](docs/architecture/CHATBOT_HISTORY_IMPLEMENTATION.md)
- [PostgreSQL Memory Setup](docs/setup/POSTGRESQL_MEMORY_SETUP.md)

### Features Reference
See [FEATURES_INDEX.md](FEATURES_INDEX.md) for complete feature list with links.

## 🔧 Development

### Project Structure

```
ai-advent-challenge/
├── backend/           # Spring Boot microservices
├── frontend/          # Vue 3 application
├── mcp-servers/       # Node.js MCP servers
├── infra/             # Infrastructure configs
├── docs/              # Organized documentation
└── *.sh               # Utility scripts
    ├── index-project-docs.sh    # Index all docs into RAG
    ├── start-backend.sh         # Start all backend services
    ├── test-*.sh                # Feature testing scripts
    └── ...
```

### Build Commands

```bash
# Backend (all services)
mvn clean install -DskipTests

# Frontend
cd frontend
npm run build

# MCP Server
cd mcp-servers/perplexity-mcp-server
npm install
npm run build
```

### Database Migrations

```bash
# Initialize schema
psql -U ai_user -d ai_memory -f infra/sql/schema.sql

# Enable pgvector
psql -U ai_user -d ai_memory -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

## 🐳 Docker Support (Coming Soon)

```bash
docker-compose up -d
```

## 🤝 Tech Stack

**Backend:**
- Spring Boot 3.4.0
- Java 21
- PostgreSQL 15 + pgvector
- WebFlux (reactive HTTP)
- JPA/Hibernate

**Frontend:**
- Vue 3.5
- TypeScript 5.9
- Vite 7.2
- Sass

**AI/ML:**
- OpenRouter API (100+ models)
- Perplexity Sonar API
- Model Context Protocol (MCP)
- Custom embedding models

## 📝 Environment Variables Reference

| Variable | Description | Default |
|----------|-------------|---------|
| `OPENROUTER_API_KEY` | OpenRouter API key | Required |
| `PERPLEXITY_API_KEY` | Perplexity API key | Required |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/ai_memory` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `ai_user` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | Required |
| `GOOGLE_CLIENT_ID` | Google Calendar OAuth | Optional |
| `GOOGLE_CLIENT_SECRET` | Google Calendar OAuth | Optional |

## 🛡️ License

This project is for educational purposes.

## 🙏 Acknowledgments

- [OpenRouter](https://openrouter.ai/) - Multi-model AI API
- [Perplexity AI](https://www.perplexity.ai/) - Real-time search
- [Anthropic MCP](https://modelcontextprotocol.io/) - Tool protocol
- [pgvector](https://github.com/pgvector/pgvector) - Vector similarity

---

**Last Updated:** 2026-01-12


