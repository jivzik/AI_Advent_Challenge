#!/usr/bin/env node

/**
 * Perplexity MCP Server mit HTTP/REST Support
 *
 * Unterstützt sowohl:
 * 1. stdio (Original MCP)
 * 2. HTTP REST (Neu - für dynamische Abfragen)
 *
 * REST Endpoints:
 * - GET  /api/tools           - Liste alle Tools auf
 * - POST /api/tools/search    - Führe perplexity_search aus
 * - POST /api/tools/ask       - Führe perplexity_ask aus
 * - POST /api/execute         - Generischer Tool-Executor
 */

import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from '@modelcontextprotocol/sdk/types.js';
import axios from 'axios';
import dotenv from 'dotenv';
import express from 'express';
import cors from 'cors';

dotenv.config();

const PERPLEXITY_API_KEY = process.env.PERPLEXITY_API_KEY;
const PERPLEXITY_API_URL = 'https://api.perplexity.ai/chat/completions';
const HTTP_PORT = process.env.HTTP_PORT || 3001;
const ENABLE_REST = process.env.ENABLE_REST !== 'false';

if (!PERPLEXITY_API_KEY) {
  console.error('Error: PERPLEXITY_API_KEY environment variable is not set');
  process.exit(1);
}

// ============================================================================
// PERPLEXITY API INTEGRATION
// ============================================================================

async function callPerplexitySonar(prompt, model = 'sonar', temperature = 0.7, maxTokens = 1000) {
  try {
    const response = await axios.post(
      PERPLEXITY_API_URL,
      {
        model: model,
        messages: [
          {
            role: 'user',
            content: prompt
          }
        ],
        temperature: temperature,
        max_tokens: maxTokens
      },
      {
        headers: {
          'Authorization': `Bearer ${PERPLEXITY_API_KEY}`,
          'Content-Type': 'application/json'
        },
        timeout: 60000
      }
    );

    return {
      success: true,
      answer: response.data.choices[0].message.content,
      model: response.data.model,
      usage: response.data.usage,
      citations: response.data.citations || []
    };
  } catch (error) {
    console.error('Perplexity API error:', error.response?.data || error.message);
    return {
      success: false,
      error: error.response?.data?.error?.message || error.message
    };
  }
}

// ============================================================================
// MCP SERVER (stdio)
// ============================================================================

const server = new Server(
  {
    name: 'perplexity-mcp-server',
    version: '1.0.0',
  },
  {
    capabilities: {
      tools: {},
    },
  }
);

/**
 * Handler for listing available tools
 */
server.setRequestHandler(ListToolsRequestSchema, async () => {
  return {
    tools: getAvailableTools()
  };
});

/**
 * Handler for tool execution
 */
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;
  return await executeToolRequest(name, args);
});

// ============================================================================
// TOOL DEFINITIONS
// ============================================================================

function getAvailableTools() {
  return [
    {
      name: 'perplexity_ask',
      description: 'Ask a question to Perplexity Sonar AI model. This tool uses Perplexity API to get answers with real-time internet search capabilities.',
      inputSchema: {
        type: 'object',
        properties: {
          prompt: {
            type: 'string',
            description: 'The question or prompt to send to Perplexity Sonar'
          },
          model: {
            type: 'string',
            description: 'Perplexity model to use (default: sonar)',
            default: 'sonar'
          },
          temperature: {
            type: 'number',
            description: 'Temperature for response generation (0.0-1.0, default: 0.7)',
            default: 0.7
          },
          max_tokens: {
            type: 'number',
            description: 'Maximum tokens in response (default: 1000)',
            default: 1000
          }
        },
        required: ['prompt']
      }
    },
    {
      name: 'perplexity_search',
      description: 'Search for information using Perplexity with internet access. Returns detailed answers with citations.',
      inputSchema: {
        type: 'object',
        properties: {
          query: {
            type: 'string',
            description: 'Search query'
          }
        },
        required: ['query']
      }
    }
  ];
}

/**
 * Возвращает инструменты в формате, совместимом с ожидаемой структурой
 */
function getAvailableToolsFormatted() {
  return getAvailableTools();
}

// ============================================================================
// TOOL EXECUTION
// ============================================================================

async function executeToolRequest(name, args) {
  try {
    if (name === 'perplexity_ask') {
      const { prompt, model = 'sonar', temperature = 0.7, max_tokens = 1000 } = args;

      if (!prompt) {
        throw new Error('prompt is required');
      }

      const result = await callPerplexitySonar(prompt, model, temperature, max_tokens);

      return {
        content: [
          {
            type: 'text',
            text: JSON.stringify(result, null, 2)
          }
        ]
      };
    } else if (name === 'perplexity_search') {
      const { query } = args;

      if (!query) {
        throw new Error('query is required');
      }

      const result = await callPerplexitySonar(query, 'sonar', 0.2, 1500);

      return {
        content: [
          {
            type: 'text',
            text: JSON.stringify(result, null, 2)
          }
        ]
      };
    } else {
      throw new Error(`Unknown tool: ${name}`);
    }
  } catch (error) {
    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify({
            success: false,
            error: error.message
          })
        }
      ],
      isError: true
    };
  }
}

// ============================================================================
// HTTP/REST SERVER (Neu)
// ============================================================================

