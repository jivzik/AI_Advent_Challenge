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
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';

interface ResponseMetrics {
  inputTokens: number | null;
  outputTokens: number | null;
  totalTokens: number | null;
  cost: number | null;
  responseTimeMs: number | null;
  model: string | null;
  provider: string | null;
}

interface Props {
  metrics: ResponseMetrics | null | undefined;
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
.metrics-card {
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  border: 2px solid #3498db;
  border-radius: 12px;
  padding: 16px;
  margin: 16px 0;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  font-size: 14px;
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 6px 12px rgba(0, 0, 0, 0.15);
    transform: translateY(-2px);
  }
}

.metrics-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  cursor: pointer;
  user-select: none;
}

.metrics-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #2c3e50;
  font-size: 16px;

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
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  border: 1px solid rgba(52, 152, 219, 0.3);
  transition: all 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.95);
    border-color: #3498db;
  }

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

