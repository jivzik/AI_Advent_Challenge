# Scripts Directory

This directory contains all shell scripts for the AI Advent Challenge project, organized by category.

## 📁 Directory Structure

```
scripts/
├── tests/           # Integration and feature tests
├── backend/         # Backend service scripts
├── mcp/             # MCP server scripts
└── utils/           # Utility scripts
```

## 🧪 Test Scripts (`scripts/tests/`)

Integration tests for various features and services. All test scripts can be run from the project root.

| Script | Description |
|--------|-------------|
| `test-llm-chat.sh` | Test llm-chat-service endpoints |
| `test-ollama-chat.sh` | Test Ollama integration |
| `test-dev-assistant.sh` | Test developer assistant features |
| `test-doc-indexing.sh` | Test RAG document indexing |
| `test-rag-upload-metadata.sh` | Test RAG metadata upload |
| `test-git-tools.sh` | Test Git tools integration |
| `test-github-issues.sh` | Test GitHub issues integration |
| `test-get-pr-info.sh` | Test GitHub PR info retrieval |
| `test-list-open-prs.sh` | Test listing open pull requests |

### Usage

```bash
# Run from project root
./scripts/tests/test-llm-chat.sh
```

## 🔧 Backend Service Scripts (`scripts/backend/`)

Scripts for managing backend services. These scripts automatically navigate to the correct service directory.

### Agent Service

| Script | Description |
|--------|-------------|
| `agent-service/start-cli.sh` | Launch the AI DevOps Agent CLI |

**Usage:**
```bash
# Run from anywhere
./scripts/backend/agent-service/start-cli.sh

# Skip build (faster restart)
./scripts/backend/agent-service/start-cli.sh --skip-build
```

### LLM Chat Service

| Script | Description |
|--------|-------------|
| `llm-chat-service/start.sh` | Start the LLM Chat Service |
| `llm-chat-service/examples.sh` | Example curl commands for testing |
| `llm-chat-service/test-optimization.sh` | Test LLM optimization configurations |

**Usage:**
```bash
# Start service
./scripts/backend/llm-chat-service/start.sh

# View example requests
./scripts/backend/llm-chat-service/examples.sh

# Test optimization
./scripts/backend/llm-chat-service/test-optimization.sh
```

### Perplexity Service

| Script | Description |
|--------|-------------|
| `perplexity-service/install-perplexity-mcp.sh` | Install Perplexity MCP server |

**Usage:**
```bash
./scripts/backend/perplexity-service/install-perplexity-mcp.sh
```

## 🔌 MCP Server Scripts (`scripts/mcp/`)

Scripts for MCP (Model Context Protocol) servers.

### Perplexity MCP Server

| Script | Description |
|--------|-------------|
| `perplexity-mcp-server/query-tools.sh` | Query available MCP tools |

**Usage:**
```bash
# Simple output
./scripts/mcp/perplexity-mcp-server/query-tools.sh

# JSON export
./scripts/mcp/perplexity-mcp-server/query-tools.sh --json

# Detailed view
./scripts/mcp/perplexity-mcp-server/query-tools.sh --details
```

## 🛠️ Utility Scripts (`scripts/utils/`)

General-purpose utility scripts for project maintenance.

| Script | Description |
|--------|-------------|
| `index-project-docs.sh` | Index project documentation into RAG system |
| `start-local-llm-demo.sh` | Start local LLM demo environment |
| `stop-local-llm-demo.sh` | Stop local LLM demo environment |

### Usage

```bash
# Index documentation
./scripts/utils/index-project-docs.sh

# Dry run (see what would be indexed)
./scripts/utils/index-project-docs.sh --dry-run

# Force reindex
./scripts/utils/index-project-docs.sh --force

# Start local LLM demo
./scripts/utils/start-local-llm-demo.sh

# Stop local LLM demo
./scripts/utils/stop-local-llm-demo.sh
```

## 💡 Tips

- All scripts can be run from anywhere in the project (they auto-navigate to correct directories)
- Scripts use absolute paths internally, so they're safe to run from any location
- Make scripts executable: `chmod +x scripts/**/*.sh`
- Check script output for detailed information and error messages

## 🔍 Script Conventions

All scripts in this directory follow these conventions:

1. **Shebang**: Use `#!/bin/bash`
2. **Error handling**: Set `set -e` to exit on errors
3. **Path resolution**: Use `SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"`
4. **Colors**: Use ANSI color codes for better readability
5. **Documentation**: Include header comments explaining purpose and usage

## 📝 Adding New Scripts

When adding new scripts:

1. Place in appropriate category directory
2. Make executable: `chmod +x scripts/category/script.sh`
3. Add path resolution for portability
4. Update this README with description and usage
5. Follow existing naming conventions
