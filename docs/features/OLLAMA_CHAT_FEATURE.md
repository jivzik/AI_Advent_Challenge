# Ollama Chat Feature Documentation

## Overview

The Ollama Chat feature provides a simple, user-friendly interface for chatting with local LLM models via Ollama. Conversation history is automatically saved in the browser's localStorage, allowing you to continue conversations even after refreshing the page.

## Architecture

### Backend (llm-chat-service)

**Location**: `backend/llm-chat-service/`

**Port**: 8090

**Key Components**:
- `ChatController.java` - REST API endpoints for chat operations
- `LlmChatService.java` - Business logic for LLM communication
- `CorsConfig.java` - CORS configuration for frontend access
- `WebClientConfig.java` - Reactive HTTP client for Ollama API

**API Endpoints**:

1. **POST /api/chat** - Send message to LLM
   ```json
   Request:
   {
     "message": "Hello, how are you?",
     "model": "llama2",          // optional
     "temperature": 0.7,         // optional (0.0-2.0)
     "maxTokens": 2000,          // optional
     "systemPrompt": "...",      // optional
     "stream": false             // optional
   }

   Response:
   {
     "response": "I'm doing well, thank you!",
     "model": "llama2",
     "timestamp": "2026-01-21T...",
     "processingTimeMs": 1234,
     "tokensGenerated": 42,
     "done": true,
     "error": null
   }
   ```

2. **GET /api/status** - Service health check
   ```json
   {
     "service": "llm-chat-service",
     "status": "UP",
     "ollamaBaseUrl": "http://localhost:11434",
     "defaultModel": "llama2",
     "defaultTemperature": 0.7,
     "defaultMaxTokens": 2000,
     "timeoutSeconds": 120
   }
   ```

3. **GET /api/models** - Get model information
   ```json
   {
     "currentModel": "llama2",
     "baseUrl": "http://localhost:11434",
     "note": "To change model, use 'model' parameter..."
   }
   ```

### Frontend (OllamaChatPage.vue)

**Location**: `frontend/src/components/OllamaChatPage.vue`

**Features**:
- ✅ Send messages to local Ollama LLM
- ✅ Automatic conversation history saving (localStorage)
- ✅ Markdown formatting support (**bold**, *italic*, `code`)
- ✅ Loading indicators with animated typing dots
- ✅ Message metadata display (model, processing time, tokens)
- ✅ Clear history button with confirmation
- ✅ Auto-scroll to latest message
- ✅ Responsive design matching SupportChat.vue style
- ✅ Error handling with user-friendly messages
- ✅ Keyboard shortcuts (Enter to send, Shift+Enter for new line)

**Data Storage**:
- **localStorage keys**:
  - `ollama-chat-messages` - Array of message objects
  - `ollama-chat-conversation-id` - Unique conversation identifier

**Message Format**:
```typescript
interface Message {
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
  metadata?: {
    model?: string;
    processingTimeMs?: number;
    tokensGenerated?: number;
    done?: boolean;
  };
}
```

## Setup Instructions

### Prerequisites

1. **Java 21** installed
2. **Node.js 18+** and npm installed
3. **Ollama** installed and running
4. **LLM model** pulled in Ollama (e.g., `ollama pull llama2`)

### Backend Setup

1. **Set environment variable**:
   ```bash
   export OLLAMA_MODEL=llama2
   # or any other model you have: mistral, codellama, neural-chat, etc.
   ```

2. **Start llm-chat-service**:
   ```bash
   cd backend/llm-chat-service
   ./mvnw spring-boot:run
   ```

3. **Verify service is running**:
   ```bash
   curl http://localhost:8090/api/status
   ```

   Expected output:
   ```json
   {
     "service": "llm-chat-service",
     "status": "UP",
     "ollamaBaseUrl": "http://localhost:11434",
     "defaultModel": "llama2",
     ...
   }
   ```

### Frontend Setup

1. **Install dependencies** (if not already done):
   ```bash
   cd frontend
   npm install
   ```

2. **Start development server**:
   ```bash
   npm run dev
   ```

3. **Open browser**:
   ```
   http://localhost:5173
   ```

