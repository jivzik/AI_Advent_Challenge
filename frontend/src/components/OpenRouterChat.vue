<template>
  <div class="openrouter-chat-container">
    <!-- Chat Section -->
    <div class="chat-section">
      <div class="chat-header">
        <div class="header-content">
          <div>
            <h1>🔧 OpenRouter Tools Chat</h1>
            <p>Powered by {{ currentModelLabel }}</p>
          </div>
          <div class="header-controls">
            <button
                @click="toggleSettings"
                class="settings-button"
                :class="{ active: showSettings }"
                :disabled="isLoading"
                title="Settings"
            >
              ⚙️
            </button>
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

      <!-- Collapsible Settings Panel -->
      <div v-if="showSettings" class="settings-panel">
        <!-- System Prompt -->
        <div class="settings-row">
          <div class="settings-label">
            <span>🎭 System Prompt</span>
            <span class="settings-hint">(Defines AI personality)</span>
          </div>
          <textarea
              v-model="systemPrompt"
              class="system-prompt-input"
              placeholder="You are a helpful assistant with access to tools."
              :disabled="isLoading"
              rows="2"
          ></textarea>
        </div>

        <!-- Temperature -->
        <div class="settings-row">
          <div class="settings-label">
            <span>🌡️ Temperature</span>
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
            <span>0 — Precise</span>
            <span>1 — Balanced</span>
            <span>2 — Creative</span>
          </div>
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
            <div class="message-text markdown-content" v-html="renderMarkdown(msg.content)"></div>
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
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, computed } from 'vue';
import { OpenRouterChatService } from '../services/openRouterChatService';
import { marked } from 'marked';
import type { Message } from "../types/types";

const messages = ref<Message[]>([]);
const currentMessage = ref('');
const isLoading = ref(false);
const error = ref('');
const messagesContainer = ref<HTMLElement | null>(null);
const systemPrompt = ref('You are a helpful assistant with access to tools.');
const temperature = ref(0.7);
const showModelDropdown = ref(false);
const showSettings = ref(false);

// Available OpenRouter models
const availableModels = [
  { id: 'claude-sonnet', name: 'Claude Sonnet', emoji: '🧠', model: 'anthropic/claude-sonnet-4' },
  { id: 'gemma-3n', name: 'Gemma 3N', emoji: '✨', model: 'google/gemma-3n-e4b-it' },
  { id: 'mistral-small', name: 'Mistral Small', emoji: '⚡', model: 'mistralai/mistral-small-24b-instruct-2501' },
  { id: 'gpt-4.1', name: 'GPT-4.1', emoji: '🚀', model: 'openai/gpt-4.1' },
  { id: 'gpt-4.1-mini', name: 'GPT-4.1 Mini', emoji: '🤖', model: 'openai/gpt-4.1-mini' },
];

const selectedModelId = ref('claude-sonnet');

// Computed properties
const currentModelLabel = computed(() => {
  const model = availableModels.find(m => m.id === selectedModelId.value);
  return model ? model.name : 'Select Model';
});

const currentModelEmoji = computed(() => {
  const model = availableModels.find(m => m.id === selectedModelId.value);
  return model ? model.emoji : '🤖';
});

const selectedModel = computed(() => {
  const model = availableModels.find(m => m.id === selectedModelId.value);
  return model?.model || '';
});

// Generate unique conversation ID
const conversationId = ref<string>('openrouter-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9));

// Configure marked for safe HTML rendering
marked.setOptions({
  breaks: true,
  gfm: true
});

// Render Markdown to HTML
const renderMarkdown = (content: string): string => {
  try {
    return marked.parse(content) as string;
  } catch {
    return content;
  }
};

// Get temperature description
const getTemperatureDescription = (): string => {
  const temp = temperature.value;
  if (temp <= 0.3) {
    return '0–0.3: Strict precision, minimal creativity';
  } else if (temp <= 0.9) {
    return '0.4–0.9: Balance of precision and creativity';
  } else {
    return '1.0–2.0: Maximum creativity, may hallucinate';
  }
};

const toggleModelDropdown = () => {
  showModelDropdown.value = !showModelDropdown.value;
};

const toggleSettings = () => {
  showSettings.value = !showSettings.value;
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
    const data = await OpenRouterChatService.sendMessage({
      message: userMessage,
      conversationId: conversationId.value,
      systemPrompt: systemPrompt.value,
      temperature: temperature.value,
      model: selectedModel.value
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

const clearConversation = () => {
  if (!confirm('Start a new conversation? Current chat history will be cleared.')) {
    return;
  }

  messages.value = [];
  error.value = '';
  conversationId.value = 'openrouter-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
  console.log('✅ New conversation started. ID:', conversationId.value);
};

onMounted(() => {
  console.log('OpenRouter Chat initialized with conversation ID:', conversationId.value);
});
</script>
