#!/bin/bash

# Quick Start Script für Local LLM Integration

echo "🚀 Starting Local LLM Support Chat Demo"
echo "========================================"
echo ""

# Farben für bessere Lesbarkeit
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 1. Prüfe ob Ollama läuft
echo -e "${YELLOW}[1/5] Checking Ollama...${NC}"
if ! curl -s http://localhost:11434/api/version > /dev/null 2>&1; then
    echo -e "${RED}❌ Ollama is not running!${NC}"
    echo "   Please start Ollama:"
    echo "   $ ollama serve"
    exit 1
fi
echo -e "${GREEN}✅ Ollama is running${NC}"

# 2. Prüfe ob Modell verfügbar ist
echo -e "${YELLOW}[2/5] Checking if gemma2:2b is installed...${NC}"
if ! ollama list | grep -q "gemma2:2b"; then
    echo -e "${YELLOW}⚠️  Model gemma2:2b not found. Downloading...${NC}"
    ollama pull gemma2:2b
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ Failed to download model${NC}"
        exit 1
    fi
fi
echo -e "${GREEN}✅ Model gemma2:2b is available${NC}"

# 3. Backend kompilieren
echo -e "${YELLOW}[3/5] Compiling Backend...${NC}"
cd backend/support-service
mvn clean package -DskipTests > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Backend compilation failed${NC}"
    mvn clean package -DskipTests
    exit 1
fi
echo -e "${GREEN}✅ Backend compiled successfully${NC}"
cd ../..

# 4. Frontend kompilieren
echo -e "${YELLOW}[4/5] Building Frontend...${NC}"
cd frontend
npm run build > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Frontend build failed${NC}"
    npm run build
    exit 1
fi
echo -e "${GREEN}✅ Frontend built successfully${NC}"
cd ..

# 5. Services starten
echo -e "${YELLOW}[5/5] Starting Services...${NC}"
echo ""
echo "Starting Backend (support-service)..."
cd backend/support-service
mvn spring-boot:run > backend.log 2>&1 &
BACKEND_PID=$!
cd ../..

# Warte bis Backend bereit ist
echo "Waiting for backend to be ready..."
for i in {1..30}; do
    if curl -s http://localhost:8088/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Backend is ready${NC}"
        break
    fi
    sleep 1
    if [ $i -eq 30 ]; then
        echo -e "${RED}❌ Backend startup timeout${NC}"
        kill $BACKEND_PID
        exit 1
    fi
done

echo ""
echo "Starting Frontend..."
cd frontend
npm run dev > frontend.log 2>&1 &
FRONTEND_PID=$!
cd ..

# Warte bis Frontend bereit ist
echo "Waiting for frontend to be ready..."
sleep 3

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}🎉 All services started successfully!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "📋 Service URLs:"
echo "   Frontend:  http://localhost:5173"
echo "   Backend:   http://localhost:8088"
echo "   Ollama:    http://localhost:11434"
echo ""
echo "📝 Logs:"
echo "   Backend:  backend/support-service/backend.log"
echo "   Frontend: frontend/frontend.log"
echo ""
echo "🎮 Usage:"
echo "   1. Open http://localhost:5173 in your browser"
echo "   2. Navigate to Support Chat"
echo "   3. Toggle between Local (🤖) and Remote (☁️) LLM"
echo ""
echo "🛑 To stop services:"
echo "   $ kill $BACKEND_PID $FRONTEND_PID"
echo ""
echo "   Or save PIDs to file:"
echo "   $ echo $BACKEND_PID > .backend.pid"
echo "   $ echo $FRONTEND_PID > .frontend.pid"
echo ""

# PIDs speichern
echo $BACKEND_PID > .backend.pid
echo $FRONTEND_PID > .frontend.pid

echo -e "${GREEN}✅ PIDs saved to .backend.pid and .frontend.pid${NC}"
echo ""
echo "Press Ctrl+C to view logs, or run:"
echo "  $ tail -f backend/support-service/backend.log"
echo "  $ tail -f frontend/frontend.log"

# Warte auf Benutzer-Interrupt
wait

