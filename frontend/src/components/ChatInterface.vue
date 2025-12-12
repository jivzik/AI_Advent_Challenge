<template>
  <div class="chat-container">
    <!-- Conversation Sidebar -->
    <ConversationSidebar
        ref="sidebarRef"
        :activeConversationId="conversationId"
        @select="handleConversationSelect"
        @delete="handleConversationDelete"
        @newConversation="handleNewConversation"
    />
    <div class="chat-wrapper">
      <!-- Chat Section -->
      <div class="chat-section">
        <div class="chat-header">
          <div class="header-content">
            <div>
              <h1>AI Chat Agent</h1>
              <p>Powered by {{ currentModelLabel }}</p>
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

        <!-- Temperature Control Section -->
        <div class="temperature-section">
          <div class="temperature-header">
            <span class="temperature-label">🌡️ Temperature</span>
            <span class="temperature-value">{{ temperature.toFixed(1) }}</span>
          </div>
          <div class="temperature-description">
            {{ getTemperatureDescription() }}
          </div>
          <input
              type="range"
              v-model.number="temperature"
              min="0"
              max="2"
              step="0.1"
              class="temperature-slider"
              :disabled="isLoading"
          />
          <div class="temperature-range-labels">
            <span class="range-label-left">0 — Точность</span>
            <span class="range-label-center">1 — Баланс</span>
            <span class="range-label-right">2 — Креативность</span>
          </div>
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
            <!-- Model Selector Dropdown -->
            <div class="model-selector">
              <button
                  type="button"
                  @click="toggleModelDropdown"
                  :disabled="isLoading"
                  class="model-button"
                  :title="currentModelLabel"
              >
                {{ currentModelEmoji }}
              </button>
              <div v-if="showModelDropdown" class="model-dropdown">
                <button
                    v-for="model in availableModels"
                    :key="model.id"
                    type="button"
                    @click="selectModel(model)"
                    :class="{ active: selectedModelId === model.id }"
                    class="model-option"
                >
                  {{ model.emoji }} {{ model.name }}
                </button>
              </div>
            </div>
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
      <!-- Metrics Sidebar -->
      <div class="metrics-sidebar">
        <MetricsCard v-if="lastMetrics" :metrics="lastMetrics"  :compressionInfo="compressionInfo"/>
        <div v-else class="metrics-empty">
          <div class="empty-state">
            <div class="empty-icon">📊</div>
            <div class="empty-text">Metrics will appear here</div>
            <div class="empty-hint">Send a message to see response metrics</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, onMounted, computed } from 'vue';
import { ChatService } from '../services/chatService';
import { JsonFormatter } from '../utils/jsonFormatter';
import { marked } from 'marked';
import MetricsCard from './MetricsCard.vue';
import ConversationSidebar from './ConversationSidebar.vue';
import type { CompressionInfo, ResponseMetrics, Message } from "../types/types";

// ⭐ Message interface now imported from types.ts!
// No local duplicate interface

const messages = ref<Message[]>([]);
const currentMessage = ref('');
const isLoading = ref(false);
const error = ref('');
const messagesContainer = ref<HTMLElement | null>(null);
const jsonResponseMode = ref(false);
const autoSchemaMode = ref(true);
const expandedJson = reactive<Record<number, boolean>>({});
const messageMetrics = reactive<Record<number, ResponseMetrics | null>>({});
const systemPrompt = ref('Ты дружелюбный ассистент, отвечай кратко и по делу.');
const temperature = ref(0.7);
const showModelDropdown = ref(false);
const compressionInfo = ref<CompressionInfo | null>(null);
const sidebarRef = ref<InstanceType<typeof ConversationSidebar> | null>(null);

// Available models with metadata
const availableModels = [
  { id: 'perplexity-sonar', name: 'Perplexity Sonar', emoji: '🔍', provider: 'perplexity', model: '' },
  { id: 'claude-sonnet', name: 'Claude Sonnet', emoji: '🧠', provider: 'openrouter', model: 'anthropic/claude-sonnet-4' },
  { id: 'gemma-3n', name: 'Gemma 3N', emoji: '✨', provider: 'openrouter', model: 'google/gemma-3n-e4b-it' },
  { id: 'mistral-small', name: 'Mistral Small', emoji: '⚡', provider: 'openrouter', model: 'mistralai/mistral-small-24b-instruct-2501' },
  { id: 'gpt-5', name: 'GPT-5 2025', emoji: '🚀', provider: 'openrouter', model: 'openai/gpt-5-2025-08-07' },
  { id: 'gpt-5.1', name: 'GPT-5.1', emoji: '🚀', provider: 'openrouter', model: 'openai/gpt-5.1' },
];

