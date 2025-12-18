<template>
  <div class="reminder-dashboard">
    <!-- Header -->
    <div class="dashboard-header">
      <div class="header-left">
        <h1>📋 Reminder Dashboard</h1>
        <p class="subtitle">Automatische Aufgaben-Zusammenfassungen</p>
      </div>
      <div class="header-right">
        <button
          class="trigger-button"
          @click="triggerManualReminder"
          :disabled="isLoading"
        >
          <span v-if="isLoading" class="spinner"></span>
          <span v-else>🔄</span>
          {{ isLoading ? 'Lädt...' : 'Jetzt aktualisieren' }}
        </button>
      </div>
    </div>

    <!-- Status Bar -->
    <div class="status-bar" v-if="status">
      <div class="status-item">
        <span class="status-icon" :class="{ active: status.schedulerEnabled }">●</span>
        <span>Scheduler {{ status.schedulerEnabled ? 'Aktiv' : 'Inaktiv' }}</span>
      </div>
      <div class="status-item">
        <span class="status-icon pending">⏳</span>
        <span>{{ status.pendingNotifications }} ausstehend</span>
      </div>
      <div class="status-item">
        <span class="status-icon">🕐</span>
        <span>Letzte: {{ formatDateTime(status.latestSummaryTime) }}</span>
      </div>
    </div>

    <!-- Latest Summary Card -->
    <div class="latest-summary" v-if="latestSummary">
      <div class="card-header">
        <span class="card-title">🌟 Neueste Zusammenfassung</span>
        <span class="priority-badge" :class="latestSummary.priority.toLowerCase()">
          {{ latestSummary.priority }}
        </span>
      </div>
      <div class="card-content">
        <h2>{{ latestSummary.title }}</h2>
        <div class="summary-meta">
          <span>📅 {{ formatDateTime(latestSummary.createdAt) }}</span>
          <span>📊 {{ latestSummary.itemsCount || 0 }} Items</span>
          <span>{{ getSummaryTypeIcon(latestSummary.summaryType) }} {{ latestSummary.summaryType }}</span>
        </div>
        <div class="summary-content markdown-content" v-html="renderMarkdown(latestSummary.content)"></div>

        <!-- Extracted Structured Data Sections -->
        <div class="structured-sections">
          <!-- Highlights Section -->
          <div class="structured-section highlights-section" v-if="extractHighlights(latestSummary.content).length > 0">
            <h3 class="section-header">🌟 Highlights</h3>
            <ul class="highlights-list">
              <li v-for="(highlight, idx) in extractHighlights(latestSummary.content)" :key="`h-${idx}`">
                {{ highlight }}
              </li>
            </ul>
          </div>

          <!-- Due Soon Section -->
          <div class="structured-section due-soon-section" v-if="extractDueSoon(latestSummary.content).length > 0">
            <h3 class="section-header">⏱️ Bald fällig</h3>
            <div class="task-list">
              <div v-for="(task, idx) in extractDueSoon(latestSummary.content)" :key="`ds-${idx}`" class="task-item">
                <span class="task-name">{{ task.name }}</span>
                <span class="task-due">{{ task.due }}</span>
              </div>
            </div>
          </div>

          <!-- Overdue Section -->
          <div class="structured-section overdue-section" v-if="extractOverdue(latestSummary.content).length > 0">
            <h3 class="section-header">⚠️ Überfällig</h3>
            <div class="task-list overdue-tasks">
              <div v-for="(task, idx) in extractOverdue(latestSummary.content)" :key="`o-${idx}`" class="task-item overdue-item">
                <span class="task-name">{{ task.name }}</span>
                <span class="task-due">{{ task.due }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- No Summary State -->
    <div class="empty-state" v-else-if="!isLoading">
      <span class="empty-icon">📭</span>
      <h3>Keine Zusammenfassungen vorhanden</h3>
      <p>Klicke "Jetzt aktualisieren" um eine neue Zusammenfassung zu erstellen.</p>
    </div>

    <!-- Summary History -->
    <div class="history-section" v-if="summaries.length > 1">
      <h3 class="section-title">📜 Verlauf</h3>
      <div class="summary-grid">
        <div
          v-for="summary in summaries.slice(1)"
          :key="summary.id"
          class="summary-card"
          @click="selectSummary(summary)"
          :class="{ selected: selectedSummary?.id === summary.id }"
        >
          <div class="card-header-small">
            <span class="priority-dot" :class="summary.priority.toLowerCase()"></span>
            <span class="card-title-small">{{ summary.title }}</span>
          </div>
          <div class="card-meta-small">
            <span>{{ formatDate(summary.createdAt) }}</span>
            <span>{{ summary.itemsCount || 0 }} Items</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Selected Summary Detail Modal -->
    <div class="modal-overlay" v-if="selectedSummary" @click.self="selectedSummary = null">
      <div class="modal-content">
        <div class="modal-header">
          <h2>{{ selectedSummary.title }}</h2>
          <button class="close-button" @click="selectedSummary = null">✕</button>
        </div>
        <div class="modal-meta">
          <span class="priority-badge" :class="selectedSummary.priority.toLowerCase()">
            {{ selectedSummary.priority }}
          </span>
          <span>📅 {{ formatDateTime(selectedSummary.createdAt) }}</span>
          <span>📊 {{ selectedSummary.itemsCount || 0 }} Items</span>
        </div>
        <div class="modal-body markdown-content" v-html="renderMarkdown(selectedSummary.content)"></div>

        <!-- Raw Data Section -->
        <details class="raw-data-section" v-if="selectedSummary.rawData">
          <summary>🔧 Rohdaten anzeigen</summary>
          <pre class="raw-data">{{ selectedSummary.rawData }}</pre>
        </details>
      </div>
    </div>

    <!-- Loading State -->
    <div class="loading-overlay" v-if="isLoading && !latestSummary">
      <div class="loading-spinner"></div>
      <p>Lade Zusammenfassungen...</p>
    </div>

    <!-- Error Message -->
    <div class="error-message" v-if="errorMessage">
      <span>❌ {{ errorMessage }}</span>
      <button @click="errorMessage = ''">✕</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { marked } from 'marked';
import { reminderService, type ReminderSummary, type ReminderStatus } from '../services/reminderService';

// State
const isLoading = ref(false);
const errorMessage = ref('');
const status = ref<ReminderStatus | null>(null);
const latestSummary = ref<ReminderSummary | null>(null);
const summaries = ref<ReminderSummary[]>([]);
const selectedSummary = ref<ReminderSummary | null>(null);

// Default user ID - can be made configurable
const userId = 'system';

// Load data on mount
onMounted(async () => {
  await loadData();
});

/**
 * Load all dashboard data
 */
async function loadData() {
  isLoading.value = true;
  errorMessage.value = '';

  try {
    // Load in parallel
    const [statusData, latestData, summariesData] = await Promise.all([
      reminderService.getStatus(),
      reminderService.getLatestSummary(),
      reminderService.getSummaries(userId)
    ]);

    status.value = statusData;
    latestSummary.value = latestData;
    summaries.value = summariesData;
  } catch (error: any) {
    console.error('Failed to load reminder data:', error);
    errorMessage.value = 'Fehler beim Laden der Daten: ' + (error.message || 'Unbekannter Fehler');
  } finally {
    isLoading.value = false;
  }
}

/**
 * Trigger manual reminder workflow
 */
async function triggerManualReminder() {
  isLoading.value = true;
  errorMessage.value = '';

  try {
    const newSummary = await reminderService.triggerReminder(userId);

    // Update local state
    latestSummary.value = newSummary;
    summaries.value = [newSummary, ...summaries.value];

    // Refresh status
    status.value = await reminderService.getStatus();
  } catch (error: any) {
    console.error('Failed to trigger reminder:', error);
    errorMessage.value = 'Fehler beim Erstellen der Zusammenfassung: ' + (error.message || 'Unbekannter Fehler');
  } finally {
    isLoading.value = false;
  }
}

/**
 * Select a summary to view details
 */
function selectSummary(summary: ReminderSummary) {
  selectedSummary.value = summary;
}

/**
 * Render markdown to HTML
 */
function renderMarkdown(content: string): string {
  if (!content) return '';
  return marked(content) as string;
}

/**
 * Format datetime for display
 */
function formatDateTime(dateStr: string): string {
  if (!dateStr || dateStr === 'never') return 'Nie';
  try {
    const date = new Date(dateStr);
    return date.toLocaleString('de-DE', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  } catch {
    return dateStr;
  }
}

/**
 * Format date only
 */
function formatDate(dateStr: string): string {
  if (!dateStr) return '';
  try {
    const date = new Date(dateStr);
    return date.toLocaleDateString('de-DE', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  } catch {
    return dateStr;
  }
}

/**
 * Get icon for summary type
 */
function getSummaryTypeIcon(type: string): string {
  const icons: Record<string, string> = {
    TASKS: '✅',
    CALENDAR: '📅',
    GENERAL: '📝',
    DAILY_DIGEST: '☀️',
    WEEKLY_DIGEST: '📆'
  };
  return icons[type] || '📋';
}

/**
 * Extrahiere Highlights aus dem Markdown-Content
 */
function extractHighlights(content: string): string[] {
  const highlights: string[] = [];
  if (!content) return highlights;

  const regex = /^## 🌟 Highlights\s*$([\s\S]*?)(?=^##|$)/m;
  const match = content.match(regex);

  if (match && match[1]) {
    const lines = match[1].split('\n');
    for (const line of lines) {
      const trimmed = line.trim();
      if (trimmed.startsWith('- ')) {
        highlights.push(trimmed.substring(2));
      }
    }
  }

  return highlights;
}

/**
 * Extrahiere "Due Soon" Aufgaben aus dem Markdown-Content
 */
function extractDueSoon(content: string): Array<{ name: string; due: string }> {
  const tasks: Array<{ name: string; due: string }> = [];
  if (!content) return tasks;

  const regex = /^## ⏱️ Bald fällig\s*$([\s\S]*?)(?=^##|$)/m;
  const match = content.match(regex);

  if (match && match[1]) {
    const lines = match[1].split('\n');
    let i = 0;
    while (i < lines.length) {
      const line = lines[i]?.trim() || '';
      if (line.startsWith('- **')) {
        // Extrahiere Task-Name
        const nameMatch = line.match(/- \*\*(.*?)\*\*/);
        if (nameMatch && nameMatch[1]) {
          const name = nameMatch[1];
          // Prüfe nächste Zeile für Due-Info
          let due = '';
          if (i + 1 < lines.length) {
            const dueLine = lines[i + 1]?.trim() || '';
            if (dueLine.startsWith('- Fällig:')) {
              due = dueLine.substring(9).trim();
              i++; // Überspringe die Due-Zeile
            }
          }
          tasks.push({ name, due: due || 'Keine Info' });
        }
      }
      i++;
    }
  }

  return tasks;
}

/**
 * Extrahiere "Overdue" Aufgaben aus dem Markdown-Content
 */
function extractOverdue(content: string): Array<{ name: string; due: string }> {
  const tasks: Array<{ name: string; due: string }> = [];
  if (!content) return tasks;

  const regex = /^## ⚠️ Überfällig\s*$([\s\S]*?)(?=^##|$)/m;
  const match = content.match(regex);

  if (match && match[1]) {
    const lines = match[1].split('\n');
    let i = 0;
    while (i < lines.length) {
      const line = lines[i]?.trim() || '';
      if (line.startsWith('- **')) {
        // Extrahiere Task-Name
        const nameMatch = line.match(/- \*\*(.*?)\*\*/);
        if (nameMatch && nameMatch[1]) {
          const name = nameMatch[1];
          // Prüfe nächste Zeile für Zeitangabe
          let due = '';
          if (i + 1 < lines.length) {
            const dueLine = lines[i + 1]?.trim() || '';
            if (dueLine.startsWith('- ')) {
              due = dueLine.substring(2).trim();
              i++; // Überspringe die Zeitzeile
            }
          }
          tasks.push({ name, due: due || 'Keine Info' });
        }
      }
      i++;
    }
  }

  return tasks;
}
</script>

<style scoped>
.reminder-dashboard {
  max-width: 100%;
  margin: 0;
  padding: 80px 12px 12px 12px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: linear-gradient(135deg, rgba(0, 0, 0, 0.95) 0%, rgba(20, 30, 60, 0.98) 100%);
  color: #333;
  min-height: 100vh;
  width: 100%;
  height: 100%;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  overflow-x: hidden;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
  z-index: 1;
}

.reminder-dashboard > * {
  width: 95%;
  max-width: 1400px;
  height: auto;
  margin: 6px auto;
}

/* Header */
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
  flex-shrink: 0;
}

.header-left h1 {
  font-size: 32px;
  margin: 0;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  color: #999;
  margin: 4px 0 0 0;
  font-size: 16px;
}

.trigger-button {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 16px;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.trigger-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4);
}

.trigger-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.spinner {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: white;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Status Bar */
.status-bar {
  display: flex;
  gap: 12px;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 6px;
  margin-bottom: 8px;
  flex-shrink: 0;
  font-size: 14px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  color: #666;
}

.status-icon {
  font-size: 8px;
  color: #999;
}

.status-icon.active {
  color: #4ade80;
}

.status-icon.pending {
  color: #fbbf24;
}

/* Latest Summary Card */
.latest-summary {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  border-radius: 12px;
  overflow: auto;
  margin-bottom: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  flex-shrink: 0;
  flex: 0 0 55%;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
}

/* Latest Summary Scrollbar */
.latest-summary::-webkit-scrollbar {
  width: 10px;
}

.latest-summary::-webkit-scrollbar-track {
  background: transparent;
}

.latest-summary::-webkit-scrollbar-thumb {
  background: rgba(102, 126, 234, 0.5);
  border-radius: 5px;
  border: 2px solid transparent;
  background-clip: padding-box;
}

.latest-summary::-webkit-scrollbar-thumb:hover {
  background: rgba(102, 126, 234, 0.7);
  background-clip: padding-box;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: rgba(102, 126, 234, 0.2);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #667eea;
}

.priority-badge {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  text-transform: uppercase;
}

.priority-badge.high {
  background: rgba(239, 68, 68, 0.2);
  color: #f87171;
}

.priority-badge.medium {
  background: rgba(251, 191, 36, 0.2);
  color: #fbbf24;
}

.priority-badge.low {
  background: rgba(74, 222, 128, 0.2);
  color: #4ade80;
}

.card-content {
  padding: 12px 16px;
}

.card-content h2 {
  margin: 0 0 8px 0;
  font-size: 18px;
  color: #fff;
}

.summary-meta {
  display: flex;
  gap: 10px;
  margin-bottom: 8px;
  font-size: 13px;
  color: #888;
}

.summary-content {
  line-height: 1.5;
  color: #ccc;
  font-size: 14px;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 40px 20px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 12px;
  margin-bottom: 12px;
}

.empty-icon {
  font-size: 36px;
  display: block;
  margin-bottom: 12px;
}

.empty-state h3 {
  margin: 0 0 6px 0;
  color: #333;
  font-size: 14px;
}

.empty-state p {
  color: #999;
  margin: 0;
  font-size: 12px;
}

/* History Section */
.history-section {
  margin-top: 0;
  border-radius: 12px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  padding: 12px;
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

.section-title {
  font-size: 14px;
  margin: 0 0 8px 0;
  color: #fff;
  flex-shrink: 0;
  padding: 0 3px;
  font-weight: 600;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
  overflow-y: scroll;
  overflow-x: hidden;
  padding-right: 6px;
  padding-bottom: 6px;
  flex: 1;
}

/* Scrollbar styling */
.summary-grid::-webkit-scrollbar {
  width: 10px;
}

.summary-grid::-webkit-scrollbar-track {
  background: transparent;
}

.summary-grid::-webkit-scrollbar-thumb {
  background: rgba(102, 126, 234, 0.5);
  border-radius: 5px;
  border: 2px solid transparent;
  background-clip: padding-box;
}

.summary-grid::-webkit-scrollbar-thumb:hover {
  background: rgba(102, 126, 234, 0.7);
  background-clip: padding-box;
}

.summary-card {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  padding: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.summary-card:hover {
  background: rgba(255, 255, 255, 0.08);
  transform: translateY(-2px);
}

.summary-card.selected {
  border-color: #667eea;
}

.card-header-small {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.priority-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.priority-dot.high { background: #f87171; }
.priority-dot.medium { background: #fbbf24; }
.priority-dot.low { background: #4ade80; }

.card-title-small {
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-meta-small {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: #888;
}

/* Modal */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-content {
  background: #1e1e2e;
  border-radius: 16px;
  max-width: 700px;
  width: 100%;
  max-height: 80vh;
  overflow-y: auto;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.modal-header h2 {
  margin: 0;
  font-size: 20px;
  color: #fff;
}

.close-button {
  background: none;
  border: none;
  color: #888;
  font-size: 20px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
}

.close-button:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.modal-meta {
  display: flex;
  gap: 16px;
  align-items: center;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.05);
  font-size: 13px;
  color: #888;
}

.modal-body {
  padding: 20px;
  line-height: 1.6;
  color: #ccc;
}

.raw-data-section {
  margin: 20px;
  padding: 16px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 8px;
}

.raw-data-section summary {
  cursor: pointer;
  font-size: 13px;
  color: #888;
}

.raw-data {
  margin-top: 12px;
  padding: 12px;
  background: rgba(0, 0, 0, 0.4);
  border-radius: 6px;
  font-size: 11px;
  color: #4ade80;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

/* Loading */
.loading-overlay {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 12px;
  margin-bottom: 12px;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 2px solid rgba(102, 126, 234, 0.3);
  border-radius: 50%;
  border-top-color: #667eea;
  animation: spin 0.8s linear infinite;
  margin-bottom: 12px;
}

.loading-overlay p {
  color: #666;
  margin: 0;
  font-size: 12px;
}

/* Error */
.error-message {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: rgba(239, 68, 68, 0.15);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 8px;
  margin-bottom: 12px;
  color: #c41e3a;
  font-size: 12px;
}

.error-message button {
  background: none;
  border: none;
  color: #f87171;
  cursor: pointer;
  font-size: 16px;
}

/* Markdown Content Styling */
.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3) {
  color: #fff;
  margin-top: 16px;
  margin-bottom: 8px;
}

.markdown-content :deep(h1) { font-size: 1.8em; }
.markdown-content :deep(h2) { font-size: 1.4em; }
.markdown-content :deep(h3) { font-size: 1.2em; }

/* Скрыть первый h2 заголовок в markdown (дублирование названия) */
.markdown-content :deep(h2:first-child),
.summary-content :deep(h2:first-child) {
  display: none;
}

/* Скрыть первый пустой абзац, который идет после скрытого заголовка */
.markdown-content :deep(p:first-child),
.summary-content :deep(p:first-child) {
  display: none;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 8px 0;
  padding-left: 24px;
}

.markdown-content :deep(li) {
  margin: 4px 0;
  color: #ccc;
}

.markdown-content :deep(strong) {
  color: #fff;
}

.markdown-content :deep(code) {
  background: rgba(0, 0, 0, 0.3);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.9em;
  color: #667eea;
}

.markdown-content :deep(pre) {
  background: rgba(0, 0, 0, 0.3);
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  color: #ccc;
}

/* Structured Sections */
.structured-sections {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.structured-section {
  margin-bottom: 16px;
}

.section-header {
  font-size: 14px;
  font-weight: 600;
  margin: 12px 0 8px 0;
  color: #fff;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.highlights-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.highlights-list li {
  padding: 8px 12px;
  margin: 4px 0;
  background: rgba(102, 126, 234, 0.1);
  border-left: 3px solid #667eea;
  border-radius: 4px;
  color: #ddd;
  font-size: 13px;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.task-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 6px;
  font-size: 13px;
  border-left: 3px solid #ffa500;
}

.task-item.overdue-item {
  border-left-color: #ff6b6b;
  background: rgba(255, 107, 107, 0.1);
}

.task-name {
  font-weight: 500;
  color: #fff;
  flex: 1;
}

.task-due {
  color: #999;
  font-size: 12px;
  margin-left: 12px;
  white-space: nowrap;
}

.overdue-item .task-due {
  color: #ff6b6b;
  font-weight: 500;
}
</style>

