#!/usr/bin/env node

/**
 * Perplexity MCP Server - JSON Export
 *
 * Exportiert alle verfügbaren Tools als JSON
 * Usage: node export-tools.js > tools.json
 */

import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js';

// Erstelle MCP Client
const transport = new StdioClientTransport({
  command: 'node',
  args: ['index.js'],
  env: {
    ...process.env,
    NODE_ENV: 'production'
  }
});

const client = new Client(
  {
    name: 'perplexity-json-exporter',
    version: '1.0.0',
  },
  {
    capabilities: {},
  }
);

try {
  // Verbinde dich mit dem Server
  await client.connect(transport);

  // Liste alle verfügbaren Tools auf
  const toolsResponse = await client.listTools();

  // Formatiere als JSON mit Details
  const output = {
    server: {
      name: 'Perplexity MCP Server',
      version: '1.0.0',
      description: 'MCP Server for Perplexity AI Integration',
      timestamp: new Date().toISOString()
    },
    tools: toolsResponse.tools.map(tool => ({
      name: tool.name,
      description: tool.description,
      inputSchema: tool.inputSchema,
      usage: getUsageExample(tool.name)
    }))
  };

  // Ausgabe als JSON
  console.log(JSON.stringify(output, null, 2));

  // Bereinigung
  await client.close();
  process.exit(0);

} catch (error) {
  console.error(JSON.stringify({
    error: true,
    message: error.message
  }));
  process.exit(1);
}

function getUsageExample(toolName) {
  const examples = {
    'perplexity_ask': {
      description: 'Ask a question to Perplexity Sonar',
      example: {
        prompt: 'What is the current status of AI development in 2025?',
        model: 'sonar',
        temperature: 0.7,
        max_tokens: 1000
      }
    },
    'perplexity_search': {
      description: 'Search for information with internet access',
      example: {
        query: 'Latest news about Spring Conference 2025'
      }
    }
  };

  return examples[toolName] || {
    description: 'Unknown tool',
    example: {}
  };
}

