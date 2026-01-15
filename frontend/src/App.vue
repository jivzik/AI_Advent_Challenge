<template>
  <div class="app-container">
    <!-- Mode Selector Tabs -->
    <div class="mode-selector">
      <button
        @click="currentMode = 'normal'"
        :class="{ active: currentMode === 'normal' }"
      >
        💬 System/User-Prompt
      </button>
      <button
        @click="currentMode = 'meta'"
        :class="{ active: currentMode === 'meta' }"
      >
        🎯 Meta-Prompt (Universal AI)
      </button>
      <button
        @click="currentMode = 'reminder'"
        :class="{ active: currentMode === 'reminder' }"
      >
        📋 Reminder Dashboard
      </button>
      <button
        @click="currentMode = 'openrouter'"
        :class="{ active: currentMode === 'openrouter' }"
      >
        🔧 OpenRouter Tools
      </button>
      <button
        @click="currentMode = 'rag-upload'"
        :class="{ active: currentMode === 'rag-upload' }"
      >
        📤 RAG Upload
      </button>
      <button
        @click="currentMode = 'rag-search'"
        :class="{ active: currentMode === 'rag-search' }"
      >
        🔍 RAG Search
      </button>
      <button
        @click="currentMode = 'rag-library'"
        :class="{ active: currentMode === 'rag-library' }"
      >
        📚 RAG Library
      </button>
      <button
          @click="currentMode = 'support'"
          :class="{ active: currentMode === 'support' }"
      >
        Support Chat
      </button>
    </div>

    <!-- Component Display -->
    <ChatInterface v-if="currentMode === 'normal'" />
    <MetaPromptChat v-else-if="currentMode === 'meta'" />
    <ReminderDashboard v-else-if="currentMode === 'reminder'" />
    <OpenRouterChat v-else-if="currentMode === 'openrouter'" />
    <DocumentUpload v-else-if="currentMode === 'rag-upload'" @navigate="handleNavigate" />
    <DocumentSearch v-else-if="currentMode === 'rag-search'" @navigate="handleNavigate" />
    <DocumentLibrary v-else-if="currentMode === 'rag-library'" @navigate="handleNavigate" />
    <SupportChat v-else-if="currentMode === 'support'" @navigate="handleNavigate" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import ChatInterface from './components/ChatInterface.vue';
import MetaPromptChat from './components/MetaPromptChat.vue';
import ReminderDashboard from './components/ReminderDashboard.vue';
import OpenRouterChat from './components/OpenRouterChat.vue';
import DocumentUpload from './components/DocumentUpload.vue';
import DocumentSearch from './components/DocumentSearch.vue';
import DocumentLibrary from './components/DocumentLibrary.vue';
import SupportChat from './components/SupportChat.vue';


type Mode = 'normal' | 'meta' | 'reminder' | 'openrouter' | 'rag-upload' | 'rag-search' | 'rag-library' |  'support';
const currentMode = ref<Mode>('normal');

const handleNavigate = (mode: string) => {
  currentMode.value = mode as Mode;
};
</script>


