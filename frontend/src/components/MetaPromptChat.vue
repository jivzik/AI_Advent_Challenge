<template>
  <div class="chat-container">
    <!-- Header matching ChatInterface.vue -->
    <div class="chat-header">
      <div class="header-content">
        <div>
          <h1>Universal AI Assistant</h1>
          <p v-if="currentPhase === 'init'">What would you like to create or plan today?</p>
          <p v-else-if="currentPhase === 'collecting' && goalType">{{ goalType }} - {{ completionPercentage }}% complete</p>
          <p v-else-if="currentPhase === 'complete'">Done!</p>
        </div>
        <div class="header-controls">
          <button
            v-if="currentPhase === 'collecting' && completionPercentage >= 70"
            @click="forceComplete"
            class="clear-button"
            :disabled="isLoading"
          >
            Complete Now
          </button>
          <button
            v-if="currentPhase !== 'init'"
            @click="startOver"
            class="clear-button"
            :disabled="isLoading"
          >
            New Goal
          </button>
        </div>
      </div>
    </div>

    <!-- Progress Bar (only during collecting) -->
    <div v-if="currentPhase === 'collecting' && metaData" class="progress-section">
      <div class="progress-header">
        <h3>Progress</h3>
        <span class="progress-fraction">{{ fieldsCollected }} / {{ fieldsTotal }} Fields</span>
      </div>

      <div class="progress-bar-container">
        <div
          class="progress-bar"
          :style="{ width: completionPercentage + '%' }"
          :class="getProgressClass(completionPercentage)"
        >
          {{ completionPercentage }}%
        </div>
      </div>

      <!-- Missing Fields (optional, only if < 5 missing) -->
      <div v-if="missingFields.length > 0 && missingFields.length <= 5" class="missing-fields">
        <p><strong>Still needed:</strong></p>
        <div class="field-chips">
          <span v-for="field in missingFields" :key="field" class="field-chip">
            {{ formatFieldName(field) }}
          </span>
        </div>
      </div>
    </div>

    <!-- Chat Messages matching ChatInterface.vue -->
    <div class="chat-messages" v-if="currentPhase !== 'complete'" ref="messagesContainer">
      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        :class="['message', msg.role]"
      >
        <div class="message-content">
          <div class="message-role">{{ msg.role === 'user' ? 'You' : 'AI Assistant' }}</div>
          <div class="message-text">{{ msg.content }}</div>
        </div>
      </div>

      <div v-if="isLoading" class="message assistant loading">
        <div class="message-content">
          <div class="message-role">AI Assistant</div>
          <div class="typing-indicator">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>
    </div>

    <!-- Input Section matching ChatInterface.vue -->
    <div class="chat-input-container" v-if="currentPhase !== 'complete'">
      <form @submit.prevent="sendMessage" class="chat-input-form">
        <input
          v-model="currentMessage"
          type="text"
          placeholder="Your answer..."
          :disabled="isLoading"
          class="chat-input"
          ref="inputField"
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

    <!-- Final Result Display -->
    <div v-if="currentPhase === 'complete' && finalOutput" class="result-section">
      <div class="result-header">
        <h2 class="result-title">{{ finalOutput.title }}</h2>
        <p class="result-summary">{{ finalOutput.summary }}</p>
      </div>

      <div class="result-content">
        <div class="json-header">
          <span class="json-badge">JSON Result</span>
          <button @click="copyResult" class="copy-button" title="Copy JSON">
            Copy
          </button>
          <button @click="toggleJsonView" class="toggle-button" title="Toggle view">
            {{ showRawJson ? 'Tree View' : 'Raw View' }}
          </button>
        </div>
        <pre v-if="!showRawJson" class="json-formatted" v-html="formatJsonHtml(finalOutput)"></pre>
        <div v-else class="json-tree" v-html="createJsonTree(finalOutput)"></div>
      </div>

      <div class="action-buttons">
        <button @click="downloadResult" class="btn-action">
          Download
        </button>
        <button @click="startOver" class="btn-action secondary">
          New Goal
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue';
import { ChatService } from '../services/chatService';
import { JsonFormatter } from '../utils/jsonFormatter';

interface Message {
  role: 'user' | 'assistant';
  content: string;
}

interface FinalOutputSection {
  heading: string;
  content?: string;
  items?: string[];
  data?: Record<string, any>;
  subsections?: FinalOutputSection[];
}

interface FinalOutput {
  title: string;
  summary: string;
  sections: FinalOutputSection[];
  actionable_items?: string[];
  additional_info?: Record<string, any>;
}

interface MetaPromptResponse {
  phase: 'init' | 'collecting' | 'complete';
  goal_type?: string;
  fields_total?: number;
  fields_collected?: number;
  completion_percentage?: number;
  missing_fields?: string[];
  assistant_message?: string;
  collected_data?: any;
  final_output?: FinalOutput;
}

