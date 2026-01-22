#!/bin/bash

# Example curl commands for testing llm-chat-service
# These examples demonstrate various ways to use the chat API

SERVICE_URL="http://localhost:8090"

echo "========================================="
echo "LLM Chat Service - Example Requests"
echo "========================================="
echo ""
echo "Service URL: $SERVICE_URL"
echo ""

# Example 1: Simple chat
echo "1. Simple chat message:"
echo "---"
curl -X POST "$SERVICE_URL/api/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Hello! How are you?"
  }' | jq '.'
echo ""
echo ""

# Example 2: Chat with specific model
echo "2. Chat with specific model (mistral):"
echo "---"
curl -X POST "$SERVICE_URL/api/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Explain quantum computing in one sentence",
    "model": "mistral"
  }' | jq '.'
echo ""
echo ""

# Example 3: Chat with low temperature (focused)
echo "3. Focused response (low temperature):"
echo "---"
curl -X POST "$SERVICE_URL/api/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What is 2 + 2?",
    "temperature": 0.1
  }' | jq '.'
echo ""
echo ""

# Example 4: Chat with high temperature (creative)
echo "4. Creative response (high temperature):"
echo "---"
curl -X POST "$SERVICE_URL/api/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Write a creative opening line for a sci-fi story",
    "temperature": 1.5
  }' | jq '.'
echo ""
echo ""

# Example 5: Chat with system prompt
echo "5. Chat with system prompt (coding assistant):"
echo "---"
curl -X POST "$SERVICE_URL/api/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Write a Python function to reverse a string",
    "systemPrompt": "You are an expert Python developer. Provide concise, well-commented code.",
    "model": "codellama"
  }' | jq '.'
echo ""
echo ""

# Example 6: Short response
echo "6. Short response (limited tokens):"
echo "---"
curl -X POST "$SERVICE_URL/api/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Describe Spring Boot",
    "maxTokens": 100
  }' | jq '.'
echo ""
echo ""

# Example 7: Check service status
echo "7. Service status:"
echo "---"
curl -X GET "$SERVICE_URL/api/status" | jq '.'
echo ""
echo ""

# Example 8: Check model info
echo "8. Model information:"
echo "---"
curl -X GET "$SERVICE_URL/api/models" | jq '.'
echo ""
echo ""

echo "========================================="
echo "All examples completed"
echo "========================================="
