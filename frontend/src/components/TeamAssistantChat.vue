<template>
  <div class="chat-container">
    <!-- Quick Actions Sidebar -->
    <div class="quick-actions-sidebar">
      <div class="sidebar-header">
        <h2 class="sidebar-title">📚 Quick Actions</h2>
        <button
          class="settings-button"
          @click="toggleUserContext"
          :disabled="isLoading"
          title="Toggle user settings"
        >
          ⚙️
        </button>
      </div>

      <!-- User Context Panel -->
      <div v-if="showUserContext" class="user-context-panel">
        <form class="context-form">
          <div class="context-field">
            <label for="email">📧 Email</label>
            <input
              id="email"
              v-model="userContext.email"
              type="email"
              placeholder="dev@company.com"
              :disabled="isLoading"
            />
          </div>
          <div class="context-field">
            <label for="role">👤 Role</label>
            <select id="role" v-model="userContext.role" :disabled="isLoading">
              <option value="developer">Developer</option>
              <option value="pm">Product Manager</option>
              <option value="designer">Designer</option>
              <option value="qa">QA Engineer</option>
            </select>
          </div>
          <div class="context-field">
            <label for="team">🏢 Team</label>
            <select id="team" v-model="userContext.team" :disabled="isLoading">
              <option value="backend">Backend</option>
              <option value="frontend">Frontend</option>
              <option value="devops">DevOps</option>
              <option value="mobile">Mobile</option>
            </select>
          </div>
        </form>
      </div>

      <!-- Actions List -->
      <div class="sidebar-content">
        <div class="actions-list">
          <button
            v-for="(action, index) in quickActions"
            :key="index"
            class="action-item"
            @click="sendQuickAction(action)"
            :disabled="isLoading"
          >
            <span class="action-icon">{{ getActionIcon(index) }}</span>
            <span class="action-text">{{ action }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Chat Section -->
    <div class="chat-wrapper">
      <div class="chat-section">
        <!-- Header -->
        <div class="header">
          <div class="header-content">
            <div>
              <h1>🤖 Team Assistant</h1>
              <p>AI-powered team knowledge assistant</p>
            </div>
            <button
              class="new-discussion-button"
              @click="startNewDiscussion"
              :disabled="isLoading"
              title="Start new discussion"
            >
              ➕ New Discussion
            </button>
          </div>
        </div>

        <!-- Messages -->
        <div class="messages" ref="messagesContainer">
          <div
            v-for="msg in messages"
            :key="msg.id"
            :class="['message', msg.role]"
          >
            <div
              v-if="msg.role === 'assistant'"
              class="message-icon assistant-icon"
            >
              🤖
            </div>
            <div class="message-bubble">
              <div class="message-content" v-html="renderMarkdown(msg.content)"></div>

              <!-- Metadata for assistant messages -->
              <div v-if="msg.metadata && msg.role === 'assistant'" class="metadata">
                <!-- Sources -->
                <div v-if="msg.metadata.sources && msg.metadata.sources.length > 0" class="metadata-row">
                  <span class="badge sources-badge">
                    📚 Sources ({{ msg.metadata.sources.length }})
                  </span>
                  <ul class="sources-list">
                    <li v-for="source in msg.metadata.sources" :key="source">
                      {{ source }}
                    </li>
                  </ul>
                </div>

                <!-- Tools Used -->
                <div v-if="msg.metadata.toolsUsed && msg.metadata.toolsUsed.length > 0" class="metadata-row">
                  <span class="badge tools-badge">
                    🔧 Tools ({{ msg.metadata.toolsUsed.length }})
                  </span>
                  <ul class="tools-list">
                    <li v-for="tool in msg.metadata.toolsUsed" :key="tool">
                      {{ tool }}
                    </li>
                  </ul>
                </div>

                <!-- Confidence Score & Query Type & Response Time -->
                <div class="metadata-row">
                  <span
                    v-if="msg.metadata.confidenceScore !== undefined"
                    :class="['badge', getConfidenceBadgeClass(msg.metadata.confidenceScore)]"
                  >
                    {{ getConfidenceIcon(msg.metadata.confidenceScore) }}
                    {{ getConfidenceLabel(msg.metadata.confidenceScore) }}
                    ({{ msg.metadata.confidenceScore.toFixed(2) }})
                  </span>

                  <span v-if="msg.metadata.queryType" class="badge query-type">
                    {{ msg.metadata.queryType }}
                  </span>

                  <span v-if="msg.metadata.responseTimeMs" class="badge response-time">
                    ⏱️ {{ formatResponseTime(msg.metadata.responseTimeMs) }}
                  </span>
                </div>
              </div>

              <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
            </div>
            <div v-if="msg.role === 'user'" class="message-icon user-icon">
              👤
            </div>
          </div>

          <!-- Loading indicator -->
          <div v-if="isLoading" class="message assistant loading">
            <div class="message-icon assistant-icon">🤖</div>
            <div class="message-bubble">
              <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
              <div class="loading-text">AI is thinking...</div>
            </div>
          </div>
        </div>

        <!-- Input Area -->
        <div class="input-area">
          <div v-if="error" class="error-message">
            ❌ {{ error }}
          </div>

          <form @submit.prevent="sendMessage" class="input-form">
            <input
              v-model="currentQuery"
              type="text"
              placeholder="Type your question..."
              :disabled="isLoading"
              class="chat-input"
            />
            <button
              type="submit"
              :disabled="isLoading || !currentQuery.trim()"
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
import { ref, reactive, nextTick, onMounted, watch } from 'vue';
import { TeamAssistantService } from '../services/teamAssistantService';
import { marked } from 'marked';
import type {
  TeamAssistantMessage,
  UserContext,
  TeamAssistantResponse,
} from '../types/team-assistant';
import {
  getConfidenceLevel,
  formatResponseTime as formatResponseTimeUtil,
} from '../types/team-assistant';

// State
const messages = ref<TeamAssistantMessage[]>([]);
const currentQuery = ref('');
const isLoading = ref(false);
const error = ref('');
const messagesContainer = ref<HTMLElement | null>(null);
const showUserContext = ref(false);

// User context
const userContext = reactive<UserContext>({
  email: 'dev@company.com',
  role: 'developer',
  team: 'backend',
});

// Session ID (persistent for this session)
const sessionId = ref<string>('session-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9));

// Quick actions
const quickActions = [
  'How does authentication work?',
  'What are the critical bugs?',
  'Show project architecture',
  'What should I work on today?',
  'Explain the payment flow',
  'List open GitHub issues',
];

// Get icon for quick action
const getActionIcon = (index: number): string => {
  const icons = ['🔐', '🐛', '🏗️', '📋', '💳', '📝'];
  return icons[index] || '💡';
};

// Configure marked for safe HTML rendering
marked.setOptions({
  breaks: true,
  gfm: true,
});

// Render Markdown to HTML
const renderMarkdown = (content: string): string => {
  try {
    return marked.parse(content) as string;
  } catch {
    return content;
  }
};

// Toggle user context panel
const toggleUserContext = () => {
  showUserContext.value = !showUserContext.value;
};

// Start new discussion
const startNewDiscussion = () => {
  if (isLoading.value) return;

  // Clear messages
  messages.value = [];

  // Generate new session ID
  sessionId.value = 'session-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9);

  // Clear localStorage
  localStorage.removeItem('teamAssistantMessages');

  // Clear error
  error.value = '';

  console.log('Started new discussion with session ID:', sessionId.value);
};

// Scroll to bottom
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
    }
  });
};

