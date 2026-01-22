#!/bin/bash

# Test-Script für Developer Assistant API
# Testet alle Endpoints und Funktionalitäten

BASE_URL="http://localhost:8084"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "========================================"
echo "Developer Assistant API - Test Suite"
echo "========================================"
echo ""

# Test 1: Health Check
echo -e "${YELLOW}Test 1: Health Check${NC}"
response=$(curl -s -w "\n%{http_code}" ${BASE_URL}/api/dev/help/health)
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✓ Health Check successful${NC}"
    echo "  Response: $body"
else
    echo -e "${RED}✗ Health Check failed (HTTP $http_code)${NC}"
fi
echo ""

# Test 2: Service Info
echo -e "${YELLOW}Test 2: Service Info${NC}"
response=$(curl -s -w "\n%{http_code}" ${BASE_URL}/api/dev/help/info)
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✓ Service Info successful${NC}"
    echo "  Response: $(echo $body | jq -r '.serviceName')"
    echo "  Version: $(echo $body | jq -r '.version')"
    echo "  Features: $(echo $body | jq -r '.features | length') available"
else
    echo -e "${RED}✗ Service Info failed (HTTP $http_code)${NC}"
fi
echo ""

# Test 3: Developer Query (Minimal)
echo -e "${YELLOW}Test 3: Developer Query - Minimal Request${NC}"
request_body='{
  "query": "Wie erstelle ich einen MCP Provider?",
  "userId": "test-user-123"
}'

response=$(curl -s -w "\n%{http_code}" \
  -X POST ${BASE_URL}/api/dev/help \
  -H "Content-Type: application/json" \
  -d "$request_body")

http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✓ Developer Query successful${NC}"

    # Parse Response
    answer_length=$(echo $body | jq -r '.answer | length')
    model=$(echo $body | jq -r '.model')
    response_time=$(echo $body | jq -r '.responseTimeMs')
    sources_count=$(echo $body | jq -r '.sources | length')

    echo "  Answer Length: $answer_length characters"
    echo "  Model: $model"
    echo "  Response Time: ${response_time}ms"
    echo "  Sources Found: $sources_count"

    # Show first 200 chars of answer
    answer_preview=$(echo $body | jq -r '.answer' | head -c 200)
    echo "  Answer Preview: ${answer_preview}..."

else
    echo -e "${RED}✗ Developer Query failed (HTTP $http_code)${NC}"
    echo "  Response: $body"
fi
echo ""

# Test 4: Developer Query (Full Options)
echo -e "${YELLOW}Test 4: Developer Query - Full Options${NC}"
request_body='{
  "query": "Zeige mir Beispiele für Git Tools Integration",
  "userId": "test-user-456",
  "includeGitContext": true,
  "maxDocuments": 3,
  "model": "anthropic/claude-3.5-sonnet",
  "temperature": 0.5
}'

response=$(curl -s -w "\n%{http_code}" \
  -X POST ${BASE_URL}/api/dev/help \
  -H "Content-Type: application/json" \
  -d "$request_body")

http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✓ Developer Query with full options successful${NC}"

    # Parse Git Context
    has_git_context=$(echo $body | jq -r '.gitContext != null')

    if [ "$has_git_context" = "true" ]; then
        current_branch=$(echo $body | jq -r '.gitContext.currentBranch')
        has_uncommitted=$(echo $body | jq -r '.gitContext.hasUncommittedChanges')
        echo "  Git Context Included: Yes"
        echo "  Current Branch: $current_branch"
        echo "  Uncommitted Changes: $has_uncommitted"
    else
        echo "  Git Context: Not available"
    fi

else
    echo -e "${RED}✗ Developer Query with full options failed (HTTP $http_code)${NC}"
    echo "  Response: $body"
fi
echo ""

# Test 5: Invalid Request (Missing Required Field)
echo -e "${YELLOW}Test 5: Invalid Request Handling${NC}"
request_body='{
  "query": "Test query"
}'

response=$(curl -s -w "\n%{http_code}" \
  -X POST ${BASE_URL}/api/dev/help \
  -H "Content-Type: application/json" \
  -d "$request_body")

http_code=$(echo "$response" | tail -n1)

if [ "$http_code" = "400" ] || [ "$http_code" = "500" ]; then
    echo -e "${GREEN}✓ Invalid request properly rejected (HTTP $http_code)${NC}"
else
    echo -e "${RED}✗ Invalid request handling unexpected (HTTP $http_code)${NC}"
fi
echo ""

# Test 6: Swagger UI Check
echo -e "${YELLOW}Test 6: Swagger UI Availability${NC}"
response=$(curl -s -o /dev/null -w "%{http_code}" ${BASE_URL}/swagger-ui.html)

if [ "$response" = "200" ]; then
    echo -e "${GREEN}✓ Swagger UI is available${NC}"
    echo "  URL: ${BASE_URL}/swagger-ui.html"
else
    echo -e "${RED}✗ Swagger UI not available (HTTP $response)${NC}"
fi
echo ""

# Summary
echo "========================================"
echo "Test Summary"
echo "========================================"
echo ""
echo "All tests completed!"
echo ""
echo "Manual Testing:"
echo "  - Open Swagger UI: ${BASE_URL}/swagger-ui.html"
echo "  - Try different queries in Swagger"
echo "  - Check response times and quality"
echo ""
echo "Next Steps:"
echo "  1. Test with real project documentation"
echo "  2. Verify Git context accuracy"
echo "  3. Test error scenarios"
echo "  4. Load testing with multiple requests"
echo ""

