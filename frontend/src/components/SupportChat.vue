<template>
  <div class="support-chat-container">
    <div class="chat-wrapper">
      <!-- Header -->
      <div class="chat-header">
        <div class="header-content">
          <div class="header-title">
            <h1>💬 Support Chat</h1>
            <p>AI-powered customer support with FAQ knowledge base</p>
          </div>
          <div class="header-actions" v-if="currentTicket">
            <span class="ticket-badge">
              🎫 {{ currentTicket.ticketNumber }}
            </span>
            <span
                class="status-badge"
                :class="currentTicket.status"
            >
              {{ currentTicket.status }}
            </span>
          </div>
        </div>
      </div>

      <!-- Ticket Info Panel (if ticket exists) -->
      <div v-if="currentTicket" class="ticket-info-panel">
        <div class="ticket-info-row">
          <span class="ticket-label">Category:</span>
          <span class="ticket-value">{{ currentTicket.category }}</span>
        </div>
        <div class="ticket-info-row">
          <span class="ticket-label">Priority:</span>
          <span
              class="priority-badge"
              :class="currentTicket.priority"
          >
            {{ currentTicket.priority }}
          </span>
        </div>
        <div class="ticket-info-row" v-if="currentTicket.orderId">
          <span class="ticket-label">Order ID:</span>
          <span class="ticket-value">{{ currentTicket.orderId }}</span>
        </div>
      </div>

      <!-- Messages Area -->
      <div class="messages-container" ref="messagesContainer">
        <div class="messages-list">
          <!-- Welcome Message -->
          <div v-if="messages.length === 0" class="welcome-message">
            <div class="welcome-icon">👋</div>
            <h2>Добро пожаловать в Support Chat!</h2>
            <p>Задайте свой вопрос, и AI ассистент поможет найти ответ в нашей базе знаний.</p>
            <div class="welcome-features">
              <div class="feature">📚 Поиск в FAQ</div>
              <div class="feature">🤖 AI Assistant</div>
              <div class="feature">⚡ Быстрые ответы</div>
            </div>
          </div>

          <!-- Message Items -->
          <div
              v-for="(message, index) in messages"
              :key="index"
              class="message-item"
              :class="message.role"
          >
            <div class="message-avatar">
              {{ message.role === 'user' ? '👤' : '🤖' }}
            </div>
            <div class="message-content">
              <div class="message-header">
                <span class="message-sender">
                  {{ message.role === 'user' ? 'Вы' : 'AI Assistant' }}
                </span>
                <span class="message-time">{{ formatTime(message.timestamp) }}</span>
              </div>
              <div class="message-text" v-html="formatMessage(message.content)"></div>

              <!-- AI Metadata -->
              <div v-if="message.role === 'assistant' && message.metadata" class="message-metadata">
                <!-- Confidence Score -->
                <div v-if="message.metadata.confidence" class="metadata-item">
                  <span class="metadata-label">Confidence:</span>
                  <div class="confidence-bar">
                    <div
                        class="confidence-fill"
                        :style="{ width: (message.metadata.confidence * 100) + '%' }"
                        :class="getConfidenceClass(message.metadata.confidence)"
                    ></div>
                  </div>
                  <span class="confidence-value">
                    {{ (message.metadata.confidence * 100).toFixed(0) }}%
                  </span>
                </div>

                <!-- Sources -->
                <div v-if="message.metadata.sources && message.metadata.sources.length > 0" class="metadata-item sources">
                  <span class="metadata-label">📚 Sources:</span>
                  <div class="sources-list">
                    <span
                        v-for="(source, idx) in message.metadata.sources"
                        :key="idx"
                        class="source-tag"
                    >
                      {{ source }}
                    </span>
                  </div>
                </div>

                <!-- Escalation Notice -->
                <div v-if="message.metadata.needsHuman" class="escalation-notice">
                  ⚠️ <strong>Эскалация:</strong> {{ message.metadata.escalationReason }}
                </div>
              </div>
            </div>
          </div>

          <!-- Loading Indicator -->
          <div v-if="isLoading" class="message-item assistant loading">
            <div class="message-avatar">🤖</div>
            <div class="message-content">
              <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>

          <!-- Error Message -->
          <div v-if="error" class="error-message">
            ⚠️ {{ error }}
          </div>
        </div>
      </div>

      <!-- Input Area -->
      <div class="input-container">
        <div class="input-wrapper">
          <textarea
              v-model="userInput"
              @keydown.enter.exact.prevent="sendMessage"
              @keydown.shift.enter="() => {}"
              placeholder="Введите ваш вопрос..."
              class="message-input"
              rows="3"
              :disabled="isLoading"
          ></textarea>
          <button
              @click="sendMessage"
              :disabled="isLoading || !userInput.trim()"
              class="send-button"
              title="Send message (Enter)"
          >
            {{ isLoading ? '⏳' : '📤' }}
          </button>
        </div>

        <!-- Quick Actions (optional) -->
        <div class="quick-actions" v-if="messages.length === 0">
          <button
              @click="askQuickQuestion('Почему не работает авторизация?')"
              class="quick-action-btn"
          >
            🔐 Проблемы с входом
          </button>
          <button
              @click="askQuickQuestion('Где мой счет на заказ?')"
              class="quick-action-btn"
          >
            📄 Счет на заказ
          </button>
          <button
              @click="askQuickQuestion('Как отследить доставку?')"
              class="quick-action-btn"
          >
            🚚 Отслеживание доставки
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue';
import { SupportChatService } from '../services/supportChatService';

