<template>
  <div class="chat-container">
    <!-- Conversation Sidebar -->
    <OpenRouterSidebar
        ref="sidebarRef"
        :active-conversation-id="activeConversationId"
        @select="handleSelectConversation"
        @delete="handleDeleteConversation"
        @new-conversation="handleNewConversation"
    />

    <div class="chat-wrapper">
      <!-- Chat Section -->
      <div class="chat-section">
        <div class="chat-header">
          <div class="header-content">
            <div>
              <h1>🔧 OpenRouter Tools Chat</h1>
              <p>Powered by {{ currentModelLabel }}</p>
            </div>
            <div class="header-controls">
              <!-- Developer Assistant Status Indicators -->
              <div v-if="hasAnyDevMessages" class="dev-status-indicators">
                <span
                    class="status-badge"
                    :class="{ active: ragAvailable }"
                    :title="ragAvailable ? 'RAG Available' : 'RAG Unavailable'"
                >
                  📚 RAG
                </span>
                <span
                    class="status-badge"
                    :class="{ active: gitAvailable }"
                    :title="gitAvailable ? 'Git Tools Available' : 'Git Tools Unavailable'"
                >
                  🔧 Git
                </span>
              </div>

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
              :class="['message', msg.role, { 'dev-mode': msg.isDeveloperMode }]"
          >
            <div class="message-content">
              <!-- Developer Mode Badge -->
              <div v-if="msg.isDeveloperMode" class="dev-badge">
                🧑‍💻 Developer Assistant
              </div>

              <div class="message-role">
                {{ msg.role === 'user' ? 'You' : (msg.isDeveloperMode ? 'Dev Assistant' : 'AI Agent') }}
              </div>

              <div class="message-text markdown-content" v-html="renderMarkdown(msg.content)"></div>

              <!-- Sources Section (if available) -->
              <div v-if="msg.sources && msg.sources.length > 0" class="message-sources">
                <div class="sources-header">📚 Sources:</div>
                <div class="sources-list">
                  <span
                      v-for="(source, idx) in msg.sources"
                      :key="idx"
                      class="source-item"
                  >
                    {{ idx + 1 }}. <code>{{ source }}</code>
                  </span>
                </div>
              </div>

              <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
            </div>
          </div>
          <div v-if="isLoading" class="message assistant loading">
            <div class="message-content">
              <div class="message-role">
                {{ isDevAssistantRequest ? 'Dev Assistant' : 'AI Agent' }}
              </div>
              <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
              <div v-if="isDevAssistantRequest" class="loading-hint">
                Searching documentation and Git context...
              </div>
            </div>
          </div>
        </div>

        <div class="chat-input-container">
          <div v-if="error" class="error-message">
            {{ error }}
          </div>

          <!-- Command Suggestions -->
          <div v-if="showCommandSuggestions" class="command-suggestions">
            <div
                v-for="cmd in filteredCommands"
                :key="cmd.command"
                @click="insertCommand(cmd.command + ' ')"
                class="command-item"
            >
              <span class="command-icon">{{ cmd.icon }}</span>
              <div class="command-info">
                <div class="command-name">{{ cmd.command }}</div>
                <div class="command-desc">{{ cmd.description }}</div>
              </div>
            </div>
          </div>

          <form @submit.prevent="sendMessage" class="chat-input-form">
            <input
                v-model="currentMessage"
                @input="handleInput"
                type="text"
                placeholder="Type your message or /help for developer assistance..."
                :disabled="isLoading || isProcessingVoice"
                class="chat-input"
            />
            <!-- Voice Button -->
            <button
                type="button"
                @click="triggerAudioUpload"
                :disabled="isLoading || isProcessingVoice || !voiceAvailable"
                class="voice-button"
                :title="voiceAvailable ? 'Upload audio file for transcription' : 'Voice service unavailable'"
            >
              <span v-if="isProcessingVoice">⏳</span>
              <span v-else>🎤</span>
            </button>
            <!-- Hidden file input -->
            <input
                ref="audioInputRef"
                type="file"
                accept="audio/*,.mp3,.wav,.m4a,.ogg,.webm,.aac,.flac"
                @change="handleAudioUpload"
                style="display: none"
            />
            <!-- Model Selector Dropdown -->
            <div class="model-selector">
              <button
                  type="button"
                  @click="toggleModelDropdown"
                  :disabled="isLoading || isProcessingVoice"
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
                :disabled="isLoading || isProcessingVoice || !currentMessage.trim()"
                class="send-button"
            >
              {{ isLoading || isProcessingVoice ? 'Sending...' : 'Send' }}
            </button>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, computed } from 'vue';
import OpenRouterSidebar from './OpenRouterSidebar.vue';
import { OpenRouterChatService } from '../services/openRouterChatService';
import { VoiceAgentService } from '../services/voiceAgentService';

import { marked } from 'marked';
import type { Message } from "../types/types";
import {useDevAssistant} from "../services/useDevAssistant.ts";

