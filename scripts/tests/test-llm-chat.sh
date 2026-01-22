#!/bin/bash

# Test script for llm-chat-service
# Tests the REST endpoints of the local LLM chat service

set -e

SERVICE_URL="http://localhost:8090"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "========================================="
echo "Testing LLM Chat Service"
echo "========================================="
echo ""

# Function to test endpoint
test_endpoint() {
    local name=$1
    local method=$2
    local endpoint=$3
    local data=$4
    local expected_code=${5:-200}

    echo -n "Testing $name... "

    if [ -z "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$SERVICE_URL$endpoint")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$SERVICE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data")
    fi

    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    if [ "$http_code" -eq "$expected_code" ]; then
        echo -e "${GREEN}PASS${NC} (HTTP $http_code)"
        if [ ! -z "$body" ]; then
            echo "$body" | jq '.' 2>/dev/null || echo "$body"
        fi
    else
        echo -e "${RED}FAIL${NC} (Expected HTTP $expected_code, got $http_code)"
        echo "$body"
        return 1
    fi
    echo ""
}

# Check if service is running
echo "Checking if service is running..."
if ! curl -s "$SERVICE_URL/api/status" > /dev/null 2>&1; then
    echo -e "${RED}ERROR: Service is not running at $SERVICE_URL${NC}"
    echo "Please start the service with: cd backend/llm-chat-service && ./start.sh"
    exit 1
fi
echo -e "${GREEN}Service is running${NC}"
echo ""

# Test 1: Status endpoint
test_endpoint "Status endpoint" "GET" "/api/status"

# Test 2: Models endpoint
test_endpoint "Models endpoint" "GET" "/api/models"

# Test 3: Chat endpoint with minimal request
echo -e "${YELLOW}Note: The following tests require Ollama to be running${NC}"
test_endpoint "Chat with minimal request" "POST" "/api/chat" \
    '{"message": "Say hello in one word"}' 200

# Test 4: Chat with model override
test_endpoint "Chat with model override" "POST" "/api/chat" \
    '{"message": "What is 2+2? Answer in one word.", "model": "llama2", "temperature": 0.1}' 200

# Test 5: Chat with system prompt
test_endpoint "Chat with system prompt" "POST" "/api/chat" \
    '{"message": "Respond with: ACK", "systemPrompt": "You are a concise assistant that responds exactly as instructed"}' 200

# Test 6: Chat with temperature control
test_endpoint "Chat with high temperature" "POST" "/api/chat" \
    '{"message": "Say hi", "temperature": 1.5}' 200

# Test 7: Empty message (should fail)
test_endpoint "Empty message validation" "POST" "/api/chat" \
    '{"message": ""}' 400

# Test 8: Invalid JSON (should fail)
echo -n "Testing invalid JSON... "
response=$(curl -s -w "\n%{http_code}" -X POST "$SERVICE_URL/api/chat" \
    -H "Content-Type: application/json" \
    -d '{invalid json}')
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" -eq 400 ]; then
    echo -e "${GREEN}PASS${NC} (HTTP $http_code)"
else
    echo -e "${RED}FAIL${NC} (Expected HTTP 400, got $http_code)"
fi
echo ""

echo "========================================="
echo -e "${GREEN}All tests completed${NC}"
echo "========================================="

# Summary
echo ""
echo "Service endpoints:"
echo "  - POST /api/chat         - Send chat message"
echo "  - GET  /api/status       - Service health check"
echo "  - GET  /api/models       - Model information"
echo ""
echo "Example usage:"
echo '  curl -X POST http://localhost:8090/api/chat \'
echo '    -H "Content-Type: application/json" \'
echo '    -d '"'"'{"message": "Hello, LLM!"}'"'"
echo ""
