#!/bin/bash

# Start script for llm-chat-service
# Launches the Spring Boot application on port 8090

set -e

# Navigate to service directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
SERVICE_DIR="$PROJECT_ROOT/backend/llm-chat-service"

cd "$SERVICE_DIR"

echo "========================================="
echo "Starting LLM Chat Service"
echo "Working directory: $SERVICE_DIR"
echo "========================================="

# Check if Ollama is running
if ! curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "WARNING: Ollama does not appear to be running at http://localhost:11434"
    echo "Please start Ollama with: ollama serve"
    echo ""
fi

# Set environment variables if not already set
export OLLAMA_BASE_URL="${OLLAMA_BASE_URL:-http://localhost:11434}"
export OLLAMA_MODEL="${OLLAMA_MODEL:-llama2}"
export OLLAMA_TEMPERATURE="${OLLAMA_TEMPERATURE:-0.7}"
export OLLAMA_MAX_TOKENS="${OLLAMA_MAX_TOKENS:-2000}"
export OLLAMA_TIMEOUT="${OLLAMA_TIMEOUT:-120}"

echo "Configuration:"
echo "  Base URL:     $OLLAMA_BASE_URL"
echo "  Model:        $OLLAMA_MODEL"
echo "  Temperature:  $OLLAMA_TEMPERATURE"
echo "  Max Tokens:   $OLLAMA_MAX_TOKENS"
echo "  Timeout:      ${OLLAMA_TIMEOUT}s"
echo ""
echo "Service will be available at: http://localhost:8090"
echo "========================================="
echo ""

# Run the Spring Boot application
./mvnw spring-boot:run