const selectedModelId = ref('perplexity-sonar');

// Computed property for current model label
const currentModelLabel = computed(() => {
  const model = availableModels.find(m => m.id === selectedModelId.value);
  return model ? model.name : 'Select Model';
});

// Computed property for current model emoji
const currentModelEmoji = computed(() => {
  const model = availableModels.find(m => m.id === selectedModelId.value);
  return model ? model.emoji : '🤖';
});

// Computed property for current selected provider and model
const selectedProvider = computed(() => {
  const model = availableModels.find(m => m.id === selectedModelId.value);
  return model?.provider || 'perplexity';
});

const selectedModel = computed(() => {
  const model = availableModels.find(m => m.id === selectedModelId.value);
  return model?.model || '';
});


// Computed property for last metrics
const lastMetrics = computed(() => {
  // Find the last assistant message's metrics
  for (let i = messages.value.length - 1; i >= 0; i--) {
    if (messages.value[i]?.role === 'assistant' && messageMetrics[i]) {
      return messageMetrics[i];
    }
  }
  return null;
});

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

// Get temperature description based on current value
const getTemperatureDescription = (): string => {
  const temp = temperature.value;
  if (temp <= 0.3) {
    return '0–0.3: Строгая точность, минимум фантазии';
  } else if (temp <= 0.9) {
    return '0.4–0.9: Баланс точности и креативности';
  } else {
    return '1.0–2.0: Максимальная креативность, возможен бред';
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


const toggleModelDropdown = () => {
  showModelDropdown.value = !showModelDropdown.value;
};

const selectModel = (model: typeof availableModels[0]) => {
  selectedModelId.value = model.id;
  showModelDropdown.value = false;
};

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
    }
  });
};

const formatTime = (timestamp?: Date | string): string => {
  if (!timestamp) return '';

  const date = typeof timestamp === 'string' ? new Date(timestamp) : timestamp;
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
      systemPrompt: systemPrompt.value,
      temperature: temperature.value,
      provider: selectedProvider.value,
      model: selectedModel.value || undefined
    });

    // Add assistant message to UI
    const messageIndex = messages.value.length;
    messages.value.push({
      role: 'assistant',
      content: data.reply,
      timestamp: new Date(data.timestamp)
    });

    // Store metrics for this message
    if (data.metrics) {
      messageMetrics[messageIndex] = data.metrics;
      console.log('📊 Metrics stored for message:', data.metrics);
    }
    await fetchCompressionInfo();
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

const fetchCompressionInfo = async () => {
  if (!conversationId.value) return;

  try {
    const info = await ChatService.getCompressionInfo(conversationId.value);
    compressionInfo.value = info;

    console.log('🗜️ Compression info loaded:', info);
  } catch (error) {
    console.error('Failed to fetch compression info:', error);
  }
};

// ═══════════════════════════════════════════════════════════════════
// CONVERSATION SIDEBAR HANDLERS
// ═══════════════════════════════════════════════════════════════════

const handleConversationSelect = async (selectedConversationId: string) => {
  console.log('📂 Loading conversation:', selectedConversationId);

  // Switch to the selected conversation
  conversationId.value = selectedConversationId;
  error.value = '';
  compressionInfo.value = null;

  try {
    // ⭐ Load conversation messages from backend
    isLoading.value = true;
    const history = await ChatService.getConversationHistory(selectedConversationId);

    console.log('✅ Loaded messages:', history.length);

    // Display messages in chat
    messages.value = history;

    // Scroll to bottom after messages are rendered
    nextTick(() => {
      scrollToBottom();
    });

    // Refresh compression info for the selected conversation
    await fetchCompressionInfo();

  } catch (err) {
    console.error('❌ Failed to load conversation history:', err);
    error.value = 'Failed to load conversation history';
    messages.value = [];
  } finally {
    isLoading.value = false;
  }
};

const handleConversationDelete = (deletedConversationId: string) => {
  // If the deleted conversation is the current one, start a new one
  if (conversationId.value === deletedConversationId) {
    handleNewConversation();
  }
  console.log('🗑️ Conversation deleted:', deletedConversationId);
};

const handleNewConversation = () => {
  // Create a new conversation
  messages.value = [];
  error.value = '';
  compressionInfo.value = null;
  conversationId.value = 'conv-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
  console.log('✨ New conversation started. ID:', conversationId.value);
};

onMounted(() => {
  console.log('Chat initialized with conversation ID:', conversationId.value);
});
</script>

<style scoped lang="scss">
@use '../styles/chat-interface';
</style>