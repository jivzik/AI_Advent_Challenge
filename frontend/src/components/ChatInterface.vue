<template>
  <div class="chat-container">
    <div class="chat-header">
      <div class="header-content">
        <div>
          <h1>AI Chat Agent</h1>
          <p>Powered by Perplexity AI</p>
        </div>
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
    <div class="chat-messages" ref="messagesContainer">
      <div 
        v-for="(msg, index) in messages" 
        :key="index" 
        :class="['message', msg.role]"
      >
        <div class="message-content">
          <div class="message-role">{{ msg.role === 'user' ? 'You' : 'AI Agent' }}</div>
          <div class="message-text">{{ msg.content }}</div>
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
import { ref, nextTick, onMounted } from 'vue';
import { ChatService } from '../services/chatService';

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

// Generate unique conversation ID for this session (persistent until page reload)
const conversationId = ref<string>('conv-' + Date.now() + '-' + Math.random().toString(36).substr(2, 9));
const userId = ref<string>('user-' + Date.now());

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
    // Send message with conversationId to maintain history
    const data = await ChatService.sendMessage(
      userMessage,
      userId.value,
      conversationId.value
    );

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
<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 66vh;
  width: 66vw;
  min-width: 320px;
  max-width: 900px;
  min-height: 400px;
  max-height: 900px;
  margin: auto;
  background: #f5f5f5;
  border-radius: 1.5rem;
  box-shadow: 0 4px 24px rgba(0,0,0,0.08);
}
.chat-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 1.5rem;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}
.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}
.header-content > div {
  text-align: left;
}
.chat-header h1 {
  margin: 0;
  font-size: 1.8rem;
  font-weight: 600;
}
.chat-header p {
  margin: 0.5rem 0 0;
  opacity: 0.9;
  font-size: 0.9rem;
}
.clear-button {
  background: rgba(255, 255, 255, 0.25);
  color: white;
  border: 2px solid rgba(255, 255, 255, 0.5);
  padding: 0.7rem 1.4rem;
  border-radius: 0.5rem;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.clear-button:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.35);
  border-color: rgba(255, 255, 255, 0.7);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
.clear-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.message {
  display: flex;
  animation: fadeIn 0.3s ease-in;
}
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
.message.user {
  justify-content: flex-end;
}
.message.assistant {
  justify-content: flex-start;
}
.message-content {
  max-width: 70%;
  padding: 1rem;
  border-radius: 1rem;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
}
.message.user .message-content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom-right-radius: 0.25rem;
}
.message.assistant .message-content {
  background: white;
  border-bottom-left-radius: 0.25rem;
}
.message-role {
  font-size: 0.75rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
  opacity: 0.8;
}
.message-text {
  word-wrap: break-word;
  line-height: 1.5;
}
.message-time {
  font-size: 0.7rem;
  margin-top: 0.5rem;
  opacity: 0.6;
}
.typing-indicator {
  display: flex;
  gap: 0.3rem;
  padding: 0.5rem 0;
}
.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #667eea;
  animation: typing 1.4s infinite;
}
.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}
.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-10px);
  }
}
.chat-input-container {
  background: white;
  padding: 1rem;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
}
.error-message {
  background: #fee;
  color: #c33;
  padding: 0.75rem;
  border-radius: 0.5rem;
  margin-bottom: 0.75rem;
  font-size: 0.9rem;
}
.chat-input-form {
  display: flex;
  gap: 0.75rem;
}
.chat-input {
  flex: 1;
  padding: 0.875rem;
  border: 2px solid #e0e0e0;
  border-radius: 0.5rem;
  font-size: 1rem;
  transition: border-color 0.3s;
}
.chat-input:focus {
  outline: none;
  border-color: #667eea;
}
.chat-input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}
.send-button {
  padding: 0.875rem 2rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 0.5rem;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s, opacity 0.3s;
}
.send-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}
.send-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
/* Scrollbar styling */
.chat-messages::-webkit-scrollbar {
  width: 8px;
}
.chat-messages::-webkit-scrollbar-track {
  background: #f1f1f1;
}
.chat-messages::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 4px;
}
.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #555;
}
</style>
