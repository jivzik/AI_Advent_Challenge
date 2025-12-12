<template>
  <div class="conversation-list">
    <!-- Today Group -->
    <div v-if="groupedConversations.today.length > 0" class="conversation-group">
      <div class="group-header">🕒 Today</div>
      <ConversationItem
        v-for="conv in groupedConversations.today"
        :key="conv.conversationId"
        :conversation="conv"
        :isActive="conv.conversationId === activeConversationId"
        @select="handleSelect"
        @delete="handleDelete"
      />
    </div>

    <!-- Yesterday Group -->
    <div v-if="groupedConversations.yesterday.length > 0" class="conversation-group">
      <div class="group-header">📅 Yesterday</div>
      <ConversationItem
        v-for="conv in groupedConversations.yesterday"
        :key="conv.conversationId"
        :conversation="conv"
        :isActive="conv.conversationId === activeConversationId"
        @select="handleSelect"
        @delete="handleDelete"
      />
    </div>

    <!-- Last Week Group -->
    <div v-if="groupedConversations.lastWeek.length > 0" class="conversation-group">
      <div class="group-header">📆 Last Week</div>
      <ConversationItem
        v-for="conv in groupedConversations.lastWeek"
        :key="conv.conversationId"
        :conversation="conv"
        :isActive="conv.conversationId === activeConversationId"
        @select="handleSelect"
        @delete="handleDelete"
      />
    </div>

    <!-- Older Group -->
    <div v-if="groupedConversations.older.length > 0" class="conversation-group">
      <div class="group-header">📦 Older</div>
      <ConversationItem
        v-for="conv in groupedConversations.older"
        :key="conv.conversationId"
        :conversation="conv"
        :isActive="conv.conversationId === activeConversationId"
        @select="handleSelect"
        @delete="handleDelete"
      />
    </div>

    <!-- Empty State -->
    <div v-if="isEmpty" class="empty-state">
      <div class="empty-icon">💬</div>
      <div class="empty-text">No conversations yet</div>
      <div class="empty-hint">Start a new conversation</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import ConversationItem from './ConversationItem.vue';
import { groupConversationsByDate } from '../types/types';
import type { ConversationSummary } from '../types/types';

const props = defineProps<{
  conversations: ConversationSummary[];
  activeConversationId: string | null;
}>();

const emit = defineEmits<{
  select: [conversationId: string];
  delete: [conversationId: string];
}>();

const groupedConversations = computed(() => {
  return groupConversationsByDate(props.conversations);
});

const isEmpty = computed(() => {
  const groups = groupedConversations.value;
  return (
    groups.today.length === 0 &&
    groups.yesterday.length === 0 &&
    groups.lastWeek.length === 0 &&
    groups.older.length === 0
  );
});

const handleSelect = (conversationId: string) => {
  emit('select', conversationId);
};

const handleDelete = (conversationId: string) => {
  emit('delete', conversationId);
};
</script>

<style scoped lang="scss">
@use '../styles/variables' as *;

.conversation-list {
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  flex: 1;
}

.conversation-group {
  margin-bottom: $spacing-md;
}

.group-header {
  font-size: 11px;
  text-transform: uppercase;
  color: $text-muted;
  opacity: 0.6;
  padding: 8px 12px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: $spacing-xl;
  text-align: center;
  flex: 1;
}

.empty-icon {
  font-size: 2.5rem;
  margin-bottom: $spacing-sm;
  opacity: 0.5;
}

.empty-text {
  font-size: $font-size-base;
  color: $text-muted;
  margin-bottom: $spacing-xs;
}

.empty-hint {
  font-size: $font-size-sm;
  color: $text-light;
}
</style>

