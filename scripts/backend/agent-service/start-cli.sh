е #!/bin/bash

# AI DevOps Agent CLI Starter Script
# This script builds and runs the CLI application

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔═══════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   🤖 AI DevOps Agent CLI Launcher       ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════╝${NC}"
echo ""

# Determine project root and navigate to agent-service
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
SERVICE_DIR="$PROJECT_ROOT/backend/agent-service"

cd "$SERVICE_DIR"
echo -e "${BLUE}📂 Working directory: $SERVICE_DIR${NC}"
echo ""

# Check Java version
echo -e "${BLUE}🔍 Checking Java version...${NC}"
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo -e "${RED}❌ Error: Java 21 or higher is required. Found Java $JAVA_VERSION${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Java $JAVA_VERSION detected${NC}"
echo ""

# Check environment variables
echo -e "${BLUE}🔍 Checking environment variables...${NC}"

MISSING_VARS=()

if [ -z "$OPENROUTER_API_KEY" ]; then
    MISSING_VARS+=("OPENROUTER_API_KEY")
fi

if [ -z "$PERSONAL_GITHUB_TOKEN" ]; then
    MISSING_VARS+=("PERSONAL_GITHUB_TOKEN")
fi

if [ -z "$PERSONAL_GITHUB_REPOSITORY" ]; then
    MISSING_VARS+=("PERSONAL_GITHUB_REPOSITORY")
fi

if [ ${#MISSING_VARS[@]} -gt 0 ]; then
    echo -e "${YELLOW}⚠️  Warning: Missing environment variables:${NC}"
    for var in "${MISSING_VARS[@]}"; do
        echo -e "   - ${var}"
    done
    echo ""
    echo -e "${YELLOW}Some features may not work. Set them with:${NC}"
    echo -e "  export OPENROUTER_API_KEY='your-key'"
    echo -e "  export PERSONAL_GITHUB_TOKEN='your-token'"
    echo -e "  export PERSONAL_GITHUB_REPOSITORY='owner/repo'"
    echo ""
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    echo -e "${GREEN}✅ All required environment variables are set${NC}"
fi
echo ""

# Build option
if [ "$1" == "--skip-build" ]; then
    echo -e "${YELLOW}⏩ Skipping build (--skip-build flag)${NC}"
else
    echo -e "${BLUE}🔨 Building application...${NC}"
    mvn clean package -DskipTests

    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Build successful${NC}"
    else
        echo -e "${RED}❌ Build failed${NC}"
        exit 1
    fi
fi
echo ""

# Find JAR file
JAR_FILE=$(find target -name "agent-service-*.jar" -not -name "*-sources.jar" | head -1)

if [ ! -f "$JAR_FILE" ]; then
    echo -e "${RED}❌ Error: JAR file not found in target/ directory${NC}"
    echo -e "${YELLOW}Try running without --skip-build flag${NC}"
    exit 1
fi

echo -e "${BLUE}📦 Using JAR: $JAR_FILE${NC}"
echo ""

# Set default POSTGRES_PASSWORD if not set
if [ -z "$POSTGRES_PASSWORD" ]; then
    export POSTGRES_PASSWORD="local_password"
    echo -e "${YELLOW}ℹ️  Using default POSTGRES_PASSWORD${NC}"
fi

echo -e "${GREEN}🚀 Starting CLI...${NC}"
echo ""
echo "════════════════════════════════════════════════════════════════"
echo ""

# Run the application
java -jar "$JAR_FILE" --spring.profiles.active=cli

echo ""
echo "════════════════════════════════════════════════════════════════"
echo -e "${BLUE}👋 CLI shutdown complete${NC}"