4. **Navigate to Ollama Chat**:
   - Click the "🤖 Ollama Chat" tab at the top

## Testing

### Automated Tests

Run the test script to verify backend functionality:

```bash
./test-ollama-chat.sh
```

This will test:
- ✅ Service health check
- ✅ Models endpoint
- ✅ Simple chat message
- ✅ Empty message validation
- ✅ Custom temperature parameter
- ✅ CORS headers

### Manual Testing

1. **Start services**:
   ```bash
   # Terminal 1 - Backend
   cd backend/llm-chat-service
   export OLLAMA_MODEL=llama2
   ./mvnw spring-boot:run

   # Terminal 2 - Frontend
   cd frontend
   npm run dev
   ```

2. **Open browser**: http://localhost:5173

3. **Click "🤖 Ollama Chat" tab**

4. **Test scenarios**:
   - Send a simple message: "Hello!"
   - Send a complex question: "Explain quantum computing in simple terms"
   - Refresh page - history should persist
   - Click "Clear History" - should prompt confirmation
   - Check metadata display (model name, processing time, token count)
   - Test markdown: "Show me **bold** and *italic* text"

## Configuration

### Backend Configuration

**File**: `backend/llm-chat-service/src/main/resources/application.properties`

```properties
# Server Configuration
server.port=8090

# Ollama LLM Configuration
llm.ollama.base-url=${OLLAMA_BASE_URL:http://localhost:11434}
llm.ollama.model=${OLLAMA_MODEL}
llm.ollama.temperature=${OLLAMA_TEMPERATURE:0.7}
llm.ollama.max-tokens=${OLLAMA_MAX_TOKENS:2000}
llm.ollama.timeout-seconds=${OLLAMA_TIMEOUT:120}
```

