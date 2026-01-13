# Features Index

> Central hub for all features and documentation in the AI Advent Challenge project

## 🚀 Quick Start Guides

| Feature | Description | Documentation |
|---------|-------------|---------------|
| **OpenRouter Integration** | Multi-model AI provider (100+ models) | [Quick Start](docs/quickstarts/OPENROUTER_QUICKSTART.md) |
| **Perplexity MCP** | Real-time web search integration | [Quick Start](docs/quickstarts/PERPLEXITY_MCP_QUICKSTART.md) |
| **MCP Service** | Multi-provider Model Context Protocol | [Quick Start](docs/quickstarts/MCP_SERVICE_QUICKSTART.md) |
| **Temperature Control** | AI creativity adjustment (0.0-2.0) | [Quick Start](docs/quickstarts/TEMPERATURE_QUICKSTART.md) |
| **Nutritionist Agent** | Family meal planning with KBJU | [Quick Start](docs/quickstarts/NUTRITIONIST_QUICKSTART.md) |
| **Meta-Prompting** | Universal AI that adapts to any goal | [Quick Start](docs/quickstarts/META_PROMPTING_QUICKSTART.md) |

## 🎯 Core Features

### AI Provider Integration

| Feature | Status | Description | Documentation |
|---------|--------|-------------|---------------|
| **OpenRouter Multi-Provider** | ✅ Active | Access to GPT-4, Claude, Gemini, and 100+ models | [Feature Guide](docs/features/OPENROUTER_PROVIDER_FEATURE.md) |
| **Perplexity Integration** | ✅ Active | Real-time web search with Sonar models | [Quick Start](docs/quickstarts/PERPLEXITY_MCP_QUICKSTART.md) |
| **Tool Calling** | ✅ Active | Function execution for complex tasks | [Quick Start](docs/quickstarts/TOOL_CALLING_QUICKSTART.md) |
| **Model Pricing** | ✅ Active | Real-time cost tracking per model | [Feature Guide](docs/features/MODEL_PRICING_FEATURE.md) |

### Model Context Protocol (MCP)

| Feature | Status | Description | Documentation |
|---------|--------|-------------|---------------|
| **MCP Multi-Provider** | ✅ Active | Native + Perplexity MCP servers | [Architecture](docs/architecture/MCP_MULTI_PROVIDER_ARCHITECTURE.md) |
| **MCP Service** | ✅ Active | Standalone MCP server (Port 8083) | [Quick Start](docs/quickstarts/MCP_SERVICE_QUICKSTART.md) |
| **Perplexity MCP Server** | ✅ Active | Node.js MCP server for Perplexity | [Integration Guide](docs/architecture/PERPLEXITY_MCP_INTEGRATION_GUIDE.md) |
| **Docker Monitoring** | ✅ Active | MCP tools for Docker container monitoring | [README](backend/mcp-docker-monitor/README.md) |

### RAG (Retrieval Augmented Generation)

| Feature | Status | Description | Documentation |
|---------|--------|-------------|---------------|
| **pgvector Integration** | ✅ Active | Vector similarity search in PostgreSQL | [Integration Guide](docs/architecture/RAG_MCP_INTEGRATION.md) |
| **Full-Text Search** | ✅ Active | Multi-language keyword search (EN/DE/RU) | [Guide](docs/features/FULL_TEXT_SEARCH_GUIDE.md) |
| **FB2 Indexing** | ✅ Active | Russian literature format support | [Indexing Guide](docs/features/FB2_INDEXING_GUIDE.md) |
| **Document Upload** | ✅ Active | PDF, TXT, FB2 with chunking | [Quick Start](docs/quickstarts/FULL_TEXT_SEARCH_QUICKSTART.md) |
| **Relevance Filtering** | ✅ Active | LLM-powered result reranking | [Guide](docs/features/RELEVANCE_FILTERING_GUIDE.md) |
| **Reranking** | ✅ Active | Intelligent result ordering | [Integration Guide](docs/architecture/LLM_RERANKING_INTEGRATION.md) |
| **Documentation Indexing** | ✅ Active | Automatic project docs indexing | [Setup Guide](docs/setup/DOCUMENTATION_INDEXING_GUIDE.md) |

### Conversational Agents

| Feature | Status | Description | Documentation |
|---------|--------|-------------|---------------|
| **Meta-Prompting** | ✅ Active | Universal AI that adapts to any goal | [Feature Guide](docs/features/META_PROMPTING_FEATURE.md) |
| **Nutritionist Agent** | ✅ Active | Family meal planning with KBJU calculations | [Feature Guide](docs/features/NUTRITIONIST_AGENT_FEATURE.md) |
| **Auto-Schema Generation** | ✅ Active | Dynamic JSON structure generation | [Feature Guide](docs/features/AUTO_SCHEMA_FEATURE.md) |
| **JSON Mode** | ✅ Active | Structured output with schema validation | [Feature Guide](docs/features/JSON_MODE_FEATURE.md) |

