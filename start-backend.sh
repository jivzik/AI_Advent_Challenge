#!/bin/bash

echo "🚀 Starting Perplexity Service (Backend)..."

cd backend/perplexity-service

# Load environment variables if .env exists
if [ -f "../../.env" ]; then
    echo "📋 Loading environment variables..."
    set -a
    source ../../.env
    set +a
fi

# Start Spring Boot application
echo "🔄 Starting Spring Boot application..."
./mvnw spring-boot:run