// Format time
const formatTime = (timestamp: Date): string => {
  return timestamp.toLocaleTimeString('en-US', {
    hour: '2-digit',
    minute: '2-digit',
  });
};

// Get confidence badge class
const getConfidenceBadgeClass = (score: number): string => {
  const level = getConfidenceLevel(score);
  if (level.color === 'success') return 'confidence-high';
  if (level.color === 'warning') return 'confidence-medium';
  return 'confidence-low';
};

// Get confidence icon
const getConfidenceIcon = (score: number): string => {
  return getConfidenceLevel(score).icon;
};

// Get confidence label
const getConfidenceLabel = (score: number): string => {
  return getConfidenceLevel(score).label;
};

// Format response time
const formatResponseTime = (ms: number): string => {
  return formatResponseTimeUtil(ms);
};

// Send quick action
const sendQuickAction = (action: string) => {
  currentQuery.value = action;
  sendMessage();
};

// Send message
const sendMessage = async () => {
  if (!currentQuery.value.trim() || isLoading.value) return;

  const userMessage = currentQuery.value;
  currentQuery.value = '';
  error.value = '';

  // Add user message to UI
  const userMsg: TeamAssistantMessage = {
    id: 'msg-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9),
    role: 'user',
    content: userMessage,
    timestamp: new Date(),
  };
  messages.value.push(userMsg);
  scrollToBottom();

  isLoading.value = true;

  try {
    // Send query to API
    const response: TeamAssistantResponse = await TeamAssistantService.sendQuery({
      userEmail: userContext.email,
      query: userMessage,
      sessionId: sessionId.value,
    });

    // Add assistant message to UI
    const assistantMsg: TeamAssistantMessage = {
      id: 'msg-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9),
      role: 'assistant',
      content: response.answer,
      timestamp: new Date(response.timestamp),
      metadata: {
        sources: response.sources,
        toolsUsed: response.toolsUsed,
        confidenceScore: response.confidenceScore,
        queryType: response.queryType,
        responseTimeMs: response.responseTimeMs,
        actions: response.actions,
      },
    };
    messages.value.push(assistantMsg);
    scrollToBottom();
  } catch (err: any) {
    error.value = err.message || 'An error occurred';
    console.error('Error sending message:', err);
  } finally {
    isLoading.value = false;
  }
};

// Load from localStorage on mount
onMounted(() => {
  // Load user context from localStorage
  const savedContext = localStorage.getItem('teamAssistantUserContext');
  if (savedContext) {
    try {
      const parsed = JSON.parse(savedContext);
      Object.assign(userContext, parsed);
    } catch (e) {
      console.error('Failed to parse saved user context:', e);
    }
  }

  // Load messages from localStorage
  const savedMessages = localStorage.getItem('teamAssistantMessages');
  if (savedMessages) {
    try {
      const parsed = JSON.parse(savedMessages);
      // Convert timestamp strings back to Date objects
      messages.value = parsed.map((msg: any) => ({
        ...msg,
        timestamp: new Date(msg.timestamp),
      }));
      scrollToBottom();
    } catch (e) {
      console.error('Failed to parse saved messages:', e);
    }
  }

  console.log('Team Assistant initialized with session ID:', sessionId.value);
});

// Save to localStorage when context or messages change
watch(
  userContext,
  (newContext) => {
    localStorage.setItem('teamAssistantUserContext', JSON.stringify(newContext));
  },
  { deep: true }
);

watch(
  messages,
  (newMessages) => {
    // Save last 50 messages
    const toSave = newMessages.slice(-50);
    localStorage.setItem('teamAssistantMessages', JSON.stringify(toSave));
  },
  { deep: true }
);
</script>

<style scoped lang="scss">
@use '../styles/team-assistant-chat';
</style>