// Types
interface Message {
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
  metadata?: {
    confidence?: number;
    sources?: string[];
    needsHuman?: boolean;
    escalationReason?: string;
  };
}

interface TicketInfo {
  ticketNumber: string;
  status: string;
  category: string;
  priority: string;
  orderId?: string;
  messageCount: number;
}

// State
const userInput = ref('');
const messages = ref<Message[]>([]);
const isLoading = ref(false);
const error = ref('');
const messagesContainer = ref<HTMLElement | null>(null);
const currentTicket = ref<TicketInfo | null>(null);

// User info (в реальном приложении получать из auth)
const userEmail = ref('test@company.ru');

// Props (если нужно передать ticketNumber извне)
const props = defineProps<{
  ticketNumber?: string;
  userEmail?: string;
}>();

// Methods
const sendMessage = async () => {
  if (!userInput.value.trim() || isLoading.value) return;

  const messageText = userInput.value.trim();
  userInput.value = '';
  error.value = '';

  // Add user message
  messages.value.push({
    role: 'user',
    content: messageText,
    timestamp: new Date().toISOString()
  });

  scrollToBottom();
  isLoading.value = true;

  try {
    // Send to support service
    const response = await SupportChatService.sendMessage({
      userEmail: props.userEmail || userEmail.value,
      message: messageText,
      ticketNumber: props.ticketNumber || currentTicket.value?.ticketNumber,
      category: 'other', // можно добавить выбор категории
      priority: 'medium'
    });

    // Update current ticket info
    currentTicket.value = {
      ticketNumber: response.ticketNumber,
      status: response.status,
      category: 'other', // TODO: получать из response
      priority: 'medium',
      messageCount: response.messageCount || 0
    };

    // Add assistant message
    messages.value.push({
      role: 'assistant',
      content: response.answer,
      timestamp: response.timestamp,
      metadata: {
        confidence: response.confidenceScore,
        sources: response.sources,
        needsHuman: response.needsHumanAgent,
        escalationReason: response.escalationReason
      }
    });

    scrollToBottom();
  } catch (err) {
    console.error('Error sending message:', err);
    error.value = 'Ошибка при отправке сообщения. Попробуйте еще раз.';

    // Remove user message on error
    messages.value.pop();
  } finally {
    isLoading.value = false;
  }
};

const askQuickQuestion = (question: string) => {
  userInput.value = question;
  sendMessage();
};

const formatTime = (timestamp: string): string => {
  const date = new Date(timestamp);
  return date.toLocaleTimeString('ru-RU', {
    hour: '2-digit',
    minute: '2-digit'
  });
};

const formatMessage = (content: string): string => {
  // Convert markdown-style formatting to HTML
  let formatted = content
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/`(.*?)`/g, '<code>$1</code>')
      .replace(/\n/g, '<br>');

  return formatted;
};

const getConfidenceClass = (confidence: number): string => {
  if (confidence >= 0.8) return 'high';
  if (confidence >= 0.6) return 'medium';
  return 'low';
};

const scrollToBottom = async () => {
  await nextTick();
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
  }
};

// Load ticket history if ticketNumber provided
onMounted(async () => {
  if (props.ticketNumber) {
    try {
      const history = await SupportChatService.getTicketHistory(props.ticketNumber);
      messages.value = history.map(msg => ({
        role: msg.senderType === 'customer' ? 'user' : 'assistant',
        content: msg.message,
        timestamp: msg.createdAt,
        metadata: msg.senderType === 'ai' ? {
          confidence: msg.confidenceScore,
          sources: msg.sources,
          needsHuman: false // history messages don't need escalation info
        } : undefined
      }));

      scrollToBottom();
    } catch (err) {
      console.error('Error loading ticket history:', err);
    }
  }
});
</script>

<style scoped lang="scss">
@use '../styles/support-chat';
</style>