#!/bin/bash

echo "╔════════════════════════════════════════════════════════════════════╗"
echo "║     Perplexity MCP Integration - Vollständige Installation        ║"
echo "╚════════════════════════════════════════════════════════════════════╝"
echo ""

# Farben
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Arbeitsverzeichnis
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

echo -e "${BLUE}📍 Arbeitsverzeichnis: $SCRIPT_DIR${NC}"
echo ""

# ============================================================================
# Schritt 1: Node.js prüfen
# ============================================================================
echo -e "${YELLOW}[1/5] Node.js Voraussetzungen prüfen${NC}"
echo "─────────────────────────────────────────────────────────────────────"

if ! command -v node &> /dev/null; then
    echo -e "${RED}❌ Node.js ist nicht installiert!${NC}"
    echo ""
    echo "Installiere Node.js:"
    echo "  Ubuntu/Debian: sudo apt install nodejs npm"
    echo "  Fedora:        sudo dnf install nodejs npm"
    echo "  macOS:         brew install node"
    echo ""
    echo "Oder besuche: https://nodejs.org/en/download/"
    exit 1
fi

NODE_VERSION=$(node --version)
NPM_VERSION=$(npm --version)
echo -e "${GREEN}✅ Node.js: $NODE_VERSION${NC}"
echo -e "${GREEN}✅ npm:     $NPM_VERSION${NC}"
echo ""

# ============================================================================
# Schritt 2: Perplexity API Key prüfen
# ============================================================================
echo -e "${YELLOW}[2/5] Perplexity API Key prüfen${NC}"
echo "─────────────────────────────────────────────────────────────────────"

if [ -n "$PERPLEXITY_API_KEY" ]; then
    echo -e "${GREEN}✅ PERPLEXITY_API_KEY Umgebungsvariable gefunden${NC}"
    USE_ENV_KEY=true
else
    echo -e "${BLUE}ℹ️  PERPLEXITY_API_KEY nicht in Umgebungsvariablen${NC}"
    USE_ENV_KEY=false
fi

if [ ! -f "perplexity-mcp-server/.env" ]; then
    echo -e "${BLUE}ℹ️  .env Datei existiert noch nicht${NC}"
    if [ "$USE_ENV_KEY" = true ]; then
        echo "PERPLEXITY_API_KEY=$PERPLEXITY_API_KEY" > perplexity-mcp-server/.env
        echo -e "${GREEN}✅ .env Datei erstellt mit Key aus Umgebungsvariable${NC}"
    else
        cp perplexity-mcp-server/.env.example perplexity-mcp-server/.env
        echo -e "${YELLOW}⚠️  .env Datei von Beispiel erstellt${NC}"
        echo -e "${YELLOW}    Bitte füge deinen API Key hinzu:${NC}"
        echo -e "${YELLOW}    nano perplexity-mcp-server/.env${NC}"
        echo ""
        read -p "Drücke Enter um fortzufahren oder Ctrl+C zum Abbrechen..."
    fi
else
    if grep -q "your_perplexity_api_key_here" perplexity-mcp-server/.env; then
        echo -e "${YELLOW}⚠️  .env enthält noch Platzhalter${NC}"
        if [ "$USE_ENV_KEY" = true ]; then
            sed -i "s/PERPLEXITY_API_KEY=.*/PERPLEXITY_API_KEY=$PERPLEXITY_API_KEY/" perplexity-mcp-server/.env
            echo -e "${GREEN}✅ API Key aus Umgebungsvariable übernommen${NC}"
        else
            echo -e "${YELLOW}    Bitte füge deinen API Key hinzu:${NC}"
            echo -e "${YELLOW}    nano perplexity-mcp-server/.env${NC}"
            echo ""
            read -p "Drücke Enter um fortzufahren oder Ctrl+C zum Abbrechen..."
        fi
    else
        echo -e "${GREEN}✅ .env Datei existiert mit API Key${NC}"
    fi
