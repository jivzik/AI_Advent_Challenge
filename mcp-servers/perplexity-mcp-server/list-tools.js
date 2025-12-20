#!/usr/bin/env node

/**
 * Perplexity MCP Server - List Tools CLI
 *
 * Dieses Script verbindet sich mit dem Perplexity MCP Server
 * und ruft alle verfügbaren Tools auf.
 *
 * Usage: node list-tools.js
 */

import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js';
import { spawn } from 'child_process';

// Starte den MCP Server als Subprocess
const serverProcess = spawn('node', ['index.js'], {
  cwd: process.cwd(),
  stdio: ['pipe', 'pipe', 'pipe']
});

// Error Handling
serverProcess.stderr.on('data', (data) => {
  console.error(`[Server] ${data}`);
});

serverProcess.on('error', (error) => {
  console.error('Failed to start server:', error);
  process.exit(1);
});

// Warte kurz, bis Server bereit ist
await new Promise(resolve => setTimeout(resolve, 1000));

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
    name: 'perplexity-cli-client',
    version: '1.0.0',
  },
  {
    capabilities: {},
  }
);

try {
  // Verbinde dich mit dem Server
  console.log('📡 Connecting to Perplexity MCP Server...\n');
  await client.connect(transport);

  // Liste alle verfügbaren Tools auf
  console.log('🔧 Fetching available tools...\n');
  const toolsResponse = await client.listTools();

  console.log('✅ Available Tools:\n');
  console.log('='.repeat(60));

  toolsResponse.tools.forEach((tool, index) => {
    console.log(`\n[${index + 1}] ${tool.name.toUpperCase()}`);
    console.log('-'.repeat(40));
    console.log(`Description: ${tool.description}`);

    if (tool.inputSchema) {
      console.log('\nInput Schema:');
      if (tool.inputSchema.properties) {
        Object.entries(tool.inputSchema.properties).forEach(([key, value]) => {
          console.log(`  • ${key}`);
          console.log(`    - Type: ${value.type}`);
          if (value.description) {
            console.log(`    - Description: ${value.description}`);
          }
          if (value.default) {
            console.log(`    - Default: ${value.default}`);
          }
          if (value.enum) {
            console.log(`    - Values: ${value.enum.join(', ')}`);
          }
        });
      }

      if (tool.inputSchema.required) {
        console.log(`\nRequired Parameters: ${tool.inputSchema.required.join(', ')}`);
      }
    }
  });

  console.log('\n' + '='.repeat(60));
  console.log(`\n📊 Total Tools: ${toolsResponse.tools.length}`);
  console.log('\n✨ Done!\n');

  // Bereinigung
  await client.close();
  serverProcess.kill();
  process.exit(0);

} catch (error) {
  console.error('❌ Error:', error.message);
  serverProcess.kill();
  process.exit(1);
}