function startHttpServer() {
  const app = express();

  // Middleware
  app.use(express.json());
  app.use(cors());

  // Health Check
  app.get('/health', (req, res) => {
    res.json({
      status: 'ok',
      server: 'Perplexity MCP Server',
      version: '1.0.0',
      timestamp: new Date().toISOString()
    });
  });

  // =========================================================================
  // REST ENDPOINTS
  // =========================================================================

  /**
   * GET /api/tools
   * Listet alle verfügbaren Tools auf
   * Rückgabe: Array von Tools
   */
  app.get('/api/tools', (req, res) => {
    try {
      const tools = getAvailableTools();
      res.json(tools);
    } catch (error) {
      res.status(500).json({
        success: false,
        error: error.message
      });
    }
  });

  /**
   * GET /api/tools/:name
   * Ruft Details zu einem spezifischen Tool ab
   * Rückgabe: Tool Objekt
   */
  app.get('/api/tools/:name', (req, res) => {
    try {
      const { name } = req.params;
      const tools = getAvailableTools();
      const tool = tools.find(t => t.name === name);

      if (!tool) {
        return res.status(404).json({
          error: `Tool not found: ${name}`
        });
      }

      res.json(tool);
    } catch (error) {
      res.status(500).json({
        error: error.message
      });
    }
  });

  /**
   * POST /api/tools/search
   * Führt perplexity_search aus
   *
   * Body: { "query": "search term" }
   */
  app.post('/api/tools/search', async (req, res) => {
    try {
      const { query } = req.body;

      if (!query) {
        return res.status(400).json({
          success: false,
          error: 'query parameter is required'
        });
      }

      console.log(`🔍 Search query: ${query}`);
      const result = await callPerplexitySonar(query, 'sonar', 0.2, 1500);

      res.json({
        success: result.success,
        query: query,
        result: result,
        timestamp: new Date().toISOString()
      });
    } catch (error) {
      res.status(500).json({
        success: false,
        error: error.message
      });
    }
  });

  /**
   * POST /api/tools/ask
   * Führt perplexity_ask aus
   *
   * Body: { "prompt": "question", "temperature": 0.7, "max_tokens": 1000 }
   */
  app.post('/api/tools/ask', async (req, res) => {
    try {
      const {
        prompt,
        model = 'sonar',
        temperature = 0.7,
        max_tokens = 1000
      } = req.body;

      if (!prompt) {
        return res.status(400).json({
          success: false,
          error: 'prompt parameter is required'
        });
      }

      console.log(`❓ Ask prompt: ${prompt}`);
      const result = await callPerplexitySonar(prompt, model, temperature, max_tokens);

      res.json({
        success: result.success,
        prompt: prompt,
        result: result,
        timestamp: new Date().toISOString()
      });
    } catch (error) {
      res.status(500).json({
        success: false,
        error: error.message
      });
    }
  });

  /**
   * POST /api/execute
   * Generischer Tool-Executor
   *
   * Body: { "name": "tool_name", "arguments": {...} }
   */
  app.post('/api/tools/execute', async (req, res) => {
    try {
      const { toolName, arguments: args } = req.body;

      if (!toolName) {
        return res.status(400).json({
          success: false,
          error: 'name parameter is required'
        });
      }

      console.log(`🔧 Executing tool: ${toolName}`);
      const response = await executeToolRequest(toolName, args || {});

      // Parse content wenn es JSON ist
      let parsedContent = response.content[0]?.text;
      try {
        parsedContent = JSON.parse(parsedContent);
      } catch (e) {
        // Ist nicht JSON, verwende Text
      }

      res.json({
        success: !response.isError,
        tool: toolName,
        result: parsedContent,
        timestamp: new Date().toISOString()
      });
    } catch (error) {
      res.status(500).json({
        success: false,
        error: error.message
      });
    }
  });

  /**
   * GET /api/status
   * Server Status
   */
  app.get('/api/status', (req, res) => {
    res.json({
      success: true,
      server: 'Perplexity MCP Server',
      version: '1.0.0',
      transport: ['stdio', 'http'],
      tools: getAvailableTools().map(t => t.name),
      endpoints: [
        'GET /health',
        'GET /api/tools',
        'GET /api/tools/:name',
        'POST /api/tools/search',
        'POST /api/tools/ask',
        'POST /api/execute',
        'GET /api/status'
      ],
      timestamp: new Date().toISOString()
    });
  });

  // Error Handler
  app.use((err, req, res, next) => {
    console.error('Error:', err);
    res.status(500).json({
      success: false,
      error: err.message
    });
  });

  // Starte HTTP Server
  app.listen(HTTP_PORT, () => {
    console.error(`🌐 HTTP REST Server listening on http://localhost:${HTTP_PORT}`);
    console.error(`📚 API Endpoints:`);
    console.error(`   GET  /health`);
    console.error(`   GET  /api/tools`);
    console.error(`   GET  /api/tools/:name`);
    console.error(`   POST /api/tools/search`);
    console.error(`   POST /api/tools/ask`);
    console.error(`   POST /api/tools/execute`);
    console.error(`   GET  /api/status`);
  });
}

// ============================================================================
// MAIN - Starte beide Server (stdio + http)
// ============================================================================

async function main() {
  // Starte HTTP Server (wenn aktiviert)
  if (ENABLE_REST) {
    startHttpServer();
  }

  // Starte MCP stdio Server
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error('🔗 Perplexity MCP Server (stdio) connected');
}

main().catch((error) => {
  console.error('Server error:', error);
  process.exit(1);
});

