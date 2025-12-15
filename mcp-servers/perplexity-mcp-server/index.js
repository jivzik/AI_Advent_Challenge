#!/usr/bin/env node

import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from '@modelcontextprotocol/sdk/types.js';
import axios from 'axios';
import dotenv from 'dotenv';

dotenv.config();

const PERPLEXITY_API_KEY = process.env.PERPLEXITY_API_KEY;
const PERPLEXITY_API_URL = 'https://api.perplexity.ai/chat/completions';

if (!PERPLEXITY_API_KEY) {
  console.error('Error: PERPLEXITY_API_KEY environment variable is not set');
  process.exit(1);
}

/**
 * Call Perplexity Sonar API
 */
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

/**
 * Create and configure MCP Server
 */
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
    tools: [
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
    ]
  };
});

/**
 * Handler for tool execution
 */
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;

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
});

/**
 * Start the server
 */
async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error('Perplexity MCP Server running on stdio');
}

main().catch((error) => {
  console.error('Server error:', error);
  process.exit(1);
});

