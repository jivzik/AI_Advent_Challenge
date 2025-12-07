<template>
  <div class="chat-container">
    <div class="chat-header">
      <div class="header-content">
        <div>
          <h1>AI Chat Agent</h1>
          <p>Powered by Perplexity AI</p>
        </div>
        <div class="header-controls">
          <label class="json-toggle">
            <input
              type="checkbox"
              v-model="jsonResponseMode"
              :disabled="isLoading"
            />
            <span>JSON-Antworten</span>
          </label>
          <label v-if="jsonResponseMode" class="auto-schema-toggle">
            <input
              type="checkbox"
              v-model="autoSchemaMode"
              :disabled="isLoading"
            />
            <span>🤖 Auto-Schema</span>
          </label>
          <button
            v-if="messages.length > 0"
            @click="clearConversation"
            class="clear-button"
            :disabled="isLoading"
            title="Start new conversation"
          >
            🗑️ New Conversation
          </button>
        </div>
      </div>
    </div>

    <!-- System Prompt Section -->
    <div class="system-prompt-section">
      <div class="system-prompt-header">
        <span class="system-prompt-label">🎭 System Prompt</span>
        <span class="system-prompt-hint">(Defines AI personality - can be changed mid-conversation)</span>
      </div>
      <textarea
        v-model="systemPrompt"
        class="system-prompt-input"
        placeholder="Ты дружелюбный ассистент, отвечай кратко и по делу."
        :disabled="isLoading"
        rows="2"
      ></textarea>
    </div>

    <div class="chat-messages" ref="messagesContainer">
      <div 
        v-for="(msg, index) in messages" 
        :key="index" 
        :class="['message', msg.role]"
      >
        <div class="message-content">
          <div class="message-role">{{ msg.role === 'user' ? 'You' : 'AI Agent' }}</div>
          <div class="message-text markdown-content" v-if="!isJsonContent(msg.content)" v-html="renderMarkdown(msg.content)"></div>
          <div v-else class="message-json">
            <div class="json-header">
              <span class="json-badge">JSON</span>
              <button @click="copyToClipboard(msg.content)" class="copy-button" title="Copy JSON">
                📋 Copy
              </button>
              <button @click="toggleJsonView(index)" class="toggle-button" title="Toggle view">
                {{ expandedJson[index] ? '📄 Raw' : '📖 Tree' }}
              </button>
            </div>
            <pre v-if="!expandedJson[index]" class="json-formatted" v-html="formatJsonHtml(msg.content)"></pre>
            <div v-else class="json-tree" v-html="createJsonTree(msg.content)"></div>
          </div>
          <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
        </div>
      </div>
      <div v-if="isLoading" class="message assistant loading">
        <div class="message-content">
          <div class="message-role">AI Agent</div>
          <div class="typing-indicator">
            <span></span>
            <span></span>
            <span></span>
          </div>
        </div>
      </div>
    </div>
    <div class="chat-input-container">
      <div v-if="error" class="error-message">
        {{ error }}
      </div>
      <form @submit.prevent="sendMessage" class="chat-input-form">
        <input
          v-model="currentMessage"
          type="text"
          placeholder="Type your message..."
          :disabled="isLoading"
          class="chat-input"
        />
        <button 
          type="submit" 
          :disabled="isLoading || !currentMessage.trim()"
          class="send-button"
        >
          {{ isLoading ? 'Sending...' : 'Send' }}
        </button>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, onMounted } from 'vue';
import { ChatService } from '../services/chatService';
import { JsonFormatter } from '../utils/jsonFormatter';
import { marked } from 'marked';

interface Message {
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

const messages = ref<Message[]>([]);
const currentMessage = ref('');
const isLoading = ref(false);
const error = ref('');
const messagesContainer = ref<HTMLElement | null>(null);
const jsonResponseMode = ref(false);
const autoSchemaMode = ref(true); // Auto-Schema is enabled by default when JSON mode is on
const expandedJson = reactive<Record<number, boolean>>({}); // Track JSON view state per message (reactive!)
const systemPrompt = ref('Ты дружелюбный ассистент, отвечай кратко и по делу.'); // Default system prompt

// Configure marked for safe HTML rendering
marked.setOptions({
  breaks: true, // Convert \n to <br>
  gfm: true     // GitHub Flavored Markdown
});

// Render Markdown to HTML
const renderMarkdown = (content: string): string => {
  try {
    return marked.parse(content) as string;
  } catch {
    return content;
  }
};

// Generate unique conversation ID for this session (persistent until page reload)
const conversationId = ref<string>('conv-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9));
const userId = ref<string>('user-' + Date.now());

// JSON handling methods
const isJsonContent = (content: string): boolean => {
  return JsonFormatter.isValidJson(content);
};

const formatJsonHtml = (content: string): string => {
  return JsonFormatter.toHtml(content);
};

const createJsonTree = (content: string): string => {
  try {
    const parsed = JSON.parse(content);
    return JsonFormatter.createTreeView(parsed);
  } catch {
    return content;
  }
};

const toggleJsonView = (index: number) => {
  expandedJson[index] = !expandedJson[index];
};

const copyToClipboard = async (content: string) => {
  try {
    const formatted = JsonFormatter.formatJson(content);
    await navigator.clipboard.writeText(formatted);
    console.log('✅ JSON copied to clipboard');
  } catch (err) {
    console.error('❌ Failed to copy:', err);
    alert('Failed to copy to clipboard');
  }
};

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
    }
  });
};

const formatTime = (date: Date) => {
  return date.toLocaleTimeString('en-US', {
    hour: '2-digit',
    minute: '2-digit'
  });
};

const sendMessage = async () => {
  if (!currentMessage.value.trim() || isLoading.value) return;

  const userMessage = currentMessage.value;
  currentMessage.value = '';
  error.value = '';

  // Add user message to UI
  messages.value.push({
    role: 'user',
    content: userMessage,
    timestamp: new Date()
  });
  scrollToBottom();

  isLoading.value = true;

  try {
    // Send message with conversationId and systemPrompt to maintain history
    const data = await ChatService.sendMessageWithOptions({
      message: userMessage,
      userId: userId.value,
      conversationId: conversationId.value,
      jsonMode: jsonResponseMode.value,
      autoSchema: autoSchemaMode.value,
      systemPrompt: systemPrompt.value
    });

    // Add assistant message to UI
    messages.value.push({
      role: 'assistant',
      content: data.reply,
      timestamp: new Date(data.timestamp)
    });
    scrollToBottom();
  } catch (err: any) {
    error.value = err.message || 'An error occurred';
    console.error('Error sending message:', err);
  } finally {
    isLoading.value = false;
  }
};

const clearConversation = async () => {
  if (!confirm('Do you really want to start a new conversation? The current chat history will be deleted.')) {
    return;
  }

  try {
    await ChatService.clearConversation(conversationId.value);
    messages.value = [];
    error.value = '';
    conversationId.value = 'conv-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
    console.log('✅ New conversation started. ID:', conversationId.value);
  } catch (err: any) {
    error.value = 'Error clearing conversation: ' + err.message;
    console.error('Error clearing conversation:', err);
  }
};

onMounted(() => {
  console.log('Chat initialized with conversation ID:', conversationId.value);
});
</script>

