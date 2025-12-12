<template>
  <div v-if="metrics" class="metrics-card">
    <div class="metrics-header">
      <div class="metrics-title">
        <span class="metrics-icon">📊</span>
        <span>Response Metrics</span>
      </div>
      <button v-if="!isCollapsed" @click="toggleCollapse" class="collapse-button" title="Collapse">
        ▼
      </button>
      <button v-else @click="toggleCollapse" class="collapse-button" title="Expand">
        ▶
      </button>
    </div>

    <div v-if="!isCollapsed" class="metrics-content">
      <!-- Model & Provider Info -->
      <div class="metrics-row">
        <div class="metrics-item">
          <span class="metrics-label">🤖 Model</span>
          <span class="metrics-value">{{ metrics.model || 'Unknown' }}</span>
        </div>
        <div class="metrics-item">
          <span class="metrics-label">🔌 Provider</span>
          <span class="metrics-value">{{ metrics.provider || 'Unknown' }}</span>
        </div>
      </div>

      <!-- Tokens -->
      <div class="metrics-row">
        <div class="metrics-item">
          <span class="metrics-label">📥 Input Tokens</span>
          <span class="metrics-value">{{ metrics.inputTokens ?? '—' }}</span>
        </div>
        <div class="metrics-item">
          <span class="metrics-label">📤 Output Tokens</span>
          <span class="metrics-value">{{ metrics.outputTokens ?? '—' }}</span>
        </div>
        <div class="metrics-item">
          <span class="metrics-label">📊 Total Tokens</span>
          <span class="metrics-value">{{ metrics.totalTokens ?? '—' }}</span>
        </div>
      </div>

      <!-- Cost -->
      <div v-if="metrics.cost !== null && metrics.cost !== undefined" class="metrics-row cost-row">
        <div class="metrics-item highlight">
          <span class="metrics-label">💰 Cost</span>
          <span class="metrics-value cost-value">${{ formatCost(metrics.cost) }}</span>
        </div>
      </div>

      <!-- Response Time -->
      <div v-if="metrics.responseTimeMs" class="metrics-row">
        <div class="metrics-item">
          <span class="metrics-label">⏱️ Response Time</span>
          <span class="metrics-value">{{ metrics.responseTimeMs }}ms</span>
        </div>
      </div>

      <!-- Progress Bar for Tokens -->
      <div v-if="metrics.totalTokens" class="metrics-progress">
        <div class="progress-label">Token Distribution</div>
        <div class="progress-bar-container">
          <div
              class="progress-bar input-bar"
              :style="{ width: inputPercentage + '%' }"
              :title="`Input: ${metrics.inputTokens}`"
          ></div>
          <div
              class="progress-bar output-bar"
              :style="{ width: outputPercentage + '%' }"
              :title="`Output: ${metrics.outputTokens}`"
          ></div>
        </div>
        <div class="progress-labels">
          <span class="progress-label-input">📥 {{ metrics.inputTokens }}</span>
          <span class="progress-label-output">📤 {{ metrics.outputTokens }}</span>
        </div>
      </div>
      <div v-if="compressionInfo && compressionInfo.compressed" class="compression-section">
        <div class="compression-header">
          <span class="compression-icon">🗜️</span>
          <span class="compression-title">History Compression</span>
        </div>

        <div class="compression-stats-grid">
          <div class="compression-stat full">
            <div class="compression-stat-icon">📚</div>
            <div class="compression-stat-content">
              <div class="compression-stat-label">Full History</div>
              <div class="compression-stat-value">{{ compressionInfo.fullHistorySize }}</div>
              <div class="compression-stat-unit">messages</div>
            </div>
          </div>

          <div class="compression-arrow">→</div>

          <div class="compression-stat compressed">
            <div class="compression-stat-icon">✨</div>
            <div class="compression-stat-content">
              <div class="compression-stat-label">Compressed</div>
              <div class="compression-stat-value">{{ compressionInfo.compressedHistorySize }}</div>
              <div class="compression-stat-unit">messages</div>
            </div>
          </div>
        </div>

        <div class="compression-badges">
          <div class="compression-badge savings">
            <span class="badge-icon">💾</span>
            <span class="badge-text">{{ compressionInfo.messagesSaved }} msgs saved ({{ compressionInfo.compressionRatio }})</span>
          </div>
          <div class="compression-badge tokens">
            <span class="badge-icon">⚡</span>
            <span class="badge-text">~{{ compressionInfo.estimatedTokensSaved }} tokens saved</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import type {CompressionInfo, ResponseMetrics} from "../types/types.ts";

