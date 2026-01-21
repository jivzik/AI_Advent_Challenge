<template>
  <div class="chat-container">
    <div class="chat-wrapper">
      <!-- Chat Section -->
      <div class="chat-section">
        <!-- Header -->
        <div class="chat-header">
          <div class="header-content">
            <div>
              <h1>🤖 Ollama Chat</h1>
              <p>Local LLM chat with conversation memory</p>
            </div>
            <div class="header-controls">
              <button
                  v-if="messages.length > 0"
                  @click="clearHistory"
                  class="clear-button"
                  :disabled="isLoading"
                  title="Clear conversation history"
              >
                🗑️ Clear History
              </button>
              <div class="conversation-info">
                <span class="info-badge">
                  💬 {{ messages.length }} messages
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Messages Area -->
        <div class="chat-messages" ref="messagesContainer">
          <!-- Welcome Message -->
          <div v-if="messages.length === 0" class="welcome-message">
            <div class="welcome-icon">👋</div>
            <h2>Welcome to Ollama Chat!</h2>
            <p>Start a conversation with your local LLM. All history is saved in your browser.</p>
            <div class="welcome-features">
              <div class="feature">🤖 Local Ollama</div>
              <div class="feature">💾 Auto-save History</div>
              <div class="feature">⚡ Fast Response</div>
            </div>
          </div>

          <!-- Message Items -->
          <div
              v-for="(message, index) in messages"
              :key="index"
              :class="['message', message.role]"
          >
            <div class="message-content">
              <div class="message-role">{{ message.role === 'user' ? 'You' : 'Ollama' }}</div>
              <div class="message-text markdown-content" v-html="formatMessage(message.content)"></div>

              <!-- AI Metadata -->
              <div v-if="message.role === 'assistant' && message.metadata" class="message-metadata">
                <div class="metadata-item">
                  <span class="metadata-label">Model:</span>
                  <span class="metadata-value">{{ message.metadata.model || 'unknown' }}</span>
                </div>
                <div v-if="message.metadata.processingTimeMs" class="metadata-item">
                  <span class="metadata-label">Time:</span>
                  <span class="metadata-value">{{ message.metadata.processingTimeMs }}ms</span>
                </div>
                <div v-if="message.metadata.tokensGenerated" class="metadata-item">
                  <span class="metadata-label">Tokens:</span>
                  <span class="metadata-value">{{ message.metadata.tokensGenerated }}</span>
                </div>
              </div>
              <div class="message-time">{{ formatTime(message.timestamp) }}</div>
            </div>
          </div>

          <!-- Loading Indicator -->
          <div v-if="isLoading" class="message assistant loading">
            <div class="message-content">
              <div class="message-role">Ollama</div>
              <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        </div>

        <!-- Input Area -->
        <div class="chat-input-container">
          <div v-if="error" class="error-message">
            {{ error }}
          </div>
          <form @submit.prevent="sendMessage" class="chat-input-form">
            <input
                v-model="userInput"
                type="text"
                placeholder="Type your message..."
                :disabled="isLoading"
                class="chat-input"
            />
            <button
                type="submit"
                :disabled="isLoading || !userInput.trim()"
                class="send-button"
            >
              {{ isLoading ? 'Sending...' : 'Send' }}
            </button>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue';
import axios from 'axios';

// Types
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

// State
const userInput = ref('');
const messages = ref<Message[]>([]);
const isLoading = ref(false);
const error = ref('');
const messagesContainer = ref<HTMLElement | null>(null);
const conversationId = ref<string>(generateId());

// LocalStorage keys
const STORAGE_KEY_MESSAGES = 'ollama-chat-messages';
const STORAGE_KEY_CONVERSATION_ID = 'ollama-chat-conversation-id';

// Constants
const API_BASE_URL = 'http://localhost:8090/api';

// Methods
function generateId(): string {
  return 'conv-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
}

function loadHistory() {
  try {
    const savedMessages = localStorage.getItem(STORAGE_KEY_MESSAGES);
    const savedConversationId = localStorage.getItem(STORAGE_KEY_CONVERSATION_ID);

    if (savedMessages) {
      messages.value = JSON.parse(savedMessages);
      console.log('Loaded', messages.value.length, 'messages from localStorage');
    }

    if (savedConversationId) {
      conversationId.value = savedConversationId;
    } else {
      saveConversationId();
    }
  } catch (err) {
    console.error('Error loading history from localStorage:', err);
  }
}

function saveHistory() {
  try {
    localStorage.setItem(STORAGE_KEY_MESSAGES, JSON.stringify(messages.value));
    console.log('Saved', messages.value.length, 'messages to localStorage');
  } catch (err) {
    console.error('Error saving history to localStorage:', err);
    error.value = 'Failed to save conversation history';
  }
}

function saveConversationId() {
  try {
    localStorage.setItem(STORAGE_KEY_CONVERSATION_ID, conversationId.value);
  } catch (err) {
    console.error('Error saving conversation ID:', err);
  }
}

