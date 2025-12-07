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
    </div>

    <!-- Component Display -->
    <ChatInterface v-if="currentMode === 'normal'" />
    <MetaPromptChat v-else-if="currentMode === 'meta'" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import ChatInterface from './components/ChatInterface.vue';
import MetaPromptChat from './components/MetaPromptChat.vue';

const currentMode = ref<'normal' | 'meta'>('normal'); // Default to meta-prompt
</script>


