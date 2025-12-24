<template>
  <div class="rag-container">
    <div class="rag-section">
      <!-- Header -->
      <div class="rag-header">
        <div class="header-content">
          <div>
            <h1>🔍 Semantic Search</h1>
            <p>Search across all indexed documents</p>
          </div>
          <div class="header-controls">
            <span class="docs-count">{{ documentsCount }} docs indexed</span>
            <button
                @click="showSettings = !showSettings"
                class="settings-button"
                :class="{ active: showSettings }"
            >
              ⚙️ Settings
            </button>
          </div>
        </div>
      </div>

      <!-- Settings Panel -->
      <div v-if="showSettings" class="settings-panel">
        <div class="settings-title">🎚️ Search Parameters</div>

        <!-- Search Mode Selector -->
        <div class="search-mode-section">
          <label class="section-label">Search Mode</label>
          <div class="mode-radio-group">
            <button
                v-for="mode in (['semantic', 'keyword', 'hybrid'] as const)"
                :key="mode"
                class="mode-radio"
                :class="{ active: searchParams.searchMode === mode }"
                @click="searchParams.searchMode = (mode as 'semantic' | 'keyword' | 'hybrid')"
            >
              <span class="mode-icon">
                {{ mode === 'semantic' ? '🧠' : mode === 'keyword' ? '📝' : '🔄' }}
              </span>
              <span class="mode-label">
                {{ mode === 'semantic' ? 'Semantic' : mode === 'keyword' ? 'Keyword' : 'Hybrid' }}
              </span>
            </button>
          </div>
        </div>

        <!-- Weights Slider (Hybrid Mode Only) -->
        <div v-if="searchParams.searchMode === 'hybrid'" class="weights-section">
          <label class="section-label">
            Balance: Semantic ←→ Keyword
          </label>
          <div class="weights-slider-container">
            <span class="weight-label keyword-label">📝 Keyword</span>
            <input
                v-model.number="searchParams.semanticWeight"
                type="range"
                min="0"
                max="1"
                step="0.05"
                class="weights-slider"
            />
            <span class="weight-label semantic-label">🧠 Semantic</span>
          </div>
          <div class="weights-display">
            <span class="weight-value keyword-weight">
              {{ Math.round((1 - searchParams.semanticWeight) * 100) }}% Keyword
            </span>
            <span class="weight-separator">|</span>
            <span class="weight-value semantic-weight">
              {{ Math.round(searchParams.semanticWeight * 100) }}% Semantic
            </span>
          </div>
        </div>

        <div class="settings-grid">
          <div class="setting-item">
            <label>Results Count: {{ searchParams.topK }}</label>
            <input
                v-model.number="searchParams.topK"
                type="range"
                min="1"
                max="20"
            />
          </div>
          <div class="setting-item">
            <label>{{ getThresholdLabel() }}: {{ Math.round(searchParams.threshold * 100) }}%</label>
            <input
                v-model.number="searchParams.threshold"
                type="range"
                min="0"
                max="1"
                step="0.05"
            />
            <span class="threshold-hint">{{ getThresholdHint() }}</span>
          </div>
        </div>
        <div class="filter-section">
          <label>Filter by document:</label>
          <div class="document-filters">
            <label
                v-for="doc in availableDocuments"
                :key="doc"
                class="filter-checkbox"
            >
              <input
                  type="checkbox"
                  :value="doc"
                  v-model="searchParams.documentFilter"
              />
              {{ doc }}
            </label>
          </div>
        </div>
      </div>

      <!-- Content Area -->
      <div class="content-area">
        <!-- Search Input -->
        <div class="search-input-container" :class="{ focused: isSearchFocused }">
          <span class="search-icon">🔍</span>
          <input
              v-model="searchQuery"
              type="text"
              placeholder="Search in documents..."
              @focus="isSearchFocused = true"
              @blur="isSearchFocused = false"
              @keyup.enter="performSearch"
          />
          <button
              v-if="searchQuery"
              @click="searchQuery = ''"
              class="clear-button"
          >
            ✕
          </button>
          <button
              @click="performSearch"
              class="search-button"
              :disabled="!searchQuery.trim() || isSearching"
          >
            {{ isSearching ? 'Searching...' : 'Search' }}
          </button>
        </div>

        <!-- Filter Chips -->
        <div class="filter-chips">
          <button
              class="chip"
              :class="{ active: searchParams.documentFilter.length === 0 }"
              @click="searchParams.documentFilter = []"
          >
            All documents
          </button>
          <button
              v-for="doc in searchParams.documentFilter"
              :key="doc"
              class="chip active"
              @click="removeFilter(doc)"
          >
            {{ doc }} ✕
          </button>
        </div>

        <!-- Results Section -->
        <div class="results-section">
          <!-- Results Header -->
          <div v-if="searchResults.length > 0 && !isSearching" class="results-header">
            Found {{ searchResults.length }} results in {{ processingTime }}
          </div>

          <!-- Loading Skeletons -->
          <div v-if="isSearching" class="skeletons">
            <div v-for="i in 3" :key="i" class="result-skeleton"></div>
          </div>

          <!-- Results List -->
          <div v-else-if="searchResults.length > 0" class="results-list">
            <div
                v-for="(result, index) in searchResults"
                :key="index"
                class="result-card"
                @click="showContext(result)"
            >
              <div class="result-header">
                <div class="result-doc">
                  <span class="doc-icon">📄</span>
                  <span class="doc-name">{{ result.documentName }}</span>
                </div>
                <div
                    class="similarity-badge"
                    :class="getSimilarityClass(getScore(result))"
                >
                  {{ Math.round(getScore(result) * 100) }}%
                </div>
              </div>

              <!-- Search Mode Badge -->
              <div class="search-mode-badge" :class="searchParams.searchMode">
                <span v-if="searchParams.searchMode === 'semantic'" class="badge-content">
                  🧠 Semantic Score: <span class="score">{{ Math.round(getScore(result) * 100) }}%</span>
                </span>
                <span v-else-if="searchParams.searchMode === 'keyword'" class="badge-content">
                  📝 Keyword Score: <span class="score">{{ Math.round(getScore(result) * 100) }}%</span>
                </span>
                <span v-else class="badge-content">
                  🔄 Hybrid |
                  <span class="score-pair">
                    Combined: <span class="score">{{ Math.round(getScore(result) * 100) }}%</span>
                  </span>
                </span>
              </div>

              <div
                  class="result-content"
                  v-html="highlightKeywords(result.chunkText, searchQuery)"
              ></div>
              <div class="result-footer">
                <span class="chunk-info">
                  Chunk #{{ result.chunkIndex }}
                  <span v-if="result.tokensCount"> | {{ result.tokensCount }} tokens</span>
                </span>
                <div class="result-actions">
                  <button @click.stop="copyChunk(result.chunkText)" class="action-btn">
                    📋 Copy
                  </button>
                  <button @click.stop="showContext(result)" class="action-btn">
                    🔗 Context
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Empty State -->
          <div v-else-if="hasSearched && !isSearching" class="empty-state">
            <div class="empty-icon">🔍</div>
            <h3>No results found</h3>
            <p>Try different keywords or lower the similarity threshold</p>
            <button @click="resetFilters" class="reset-button">
              Clear filters
            </button>
          </div>

          <!-- Initial State -->
          <div v-else class="initial-state">
            <div class="initial-icon">📚</div>
            <h3>Start searching</h3>
            <p>Enter a query to search across your indexed documents</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Context Popup Modal -->
    <Teleport to="body">
      <div v-if="contextModal.show" class="modal-overlay" @click="closeContextModal">
        <div class="context-modal" @click.stop>
          <div class="modal-header">
            <div class="modal-title">
              <span class="modal-icon">📄</span>
              <div class="modal-title-text">
                <h3>{{ contextModal.result?.documentName }}</h3>
                <span class="chunk-badge">Chunk #{{ contextModal.result?.chunkIndex }}</span>
              </div>
            </div>
            <button class="modal-close" @click="closeContextModal">✕</button>
          </div>

          <div class="modal-body">
            <div class="context-score">
              <span class="score-label">
                {{ searchParams.searchMode === 'keyword' ? 'Relevance' : 'Similarity' }}:
              </span>
              <span
                  class="score-value"
                  :class="getSimilarityClass(getScore(contextModal.result!))"
              >
                {{ Math.round(getScore(contextModal.result!) * 100) }}%
              </span>
            </div>

            <div class="context-text">
              <div
                  class="full-text"
                  v-html="highlightKeywords(contextModal.result?.chunkText || '', searchQuery)"
              ></div>
            </div>
          </div>

          <div class="modal-footer">
            <button class="modal-btn secondary" @click="closeContextModal">
              Close
            </button>
            <button class="modal-btn primary" @click="copyChunk(contextModal.result?.chunkText || '')">
              📋 Copy Text
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ragDocumentService, type SearchResult } from '../services/ragDocumentService';

