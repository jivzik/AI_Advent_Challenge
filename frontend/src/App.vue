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
    </div>

    <!-- Component Display -->
    <ChatInterface v-if="currentMode === 'normal'" />
    <MetaPromptChat v-else-if="currentMode === 'meta'" />
    <ReminderDashboard v-else-if="currentMode === 'reminder'" />
    <OpenRouterChat v-else-if="currentMode === 'openrouter'" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import ChatInterface from './components/ChatInterface.vue';
import MetaPromptChat from './components/MetaPromptChat.vue';
import ReminderDashboard from './components/ReminderDashboard.vue';
import OpenRouterChat from './components/OpenRouterChat.vue';

const currentMode = ref<'normal' | 'meta' | 'reminder' | 'openrouter'>('normal'); // Default to normal
</script>