// State
const messages = ref<Message[]>([]);
const currentMessage = ref('');
const isLoading = ref(false);
const conversationId = ref(`meta_${Date.now()}`);
const metaData = ref<MetaPromptResponse | null>(null);
const messagesContainer = ref<HTMLElement | null>(null);
const inputField = ref<HTMLInputElement | null>(null);
const showRawJson = ref(false);

// Computed
const currentPhase = computed(() => metaData.value?.phase || 'init');
const goalType = computed(() => metaData.value?.goal_type);
const fieldsTotal = computed(() => metaData.value?.fields_total || 0);
const fieldsCollected = computed(() => metaData.value?.fields_collected || 0);
const completionPercentage = computed(() => metaData.value?.completion_percentage || 0);
const missingFields = computed(() => metaData.value?.missing_fields || []);
const finalOutput = computed(() => metaData.value?.final_output);

// Methods
const sendMessage = async () => {
  const userMessage = currentMessage.value.trim();

  if (!userMessage || isLoading.value) {
    return;
  }

  messages.value.push({ role: 'user', content: userMessage });
  currentMessage.value = '';
  isLoading.value = true;

  try {
    const response = await ChatService.sendMessageWithOptions({
      message: userMessage,
      userId: 'web_user',
      conversationId: conversationId.value,
      jsonMode: true,
      jsonSchema: 'meta_prompt'
    });

    // Parse the JSON response
    const data: MetaPromptResponse = JSON.parse(response.reply);
    metaData.value = data;

    // Add assistant response to chat (if phase is not complete)
    if (data.phase !== 'complete' && data.assistant_message) {
      messages.value.push({ role: 'assistant', content: data.assistant_message });
    }

    // Scroll to bottom
    await nextTick();
    scrollToBottom();

    // Focus input if still collecting
    if (data.phase !== 'complete') {
      inputField.value?.focus();
    }

  } catch (error) {
    console.error('Error sending message:', error);
    messages.value.push({
      role: 'assistant',
      content: 'Sorry, there was an error. Please try again.'
    });
  } finally {
    isLoading.value = false;
  }
};

const forceComplete = async () => {
  if (isLoading.value) {
    return;
  }

  const completeMessage = 'Please create the result now with the information collected so far.';
  messages.value.push({ role: 'user', content: completeMessage });
  isLoading.value = true;

  try {
    const response = await ChatService.sendMessageWithOptions({
      message: completeMessage,
      userId: 'web_user',
      conversationId: conversationId.value,
      jsonMode: true,
      jsonSchema: 'meta_prompt'
    });

    // Parse the JSON response
    const data: MetaPromptResponse = JSON.parse(response.reply);
    metaData.value = data;

    // Add assistant response to chat (if phase is not complete)
    if (data.phase !== 'complete' && data.assistant_message) {
      messages.value.push({ role: 'assistant', content: data.assistant_message });
    }

    // Scroll to bottom
    await nextTick();
    scrollToBottom();

  } catch (error) {
    console.error('Error forcing completion:', error);
    messages.value.push({
      role: 'assistant',
      content: 'Sorry, there was an error. Please try again.'
    });
  } finally {
    isLoading.value = false;
  }
};

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
  }
};

const formatFieldName = (field: string): string => {
  // Convert snake_case to readable format
  return field
    .split('_')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
};

const getProgressClass = (percent: number): string => {
  if (percent < 30) return 'progress-low';
  if (percent < 70) return 'progress-medium';
  return 'progress-high';
};

const formatJsonHtml = (obj: any): string => {
  return JsonFormatter.toHtml(JSON.stringify(obj));
};

const createJsonTree = (obj: any): string => {
  return JsonFormatter.createTreeView(obj);
};

const toggleJsonView = () => {
  showRawJson.value = !showRawJson.value;
};

const copyResult = async () => {
  if (!finalOutput.value) return;

  try {
    // Convert structured output to JSON string
    const jsonString = JSON.stringify(finalOutput.value, null, 2);
    await navigator.clipboard.writeText(jsonString);
    alert('Result copied to clipboard!');
  } catch (error) {
    console.error('Copy failed:', error);
  }
};