defineEmits<{
  navigate: [mode: string]
}>();

// State
const searchQuery = ref('');
const searchResults = ref<SearchResult[]>([]);
const isSearching = ref(false);
const isSearchFocused = ref(false);
const hasSearched = ref(false);
const processingTime = ref('');
const error = ref('');
const showSettings = ref(false);
const availableDocuments = ref<string[]>([]);

const searchParams = ref({
  topK: 10,
  threshold: 0.35,
  searchMode: 'hybrid' as 'semantic' | 'keyword' | 'hybrid',
  semanticWeight: 0.6,
  documentFilter: [] as string[]
});

// Context Modal State
const contextModal = ref<{
  show: boolean;
  result: SearchResult | null;
}>({
  show: false,
  result: null
});

// Computed
const documentsCount = computed(() => availableDocuments.value.length);

// Methods

/**
 * Получить score результата (similarity или relevance в зависимости от режима)
 */
const getScore = (result: SearchResult): number => {
  // Бэкенд возвращает relevance для keyword, similarity для semantic
  // Проверяем оба поля
  const score = result.similarity ?? (result as any).relevance ?? 0;
  return score;
};

const performSearch = async () => {
  if (!searchQuery.value.trim()) return;

  isSearching.value = true;
  hasSearched.value = true;
  error.value = '';

  try {
    const response = await ragDocumentService.search(
        searchQuery.value,
        searchParams.value.topK,
        searchParams.value.threshold,
        searchParams.value.documentFilter.length > 0 ? searchParams.value.documentFilter : undefined,
        searchParams.value.searchMode,
        searchParams.value.semanticWeight
    );

    searchResults.value = response.results;
    processingTime.value = response.processingTime;
  } catch (err: unknown) {
    error.value = err instanceof Error ? err.message : 'Search failed';
    searchResults.value = [];
  } finally {
    isSearching.value = false;
  }
};

