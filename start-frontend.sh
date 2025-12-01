#!/bin/bash

echo "🎨 Starting Frontend (Vue 3)..."

cd frontend

# Install dependencies if node_modules doesn't exist
if [ ! -d "node_modules" ]; then
    echo "📦 Installing dependencies..."
    npm install
fi

# Start Vite dev server
echo "🔄 Starting Vite dev server..."
npm run dev

