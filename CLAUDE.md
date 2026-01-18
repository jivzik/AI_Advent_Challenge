# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an enterprise-grade multi-service AI platform integrating OpenRouter and Perplexity LLMs with Model Context Protocol (MCP) servers, Retrieval Augmented Generation (RAG), and specialized AI agents. The codebase uses a Maven-based microservices architecture with a Vue 3 frontend.

## Architecture

### Backend Services (Spring Boot 3.4.0, Java 21)

The backend is organized as a Maven aggregator with multiple specialized services, each running on distinct ports:

- **openrouter-service** (Port 8084): Primary API - OpenRouter LLM integration, chat endpoint, MCP tool orchestration, JSON mode with schema generation
- **perplexity-service** (Port 8080): Web search integration with Perplexity Sonar models
- **mcp-server** (Port 8081): Multi-provider MCP coordination hub for tool management
- **rag-mcp-server** (Port 8086): PostgreSQL + pgvector vector database, semantic + keyword search, document indexing (PDF, TXT, FB2)
- **google-service** (Port 8082): Google Calendar OAuth integration for reminder scheduling
- **mcp-docker-monitor** (Port 8083): Docker container monitoring via MCP
- **agent-service** (Port 8087): Specialized agents (Meta-Prompt, Nutritionist with KBJU calculations)
- **support-service** (Port 8088): User support chatbot
- **team-assistant-service** (Port 8089): Team collaboration tools

### Frontend (Vue 3 + TypeScript)

Vue 3 + TypeScript + Vite application on port 5173. Components include ChatInterface, MetaPromptChat, ReminderDashboard, and RAG document upload/search interfaces.

### MCP Servers (Node.js)

- **perplexity-mcp-server**: Node.js MCP server bridge for Perplexity integration

### Data Layer

- **PostgreSQL 15+** with pgvector extension for vector embeddings
- JPA/Hibernate for ORM across all services
- Spring Data repositories for data access

## Essential Commands

### Backend Development

```bash
# Build all backend services (Maven aggregator)
mvn clean install -DskipTests

# Build single service
cd backend/<service-name>
./mvnw clean install -DskipTests

# Run single service (e.g., openrouter-service)
cd backend/openrouter-service
./mvnw spring-boot:run

# Run all backend services simultaneously
./start-backend.sh

# Run tests for all services
./test-all.sh

# Run specific feature tests
./test-openrouter.sh
./test-perplexity-google-integration.sh
./test-meta-prompt.sh
./test-nutritionist.sh
```

### Frontend Development

```bash
cd frontend
npm install
npm run dev          # Start dev server on port 5173
npm run build        # Production build with type checking
npm run preview      # Preview production build
```

### Database Setup

```bash
# Using Docker with pgvector
docker run -d \
  --name ai-postgres \
  -e POSTGRES_USER=ai_user \
  -e POSTGRES_PASSWORD=ai_password \
  -e POSTGRES_DB=ai_memory \
  -p 5432:5432 \
  pgvector/pgvector:pg16

# Or use docker-compose
cd infra/localdev
docker-compose up -d

# Initialize database schema
psql -U ai_user -d ai_memory -f infra/sql/schema.sql
psql -U ai_user -d ai_memory -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

### RAG and Documentation Indexing

```bash
# Index all project documentation into RAG
./index-project-docs.sh

# Dry-run to see what would be indexed
./index-project-docs.sh --dry-run

# Force reindex (deletes existing docs first)
./index-project-docs.sh --force
```

## Configuration

### Environment Variables

Create `.env` in project root:

```bash
# AI Providers
OPENROUTER_API_KEY=sk-or-v1-your-key-here
PERPLEXITY_API_KEY=pplx-your-key-here

# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ai_memory
SPRING_DATASOURCE_USERNAME=ai_user
SPRING_DATASOURCE_PASSWORD=ai_password

# MCP Services
MCP_GOOGLE_ENABLED=false
MCP_GOOGLE_URL=http://localhost:8082
MCP_PERPLEXITY_URL=http://localhost:3001
MCP_RAG_URL=http://localhost:8086