const messages = ref<Message[]>([]);
const currentMessage = ref('');
const isLoading = ref(false);
const error = ref('');
const messagesContainer = ref<HTMLElement | null>(null);
const systemPrompt = ref('You are a helpful assistant with access to tools.');
const temperature = ref(0.7);
const showModelDropdown = ref(false);
const showSettings = ref(false);
const sidebarRef = ref<InstanceType<typeof OpenRouterSidebar> | null>(null);
const showCommandSuggestions = ref(false);
const isDevAssistantRequest = ref(false);

// Voice Agent
const isProcessingVoice = ref(false);
const audioInputRef = ref<HTMLInputElement | null>(null);
const voiceAvailable = ref(false);

// Active conversation tracking
const activeConversationId = ref<string | null>(null);

// Developer Assistant composable
const {
  ragAvailable,
  gitAvailable,
  availableCommands,
  checkAvailability,
  isHelpCommand,
  extractHelpQuery,
  askDeveloperAssistant
} = useDevAssistant();

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

const hasAnyDevMessages = computed(() => {
  return messages.value.some(msg => msg.isDeveloperMode === true);
});

const filteredCommands = computed(() => {
  const input = currentMessage.value.toLowerCase();
  if (!input.startsWith('/')) return [];

  return availableCommands.filter(cmd =>
      cmd.command.toLowerCase().startsWith(input)
  );
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

const handleInput = () => {
  // Show command suggestions if starts with /
  showCommandSuggestions.value = currentMessage.value.startsWith('/') &&
      currentMessage.value.length > 1;
};

const insertCommand = (command: string) => {
  currentMessage.value = command;
  showCommandSuggestions.value = false;
  // Focus input
  nextTick(() => {
    const input = document.querySelector('.chat-input') as HTMLInputElement;
    input?.focus();
  });
};

const sendMessage = async () => {
  if (!currentMessage.value.trim() || isLoading.value) return;

  const userMessage = currentMessage.value;
  currentMessage.value = '';
  error.value = '';
  showCommandSuggestions.value = false;

  // If no active conversation, create new one
  if (!activeConversationId.value) {
    activeConversationId.value = conversationId.value;
  }

  // Add user message to UI
  messages.value.push({
    role: 'user',
    content: userMessage,
    timestamp: new Date()
  });
  scrollToBottom();

  isLoading.value = true;

  try {
    // Check if it's a /help command
    if (isHelpCommand(userMessage)) {
      isDevAssistantRequest.value = true;
      const query = extractHelpQuery(userMessage);

      console.log('🧑‍💻 Developer Assistant request:', query);

      // Call Developer Assistant
      const assistantMessage = await askDeveloperAssistant(
          query,
          conversationId.value,
          'user-' + Date.now()
      );

      messages.value.push(assistantMessage);

    } else {
      // Normal chat flow
      isDevAssistantRequest.value = false;

      const data = await OpenRouterChatService.sendMessage({
        message: userMessage,
        conversationId: conversationId.value,
        systemPrompt: systemPrompt.value,
        temperature: temperature.value,
        model: selectedModel.value
      });

      messages.value.push({
        role: 'assistant',
        content: data.reply,
        timestamp: new Date(data.timestamp)
      });
    }

    scrollToBottom();

    // Refresh sidebar to show updated conversation
    sidebarRef.value?.refresh();
  } catch (err: any) {
    error.value = err.message || 'An error occurred';
    console.error('Error sending message:', err);
  } finally {
    isLoading.value = false;
    isDevAssistantRequest.value = false;
  }
};

const clearConversation = () => {
  if (!confirm('Start a new conversation? Current chat history will be cleared.')) {
    return;
  }

  messages.value = [];
  error.value = '';
  conversationId.value = 'openrouter-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
  activeConversationId.value = conversationId.value;
  console.log('✅ New conversation started. ID:', conversationId.value);
};

// Sidebar event handlers
const handleSelectConversation = async (selectedConversationId: string) => {
  activeConversationId.value = selectedConversationId;
  conversationId.value = selectedConversationId;

  try {
    // Load conversation history
    const history = await OpenRouterChatService.getConversationHistory(selectedConversationId);
    messages.value = history;
    scrollToBottom();
    console.log(`✅ Loaded conversation ${selectedConversationId} with ${history.length} messages`);
  } catch (err) {
    console.error('Failed to load conversation history:', err);
    error.value = 'Failed to load conversation history';
  }
};

const handleDeleteConversation = (deletedConversationId: string) => {
  if (activeConversationId.value === deletedConversationId) {
    handleNewConversation();
  }
};

const handleNewConversation = () => {
  messages.value = [];
  error.value = '';
  conversationId.value = 'openrouter-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);
  activeConversationId.value = conversationId.value;
  console.log('✅ New conversation started. ID:', conversationId.value);
};

// Voice Agent Functions
const triggerAudioUpload = () => {
  audioInputRef.value?.click();
};

const handleAudioUpload = async (event: Event) => {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];

  if (!file) return;

  console.log('🎤 Audio file selected:', file.name, file.type, file.size, 'bytes');

  isProcessingVoice.value = true;
  error.value = '';

  // If no active conversation, create new one
  if (!activeConversationId.value) {
    activeConversationId.value = conversationId.value;
  }

  try {
    // Add voice message indicator to UI
    messages.value.push({
      role: 'user',
      content: `🎤 Processing audio: ${file.name}...`,
      timestamp: new Date()
    });
    scrollToBottom();

    // Call Voice Agent API
    const voiceResponse = await VoiceAgentService.processVoiceCommand({
      audioFile: file,
      userId: 'user-' + Date.now(),
      language: 'auto', // Auto-detect or use specific language
      model: selectedModel.value,
      temperature: temperature.value,
      systemPrompt: systemPrompt.value
    });

    console.log('✅ Voice processing completed:', voiceResponse);

    // Replace loading message with transcription
    messages.value[messages.value.length - 1] = {
      role: 'user',
      content: `🎤 **[Audio Transcription]**\n\n${voiceResponse.transcription}\n\n_Language: ${voiceResponse.language} • Transcription: ${voiceResponse.transcriptionTimeMs}ms_`,
      timestamp: new Date(voiceResponse.timestamp)
    };

    // Add LLM response
    messages.value.push({
      role: 'assistant',
      content: voiceResponse.response,
      timestamp: new Date(voiceResponse.timestamp)
    });

    scrollToBottom();

    // Refresh sidebar
    sidebarRef.value?.refresh();

  } catch (err: any) {
    console.error('❌ Voice processing error:', err);
    error.value = err.message || 'Voice processing failed';

    // Remove loading message
    messages.value.pop();
  } finally {
    isProcessingVoice.value = false;
    // Reset file input
    if (audioInputRef.value) {
      audioInputRef.value.value = '';
    }
  }
};

onMounted(async () => {
  console.log('OpenRouter Chat initialized with conversation ID:', conversationId.value);

  // Check Developer Assistant availability
  await checkAvailability();

  // Check Voice Agent availability
  try {
    voiceAvailable.value = await VoiceAgentService.healthCheck();
    console.log('🎤 Voice Agent available:', voiceAvailable.value);
  } catch (err) {
    console.warn('Voice Agent unavailable:', err);
    voiceAvailable.value = false;
  }
});
</script>

<style scoped lang="scss">
/* Existing styles remain the same... */

/* Developer Mode Styles */
.dev-status-indicators {
  display: flex;
  gap: 0.5rem;
  margin-right: 1rem;
}

.status-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 500;
  background: rgba(128, 128, 128, 0.1);
  color: #666;
  border: 1px solid rgba(128, 128, 128, 0.2);
  transition: all 0.3s ease;

  &.active {
    background: rgba(76, 175, 80, 0.1);
    color: #4caf50;
    border-color: #4caf50;
  }
}

