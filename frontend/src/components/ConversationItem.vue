<template>
  <div
      class="conversation-item"
      :class="{ active: isActive }"
      @click="handleSelect"
  >
    <div class="conversation-icon">💬</div>
    <div class="conversation-content">
      <div class="conversation-text">{{ truncatedMessage }}</div>
      <div class="conversation-meta">
        <span class="message-count">{{ conversation.messageCount }} msgs</span>
        <span v-if="conversation.hasCompression" class="compression-icon" title="Compressed">🗜️</span>
      </div>
    </div>
    <div class="conversation-right">
      <span class="conversation-time">{{ relativeTime }}</span>
      <button
          class="delete-button"
          @click.stop="handleDelete"
          title="Delete conversation"
      >
        🗑️
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { formatRelativeTime } from '../types/types';
import type { ConversationSummary } from '../types/types';

const props = defineProps<{
  conversation: ConversationSummary;
  isActive: boolean;
}>();

const emit = defineEmits<{
  select: [conversationId: string];
  delete: [conversationId: string];
}>();

const truncatedMessage = computed(() => {
  const msg = props.conversation.firstMessage || 'Empty conversation';
  return msg.length > 45 ? msg.substring(0, 45) + '...' : msg;
});

const relativeTime = computed(() => {
  return formatRelativeTime(props.conversation.lastMessageTime);
});

const handleSelect = () => {
  emit('select', props.conversation.conversationId);
};

const handleDelete = () => {
  emit('delete', props.conversation.conversationId);
};
</script>

<style scoped lang="scss">
@use '../styles/variables' as *;

.conversation-item {
  display: flex;
  align-items: center;
  padding: $spacing-md;
  margin-bottom: $spacing-xs;
  cursor: pointer;
  transition: all $transition-fast;
  gap: $spacing-sm;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(52, 152, 219, 0.2);
  border-radius: $radius-md;

  &:hover {
    background: rgba(255, 255, 255, 0.95);
    border-color: #3498db;
    box-shadow: 0 2px 8px rgba(52, 152, 219, 0.15);

    .delete-button {
      opacity: 1;
      visibility: visible;
    }
  }

  &.active {
    background: rgba(52, 152, 219, 0.15);
    border-color: #3498db;
    border-width: 2px;
    box-shadow: 0 2px 12px rgba(52, 152, 219, 0.2);

    .conversation-text {
      color: #2980b9 !important;
      font-weight: 600;
    }

    .conversation-icon {
      color: #3498db;
    }
  }
}

.conversation-icon {
  font-size: 18px;
  flex-shrink: 0;
  color: #7f8c8d;
  transition: color $transition-fast;
}

.conversation-content {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.conversation-text {
  font-size: $font-size-sm;
  font-weight: 500;
  color: #2c3e50 !important;  /* Force color! */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
  transition: color $transition-fast;
}

.conversation-meta {
  display: flex;
  align-items: center;
  gap: $spacing-xs;
  font-size: $font-size-xs;
}

.message-count {
  color: #95a5a6;
  font-size: $font-size-xs;
}

.compression-icon {
  font-size: $font-size-xs;
  color: #27ae60;
}

.conversation-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  flex-shrink: 0;
}

.conversation-time {
  font-size: $font-size-xs;
  color: #95a5a6;
  white-space: nowrap;
}

.delete-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  background: rgba(231, 76, 60, 0.1);
  border: 1px solid transparent;
  border-radius: $radius-sm;
  color: #e74c3c;
  font-size: $font-size-sm;
  cursor: pointer;
  opacity: 0;
  visibility: hidden;
  transition: all $transition-fast;
  padding: 0;

  &:hover {
    background: rgba(231, 76, 60, 0.2);
    border-color: rgba(231, 76, 60, 0.3);
    transform: scale(1.1);
  }

  &:focus {
    outline: none;
    opacity: 1;
    visibility: visible;
    box-shadow: 0 0 0 2px rgba(231, 76, 60, 0.3);
  }
}
</style>