interface Props {
  metrics: ResponseMetrics | null | undefined;
  compressionInfo?: CompressionInfo | null;
}

const props = defineProps<Props>();

const isCollapsed = ref(false);

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value;
};

const formatCost = (cost: number): string => {
  return cost.toFixed(6);
};

const inputPercentage = computed(() => {
  if (!props.metrics?.inputTokens || !props.metrics?.totalTokens) return 0;
  return (props.metrics.inputTokens / props.metrics.totalTokens) * 100;
});

const outputPercentage = computed(() => {
  if (!props.metrics?.outputTokens || !props.metrics?.totalTokens) return 0;
  return (props.metrics.outputTokens / props.metrics.totalTokens) * 100;
});
</script>

<style scoped lang="scss">
@use '../styles/mixins' as *;

.metrics-card {
  // ⭐ Используем тот же mixin что и ConversationSidebar!
  @include sidebar-card;

  padding: 16px;
  margin: 16px 0;
  font-size: 14px;
}

.metrics-header {
  @include sidebar-card-header;
  cursor: pointer;
  user-select: none;

  .metrics-icon {
    font-size: 18px;
  }
}

.collapse-button {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 18px;
  color: #3498db;
  padding: 4px 8px;
  transition: transform 0.2s ease;

  &:hover {
    transform: scale(1.2);
  }
}

.metrics-content {
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from {
    opacity: 0;
    max-height: 0;
    overflow: hidden;
  }
  to {
    opacity: 1;
    max-height: 500px;
  }
}

.metrics-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 12px;

  &.cost-row {
    background: rgba(46, 204, 113, 0.1);
    padding: 12px;
    border-radius: 8px;
    border-left: 4px solid #2ecc71;
  }
}

.metrics-item {
  // ⭐ Используем mixin для единого стиля!
  @include sidebar-card-item;

  &.highlight {
    background: rgba(46, 204, 113, 0.2);
    border-color: #2ecc71;
  }
}

.metrics-label {
  font-size: 12px;
  color: #7f8c8d;
  text-transform: uppercase;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.metrics-value {
  font-size: 14px;
  font-weight: 500;
  color: #2c3e50;
  word-break: break-word;

  &.cost-value {
    color: #27ae60;
    font-size: 16px;
    font-weight: 600;
  }
}

.metrics-progress {
  margin-top: 16px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  border: 1px solid rgba(52, 152, 219, 0.3);
}

.progress-label {
  font-size: 12px;
  color: #7f8c8d;
  text-transform: uppercase;
  font-weight: 600;
  margin-bottom: 8px;
  letter-spacing: 0.5px;
}

.progress-bar-container {
  display: flex;
  height: 24px;
  background: #ecf0f1;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 8px;
  box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.1);
}