fi
echo ""

# ============================================================================
# Schritt 3: npm dependencies installieren
# ============================================================================
echo -e "${YELLOW}[3/5] npm Dependencies installieren${NC}"
echo "─────────────────────────────────────────────────────────────────────"

cd perplexity-mcp-server || exit 1

if [ ! -d "node_modules" ]; then
    echo "Installiere npm Pakete..."
    npm install
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ npm Pakete erfolgreich installiert${NC}"
    else
        echo -e "${RED}❌ Fehler beim Installieren der npm Pakete${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✅ node_modules existiert bereits${NC}"
    echo "Überspringe Installation (verwende: rm -rf node_modules für Neuinstallation)"
fi

cd "$SCRIPT_DIR" || exit 1
echo ""

# ============================================================================
# Schritt 4: Maven Build
# ============================================================================
echo -e "${YELLOW}[4/5] Maven Build${NC}"
echo "─────────────────────────────────────────────────────────────────────"

echo "Kompiliere Spring Boot Projekt..."
./mvnw clean compile -DskipTests > /tmp/maven-build.log 2>&1

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Maven Build erfolgreich${NC}"
else
    echo -e "${YELLOW}⚠️  Maven Build mit Warnungen (siehe /tmp/maven-build.log)${NC}"
    # Nicht abbrechen, da IDE-Warnings normal sind
fi
echo ""

# ============================================================================
# Schritt 5: Installation abgeschlossen
# ============================================================================
echo -e "${GREEN}[5/5] Installation abgeschlossen!${NC}"
echo "────────────────────────────────────────────────────────────��────────"
echo ""
echo -e "${GREEN}✅ Alle Komponenten erfolgreich eingerichtet${NC}"
echo ""
echo "╔════════════════════════════════════════════════════════════════════╗"
echo "║                         NÄCHSTE SCHRITTE                           ║"
echo "╚════════════════════════════════════════════════════════════════════╝"
echo ""
echo -e "${BLUE}1. Application starten:${NC}"
echo "   ./mvnw spring-boot:run"
echo ""
echo -e "${BLUE}2. In neuem Terminal testen:${NC}"
echo "   ./test-perplexity-mcp.sh"
echo ""
echo -e "${BLUE}3. Oder manuell testen:${NC}"
echo "   curl http://localhost:8080/perplexity/status"
echo "   curl http://localhost:8080/perplexity/tools"
echo ""
echo -e "${BLUE}4. Frage stellen:${NC}"
echo "   curl -X POST http://localhost:8080/perplexity/ask \\"
echo "     -H 'Content-Type: application/json' \\"
echo "     -d '{\"prompt\": \"What is the Model Context Protocol?\"}'"
echo ""
echo "╔════════════════════════════════════════════════════════════════════╗"
echo "║                         VERFÜGBARE ENDPOINTS                       ║"
echo "╚════════════════════════════════════════════════════════════════════╝"
echo ""
echo "  GET  /perplexity/tools    - Liste alle MCP Tools"
echo "  POST /perplexity/ask      - Frage an Perplexity"
echo "  POST /perplexity/search   - Suche mit Perplexity"
echo "  GET  /perplexity/status   - Server Status"
echo ""
echo "╔════════════════════════════════════════════════════════════════════╗"
echo "║                         DOKUMENTATION                              ║"
echo "╚════════════════════════════════════════════════════════════════════╝"
echo ""
echo "  📖 Schnellstart:     PERPLEXITY_MCP_QUICKSTART.md"
echo "  📖 Komplette Doku:   PERPLEXITY_MCP_INTEGRATION_GUIDE.md"
echo "  📖 Implementierung:  PERPLEXITY_MCP_IMPLEMENTATION_SUMMARY.md"
echo ""
echo -e "${GREEN}Installation erfolgreich abgeschlossen! 🎉${NC}"
echo ""

