<template>
  <div class="app-container">
    <!-- Mode Selector Tabs -->
    <div class="mode-selector">
      <button
        @click="currentMode = 'normal'"
        :class="{ active: currentMode === 'normal' }"
      >
        💬 Normal Chat
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

const currentMode = ref<'normal' | 'meta'>('meta'); // Default to meta-prompt
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
  background: #f5f5f5;
}

.app-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

/* Mode Selector */
.mode-selector {
  display: flex;
  background: white;
  border-bottom: 2px solid #e0e0e0;
  padding: 10px 20px;
  gap: 10px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}

.mode-selector button {
  padding: 12px 24px;
  border: 2px solid #e0e0e0;
  background: white;
  border-radius: 8px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
  transition: all 0.3s;
  color: #666;
}

.mode-selector button:hover {
  border-color: #007bff;
  color: #007bff;
  background: #f0f8ff;
}

.mode-selector button.active {
  background: #007bff;
  color: white;
  border-color: #007bff;
  box-shadow: 0 2px 8px rgba(0,123,255,0.3);
}
</style>


