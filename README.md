# AI Advent Challenge

A simple chat application that connects a Vue 3 frontend with a Spring Boot backend, powered by Perplexity AI.

## Overview

This project demonstrates a full-stack chat application where:
- **Frontend** (Vue 3 + TypeScript) sends user messages
- **Backend** (Spring Boot + Java 21) acts as an agent
- **Perplexity AI API** processes and responds to messages

## Tech Stack

### Backend
- Spring Boot 4.0.0
- Java 21
- Maven (Multi-module project)
- WebClient for HTTP requests

### Frontend
- Vue 3
- TypeScript
- Vite

## Prerequisites

- Java 21
- Maven 3.x
- Node.js 18+
- Perplexity API Key

## Quick Start

### 1. Configure API Key

Create a `.env` file in the project root:

```bash
PERPLEXITY_API_KEY=your-api-key-here
```

### 2. Start Backend

```bash
cd backend/perplexity-service
./mvnw spring-boot:run
```

Backend runs on: **http://localhost:8080**

### 3. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on: **http://localhost:5173**


## API Endpoints

### POST /api/chat
Send a chat message to the AI.

**Request:**
```json
{
  "message": "Your question here"
}
```

**Response:**
```json
{
  "reply": "AI response",
  "toolName": "PerplexityToolClient",
  "timestamp": "2025-12-01T14:30:00.000Z"
}
```


## Development

### Run Tests
```bash
cd backend/perplexity-service
./mvnw test
```

### Build for Production
```bash
# Backend
cd backend/perplexity-service
./mvnw clean package

# Frontend
cd frontend
npm run build
```


## Configuration

Backend configuration in `backend/perplexity-service/src/main/resources/application.properties`:

```properties
server.port=8080
perplexity.api.base-url=https://api.perplexity.ai
perplexity.api.key=${PERPLEXITY_API_KEY}
perplexity.api.model=sonar
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `PERPLEXITY_API_KEY` | Your Perplexity API key | - |


