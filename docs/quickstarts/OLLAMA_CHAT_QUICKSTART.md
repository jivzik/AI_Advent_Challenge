# Ollama Chat - Quick Start Guide

## 🚀 Quick Start (3 minutes)

### Step 1: Start Backend Service (Terminal 1)

```bash
cd backend/llm-chat-service
export OLLAMA_MODEL=llama2
./mvnw spring-boot:run
```

**Wait for**: `Started LlmChatServiceApplication` message

### Step 2: Start Frontend (Terminal 2)

```bash
cd frontend
npm run dev
```

**Wait for**: `Local: http://localhost:5173/`

### Step 3: Test It!

1. Open browser: **http://localhost:5173**
2. Click **"🤖 Ollama Chat"** tab
3. Type a message: "Hello!"
4. Press **Enter**
5. See response from Ollama! 🎉

## 🧪 Verify Backend Works

```bash
# Run automated tests
./test-ollama-chat.sh
```

Expected output:
```
✓ Service is UP
✓ Status endpoint returns UP
✓ Models endpoint works
✓ Chat endpoint returns response
✓ Empty message rejected correctly
✓ Chat with custom temperature works
✓ CORS headers present

Passed: 6
Failed: 0

✓ All tests passed!
```

## 📦 Features

- ✅ **Conversation History**: Auto-saved in browser localStorage
- ✅ **Markdown Support**: Use **bold**, *italic*, `code`
- ✅ **Metadata Display**: See model, processing time, tokens
- ✅ **Clear History**: Button with confirmation
- ✅ **Auto-scroll**: Automatically scroll to latest message
- ✅ **Error Handling**: User-friendly error messages
- ✅ **Keyboard Shortcuts**:
  - Enter = Send message
  - Shift+Enter = New line

## 🐛 Troubleshooting

### Backend won't start

**Problem**: `OLLAMA_MODEL environment variable is required`

**Fix**:
```bash
export OLLAMA_MODEL=llama2
```

### Frontend can't connect

**Problem**: "Cannot connect to Ollama service"

**Fix**: Check backend is running:
```bash
curl http://localhost:8090/api/status
```

Should return: `{"service":"llm-chat-service","status":"UP",...}`

### Ollama not found

**Problem**: Backend logs show "Connection refused to localhost:11434"

**Fix**: Make sure Ollama is running:
```bash
ollama list  # Should show your models
```

If not installed:
```bash
# Install Ollama (Linux/Mac)
curl -fsSL https://ollama.com/install.sh | sh

# Pull a model
ollama pull llama2
```

## 📁 What Was Created

### New Files:
1. `frontend/src/components/OllamaChatPage.vue` - Chat UI component
2. `backend/llm-chat-service/.../config/CorsConfig.java` - CORS configuration
3. `test-ollama-chat.sh` - Automated test script
4. `OLLAMA_CHAT_FEATURE.md` - Full documentation
5. `OLLAMA_CHAT_QUICKSTART.md` - This guide

### Modified Files:
1. `frontend/src/App.vue` - Added "🤖 Ollama Chat" tab

## 🎯 Next Steps

1. Try different models:
   ```bash
   ollama pull mistral
   export OLLAMA_MODEL=mistral
   # Restart backend
   ```

2. Adjust temperature for more creative responses:
   - Edit `OllamaChatPage.vue` line 235
   - Add `temperature: 0.9` to request

3. Test markdown formatting:
   - Send: "Show me **bold** and *italic* text with `code`"

4. Test conversation persistence:
   - Send a few messages
   - Refresh page (F5)
   - History should be preserved!

## 📚 Full Documentation

See `OLLAMA_CHAT_FEATURE.md` for:
- Architecture details
- API reference
- Configuration options
- Advanced troubleshooting
- Future enhancements

## ✅ Success Checklist

- [ ] Backend starts without errors
- [ ] `curl http://localhost:8090/api/status` returns UP
- [ ] `./test-ollama-chat.sh` passes all tests
- [ ] Frontend loads at http://localhost:5173
- [ ] "🤖 Ollama Chat" tab is visible
- [ ] Can send message and get response
- [ ] Messages show metadata (model, time, tokens)
- [ ] Refresh page preserves history
- [ ] Clear History button works

If all checked ✅ - Congratulations! Your Ollama Chat is working! 🎊