async function sendMessage() {
  if (!userInput.value.trim() || isLoading.value) return;

  const messageText = userInput.value.trim();
  userInput.value = '';
  error.value = '';

  // Add user message
  const userMessage: Message = {
    role: 'user',
    content: messageText,
    timestamp: new Date().toISOString()
  };
  messages.value.push(userMessage);
  saveHistory();

  scrollToBottom();
  isLoading.value = true;

  try {
    // Send to Ollama via llm-chat-service
    const response = await axios.post(`${API_BASE_URL}/chat`, {
      message: messageText,
      stream: false
    });

    // Add assistant message
    const assistantMessage: Message = {
      role: 'assistant',
      content: response.data.response,
      timestamp: response.data.timestamp || new Date().toISOString(),
      metadata: {
        model: response.data.model,
        processingTimeMs: response.data.processingTimeMs,
        tokensGenerated: response.data.tokensGenerated,
        done: response.data.done
      }
    };
    messages.value.push(assistantMessage);

    // Save history
    saveHistory();
    scrollToBottom();
  } catch (err: any) {
    console.error('Error sending message:', err);

    // Show detailed error
    if (err.response?.data?.error) {
      error.value = `Error: ${err.response.data.error}`;
    } else if (err.code === 'ERR_NETWORK') {
      error.value = 'Cannot connect to Ollama service. Is it running on port 8090?';
    } else {
      error.value = `Failed to send message: ${err.message}`;
    }

    // Remove user message on error (optional - you can keep it if you prefer)
    // messages.value.pop();
    // saveHistory();
  } finally {
    isLoading.value = false;
  }
}

function clearHistory() {
  if (confirm('Clear all conversation history? This cannot be undone.')) {
    messages.value = [];
    conversationId.value = generateId();
    localStorage.removeItem(STORAGE_KEY_MESSAGES);
    saveConversationId();
    error.value = '';
    console.log('History cleared');
  }
}

function formatTime(timestamp: string): string {
  if (!timestamp) return '';

  const date = new Date(timestamp);
  return date.toLocaleTimeString('en-US', {
    hour: '2-digit',
    minute: '2-digit'
  });
}

function formatMessage(content: string): string {
  // Convert markdown-style formatting to HTML
  return content
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')  // **bold**
      .replace(/\*(.*?)\*/g, '<em>$1</em>')              // *italic*
      .replace(/`(.*?)`/g, '<code>$1</code>')            // `code`
      .replace(/\n/g, '<br>');                           // newlines
}

async function scrollToBottom() {
  await nextTick();
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
  }
}

// Load history on mount
onMounted(() => {
  loadHistory();
  scrollToBottom();
});
</script>

<style scoped lang="scss">
@use '../styles/variables' as *;
@use '../styles/chat-interface';

// ============================================================================
// OLLAMA CHAT CONTAINER OVERRIDE - Fix width for single column layout
// ============================================================================

.chat-container {
  // Override the ChatInterface width calculation (which includes sidebar)
  width: $chat-width;
  max-width: $chat-max-width;
  min-width: $chat-min-width;

  // Single column layout, no sidebar
  flex-direction: column;
}

.chat-wrapper {
  // No gap needed for single column
  gap: 0;
}

// ============================================================================
// OLLAMA CHAT SPECIFIC STYLES
// ============================================================================

// Conversation info badge in header
.conversation-info {
  display: flex;
  align-items: center;
}

.info-badge {
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.1);
  color: white;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
}

// Welcome message
.welcome-message {
  text-align: center;
  padding: 3rem 2rem;
  color: var(--text-muted, #666);

  .welcome-icon {
    font-size: 4rem;
    margin-bottom: 1.5rem;
  }

  h2 {
    margin: 0 0 0.75rem;
    font-size: 1.75rem;
    color: var(--text-dark, #1a1a1a);
  }

  p {
    margin: 0 0 2rem;
    font-size: 1rem;
    line-height: 1.6;
  }

  .welcome-features {
    display: flex;
    justify-content: center;
    gap: 1rem;
    flex-wrap: wrap;

    .feature {
      padding: 0.75rem 1.25rem;
      background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
      border: 1px solid var(--border-primary, rgba(102, 126, 234, 0.3));
      border-radius: 8px;
      font-size: 0.875rem;
      font-weight: 500;
      color: var(--primary-color, #667eea);
    }
  }
}

// Message metadata specific styling
.message-metadata {
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  font-size: 0.75rem;

  .message.user & {
    border-top-color: rgba(255, 255, 255, 0.3);
  }
}

.metadata-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.metadata-label {
  font-weight: 600;
  opacity: 0.7;
}

.metadata-value {
  font-family: 'Courier New', monospace;
  opacity: 0.9;
}

// Error message styling
.error-message {
  padding: 0.75rem 1rem;
  background: #fee;
  border: 1px solid #fcc;
  border-radius: 8px;
  color: #c33;
  font-size: 0.875rem;
  margin-bottom: 0.75rem;
}
</style>
