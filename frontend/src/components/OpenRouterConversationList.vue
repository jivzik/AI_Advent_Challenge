<template>
  <div class="openrouter-conversation-list">
    <div v-if="conversations.length === 0" class="empty-state">
      <div class="empty-icon">💬</div>
      <p class="empty-text">No conversations yet</p>
      <p class="empty-subtext">Start a new conversation to see it here</p>
    </div>

    <div v-else class="conversations-container">
      <div
          v-for="conversation in conversations"
          :key="conversation.conversationId"
          class="conversation-item"
          :class="{ active: conversation.conversationId === activeConversationId }"
          @click="handleSelect(conversation.conversationId)"
      >
        <div class="conversation-icon">💬</div>
        <div class="conversation-content">
          <div class="conversation-text">{{ truncateMessage(conversation.firstMessage) }}</div>
          <div class="conversation-meta">
            <span class="message-count">{{ conversation.messageCount }} msgs</span>
            <span v-if="conversation.hasCompression" class="compression-icon" title="Compressed">🗜️</span>
          </div>
        </div>
        <div class="conversation-right">
          <span class="conversation-time">{{ formatTime(conversation.lastMessageTime) }}</span>
          <button
              class="delete-button"
              @click.stop="handleDelete(conversation.conversationId)"
              title="Delete conversation"
          >
            🗑️
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
interface ConversationItem {
  conversationId: string;
  firstMessage: string;
  lastMessageTime: string;
  messageCount: number;
  hasCompression?: boolean;
}

defineProps<{
  conversations: ConversationItem[];
  activeConversationId: string | null;
}>();

const emit = defineEmits<{
  select: [conversationId: string];
  delete: [conversationId: string];
}>();

const truncateMessage = (message: string): string => {
  const max = 45;
  return message.length > max ? message.substring(0, max) + '...' : message;
};

const formatTime = (timestamp: string): string => {
  const date = new Date(timestamp);
  const now = new Date();
  const diff = now.getTime() - date.getTime();

  // Менее 1 часа: минуты
  if (diff < 3600000) {
    const mins = Math.floor(diff / 60000);
    return `${mins}m ago`;
  }

  // Менее 1 дня: часы
  if (diff < 86400000) {
    const hours = Math.floor(diff / 3600000);
    return `${hours}h ago`;
  }

  // Менее 1 недели: дни
  if (diff < 604800000) {
    const days = Math.floor(diff / 86400000);
    return `${days}d ago`;
  }

  // Иначе: дата
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
};

const handleSelect = (conversationId: string) => {
  emit('select', conversationId);
};

const handleDelete = (conversationId: string) => {
  if (confirm('Delete this conversation?')) {
    emit('delete', conversationId);
  }
};
</script>

<style scoped lang="scss">
@use '../styles/variables' as *;

.openrouter-conversation-list {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
  gap: $spacing-sm;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  padding: $spacing-lg;
  color: #95a5a6;
  background: rgba(255, 255, 255, 0.5);
  border-radius: $radius-lg;
  border: 2px dashed rgba(52, 152, 219, 0.2);
  gap: $spacing-sm;
}

.empty-icon {
  font-size: 32px;
  opacity: 0.5;
}

.empty-text {
  font-size: $font-size-sm;
  font-weight: 600;
  margin: 0;
  color: #7f8c8d;
}

.empty-subtext {
  font-size: $font-size-xs;
  margin: 0;
  color: #bdc3c7;
}

.conversations-container {
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  gap: $spacing-xs;
  padding-right: 4px;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: rgba(52, 152, 219, 0.3);
    border-radius: 3px;

    &:hover {
      background: rgba(52, 152, 219, 0.5);
    }
  }
}

.conversation-item {
  display: flex;
  align-items: center;
  padding: $spacing-md;
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
  color: #2c3e50 !important;
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

