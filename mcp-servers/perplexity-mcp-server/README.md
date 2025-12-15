# Perplexity MCP Server

Ein Model Context Protocol (MCP) Server für Perplexity AI Integration.

## Installation

```bash
npm install
```

## Konfiguration

1. Kopiere `.env.example` zu `.env`:
```bash
cp .env.example .env
```

2. Füge deinen Perplexity API Key hinzu:
```
PERPLEXITY_API_KEY=your_actual_api_key_here
```

## Start

```bash
npm start
```

## Verfügbare MCP Tools

### 1. `perplexity_ask`
Stellt eine Frage an Perplexity Sonar AI.

**Parameter:**
- `prompt` (string, required): Die Frage oder der Prompt
- `model` (string, optional): Perplexity Modell (default: "sonar")
- `temperature` (number, optional): 0.0-1.0 (default: 0.7)
- `max_tokens` (number, optional): Max Tokens (default: 1000)

### 2. `perplexity_search`
Sucht Informationen mit Perplexity und Internet-Zugang.

**Parameter:**
- `query` (string, required): Suchanfrage

## Verwendung mit Java MCP Client

Der Server kommuniziert über STDIO und folgt dem MCP-Protokoll.

