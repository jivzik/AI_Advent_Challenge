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

