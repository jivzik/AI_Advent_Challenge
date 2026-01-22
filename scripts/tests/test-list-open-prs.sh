#!/bin/bash

# Test script for the list_open_prs tool
#
# Usage:
#   ./test-list-open-prs.sh [repository]
#
# Example:
#   ./test-list-open-prs.sh octocat/Hello-World

set -e

echo "🧪 Testing list_open_prs Tool"
echo "=============================="
echo ""

# Server URL
SERVER_URL="${MCP_SERVER_URL:-http://localhost:8081}"
REPOSITORY="${1:-}"

# Check if server is running
echo "📡 Checking if MCP server is running at $SERVER_URL..."
if ! curl -s -f "$SERVER_URL/actuator/health" > /dev/null 2>&1; then
    echo "❌ MCP server is not running at $SERVER_URL"
    echo "💡 Start the server with: cd backend/mcp-server && mvn spring-boot:run"
    exit 1
fi
echo "✅ Server is running"
echo ""

# Test 1: List tools to verify list_open_prs is registered
echo "📋 Test 1: Checking if list_open_prs tool is registered..."
TOOLS_RESPONSE=$(curl -s "$SERVER_URL/api/tools/list")
if echo "$TOOLS_RESPONSE" | jq -e '.[] | select(.name == "list_open_prs")' > /dev/null 2>&1; then
    echo "✅ list_open_prs tool is registered"
    echo ""
    echo "Tool definition:"
    echo "$TOOLS_RESPONSE" | jq '.[] | select(.name == "list_open_prs")'
else
    echo "❌ list_open_prs tool is NOT registered"
    echo "Response: $TOOLS_RESPONSE"
    exit 1
fi
echo ""

# Test 2: Execute list_open_prs tool
if [ -z "$REPOSITORY" ]; then
    echo "⚠️  No repository specified. Skipping execution test."
    echo "💡 To test with a specific repository, run:"
    echo "   ./test-list-open-prs.sh owner/repo"
    echo ""
    echo "⚠️  Or set GITHUB_REPOSITORY environment variable:"
    echo "   export GITHUB_REPOSITORY=owner/repo"
    echo "   export GITHUB_TOKEN=ghp_your_token"
    echo "   ./test-list-open-prs.sh"
else
    echo "🔧 Test 2: Executing list_open_prs for repository: $REPOSITORY"

    REQUEST_BODY=$(cat <<EOF
{
  "name": "list_open_prs",
  "arguments": {
    "repository": "$REPOSITORY",
    "state": "open",
    "limit": 5
  }
}
EOF
)

    echo "Request:"
    echo "$REQUEST_BODY" | jq .
    echo ""

    RESPONSE=$(curl -s -X POST "$SERVER_URL/api/tools/execute" \
        -H "Content-Type: application/json" \
        -d "$REQUEST_BODY")

    echo "Response:"
    echo "$RESPONSE" | jq .
    echo ""

    # Check if response contains array of PRs
    if echo "$RESPONSE" | jq -e 'type == "array"' > /dev/null 2>&1; then
        PR_COUNT=$(echo "$RESPONSE" | jq 'length')
        echo "✅ Successfully retrieved $PR_COUNT PR(s)"

        if [ "$PR_COUNT" -gt 0 ]; then
            echo ""
            echo "First PR:"
            echo "$RESPONSE" | jq '.[0]'
        fi
    else
        echo "⚠️  Response format unexpected or error occurred"
    fi
fi

echo ""
echo "✅ Tests completed!"

