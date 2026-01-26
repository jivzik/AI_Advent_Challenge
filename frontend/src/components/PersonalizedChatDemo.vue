<template>
  <div class="personalized-demo-container">
    <div class="demo-header">
      <h1>🤖 Персонализированный AI Ассистент</h1>
      <p class="demo-subtitle">Демонстрация адаптации ответов под профиль пользователя</p>
    </div>

    <!-- Profile Card -->
    <div v-if="profile" class="profile-card">
      <div class="profile-header">
        <span class="profile-icon">👤</span>
        <div class="profile-info">
          <h3>{{ profile.name }}</h3>
          <div class="profile-details">
            <span class="badge">{{ profile.expertiseLevel }}</span>
            <span class="tech-stack">{{ profile.techStack.join(', ') }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="loading" class="profile-skeleton">
      <div class="skeleton-header"></div>
      <div class="skeleton-text"></div>
    </div>

    <!-- Toggle Switch -->
    <div class="toggle-section">
      <label class="toggle-label">
        <input
          type="checkbox"
          v-model="useProfile"
          class="toggle-checkbox"
          :disabled="loading"
        />
        <span class="toggle-switch"></span>
        <span class="toggle-text">Использовать профиль пользователя</span>
      </label>
    </div>

    <!-- Split View: Two Chats Side by Side -->
    <div class="chat-comparison">
      <!-- Without Profile Chat -->
      <div class="chat-column without-profile">
        <div class="column-header">
          <h3>❌ Без персонализации</h3>
          <p>Общие ответы для всех</p>
        </div>
        <div class="messages-area" ref="messagesWithoutContainer">
          <div
            v-for="(msg, idx) in messagesWithout"
            :key="idx"
            :class="['message', msg.role]"
          >
            <div class="message-content">
              <div class="message-role">{{ msg.role === 'user' ? '👤 Вы' : '🤖 AI' }}</div>
              <div class="message-text" v-html="renderMarkdown(msg.content)"></div>
              <div v-if="msg.role === 'assistant' && msg.interactionId" class="feedback-buttons">
                <button
                  @click="handleFeedback(msg.interactionId!, 1)"
                  class="feedback-btn thumbs-up"
                  :disabled="msg.feedbackGiven"
                >
                  👍
                </button>
                <button
                  @click="handleFeedback(msg.interactionId!, -1)"
                  class="feedback-btn thumbs-down"
                  :disabled="msg.feedbackGiven"
                >
                  👎
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- With Profile Chat -->
      <div class="chat-column with-profile">
        <div class="column-header">
          <h3>✅ С персонализацией</h3>
          <p>Адаптировано под ваш профиль</p>
        </div>
        <div class="messages-area" ref="messagesWithContainer">
          <div
            v-for="(msg, idx) in messagesWith"
            :key="idx"
            :class="['message', msg.role]"
          >
            <div class="message-content">
              <div class="message-role">{{ msg.role === 'user' ? '👤 Вы' : '🤖 AI' }}</div>
              <div v-if="msg.usedProfile" class="profile-badge">✅ Использован ваш профиль</div>
              <div class="message-text" v-html="renderMarkdown(msg.content)"></div>
              <div v-if="msg.role === 'assistant' && msg.interactionId" class="feedback-buttons">
                <button
                  @click="handleFeedback(msg.interactionId!, 1)"
                  class="feedback-btn thumbs-up"
                  :disabled="msg.feedbackGiven"
                >
                  👍
                </button>
                <button
                  @click="handleFeedback(msg.interactionId!, -1)"
                  class="feedback-btn thumbs-down"
                  :disabled="msg.feedbackGiven"
                >
                  👎
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Input Area -->
    <div class="input-area">
      <textarea
        v-model="currentMessage"
        @keydown.enter.exact.prevent="handleSend"
        placeholder="Введите ваш запрос... (Enter для отправки)"
        class="message-input"
        :disabled="loading"
        rows="3"
      ></textarea>
      <button
        @click="handleSend"
        class="send-button"
        :disabled="loading || !currentMessage.trim()"
      >
        {{ loading ? '⏳ Отправка...' : '📤 Отправить' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue';
import { marked } from 'marked';
import type { UserProfile } from '../types/personalization.types';
import { getProfile, sendMessage, submitFeedback } from '../services/personalizationService';

// Configure marked
marked.setOptions({
  breaks: true,
  gfm: true
});

// State
const profile = ref<UserProfile | null>(null);
const useProfile = ref(false);
const currentMessage = ref('');
const loading = ref(false);

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  usedProfile?: boolean;
  interactionId?: number;
  feedbackGiven?: boolean;
}

const messagesWithout = ref<ChatMessage[]>([]);
const messagesWith = ref<ChatMessage[]>([]);

// Refs for scroll containers
const messagesWithoutContainer = ref<HTMLElement | null>(null);
const messagesWithContainer = ref<HTMLElement | null>(null);

// Methods
async function loadProfile() {
  loading.value = true;
  try {
    profile.value = await getProfile('default-user');
  } catch (error) {
    console.error('Failed to load profile:', error);
  } finally {
    loading.value = false;
  }
}

async function handleSend() {
  if (!currentMessage.value.trim() || loading.value) return;

  const userMessage = currentMessage.value.trim();
  currentMessage.value = '';
  loading.value = true;

  try {
    // Send both requests
    const [responseWithout, responseWith] = await Promise.all([
      sendMessage('default-user', userMessage, false),
      sendMessage('default-user', userMessage, true)
    ]);

    // Add user messages
    messagesWithout.value.push({
      role: 'user',
      content: userMessage
    });
    messagesWith.value.push({
      role: 'user',
      content: userMessage
    });

    // Add AI responses
    messagesWithout.value.push({
      role: 'assistant',
      content: responseWithout.response,
      usedProfile: responseWithout.usedProfile,
      interactionId: responseWithout.interactionId
    });
    messagesWith.value.push({
      role: 'assistant',
      content: responseWith.response,
      usedProfile: responseWith.usedProfile,
      interactionId: responseWith.interactionId
    });

    // Scroll to bottom
    await nextTick();
    scrollToBottom();
  } catch (error) {
    console.error('Failed to send message:', error);
  } finally {
    loading.value = false;
  }
}

async function handleFeedback(interactionId: number, feedback: number) {
  try {
    await submitFeedback(interactionId, feedback);

    // Mark feedback as given
    const markFeedback = (messages: ChatMessage[]) => {
      const msg = messages.find(m => m.interactionId === interactionId);
      if (msg) msg.feedbackGiven = true;
    };

    markFeedback(messagesWithout.value);
    markFeedback(messagesWith.value);
  } catch (error) {
    console.error('Failed to submit feedback:', error);
  }
}

function renderMarkdown(content: string): string {
  return marked.parse(content) as string;
}

function scrollToBottom() {
  if (messagesWithoutContainer.value) {
    messagesWithoutContainer.value.scrollTop = messagesWithoutContainer.value.scrollHeight;
  }
  if (messagesWithContainer.value) {
    messagesWithContainer.value.scrollTop = messagesWithContainer.value.scrollHeight;
  }
}

// Lifecycle
onMounted(() => {
  loadProfile();
});
</script>

<style scoped lang="scss">
@use '../styles/variables' as *;

.personalized-demo-container {
  max-width: 1600px;
  margin: 0 auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  height: 100vh;
  overflow: hidden;
}

.demo-header {
  text-align: center;

  h1 {
    font-size: 2rem;
    color: $text-dark;
    margin-bottom: 8px;
  }

  .demo-subtitle {
    color: $text-muted;
    font-size: 1rem;
  }
}

// Profile Card
.profile-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

  .profile-header {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .profile-icon {
    font-size: 3rem;
  }

  .profile-info {
    flex: 1;

    h3 {
      margin: 0 0 8px 0;
      color: $text-dark;
    }

    .profile-details {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }

    .badge {
      background: $primary-color;
      color: white;
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 0.85rem;
      font-weight: 500;
    }

    .tech-stack {
      color: $text-muted;
      font-size: 0.9rem;
    }
  }
}

.profile-skeleton {
  background: $bg-light;
  border-radius: 12px;
  padding: 20px;

  .skeleton-header {
    height: 24px;
    width: 200px;
    background: linear-gradient(90deg, #e0e0e0 25%, #f0f0f0 50%, #e0e0e0 75%);
    background-size: 200% 100%;
    animation: loading 1.5s infinite;
    border-radius: 4px;
    margin-bottom: 12px;
  }

  .skeleton-text {
    height: 16px;
    width: 300px;
    background: linear-gradient(90deg, #e0e0e0 25%, #f0f0f0 50%, #e0e0e0 75%);
    background-size: 200% 100%;
    animation: loading 1.5s infinite;
    border-radius: 4px;
  }
}

@keyframes loading {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

// Toggle Section
.toggle-section {
  display: flex;
  justify-content: center;
  padding: 16px;
  background: $bg-light;
  border-radius: 8px;

  .toggle-label {
    display: flex;
    align-items: center;
    gap: 12px;
    cursor: pointer;
    user-select: none;

    .toggle-checkbox {
      display: none;
    }

    .toggle-switch {
      position: relative;
      width: 48px;
      height: 24px;
      background: #ccc;
      border-radius: 12px;
      transition: background 0.3s;

      &::after {
        content: '';
        position: absolute;
        top: 2px;
        left: 2px;
        width: 20px;
        height: 20px;
        background: white;
        border-radius: 50%;
        transition: left 0.3s;
      }
    }

    .toggle-checkbox:checked + .toggle-switch {
      background: $success-color;

      &::after {
        left: 26px;
      }
    }

    .toggle-text {
      font-weight: 500;
      color: $text-dark;
    }
  }
}

// Chat Comparison
.chat-comparison {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  flex: 1;
  overflow: hidden;

  @media (max-width: 968px) {
    grid-template-columns: 1fr;
  }
}

.chat-column {
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

  &.without-profile {
    .column-header {
      background: linear-gradient(135deg, #9e9e9e 0%, #757575 100%);
    }
  }

  &.with-profile {
    .column-header {
      background: linear-gradient(135deg, #4caf50 0%, #388e3c 100%);
    }
  }

  .column-header {
    padding: 16px;
    color: white;

    h3 {
      margin: 0 0 4px 0;
      font-size: 1.1rem;
    }

    p {
      margin: 0;
      font-size: 0.85rem;
      opacity: 0.9;
    }
  }

  .messages-area {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
}

// Messages
.message {
  display: flex;

  &.user {
    justify-content: flex-end;

    .message-content {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
    }
  }

  &.assistant {
    justify-content: flex-start;

    .message-content {
      background: $bg-light;
      color: $text-dark;
    }
  }

  .message-content {
    max-width: 80%;
    padding: 12px 16px;
    border-radius: 12px;

    .message-role {
      font-weight: 600;
      font-size: 0.85rem;
      margin-bottom: 8px;
      opacity: 0.8;
    }

    .profile-badge {
      display: inline-block;
      background: rgba(76, 175, 80, 0.2);
      color: $success-color;
      padding: 4px 8px;
      border-radius: 6px;
      font-size: 0.75rem;
      font-weight: 600;
      margin-bottom: 8px;
    }

    .message-text {
      line-height: 1.6;

      :deep(pre) {
        background: rgba(0, 0, 0, 0.05);
        padding: 12px;
        border-radius: 6px;
        overflow-x: auto;
        margin: 8px 0;
      }

      :deep(code) {
        font-family: 'Courier New', monospace;
        font-size: 0.9em;
      }

      :deep(p) {
        margin: 0 0 8px 0;

        &:last-child {
          margin-bottom: 0;
        }
      }
    }

    .feedback-buttons {
      display: flex;
      gap: 8px;
      margin-top: 8px;

      .feedback-btn {
        background: transparent;
        border: 1px solid rgba(0, 0, 0, 0.1);
        border-radius: 6px;
        padding: 4px 12px;
        cursor: pointer;
        transition: all 0.2s;

        &:hover:not(:disabled) {
          background: rgba(0, 0, 0, 0.05);
          transform: scale(1.1);
        }

        &:disabled {
          opacity: 0.5;
          cursor: not-allowed;
        }
      }
    }
  }
}

// Input Area
.input-area {
  display: flex;
  gap: 12px;
  background: white;
  padding: 16px;
  border-radius: 12px;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.1);

  .message-input {
    flex: 1;
    padding: 12px;
    border: 2px solid $border-light;
    border-radius: 8px;
    font-family: inherit;
    font-size: 1rem;
    resize: none;
    transition: border-color 0.3s;

    &:focus {
      outline: none;
      border-color: $primary-color;
    }

    &:disabled {
      background: $bg-light;
      cursor: not-allowed;
    }
  }

  .send-button {
    padding: 12px 24px;
    background: $primary-gradient;
    color: white;
    border: none;
    border-radius: 8px;
    font-weight: 600;
    cursor: pointer;
    transition: transform 0.2s, opacity 0.3s;

    &:hover:not(:disabled) {
      transform: translateY(-2px);
    }

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
      transform: none;
    }
  }
}
</style>
