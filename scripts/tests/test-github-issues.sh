#!/bin/bash

# Test-Skript für GitHub Issue Management Tools
# Verwendung: ./test-github-issues.sh

set -e

# Farben für Ausgabe
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

BASE_URL="http://localhost:8082"

echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║     GitHub Issue Management Tools - Test Suite        ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""

# Überprüfe, ob Server läuft
echo -e "${BLUE}📡 Prüfe Server-Verfügbarkeit...${NC}"
if ! curl -s -f "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
    echo -e "${RED}❌ Server ist nicht erreichbar auf ${BASE_URL}${NC}"
    echo -e "${RED}   Bitte starten Sie den MCP Server zuerst:${NC}"
    echo -e "${RED}   cd backend/mcp-server && mvn spring-boot:run${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Server läuft${NC}"
echo ""

# Test 1: Liste alle verfügbaren Tools
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Test 1: Liste aller verfügbaren Tools${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
curl -s "${BASE_URL}/api/tools/list" | jq -r '.[] | select(.name | contains("github_issue") or . == "list_open_prs") | .name' | while read tool; do
    echo -e "${GREEN}  ✓ ${tool}${NC}"
done
echo ""

# Test 2: Liste GitHub Issues (benötigt Repository-Konfiguration)
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Test 2: Liste GitHub Issues${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# Setze hier Ihr GitHub Repository (oder nutzen Sie Umgebungsvariable)
GITHUB_REPO="${GITHUB_REPOSITORY:-octocat/Hello-World}"

echo -e "${BLUE}Repository: ${GITHUB_REPO}${NC}"

curl -s -X POST "${BASE_URL}/api/tools/execute" \
  -H "Content-Type: application/json" \
  -d "{
    \"name\": \"list_github_issues\",
    \"arguments\": {
      \"repository\": \"${GITHUB_REPO}\",
      \"state\": \"open\",
      \"limit\": 5
    }
  }" | jq -r '
    if type == "array" then
      "Gefundene Issues: \(length)",
      (.[] | "  • #\(.number): \(.title) [\(.state)]")
    else
      "Fehler: \(.message // .error // "Unbekannter Fehler")"
    end
  '
echo ""

# Test 3: Erstelle ein Test-Issue (nur wenn Token gesetzt ist)
if [ -n "$GITHUB_TOKEN" ]; then
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}Test 3: Erstelle Test-Issue${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

    echo -e "${GREEN}Hinweis: Diesen Test nur auf eigenem Test-Repository ausführen!${NC}"
    echo -e "${BLUE}Überspringe automatische Issue-Erstellung...${NC}"
    echo -e "${BLUE}Zum Testen verwenden Sie:${NC}"
    echo '
    curl -X POST http://localhost:8081/api/tools/execute \
      -H "Content-Type: application/json" \
      -d "{
        \"name\": \"create_github_issue\",
        \"arguments\": {
          \"repository\": \"YOUR_OWNER/YOUR_REPO\",
          \"title\": \"Test Issue from MCP\",
          \"body\": \"This is a test issue created via MCP Server\",
          \"labels\": [\"test\", \"automated\"]
        }
      }"
    '
    echo ""
else
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}Test 3: Erstelle Issue (übersprungen)${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${RED}⚠️  GITHUB_TOKEN nicht gesetzt. Issue-Erstellung übersprungen.${NC}"
    echo -e "${BLUE}Setzen Sie GITHUB_TOKEN für vollständige Tests:${NC}"
    echo -e "${BLUE}  export GITHUB_TOKEN=ghp_your_token_here${NC}"
    echo ""
fi

# Test 4: Tool-Definitionen überprüfen
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}Test 4: Tool-Definitionen${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

for tool in "list_github_issues" "create_github_issue" "update_github_issue" "delete_github_issue"; do
    echo -e "${BLUE}Tool: ${tool}${NC}"
    curl -s "${BASE_URL}/api/tools/list" | jq -r ".[] | select(.name == \"${tool}\") | .description"
    echo ""
done

# Zusammenfassung
echo -e "${BLUE}╔════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                  Test Zusammenfassung                  ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}✅ Alle Tests abgeschlossen${NC}"
echo ""
echo -e "${BLUE}Verfügbare GitHub Issue Tools:${NC}"
echo -e "  • ${GREEN}list_github_issues${NC}  - Issues auflisten"
echo -e "  • ${GREEN}create_github_issue${NC} - Issue erstellen"
echo -e "  • ${GREEN}update_github_issue${NC} - Issue bearbeiten"
echo -e "  • ${GREEN}delete_github_issue${NC} - Issue schließen"
echo ""
echo -e "${BLUE}Konfiguration in application.properties:${NC}"
echo -e "  github.token=\${GITHUB_TOKEN:}"
echo -e "  github.repository=\${GITHUB_REPOSITORY:}"
echo ""
echo -e "${BLUE}Dokumentation:${NC}"
echo -e "  backend/mcp-server/src/main/java/de/jivz/mcp/tools/git/README.md"
echo ""