### Conversation Management

| Feature | Status | Description | Documentation |
|---------|--------|-------------|---------------|
| **PostgreSQL Memory** | ✅ Active | Long-term conversation storage | [Setup Guide](docs/setup/POSTGRESQL_MEMORY_SETUP.md) |
| **Conversation History** | ✅ Active | Multi-turn conversation tracking | [Implementation](docs/architecture/CONVERSATION_HISTORY_IMPLEMENTATION.md) |
| **Summary Reuse** | ✅ Active | Token-efficient context management | [Feature Guide](docs/features/SUMMARY_REUSE_FEATURE.md) |
| **Reminder Scheduler** | ✅ Active | Calendar integration for reminders | [Feature Guide](docs/features/REMINDER_SCHEDULER_FEATURE.md) |

### UI/UX Features

| Feature | Status | Description | Documentation |
|---------|--------|-------------|---------------|
| **Temperature Control** | ✅ Active | Creativity slider (0.0-2.0) | [Feature Guide](docs/features/TEMPERATURE_FEATURE.md) |
| **System Prompts** | ✅ Active | Custom AI behavior instructions | [Feature Guide](docs/features/SYSTEM_PROMPT_FEATURE.md) |
| **JSON Beautification** | ✅ Active | Pretty-print JSON responses | [Feature Guide](docs/features/JSON_BEAUTIFICATION_FEATURE.md) |
| **Metrics Display** | ✅ Active | Visual token/cost tracking | [Quick Start](docs/quickstarts/METRICS_DISPLAY_QUICKSTART.md) |

## 🏗️ Architecture Documentation

| Topic | Description | Documentation |
|-------|-------------|---------------|
| **OpenRouter Service** | Complete architecture & functionality guide | [Architecture](docs/architecture/OPENROUTER_SERVICE_ARCHITECTURE.md) ⭐ |
| **MCP Multi-Provider** | How multiple MCP servers work together | [Architecture](docs/architecture/MCP_MULTI_PROVIDER_ARCHITECTURE.md) |
| **RAG Integration** | Vector + keyword search architecture | [Integration](docs/architecture/RAG_MCP_INTEGRATION.md) |
| **Chatbot History** | Message persistence design | [Implementation](docs/architecture/CHATBOT_HISTORY_IMPLEMENTATION.md) |
| **LLM Comparison** | GPT-4, Mistral, Gemma performance | [Comparison](docs/architecture/LLM-COMPARISON-GPT5-MISTRAL-GEMMA.md) |
| **LLM Reranking** | AI-powered result ordering | [Integration](docs/architecture/LLM_RERANKING_INTEGRATION.md) |

## 🛠️ Setup & Deployment

| Topic | Description | Documentation |
|-------|-------------|---------------|
| **PostgreSQL Setup** | Database initialization with pgvector | [Setup Guide](docs/setup/POSTGRESQL_MEMORY_SETUP.md) |
| **Documentation Indexing** | Automatic project docs indexing | [Setup Guide](docs/setup/DOCUMENTATION_INDEXING_GUIDE.md) |
| **Chatbot Deployment** | Production deployment checklist | [Deployment Guide](docs/setup/CHATBOT_DEPLOYMENT_GUIDE.md) |
| **PostgreSQL Implementation** | Memory system implementation details | [Implementation](docs/setup/POSTGRESQL_MEMORY_IMPLEMENTATION.md) |

## 🧪 Testing & Development

| Topic | Description | Files |
|-------|-------------|-------|
| **Test Scripts** | Automated feature testing | `test-*.sh` (16+ scripts) |
| **Start Scripts** | Quick service startup | `start-backend.sh`, `start-frontend.sh` |
| **Index Scripts** | Documentation indexing | `index-project-docs.sh` |

## 🔍 Advanced Search Features

Documentation for advanced search and filtering capabilities has been integrated into the main feature guides above.

## 🚀 Getting Started

1. **First Time Setup**: Start with [README.md](README.md)
2. **Database Setup**: Follow [docs/setup/POSTGRESQL_MEMORY_SETUP.md](docs/setup/POSTGRESQL_MEMORY_SETUP.md)
3. **Quick Tests**: Run `./test-all.sh` to verify all features
4. **Feature Exploration**: Use this index to find specific feature documentation

## 📂 Documentation Organization

```
docs/
├── quickstarts/          # Step-by-step getting started guides (15 files)
├── features/             # Feature descriptions and usage (16 files)
├── architecture/         # System design and architecture (8 files)
├── setup/                # Installation and configuration (3 files)
└── development/          # Development guides and testing
```

**Additional Documentation:**
- `backend/*/README.md` - Service-specific documentation
- `mcp-servers/*/README.md` - MCP server documentation
- Test scripts: `test-*.sh` in project root

---

**Last Updated:** 2026-01-12

**Total Features:** 40+ active features across 6 backend services

**Documentation Files:** 42 organized documentation files (down from 165+)