**Environment Variables**:
- `OLLAMA_MODEL` (required) - Model name (e.g., llama2, mistral, codellama)
- `OLLAMA_BASE_URL` (optional) - Ollama API URL (default: http://localhost:11434)
- `OLLAMA_TEMPERATURE` (optional) - Response randomness 0.0-2.0 (default: 0.7)
- `OLLAMA_MAX_TOKENS` (optional) - Max response length (default: 2000)
- `OLLAMA_TIMEOUT` (optional) - Request timeout in seconds (default: 120)

### Frontend Configuration

**File**: `frontend/src/components/OllamaChatPage.vue`

```typescript
// Line 175 - API Configuration
const API_BASE_URL = 'http://localhost:8090/api';

// Line 170-171 - localStorage keys
const STORAGE_KEY_MESSAGES = 'ollama-chat-messages';
const STORAGE_KEY_CONVERSATION_ID = 'ollama-chat-conversation-id';
```

To change these:
1. Update `API_BASE_URL` if backend runs on different port
2. Update storage keys to use different localStorage namespace

## Troubleshooting

### "Cannot connect to Ollama service"

**Problem**: Frontend shows "Cannot connect to Ollama service. Is it running on port 8090?"

**Solutions**:
1. Check if backend is running:
   ```bash
   curl http://localhost:8090/api/status
   ```

2. Check if OLLAMA_MODEL is set:
   ```bash
   echo $OLLAMA_MODEL
   ```

3. Verify Ollama is running:
   ```bash
   ollama list  # Should show your models
   ```

4. Check backend logs for errors

### CORS Errors in Browser Console

**Problem**: "Access to XMLHttpRequest at 'http://localhost:8090/api/chat' from origin 'http://localhost:5173' has been blocked by CORS policy"

**Solution**:
- Verify `CorsConfig.java` exists in backend
- Restart backend service
- Clear browser cache

### History Not Saving

**Problem**: Messages disappear after page refresh

**Solutions**:
1. Check browser console for localStorage errors
2. Verify localStorage is enabled (not in private/incognito mode)
3. Check if localStorage quota is exceeded:
   ```javascript
   // In browser console
   console.log(localStorage.length);
   console.log(localStorage.getItem('ollama-chat-messages'));
   ```

### Slow Response Times

**Problem**: LLM takes very long to respond

**Solutions**:
1. Use smaller/faster model: `export OLLAMA_MODEL=tinyllama`
2. Reduce max tokens: `export OLLAMA_MAX_TOKENS=500`
3. Check system resources (CPU/RAM usage)
4. Ensure Ollama is using GPU if available

### "Model not found" Error

**Problem**: Backend returns error about model not found

**Solutions**:
1. Pull the model:
   ```bash
   ollama pull llama2
   ```

2. Verify model name matches exactly:
   ```bash
   ollama list
   ```

3. Update OLLAMA_MODEL to match available model

## Files Created/Modified

### New Files

1. **Frontend**:
   - `frontend/src/components/OllamaChatPage.vue` - Main chat component

2. **Backend**:
   - `backend/llm-chat-service/src/main/java/de/jivz/llmchatservice/config/CorsConfig.java` - CORS config

3. **Documentation/Tests**:
   - `test-ollama-chat.sh` - Automated test script
   - `OLLAMA_CHAT_FEATURE.md` - This documentation

### Modified Files

1. **Frontend**:
   - `frontend/src/App.vue`:
     - Added "🤖 Ollama Chat" tab button
     - Added `OllamaChatPage` import and component registration
     - Added `'ollama-chat'` to Mode type

## API Integration Examples

### Using curl

**Simple chat**:
```bash
curl -X POST http://localhost:8090/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello, how are you?"}'
```

**With custom temperature**:
```bash
curl -X POST http://localhost:8090/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Tell me a short joke",
    "temperature": 0.9,
    "maxTokens": 100
  }'
```

### Using JavaScript/TypeScript

```typescript
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8090/api';

async function sendMessage(message: string) {
  const response = await axios.post(`${API_BASE_URL}/chat`, {
    message: message,
    temperature: 0.7,
    stream: false
  });

  return {
    text: response.data.response,
    model: response.data.model,
    processingTime: response.data.processingTimeMs,
    tokens: response.data.tokensGenerated
  };
}

// Usage
const result = await sendMessage("What is the capital of France?");
console.log(result.text); // "The capital of France is Paris."
```

## Future Enhancements

Potential improvements for future iterations:

1. **Conversation Management**:
   - Multiple conversation threads
   - Conversation search/filter
   - Export/import conversations
   - Share conversations

2. **Advanced Features**:
   - Streaming responses (real-time token generation)
   - Voice input/output
   - Code syntax highlighting
   - Image generation (if model supports)
   - File upload for context

3. **Backend Improvements**:
   - Conversation persistence in database
   - User authentication
   - Rate limiting
   - Conversation context management
   - Model switching without restart

4. **UI Enhancements**:
   - Dark/light theme toggle
   - Customizable message bubbles
   - Emoji reactions
   - Message editing/deletion
   - Quick action buttons
   - Conversation templates

## Performance Considerations

### Backend

- **Connection pooling**: WebClient configured with connection pool (max 50 connections)
- **Timeouts**: Configurable read/write timeouts (default 120s)
- **Buffer size**: 16MB buffer for large LLM responses
- **Keep-alive**: TCP keep-alive enabled for persistent connections

### Frontend

- **localStorage limits**: ~5-10MB per domain (varies by browser)
- **Message cleanup**: Consider implementing automatic cleanup for very old messages
- **Lazy loading**: For very long conversations, implement virtualized scrolling

### Ollama

- **Model size**: Smaller models = faster responses
  - tinyllama: ~637MB, fastest
  - llama2: ~3.8GB, balanced
  - mistral: ~4.1GB, higher quality
- **GPU acceleration**: Enable if available for 10-100x speedup
- **Context window**: Larger context = more memory usage

## Security Notes

1. **CORS**: Currently allows localhost origins only
2. **Input validation**: Backend validates empty messages
3. **Rate limiting**: Not implemented yet - consider adding for production
4. **Authentication**: Not implemented - add if exposing publicly
5. **Data privacy**: All data stored locally in browser (localStorage)

## Support

For issues or questions:
1. Check this documentation
2. Run `./test-ollama-chat.sh` to diagnose backend issues
3. Check browser console for frontend errors
4. Review backend logs for service errors
5. Verify Ollama is running: `ollama list`

## License

Same as parent project.