# Google Calendar (optional)
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret
```

### Service Configuration

Each Spring Boot service has its configuration in `src/main/resources/application.properties`:
- OpenRouter base-url defaults to https://openrouter.ai/api/v1
- MCP services communicate via HTTP (localhost ports)
- Database connections configured via Spring DataSource properties

## Key Architecture Patterns

### MCP Integration

Services use a factory pattern (`MCPFactory`) to create and manage MCP client connections to external MCP servers. The `openrouter-service` coordinates tool calling by:
1. Discovering available tools from MCP providers
2. Executing user-requested tools on appropriate MCP servers
3. Parsing and integrating results back into LLM context

### Conversation Management

PostgreSQL stores long-term conversation history. Services implement summary reuse to reduce token usage by compressing old conversation segments before new turns.

### Tool Calling Strategy

The chat endpoint supports:
- **JSON Mode**: Requests structured JSON output with optional schema validation
- **jsonSchema** parameter: "auto", "nutritionist_mode", or "meta_prompt" for specialized output formats
- **System Prompts**: Custom behavioral instructions for the LLM

### RAG Architecture

Uses hybrid search combining:
- Semantic search via pgvector embeddings
- Full-text search using PostgreSQL's FTS with multi-language morphology support (EN, DE, RU)
- Document chunking for PDF, TXT, and FB2 formats
- LLM-powered relevance filtering for result reranking

## API Entry Points

### Main Chat Endpoint

```
POST /api/chat (openrouter-service:8084)
```

Accepts:
- `message`: User query
- `userId`, `conversationId`: Session tracking
- `provider`: "openrouter" or "perplexity"
- `temperature`: 0.0-2.0 (creativity control)
- `jsonMode`: Boolean for structured output
- `jsonSchema`: Schema selection for output structure
- `systemPrompt`: Custom AI instructions

### MCP Endpoints

- `GET /mcp/status` - Service health and tool count
- `GET /mcp/providers` - List MCP providers
- `GET /mcp/tools` - List all available tools
- `POST /mcp/execute` - Execute tool with parameters

### RAG Endpoints

- `POST /rag/upload` - Index document (RAG service)
- `POST /rag/search` - Hybrid semantic + keyword search
- `GET /rag/documents` - List indexed documents
- `DELETE /rag/documents/{id}` - Remove document

## Project Structure

```
backend/
├── openrouter-service/      # Main API orchestrator
│   ├── src/main/java/.../controller/ChatWithToolsController.java
│   ├── src/main/java/.../mcp/                # MCP integration
│   └── src/main/resources/prompts/           # Specialized prompts
├── rag-mcp-server/          # Vector DB and search
├── mcp-server/              # MCP hub
├── [other services]/
└── pom.xml                  # Aggregator POM

frontend/
├── src/components/          # Vue components
│   ├── ChatInterface.vue
│   ├── MetaPromptChat.vue
│   └── RAG*.vue
├── package.json
└── vite.config.ts

mcp-servers/
└── perplexity-mcp-server/   # Node.js server
```

## Testing Strategy

Feature-specific test scripts in root directory follow the pattern `test-<feature>.sh`, using curl to validate HTTP endpoints with expected response codes and JSON parsing. Tests can be run individually or all at once with `test-all.sh`.

## Common Development Tasks

### Adding a New MCP Provider

1. Create service implementing HTTP endpoint returning MCP tools in standard format
2. Add MCPService subclass in `openrouter-service` extending `BaseMCPService`
3. Register in `MCPFactory`
4. Add service configuration to `.env` and Spring properties
5. Services use `MCPFactory` to discover and cache tools dynamically

### Modifying Chat Processing

Interaction flow: Request → `ChatWithToolsController.postChat()` → OpenRouter API call → Tool detection → MCP execution → Result aggregation → Response. Modify payload structures in `ChatRequest` and `OpenRouterApiRequest` DTOs.

### Indexing New Document Types

RAG service handles PDF, TXT, and FB2. To support new formats:
1. Add document processor in rag-mcp-server
2. Update `DocumentController.upload()` with format detection
3. Implement chunking strategy and embedding generation
4. Register in document indexing pipeline

## Documentation References

- **[MCP Integration](docs/architecture/MCP_MULTI_PROVIDER_ARCHITECTURE.md)** - Tool protocol details
- **[RAG Setup](docs/architecture/RAG_MCP_INTEGRATION.md)** - Vector search configuration
- **[Conversation History](docs/architecture/CHATBOT_HISTORY_IMPLEMENTATION.md)** - Persistence strategy
- **[OpenRouter Guide](docs/quickstarts/OPENROUTER_QUICKSTART.md)** - LLM provider setup
- **[Temperature Control](docs/features/TEMPERATURE_FEATURE.md)** - Creativity slider details
- **[Meta-Prompting](docs/features/META_PROMPTING_FEATURE.md)** - Universal agent behavior
- **[Nutritionist Agent](docs/features/NUTRITIONIST_AGENT_FEATURE.md)** - Specialized meal planning

## Technology Stack Notes

- **Spring Boot 3.4.0** uses Jakarta EE namespaces (not javax.*)
- **WebFlux** used for reactive HTTP but standard servlet stack available
- **SpringDoc OpenAPI** auto-generates Swagger UI at `/swagger-ui.html`
- **Lombok** for boilerplate reduction (`@Data`, `@Slf4j`)
- **Vue 3 with Composition API** (not Options API)
- **pgvector** requires explicit "CREATE EXTENSION" command - not automatic