const downloadResult = () => {
  if (!finalOutput.value) return;

  // Convert structured output to JSON
  const jsonString = JSON.stringify(finalOutput.value, null, 2);
  const blob = new Blob([jsonString], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `${goalType.value || 'result'}_${Date.now()}.json`;
  a.click();
  URL.revokeObjectURL(url);
};

const startOver = () => {
  messages.value = [];
  metaData.value = null;
  conversationId.value = `meta_${Date.now()}`;
  initConversation();
};

const initConversation = async () => {
  isLoading.value = true;
  try {
    const response = await ChatService.sendMessageWithOptions({
      message: 'Start',
      userId: 'web_user',
      conversationId: conversationId.value,
      jsonMode: true,
      jsonSchema: 'meta_prompt'
    });

    const data: MetaPromptResponse = JSON.parse(response.reply);
    metaData.value = data;

    if (data.assistant_message) {
      messages.value.push({ role: 'assistant', content: data.assistant_message });
    }

    await nextTick();
    inputField.value?.focus();
  } catch (error) {
    console.error('Error initializing conversation:', error);
  } finally {
    isLoading.value = false;
  }
};

// Lifecycle
onMounted(() => {
  initConversation();
});
</script>

<style scoped>
/* Matching ChatInterface.vue design */
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

.header-controls {
  display: flex;
  gap: 1rem;
  align-items: center;
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

/* Progress Section */
.progress-section {
  background: rgba(102, 126, 234, 0.1);
  padding: 1rem 1.5rem;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.progress-header h3 {
  margin: 0;
  font-size: 0.9rem;
  color: #667eea;
  font-weight: 600;
}

.progress-fraction {
  font-size: 0.85rem;
  color: #666;
}

.progress-bar-container {
  width: 100%;
  height: 8px;
  background: rgba(0,0,0,0.1);
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 12px;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  transition: width 0.5s ease;
  border-radius: 4px;
}

.missing-fields {
  margin-top: 8px;
}

.missing-fields p {
  margin: 0 0 6px 0;
  font-size: 0.85rem;
  color: #666;
}

.field-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.field-chip {
  background: rgba(102, 126, 234, 0.15);
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 0.8rem;
  color: #667eea;
}

/* Chat Messages */
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

/* Typing Indicator */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 0.5rem 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  background: #667eea;
  border-radius: 50%;
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
    opacity: 0.7;
  }
  30% {
    transform: translateY(-10px);
    opacity: 1;
  }
}

/* Input Container */
.chat-input-container {
  padding: 1rem 1.5rem;
  background: white;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.chat-input-form {
  display: flex;
  gap: 0.75rem;
}

.chat-input {
  flex: 1;
  padding: 0.875rem 1.25rem;
  border: 2px solid #e0e0e0;
  border-radius: 2rem;
  font-size: 1rem;
  outline: none;
  transition: all 0.2s;
  background: white;
}

.chat-input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
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
  border-radius: 2rem;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.send-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.send-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

/* Result Section */
.result-section {
  flex: 1;
  overflow-y: auto;
  padding: 2rem;
  background: white;
}

.result-header {
  text-align: left;
  margin-bottom: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 2px solid #e0e0e0;
}

.result-title {
  font-size: 2rem;
  color: #2c3e50;
  margin: 0 0 0.5rem 0;
  font-weight: 700;
}

.result-summary {
  color: #666;
  font-size: 1.1rem;
  margin: 0;
  line-height: 1.6;
}

.result-content {
  background: #1e1e1e;
  padding: 0;
  border-radius: 12px;
  margin-bottom: 2rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  overflow: hidden;
}

/* JSON Header */
.json-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  background: #2d2d2d;
  border-bottom: 1px solid #3d3d3d;
}

.json-badge {
  background: #667eea;
  color: white;
  padding: 0.25rem 0.75rem;
  border-radius: 0.5rem;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
}

.copy-button,
.toggle-button {
  background: rgba(255, 255, 255, 0.1);
  color: #ccc;
  border: 1px solid rgba(255, 255, 255, 0.2);
  padding: 0.4rem 0.8rem;
  border-radius: 0.5rem;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.2s;
}

.copy-button:hover,
.toggle-button:hover {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.4);
}

/* JSON Formatted View */
.json-formatted {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 1.5rem;
  margin: 0;
  overflow-x: auto;
  font-family: 'Courier New', monospace;
  font-size: 0.9rem;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
}

/* JSON Tree View */
.json-tree {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 1.5rem;
  overflow-x: auto;
  font-family: 'Courier New', monospace;
  font-size: 0.9rem;
  line-height: 1.6;
}

/* JSON Syntax Highlighting */
:deep(.json-key) {
  color: #9cdcfe;
  font-weight: 600;
}

:deep(.json-string) {
  color: #ce9178;
}

:deep(.json-number) {
  color: #b5cea8;
}

:deep(.json-boolean) {
  color: #569cd6;
}

:deep(.json-null) {
  color: #569cd6;
}

:deep(.json-bracket) {
  color: #ffd700;
}

:deep(.json-comma) {
  color: #d4d4d4;
}

.action-buttons {
  display: flex;
  gap: 1rem;
  justify-content: flex-start;
  flex-wrap: wrap;
}

.btn-action {
  padding: 0.875rem 2rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 2rem;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 600;
  transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.btn-action:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-action.secondary {
  background: #6c757d;
  box-shadow: 0 2px 8px rgba(108, 117, 125, 0.3);
}

.btn-action.secondary:hover {
  box-shadow: 0 4px 12px rgba(108, 117, 125, 0.4);
}

/* Responsive */
@media (max-width: 768px) {
  .chat-container {
    width: 95vw;
    height: 80vh;
    border-radius: 1rem;
  }

  .message-content {
    max-width: 85%;
  }

  .header-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-controls {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>