const highlightKeywords = (text: string, query: string): string => {
  if (!query.trim()) return escapeHtml(text);

  const keywords = query.split(/\s+/).filter(k => k.length > 2);
  let highlighted = escapeHtml(text);

  keywords.forEach(keyword => {
    const regex = new RegExp(`(${escapeRegExp(keyword)})`, 'gi');
    highlighted = highlighted.replace(regex, '<span class="highlight">$1</span>');
  });

  return highlighted;
};

const escapeHtml = (text: string): string => {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
};

const escapeRegExp = (string: string): string => {
  return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
};

const getSimilarityClass = (similarity: number): string => {
  if (similarity >= 0.8) return 'high';
  if (similarity >= 0.6) return 'medium';
  return 'low';
};

const getThresholdLabel = (): string => {
  const mode = searchParams.value.searchMode;
  if (mode === 'semantic') return 'Similarity';
  if (mode === 'keyword') return 'Relevance';
  return 'Min Score';
};

const getThresholdHint = (): string => {
  const threshold = searchParams.value.threshold;
  if (threshold <= 0.5) return 'Low precision, many results';
  if (threshold <= 0.8) return 'Balanced';
  return 'High precision, few results';
};

const copyChunk = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text);
    // Could show toast notification
  } catch (err) {
    console.error('Failed to copy:', err);
  }
};

/**
 * Показать popup с полным контекстом
 */
const showContext = (result: SearchResult) => {
  contextModal.value = {
    show: true,
    result: result
  };
};

/**
 * Закрыть popup
 */
const closeContextModal = () => {
  contextModal.value = {
    show: false,
    result: null
  };
};

const removeFilter = (doc: string) => {
  searchParams.value.documentFilter = searchParams.value.documentFilter.filter(d => d !== doc);
};

const resetFilters = () => {
  searchParams.value.documentFilter = [];
  searchParams.value.threshold = 0.5;
};

