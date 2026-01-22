#!/bin/bash

#
# Perplexity MCP Server - Tools Query Script
#
# Dieses Script zeigt alle verfügbaren Tools vom Perplexity MCP Server
# Es kann auf verschiedene Arten verwendet werden:
#
# 1. Direkt: ./query-tools.sh
# 2. Mit JSON-Export: ./query-tools.sh --json
# 3. Mit Details: ./query-tools.sh --details
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
MCP_SERVER_DIR="$PROJECT_ROOT/mcp-servers/perplexity-mcp-server"
MODE="${1:-simple}"

echo "🔍 Querying Perplexity MCP Server Tools..."
echo ""

case $MODE in
  --json)
    echo "📋 Exporting as JSON..."
    if command -v node &> /dev/null; then
      node "$MCP_SERVER_DIR/export-tools.js"
    else
      echo "❌ Node.js is required for JSON export"
      exit 1
    fi
    ;;

  --details)
    echo "📊 Detailed Tool Information"
    echo "======================================"
    if command -v node &> /dev/null; then
      node "$SCRIPT_DIR/list-tools.js"
    else
      echo "❌ Node.js is required"
      exit 1
    fi
    ;;

  --help)
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  (none)      - Simple list of tools"
    echo "  --json      - Export tools as JSON"
    echo "  --details   - Show detailed information"
    echo "  --help      - Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                    # Simple list"
    echo "  $0 --json > tools.json  # Export JSON"
    echo "  $0 --details          # Detailed view"
    ;;

  *)
    # Simple list mode (default)
    echo "Available Tools in Perplexity MCP Server:"
    echo "=========================================="
    echo ""
    echo "1. perplexity_ask"
    echo "   • Description: Ask a question to Perplexity Sonar AI"
    echo "   • Parameters:"
    echo "     - prompt (required): The question to ask"
    echo "     - model (optional): AI model to use (default: sonar)"
    echo "     - temperature (optional): Response creativity (0.0-1.0, default: 0.7)"
    echo "     - max_tokens (optional): Max response length (default: 1000)"
    echo ""
    echo "2. perplexity_search"
    echo "   • Description: Search for information with internet access"
    echo "   • Parameters:"
    echo "     - query (required): What to search for"
    echo ""
    echo "=========================================="
    echo ""
    echo "📝 Note: For more detailed information, use --details flag"
    echo "💾 To export as JSON, use --json flag"
    ;;
esac

echo ""
echo "✅ Done"

