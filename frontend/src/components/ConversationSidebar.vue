<template>
  <div class="conversation-sidebar">
    <div class="sidebar-header">
      <h2 class="sidebar-title">💬 Conversations</h2>
      <button
          class="new-conversation-button"
          @click="handleNewConversation"
          title="New Conversation"
      >
        ➕
      </button>
    </div>

    <div class="sidebar-content">
      <div v-if="isLoading" class="loading-state">
        <div class="loading-spinner"></div>
        <span>Loading...</span>
      </div>

      <ConversationList
          v-else
          :conversations="conversations"
          :activeConversationId="activeConversationId"
          @select="handleSelect"
          @delete="handleDelete"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import ConversationList from './ConversationList.vue';
import { ChatService } from '../services/chatService';
import type { ConversationSummary } from '../types/types';

const props = defineProps<{
  activeConversationId: string | null;
}>();

const activeConversationId = computed(() => props.activeConversationId);

const emit = defineEmits<{
  select: [conversationId: string];
  delete: [conversationId: string];
  newConversation: [];
}>();

const conversations = ref<ConversationSummary[]>([]);
const isLoading = ref(true);

const loadConversations = async () => {
  isLoading.value = true;
  try {
    const response = await ChatService.getConversations();
    // ⭐ Extract conversations array from response object
    conversations.value = response.conversations || [];
  } catch (error) {
    console.error('Failed to load conversations:', error);
    conversations.value = [];
  } finally {
    isLoading.value = false;
  }
};

const handleSelect = (conversationId: string) => {
  emit('select', conversationId);
};

const handleDelete = async (conversationId: string) => {
  if (!confirm('Delete this conversation?')) {
    return;
  }

  try {
    await ChatService.deleteConversation(conversationId);
    conversations.value = conversations.value.filter(
        c => c.conversationId !== conversationId
    );
    emit('delete', conversationId);
  } catch (error) {
    console.error('Failed to delete conversation:', error);
    alert('Failed to delete conversation');
  }
};

const handleNewConversation = () => {
  emit('newConversation');
};

const refresh = () => {
  loadConversations();
};

defineExpose({ refresh });

onMounted(() => {
  loadConversations();
});
</script>

<style scoped lang="scss">
@use '../styles/variables' as *;
@use '../styles/mixins' as *;

.conversation-sidebar {
  // ⭐ Используем новый mixin для единого стиля!
  @include sidebar-card;

  display: flex;
  flex-direction: column;
  width: 280px;
  min-width: 240px;
  max-width: 320px;
  height: 100%;
  overflow: hidden;

  // Убираем hover transform для sidebar (только для MetricsCard)
  &:hover {
    transform: none;
  }
}

.sidebar-header {
  @include sidebar-card-header;
  padding: $spacing-lg;
  border-bottom: 2px solid rgba(52, 152, 219, 0.3);

  // Фиолетовый gradient остаётся только для header
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: $text-white;

  // Для header отключаем стандартные цвета из mixin
  margin-bottom: 0;
}

.sidebar-title {
  font-size: $font-size-lg;
  font-weight: 600;
  color: $text-white;
  margin: 0;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.new-conversation-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: $radius-md;
  cursor: pointer;
  font-size: $font-size-lg;
  transition: all $transition-fast;
  color: $text-white;

  &:hover {
    background: rgba(255, 255, 255, 0.35);
    border-color: rgba(255, 255, 255, 0.5);
    transform: scale(1.1);
    box-shadow: 0 2px 8px rgba(255, 255, 255, 0.2);
  }
}

.sidebar-content {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
  padding: $spacing-md;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: $spacing-xl;
  gap: $spacing-md;
  color: #7f8c8d;
  background: rgba(255, 255, 255, 0.5);
  border-radius: $radius-lg;
  border: 1px dashed rgba(52, 152, 219, 0.3);
  margin: $spacing-md 0;
}

.loading-spinner {
  width: 28px;
  height: 28px;
  border: 3px solid rgba(52, 152, 219, 0.2);
  border-top-color: #3498db;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>