const loadDocuments = async () => {
  try {
    const docs = await ragDocumentService.getIndexedDocuments();
    availableDocuments.value = docs.map(d => d.documentName);
  } catch (err) {
    console.error('Failed to load documents:', err);
  }
};

onMounted(() => {
  loadDocuments();
});
</script>

<style scoped lang="scss">
@use '../styles/variables' as *;
@use '../styles/mixins' as *;

.rag-container {
  width: $chat-width;
  min-width: $chat-min-width;
  max-width: $chat-max-width;
  height: $chat-height;
  min-height: $chat-min-height;
  max-height: $chat-max-height;
  margin: 0 auto;
  padding: $spacing-lg;
}

.rag-section {
  background: $bg-white;
  border-radius: $radius-xl;
  box-shadow: $shadow-lg;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.rag-header {
  background: $primary-gradient;
  padding: $spacing-lg $spacing-xl;
  color: $text-white;

  .header-content {
    @include flex-between;
  }

  h1 {
    margin: 0;
    font-size: $font-size-xl;
  }

  p {
    margin: $spacing-xs 0 0;
    opacity: 0.9;
  }

  .header-controls {
    display: flex;
    gap: $spacing-md;
    align-items: center;
  }

  .docs-count {
    font-size: $font-size-sm;
    opacity: 0.9;
  }

  .settings-button {
    @include button-base;
    padding: $spacing-sm $spacing-md;
    border-radius: $radius-md;
    background: rgba(255, 255, 255, 0.2);
    color: $text-white;
    border: none;

    &:hover, &.active {
      background: rgba(255, 255, 255, 0.3);
    }
  }
}

.settings-panel {
  background: $bg-light;
  padding: $spacing-lg;
  border-bottom: 1px solid $border-light;

  .settings-title {
    font-weight: 600;
    margin-bottom: $spacing-md;
    font-size: $font-size-lg;
  }

  .section-label {
    font-weight: 600;
    font-size: $font-size-sm;
    color: $text-muted;
    margin-bottom: $spacing-sm;
    display: block;
  }

  .search-mode-section {
    margin-bottom: $spacing-lg;
  }

  .mode-radio-group {
    display: flex;
    gap: $spacing-sm;
    flex-wrap: wrap;
  }

  .mode-radio {
    display: flex;
    align-items: center;
    gap: $spacing-xs;
    padding: $spacing-sm $spacing-md;
    border: 2px solid $border-light;
    border-radius: $radius-md;
    background: $bg-white;
    cursor: pointer;
    transition: all $transition-fast;

    &:hover {
      border-color: $primary-color;
    }

    &.active {
      border-color: $primary-color;
      background: rgba($primary-color, 0.1);
    }

    .mode-icon {
      font-size: 18px;
    }

    .mode-label {
      font-weight: 500;
    }
  }

  .weights-section {
    margin-bottom: $spacing-lg;
    padding: $spacing-md;
    background: $bg-white;
    border-radius: $radius-md;
    border: 1px solid $border-light;
  }

  .weights-slider-container {
    display: flex;
    align-items: center;
    gap: $spacing-md;
    margin: $spacing-sm 0;

    .weights-slider {
      flex: 1;
      height: 8px;
      -webkit-appearance: none;
      background: linear-gradient(to right, #2196F3, #9C27B0);
      border-radius: 4px;
      outline: none;

      &::-webkit-slider-thumb {
        -webkit-appearance: none;
        width: 20px;
        height: 20px;
        background: $bg-white;
        border: 2px solid $primary-color;
        border-radius: 50%;
        cursor: pointer;
      }
    }

    .weight-label {
      font-size: $font-size-sm;
      white-space: nowrap;
    }
  }

  .weights-display {
    display: flex;
    justify-content: center;
    gap: $spacing-md;
    font-size: $font-size-sm;

    .weight-value {
      font-weight: 600;

      &.keyword-weight { color: #1976D2; }
      &.semantic-weight { color: #7B1FA2; }
    }

    .weight-separator {
      color: $text-muted;
    }
  }

  .settings-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: $spacing-lg;
    margin-bottom: $spacing-lg;

    .setting-item {
      label {
        display: block;
        font-size: $font-size-sm;
        margin-bottom: $spacing-sm;
      }

      input[type="range"] {
        width: 100%;
      }

      .threshold-hint {
        display: block;
        font-size: $font-size-xs;
        color: $text-muted;
        margin-top: $spacing-xs;
      }
    }
  }

  .filter-section {
    label {
      display: block;
      font-size: $font-size-sm;
      margin-bottom: $spacing-sm;
    }

    .document-filters {
      display: flex;
      flex-wrap: wrap;
      gap: $spacing-sm;
    }

    .filter-checkbox {
      display: flex;
      align-items: center;
      gap: $spacing-xs;
      font-size: $font-size-sm;
      cursor: pointer;
    }
  }
}

.content-area {
  flex: 1;
  padding: $spacing-lg;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.search-input-container {
  display: flex;
  align-items: center;
  height: 60px;
  background: $bg-white;
  border: 2px solid $border-light;
  border-radius: $radius-lg;
  padding: 0 $spacing-md;
  box-shadow: $shadow-sm;
  transition: all $transition-base;

  &.focused {
    border-color: $primary-color;
    box-shadow: 0 0 0 4px rgba($primary-color, 0.1);
    transform: translateY(-2px);
  }

  .search-icon {
    font-size: 24px;
    opacity: 0.5;
    margin-right: $spacing-sm;
  }

  input {
    flex: 1;
    border: none;
    background: transparent;
    font-size: 18px;
    outline: none;

    &::placeholder {
      color: $text-light;
    }
  }

  .clear-button {
    width: 32px;
    height: 32px;
    border: none;
    background: transparent;
    cursor: pointer;
    opacity: 0.5;
    font-size: 16px;

    &:hover {
      opacity: 1;
    }
  }

  .search-button {
    @include button-primary;
    padding: $spacing-sm $spacing-lg;
    margin-left: $spacing-sm;

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  }
}

.filter-chips {
  display: flex;
  gap: $spacing-sm;
  margin-top: $spacing-md;
  flex-wrap: wrap;

  .chip {
    padding: $spacing-sm $spacing-md;
    background: transparent;
    border: 1px solid $border-light;
    border-radius: $radius-full;
    font-size: $font-size-sm;
    cursor: pointer;
    transition: all $transition-fast;

    &:hover {
      border-color: $primary-color;
    }

    &.active {
      background: $primary-color;
      color: $text-white;
      border-color: $primary-color;
    }
  }
}

.results-section {
  flex: 1;
  margin-top: $spacing-lg;
}

.results-header {
  font-size: $font-size-sm;
  color: $text-muted;
  margin-bottom: $spacing-md;
}

.skeletons {
  .result-skeleton {
    height: 150px;
    background: linear-gradient(90deg, $bg-light 25%, $bg-white 50%, $bg-light 75%);
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
    border-radius: $radius-md;
    margin-bottom: $spacing-md;
  }
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.results-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.result-card {
  background: $bg-white;
  border: 1px solid $border-light;
  border-radius: $radius-md;
  padding: $spacing-lg;
  cursor: pointer;
  transition: all $transition-base;

  &:hover {
    transform: translateY(-4px);
    box-shadow: $shadow-md;
    border-color: $primary-color;
  }
}

.result-header {
  @include flex-between;
  margin-bottom: $spacing-sm;

  .result-doc {
    display: flex;
    align-items: center;
    gap: $spacing-sm;

    .doc-icon {
      font-size: 20px;
    }

    .doc-name {
      font-weight: 600;
      font-size: 16px;
    }
  }

  .similarity-badge {
    padding: $spacing-xs $spacing-sm;
    border-radius: $radius-lg;
    font-size: $font-size-sm;
    font-weight: 600;

    &.high {
      background: rgba($success-color, 0.2);
      color: #2e7d32;
    }

    &.medium {
      background: rgba(255, 193, 7, 0.2);
      color: #856404;
    }

    &.low {
      background: $bg-light;
      color: $text-muted;
    }
  }
}

// Search Mode Badge
.search-mode-badge {
  padding: $spacing-xs $spacing-md;
  border-radius: $radius-md;
  font-size: $font-size-xs;
  font-weight: 600;
  margin-bottom: $spacing-sm;
  display: inline-block;

  .badge-content {
    display: flex;
    align-items: center;
    gap: $spacing-xs;
  }

  .score {
    font-family: 'Monaco', 'Courier New', monospace;
    font-weight: 700;
  }

  .score-pair {
    display: inline-flex;
    align-items: center;
    gap: $spacing-xs;
  }

  &.semantic {
    background: rgba(#9C27B0, 0.15);
    color: #7B1FA2;
  }

  &.keyword {
    background: rgba(#2196F3, 0.15);
    color: #1976D2;
  }

  &.hybrid {
    background: rgba(#FF9800, 0.15);
    color: #E65100;
  }
}

.result-content {
  font-size: 15px;
  line-height: 1.6;
  color: $text-dark;
  max-height: 4.8em;
  overflow: hidden;
  text-overflow: ellipsis;

  :deep(.highlight) {
    background: rgba(255, 235, 59, 0.4);
    padding: 2px 4px;
    border-radius: 3px;
  }
}

.result-footer {
  @include flex-between;
  margin-top: $spacing-sm;
  padding-top: $spacing-sm;
  border-top: 1px solid $border-light;

  .chunk-info {
    font-size: $font-size-sm;
    color: $text-muted;
  }

  .result-actions {
    display: flex;
    gap: $spacing-sm;
  }

  .action-btn {
    @include button-base;
    padding: $spacing-xs $spacing-sm;
    border-radius: $radius-sm;
    font-size: $font-size-sm;
    background: transparent;
    color: $text-muted;

    &:hover {
      background: $bg-light;
      color: $text-dark;
    }
  }
}

.empty-state,
.initial-state {
  text-align: center;
  padding: $spacing-xl * 2;

  .empty-icon,
  .initial-icon {
    font-size: 64px;
    opacity: 0.3;
    margin-bottom: $spacing-md;
  }

  h3 {
    font-size: 24px;
    font-weight: 600;
    margin-bottom: $spacing-sm;
  }

  p {
    color: $text-muted;
    margin-bottom: $spacing-lg;
  }

  .reset-button {
    @include button-secondary;
    padding: $spacing-sm $spacing-lg;
  }
}

// ==================== Context Modal ====================

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: $spacing-lg;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.context-modal {
  background: $bg-white;
  border-radius: $radius-xl;
  width: 100%;
  max-width: 700px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: $shadow-xl;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $spacing-lg;
  border-bottom: 1px solid $border-light;

  .modal-title {
    display: flex;
    align-items: center;
    gap: $spacing-md;

    .modal-icon {
      font-size: 32px;
    }

    .modal-title-text {
      h3 {
        margin: 0;
        font-size: $font-size-lg;
        font-weight: 600;
      }

      .chunk-badge {
        font-size: $font-size-sm;
        color: $text-muted;
      }
    }
  }

  .modal-close {
    width: 36px;
    height: 36px;
    border: none;
    background: $bg-light;
    border-radius: $radius-full;
    cursor: pointer;
    font-size: 18px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all $transition-fast;

    &:hover {
      background: $border-light;
    }
  }
}

.modal-body {
  flex: 1;
  padding: $spacing-lg;
  overflow-y: auto;

  .context-score {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    margin-bottom: $spacing-lg;
    padding: $spacing-md;
    background: $bg-light;
    border-radius: $radius-md;

    .score-label {
      color: $text-muted;
    }

    .score-value {
      font-weight: 700;
      font-size: $font-size-lg;
      padding: $spacing-xs $spacing-sm;
      border-radius: $radius-sm;

      &.high {
        background: rgba($success-color, 0.2);
        color: #2e7d32;
      }

      &.medium {
        background: rgba(255, 193, 7, 0.2);
        color: #856404;
      }

      &.low {
        background: rgba(0, 0, 0, 0.1);
        color: $text-muted;
      }
    }
  }

  .context-text {
    .full-text {
      font-size: 16px;
      line-height: 1.8;
      color: $text-dark;
      white-space: pre-wrap;
      word-break: break-word;

      :deep(.highlight) {
        background: rgba(255, 235, 59, 0.5);
        padding: 2px 6px;
        border-radius: 4px;
        font-weight: 500;
      }
    }
  }
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: $spacing-sm;
  padding: $spacing-lg;
  border-top: 1px solid $border-light;

  .modal-btn {
    @include button-base;
    padding: $spacing-sm $spacing-lg;
    border-radius: $radius-md;
    font-weight: 500;

    &.secondary {
      background: $bg-light;
      color: $text-dark;

      &:hover {
        background: $border-light;
      }
    }

    &.primary {
      @include button-primary;
    }
  }
}
</style>