.message.dev-mode {
  border-left: 3px solid #2196f3;
  background: linear-gradient(to right, rgba(33, 150, 243, 0.05), transparent);
}

.dev-badge {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  margin-bottom: 0.5rem;
  border-radius: 12px;
  background: #2196f3;
  color: white;
  font-size: 0.75rem;
  font-weight: 600;
}

.message-sources {
  margin-top: 1rem;
  padding: 1rem;
  background: rgba(33, 150, 243, 0.05);
  border-left: 3px solid #2196f3;
  border-radius: 8px;

  .sources-header {
    font-weight: 600;
    margin-bottom: 0.5rem;
    color: #2196f3;
  }

  .sources-list {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
  }

  .source-item {
    font-size: 0.875rem;

    code {
      background: rgba(0, 0, 0, 0.05);
      padding: 0.125rem 0.5rem;
      border-radius: 4px;
      font-family: 'Courier New', monospace;
    }
  }
}

.loading-hint {
  margin-top: 0.5rem;
  font-size: 0.875rem;
  color: #666;
  font-style: italic;
}

.command-suggestions {
  position: absolute;
  bottom: 100%;
  left: 0;
  right: 0;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px 8px 0 0;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
  max-height: 200px;
  overflow-y: auto;
  z-index: 10;
}

.command-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem 1rem;
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: rgba(33, 150, 243, 0.05);
  }

  .command-icon {
    font-size: 1.5rem;
  }

  .command-info {
    flex: 1;

    .command-name {
      font-weight: 600;
      color: #2196f3;
    }

    .command-desc {
      font-size: 0.75rem;
      color: #666;
    }
  }
}

/* Voice Button Styles */
.voice-button {
  padding: 0.75rem 1rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1.25rem;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 50px;

  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
  }

  &:active:not(:disabled) {
    transform: translateY(0);
  }

  &:disabled {
    background: linear-gradient(135deg, #ccc 0%, #999 100%);
    cursor: not-allowed;
    opacity: 0.6;
  }

  span {
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>

