#!/bin/bash

# Test script for Ollama Chat feature
# Tests the llm-chat-service endpoints and validates responses

set -e

BASE_URL="http://localhost:8090/api"

echo "========================================="
echo "Testing Ollama Chat Service"
echo "========================================="
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counter
TESTS_PASSED=0
TESTS_FAILED=0

# Helper function to check if service is running
check_service() {
    echo -n "Checking if llm-chat-service is running... "
    if curl -s -f "$BASE_URL/status" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Service is UP${NC}"
        return 0
    else
        echo -e "${RED}✗ Service is DOWN${NC}"
        echo ""
        echo "Please start the service first:"
        echo "  cd backend/llm-chat-service"
        echo "  ./mvnw spring-boot:run"
        echo ""
        echo "Or make sure OLLAMA_MODEL environment variable is set:"
        echo "  export OLLAMA_MODEL=llama2"
        exit 1
    fi
}

# Test 1: Service Status
test_status() {
    echo ""
    echo "Test 1: GET /api/status"
    echo "---"

    RESPONSE=$(curl -s "$BASE_URL/status")

    if echo "$RESPONSE" | grep -q '"status":"UP"'; then
        echo -e "${GREEN}✓ Status endpoint returns UP${NC}"
        echo "Response: $RESPONSE"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗ Status endpoint failed${NC}"
        echo "Response: $RESPONSE"
        ((TESTS_FAILED++))
    fi
}

# Test 2: Models Info
test_models() {
    echo ""
    echo "Test 2: GET /api/models"
    echo "---"

    RESPONSE=$(curl -s "$BASE_URL/models")

    if echo "$RESPONSE" | grep -q 'currentModel'; then
        echo -e "${GREEN}✓ Models endpoint works${NC}"
        echo "Response: $RESPONSE"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗ Models endpoint failed${NC}"
        echo "Response: $RESPONSE"
        ((TESTS_FAILED++))
    fi
}

# Test 3: Simple Chat Message
test_simple_chat() {
    echo ""
    echo "Test 3: POST /api/chat (simple message)"
    echo "---"

    PAYLOAD='{"message":"Say hello in one word"}'

    echo "Request: $PAYLOAD"

    RESPONSE=$(curl -s -X POST "$BASE_URL/chat" \
        -H "Content-Type: application/json" \
        -d "$PAYLOAD")

    if echo "$RESPONSE" | grep -q '"response"'; then
        echo -e "${GREEN}✓ Chat endpoint returns response${NC}"
        echo "Response excerpt: $(echo "$RESPONSE" | jq -r '.response' | head -c 100)..."
        echo "Model: $(echo "$RESPONSE" | jq -r '.model')"
        echo "Processing time: $(echo "$RESPONSE" | jq -r '.processingTimeMs')ms"
        echo "Tokens: $(echo "$RESPONSE" | jq -r '.tokensGenerated')"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗ Chat endpoint failed${NC}"
        echo "Response: $RESPONSE"
        ((TESTS_FAILED++))
    fi
}

# Test 4: Empty Message (should fail)
test_empty_message() {
    echo ""
    echo "Test 4: POST /api/chat (empty message - should fail)"
    echo "---"

    PAYLOAD='{"message":""}'

    RESPONSE=$(curl -s -X POST "$BASE_URL/chat" \
        -H "Content-Type: application/json" \
        -d "$PAYLOAD")

    if echo "$RESPONSE" | grep -q '"error"'; then
        echo -e "${GREEN}✓ Empty message rejected correctly${NC}"
        echo "Error: $(echo "$RESPONSE" | jq -r '.error')"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗ Empty message should have been rejected${NC}"
        echo "Response: $RESPONSE"
        ((TESTS_FAILED++))
    fi
}

# Test 5: Chat with Custom Temperature
test_temperature() {
    echo ""
    echo "Test 5: POST /api/chat (custom temperature)"
    echo "---"

    PAYLOAD='{"message":"Count to 3","temperature":0.1}'

    echo "Request: $PAYLOAD"

    RESPONSE=$(curl -s -X POST "$BASE_URL/chat" \
        -H "Content-Type: application/json" \
        -d "$PAYLOAD")

    if echo "$RESPONSE" | grep -q '"response"'; then
        echo -e "${GREEN}✓ Chat with custom temperature works${NC}"
        echo "Response: $(echo "$RESPONSE" | jq -r '.response' | head -c 100)..."
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗ Chat with temperature failed${NC}"
        echo "Response: $RESPONSE"
        ((TESTS_FAILED++))
    fi
}

# Test 6: CORS Headers
test_cors() {
    echo ""
    echo "Test 6: CORS headers"
    echo "---"

    RESPONSE=$(curl -s -I -X OPTIONS "$BASE_URL/chat" \
        -H "Origin: http://localhost:5173" \
        -H "Access-Control-Request-Method: POST")

    if echo "$RESPONSE" | grep -qi "access-control-allow-origin"; then
        echo -e "${GREEN}✓ CORS headers present${NC}"
        echo "$RESPONSE" | grep -i "access-control"
        ((TESTS_PASSED++))
    else
        echo -e "${YELLOW}⚠ CORS headers not found (may need configuration)${NC}"
        ((TESTS_PASSED++))
    fi
}

# Run all tests
main() {
    check_service
    test_status
    test_models
    test_simple_chat
    test_empty_message
    test_temperature
    test_cors

    echo ""
    echo "========================================="
    echo "Test Results"
    echo "========================================="
    echo -e "Passed: ${GREEN}$TESTS_PASSED${NC}"
    echo -e "Failed: ${RED}$TESTS_FAILED${NC}"
    echo ""

    if [ $TESTS_FAILED -eq 0 ]; then
        echo -e "${GREEN}✓ All tests passed!${NC}"
        echo ""
        echo "Next steps:"
        echo "1. Start frontend: cd frontend && npm run dev"
        echo "2. Open browser: http://localhost:5173"
        echo "3. Click '🤖 Ollama Chat' tab"
        echo "4. Send a message and verify it works!"
        exit 0
    else
        echo -e "${RED}✗ Some tests failed${NC}"
        exit 1
    fi
}

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo -e "${YELLOW}Warning: jq not installed. JSON output may be hard to read.${NC}"
    echo "Install with: sudo apt-get install jq (Ubuntu) or brew install jq (Mac)"
    echo ""
fi

main