.progress-bar {
  transition: width 0.6s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 600;
  color: white;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);

  &.input-bar {
    background: linear-gradient(90deg, #3498db 0%, #2980b9 100%);
  }

  &.output-bar {
    background: linear-gradient(90deg, #2ecc71 0%, #27ae60 100%);
  }
}

.progress-labels {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #34495e;
  font-weight: 500;
}

.progress-label-input,
.progress-label-output {
  padding: 2px 4px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 4px;
}

.compression-section {
  margin-top: 20px;
  padding: 16px;
  border-top: 2px dashed rgba(52, 152, 219, 0.4);
  background: rgba(255, 255, 255, 0.3);
  border-radius: 8px;
  animation: slideIn 0.4s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.compression-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(52, 152, 219, 0.2);

  .compression-icon {
    font-size: 24px;
  }

  .compression-title {
    font-size: 15px;
    font-weight: 700;
    color: #2c3e50;
    text-transform: uppercase;
    letter-spacing: 0.8px;
  }
}

.compression-stats-grid {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 16px;
  align-items: stretch;
  margin-bottom: 16px;
}

.compression-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px 12px;
  border-radius: 10px;
  transition: all 0.3s ease;
  min-height: 90px;

  &.full {
    background: linear-gradient(135deg, rgba(52, 152, 219, 0.15) 0%, rgba(41, 128, 185, 0.08) 100%);
    border: 2px solid rgba(52, 152, 219, 0.4);
  }

  &.compressed {
    background: linear-gradient(135deg, rgba(46, 204, 113, 0.25) 0%, rgba(39, 174, 96, 0.15) 100%);
    border: 2px solid rgba(46, 204, 113, 0.6);
  }

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 6px 12px rgba(0, 0, 0, 0.15);
  }
}

.compression-stat-icon {
  font-size: 32px;
  margin-bottom: 4px;
}

.compression-stat-content {
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.compression-stat-label {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  color: #7f8c8d;
  letter-spacing: 0.6px;
}

.compression-stat-value {
  font-size: 28px;
  font-weight: 800;
  color: #2c3e50;
  line-height: 1;
}

.compression-stat-unit {
  font-size: 11px;
  color: #95a5a6;
  font-weight: 600;
  margin-top: 2px;
}

.compression-arrow {
  font-size: 32px;
  color: #27ae60;
  font-weight: bold;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.3);
    opacity: 0.7;
  }
}

.compression-badges {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.compression-badge {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.2s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);

  &.savings {
    background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
    color: #1e40af;
    border: 2px solid #3b82f6;

    &:hover {
      background: linear-gradient(135deg, #bfdbfe 0%, #93c5fd 100%);
      transform: translateX(3px);
      box-shadow: 0 4px 8px rgba(59, 130, 246, 0.3);
    }
  }

  &.tokens {
    background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
    color: #92400e;
    border: 2px solid #f59e0b;

    &:hover {
      background: linear-gradient(135deg, #fde68a 0%, #fcd34d 100%);
      transform: translateX(3px);
      box-shadow: 0 4px 8px rgba(245, 158, 11, 0.3);
    }
  }

  .badge-icon {
    font-size: 18px;
    flex-shrink: 0;
  }

  .badge-text {
    flex: 1;
    line-height: 1.4;
  }
}

@media (max-width: 768px) {
  .metrics-card {
    padding: 12px;
    margin: 12px 0;
  }

  .metrics-row {
    grid-template-columns: 1fr;
    gap: 8px;
    margin-bottom: 8px;
  }

  .metrics-item {
    padding: 6px;
  }

  .metrics-title {
    font-size: 14px;
  }

  .metrics-label {
    font-size: 11px;
  }

  .metrics-value {
    font-size: 13px;
  }

  /* Compression responsive */
  .compression-stats-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .compression-arrow {
    transform: rotate(90deg);
    font-size: 28px;
    padding: 8px 0;
  }

  .compression-stat {
    padding: 14px 10px;
    min-height: 80px;
  }

  .compression-stat-icon {
    font-size: 28px;
  }

  .compression-stat-value {
    font-size: 24px;
  }

  .compression-badge {
    padding: 10px 12px;
    font-size: 12px;
  }

  .badge-icon {
    font-size: 16px !important;
  }
}


@media (max-width: 768px) {
  .metrics-card {
    padding: 12px;
    margin: 12px 0;
  }

  .metrics-row {
    grid-template-columns: 1fr;
    gap: 8px;
    margin-bottom: 8px;
  }

  .metrics-item {
    padding: 6px;
  }

  .metrics-title {
    font-size: 14px;
  }

  .metrics-label {
    font-size: 11px;
  }

  .metrics-value {
    font-size: 13px;
  }
}
</style>