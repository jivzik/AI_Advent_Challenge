<template>
  <div class="rag-container">
    <div class="rag-section">
      <!-- Header -->
      <div class="rag-header">
        <div class="header-content">
          <div>
            <h1>📚 Document Library</h1>
            <p>Manage your indexed documents</p>
          </div>
          <div class="header-controls">
            <router-link to="/rag/upload" class="nav-button">
              📤 Upload New
            </router-link>
            <div class="view-toggles">
              <button
                @click="viewMode = 'cards'"
                class="view-button"
                :class="{ active: viewMode === 'cards' }"
              >
                📇
              </button>
              <button
                @click="viewMode = 'table'"
                class="view-button"
                :class="{ active: viewMode === 'table' }"
              >
                📋
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Content Area -->
      <div class="content-area">
        <!-- Stats Cards -->
        <div v-if="stats" class="stats-cards">
          <div class="stat-card">
            <div class="stat-icon">📄</div>
            <div class="stat-value">{{ stats.totalDocuments }}</div>
            <div class="stat-label">Documents</div>
            <div v-if="stats.todayDocuments > 0" class="stat-change positive">
              +{{ stats.todayDocuments }} today
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">🧩</div>
            <div class="stat-value">{{ stats.totalChunks }}</div>
            <div class="stat-label">Total Chunks</div>
            <div v-if="stats.todayChunks > 0" class="stat-change positive">
              +{{ stats.todayChunks }} today
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">🔍</div>
            <div class="stat-value">{{ stats.totalSearches }}</div>
            <div class="stat-label">Searches</div>
            <div v-if="stats.todaySearches > 0" class="stat-change positive">
              +{{ stats.todaySearches }} today
            </div>
          </div>
        </div>

        <!-- Library Controls -->
        <div class="library-controls">
          <div class="search-box">
            <span class="search-icon">🔍</span>
            <input
              v-model="searchQuery"
              type="text"
              placeholder="Search documents..."
            />
          </div>
          <div class="filter-chips">
            <button
              v-for="type in ['all', 'pdf', 'txt', 'md', 'docx']"
              :key="type"
              class="chip"
              :class="{ active: typeFilter === type }"
              @click="typeFilter = type"
            >
              {{ type.toUpperCase() }}
            </button>
          </div>
          <select v-model="sortBy" class="sort-dropdown">
            <option value="date-desc">Date (newest)</option>
            <option value="date-asc">Date (oldest)</option>
            <option value="name-asc">Name (A-Z)</option>
            <option value="size-desc">Size (largest)</option>
          </select>
        </div>

        <!-- Loading State -->
        <div v-if="isLoading" class="loading-state">
          <div class="spinner-large"></div>
          <p>Loading documents...</p>
        </div>

        <!-- Empty State -->
        <div v-else-if="documents.length === 0" class="library-empty">
          <div class="empty-icon">📚</div>
          <h3>No documents yet</h3>
          <p>Upload your first document to get started</p>
          <button class="upload-button" @click="$emit('navigate', 'rag-upload')">
            Upload Document
          </button>
        </div>

        <!-- Card View -->
        <div v-else-if="viewMode === 'cards'" class="documents-grid">
          <div
            v-for="doc in filteredDocuments"
            :key="doc.id"
            class="document-card"
          >
            <div class="card-icon">
              {{ getFileIcon(doc.fileType) }}
            </div>
            <div class="card-info">
              <div class="card-name" :title="doc.fileName">
                {{ doc.fileName }}
              </div>
              <div class="card-meta">
                <span>{{ doc.chunkCount }} chunks</span>
                <span>{{ formatSize(doc.fileSize) }}</span>
                <span>{{ formatTime(doc.createdAt) }}</span>
              </div>
            </div>
            <div class="card-actions">
              <button @click="searchInDocument(doc)" class="card-btn">
                🔍 Search
              </button>
              <button @click="viewChunks(doc)" class="card-btn">
                👁️ View
              </button>
              <button @click="deleteDocument(doc.id)" class="card-btn card-btn-danger">
                🗑️ Delete
              </button>
            </div>
          </div>
        </div>

        <!-- Table View -->
        <div v-else class="documents-table-container">
          <table class="documents-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Chunks</th>
                <th>Size</th>
                <th>Date</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="doc in filteredDocuments"
                :key="doc.id"
                class="table-row"
              >
                <td class="name-cell">
                  <span class="doc-icon">{{ getFileIcon(doc.fileType) }}</span>
                  <span class="doc-name">{{ doc.fileName }}</span>
                </td>
                <td class="number-cell">{{ doc.chunkCount }}</td>
                <td class="number-cell">{{ formatSize(doc.fileSize) }}</td>
                <td>{{ formatDate(doc.createdAt) }}</td>
                <td class="actions-cell">
                  <button @click="searchInDocument(doc)" class="icon-btn" title="Search">
                    🔍
                  </button>
                  <button @click="viewChunks(doc)" class="icon-btn" title="View">
                    👁️
                  </button>
                  <button @click="deleteDocument(doc.id)" class="icon-btn danger" title="Delete">
                    🗑️
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Chunks Modal -->
      <div v-if="showChunksModal" class="modal-overlay" @click.self="closeModal">
        <div class="modal">
          <div class="modal-header">
            <div class="modal-title">
              <span class="modal-icon">{{ selectedDocument ? getFileIcon(selectedDocument.fileType) : '📄' }}</span>
              <span>{{ selectedDocument?.fileName }}</span>
            </div>
            <button @click="closeModal" class="modal-close">✕</button>
          </div>
          <div class="modal-body">
            <div v-if="selectedDocument" class="modal-meta">
              <div class="meta-item">
                <span class="meta-label">Chunks:</span>
                <span>{{ selectedDocument.chunkCount }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">Size:</span>
                <span>{{ formatSize(selectedDocument.fileSize) }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">Created:</span>
                <span>{{ formatDate(selectedDocument.createdAt) }}</span>
              </div>
            </div>
            <div class="chunks-list">
              <div
                v-for="(chunk, index) in documentChunks"
                :key="index"
                class="chunk-card"
              >
                <div class="chunk-header">
                  Chunk #{{ chunk.index }} ({{ chunk.tokensCount }} tokens)
                </div>
                <div class="chunk-text">{{ chunk.text }}</div>
                <div class="chunk-actions">
                  <button @click="copyChunk(chunk.text)" class="chunk-btn">
                    📋 Copy
                  </button>
                </div>
              </div>
              <div v-if="documentChunks.length === 0" class="no-chunks">
                No chunks available
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button @click="closeModal" class="modal-btn">Close</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { ragDocumentService, type Document, type Chunk, type LibraryStats } from '../services/ragDocumentService';

const emit = defineEmits<{
  navigate: [mode: string]
}>();

// State
const documents = ref<Document[]>([]);
const stats = ref<LibraryStats | null>(null);
const isLoading = ref(false);
const error = ref('');
const viewMode = ref<'cards' | 'table'>('cards');
const searchQuery = ref('');
const typeFilter = ref('all');
const sortBy = ref('date-desc');
const selectedDocument = ref<Document | null>(null);
const documentChunks = ref<Chunk[]>([]);
const showChunksModal = ref(false);

// Computed
const filteredDocuments = computed(() => {
  let result = [...documents.value];

  // Search filter
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase();
    result = result.filter(d => d.fileName.toLowerCase().includes(query));
  }

  // Type filter
  if (typeFilter.value !== 'all') {
    result = result.filter(d =>
      d.fileType?.toLowerCase() === typeFilter.value.toLowerCase()
    );
  }

  // Sorting
  switch (sortBy.value) {
    case 'date-desc':
      result.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
      break;
    case 'date-asc':
      result.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());
      break;
    case 'name-asc':
      result.sort((a, b) => a.fileName.localeCompare(b.fileName));
      break;
    case 'size-desc':
      result.sort((a, b) => b.fileSize - a.fileSize);
      break;
  }

  return result;
});

// Methods
const loadDocuments = async () => {
  isLoading.value = true;
  try {
    documents.value = await ragDocumentService.getDocuments();
  } catch (err) {
    error.value = 'Failed to load documents';
  } finally {
    isLoading.value = false;
  }
};

const loadStats = async () => {
  try {
    stats.value = await ragDocumentService.getStats();
  } catch (err) {
    console.error('Failed to load stats:', err);
  }
};

const deleteDocument = async (id: string) => {
  if (!confirm('Delete this document? This action cannot be undone.')) return;

  try {
    await ragDocumentService.deleteDocument(id);
    await loadDocuments();
    await loadStats();
  } catch (err) {
    error.value = 'Failed to delete document';
  }
};

const viewChunks = async (doc: Document) => {
  selectedDocument.value = doc;
  isLoading.value = true;

  try {
    // For now, mock chunks - would need backend endpoint
    documentChunks.value = Array.from({ length: doc.chunkCount || 5 }, (_, i) => ({
      index: i,
      text: `Chunk ${i} content would be loaded from the server...`,
      tokensCount: Math.floor(Math.random() * 200) + 100
    }));
    showChunksModal.value = true;
  } catch (err) {
    error.value = 'Failed to load chunks';
  } finally {
    isLoading.value = false;
  }
};

const searchInDocument = (doc: Document) => {
  // Emit navigation event - parent component should handle this
  emit('navigate', 'rag-search');
  // Could also pass document name via localStorage or store
  localStorage.setItem('rag-search-document', doc.fileName);
};

const closeModal = () => {
  showChunksModal.value = false;
  selectedDocument.value = null;
  documentChunks.value = [];
};

const copyChunk = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text);
  } catch (err) {
    console.error('Failed to copy:', err);
  }
};

const getFileIcon = (type: string): string => {
  return ragDocumentService.getFileIcon(type || 'unknown');
};

const formatSize = (bytes: number): string => {
  return ragDocumentService.formatSize(bytes || 0);
};

const formatTime = (date: string): string => {
  return ragDocumentService.formatRelativeTime(date);
};

const formatDate = (date: string): string => {
  return new Date(date).toLocaleDateString();
};

onMounted(() => {
  loadDocuments();
  loadStats();
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
  position: relative;
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

  .nav-button {
    @include button-glass;
    text-decoration: none;
  }

  .view-toggles {
    display: flex;
    gap: 0;

    .view-button {
      width: 40px;
      height: 40px;
      border: 2px solid rgba(255, 255, 255, 0.5);
      background: transparent;
      color: $text-white;
      cursor: pointer;
      font-size: 18px;
      transition: all $transition-fast;

      &:first-child {
        border-radius: $radius-md 0 0 $radius-md;
      }

      &:last-child {
        border-radius: 0 $radius-md $radius-md 0;
        border-left: none;
      }

      &.active {
        background: rgba(255, 255, 255, 0.3);
      }

      &:hover {
        background: rgba(255, 255, 255, 0.2);
      }
    }
  }
}

.content-area {
  flex: 1;
  padding: $spacing-lg;
  overflow-y: auto;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $spacing-md;
  margin-bottom: $spacing-lg;
}

.stat-card {
  background: $bg-white;
  border: 1px solid $border-light;
  border-radius: $radius-md;
  padding: $spacing-lg;
  text-align: center;
  transition: all $transition-base;

  &:hover {
    transform: translateY(-2px);
    box-shadow: $shadow-md;
  }

  .stat-icon {
    font-size: 32px;
    margin-bottom: $spacing-sm;
  }

  .stat-value {
    font-size: 32px;
    font-weight: 700;
    color: $primary-color;
  }

  .stat-label {
    font-size: $font-size-sm;
    color: $text-muted;
    margin-top: $spacing-xs;
  }

  .stat-change {
    font-size: $font-size-xs;
    margin-top: $spacing-xs;

    &.positive {
      color: $success-color;
    }
  }
}

.library-controls {
  display: flex;
  gap: $spacing-md;
  align-items: center;
  margin-bottom: $spacing-lg;
  flex-wrap: wrap;

  .search-box {
    flex: 1;
    min-width: 200px;
    display: flex;
    align-items: center;
    padding: $spacing-sm $spacing-md;
    background: $bg-white;
    border: 1px solid $border-light;
    border-radius: $radius-md;

    .search-icon {
      margin-right: $spacing-sm;
      opacity: 0.5;
    }

    input {
      flex: 1;
      border: none;
      background: transparent;
      font-size: $font-size-base;
      outline: none;
    }
  }

  .filter-chips {
    display: flex;
    gap: $spacing-xs;

    .chip {
      padding: $spacing-xs $spacing-sm;
      border: 1px solid $border-light;
      border-radius: $radius-sm;
      background: transparent;
      font-size: $font-size-xs;
      cursor: pointer;
      transition: all $transition-fast;

      &.active {
        background: $primary-color;
        color: $text-white;
        border-color: $primary-color;
      }

      &:hover:not(.active) {
        border-color: $primary-color;
      }
    }
  }

  .sort-dropdown {
    padding: $spacing-sm;
    border: 1px solid $border-light;
    border-radius: $radius-md;
    background: $bg-white;
    font-size: $font-size-sm;
  }
}

.loading-state {
  text-align: center;
  padding: $spacing-xl * 2;

  .spinner-large {
    width: 48px;
    height: 48px;
    border: 4px solid $border-light;
    border-top-color: $primary-color;
    border-radius: 50%;
    animation: spin 1s linear infinite;
    margin: 0 auto $spacing-md;
  }

  p {
    color: $text-muted;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.library-empty {
  text-align: center;
  padding: $spacing-xl * 2;

  .empty-icon {
    font-size: 80px;
    opacity: 0.2;
    margin-bottom: $spacing-md;
  }

  h3 {
    font-size: 28px;
    font-weight: 600;
    margin-bottom: $spacing-sm;
  }

  p {
    color: $text-muted;
    margin-bottom: $spacing-lg;
  }

  .upload-button {
    @include button-primary;
    display: inline-block;
    text-decoration: none;
  }
}

.documents-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: $spacing-lg;
}

.document-card {
  background: $bg-white;
  border: 1px solid $border-light;
  border-radius: $radius-lg;
  padding: $spacing-lg;
  transition: all $transition-base;
  cursor: pointer;

  &:hover {
    transform: translateY(-4px) scale(1.02);
    box-shadow: $shadow-md;
    border-color: $primary-color;
  }

  .card-icon {
    font-size: 48px;
    margin-bottom: $spacing-md;
  }

  .card-info {
    .card-name {
      font-size: 16px;
      font-weight: 600;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      margin-bottom: $spacing-xs;
    }

    .card-meta {
      font-size: $font-size-sm;
      color: $text-muted;
      display: flex;
      flex-wrap: wrap;
      gap: $spacing-sm;

      span {
        white-space: nowrap;
      }
    }
  }

  .card-actions {
    margin-top: $spacing-md;
    display: flex;
    flex-direction: column;
    gap: $spacing-xs;

    .card-btn {
      width: 100%;
      padding: $spacing-sm;
      border: 1px solid $border-light;
      border-radius: $radius-sm;
      background: transparent;
      font-size: $font-size-sm;
      cursor: pointer;
      transition: all $transition-fast;

      &:hover {
        background: $bg-light;
      }

      &.card-btn-danger:hover {
        background: rgba(255, 0, 0, 0.1);
        border-color: $error-text;
      }
    }
  }
}

.documents-table-container {
  overflow-x: auto;
}

.documents-table {
  width: 100%;
  border-collapse: collapse;
  background: $bg-white;
  border-radius: $radius-md;
  overflow: hidden;

  thead {
    background: $bg-light;

    th {
      padding: $spacing-sm $spacing-md;
      text-align: left;
      font-size: $font-size-sm;
      font-weight: 600;
      text-transform: uppercase;
      color: $text-muted;
    }
  }

  .table-row {
    border-bottom: 1px solid $border-light;
    transition: background $transition-fast;

    &:hover {
      background: $bg-light;
    }

    td {
      padding: $spacing-sm $spacing-md;
      vertical-align: middle;
    }
  }

  .name-cell {
    display: flex;
    align-items: center;
    gap: $spacing-sm;

    .doc-icon {
      font-size: 20px;
    }

    .doc-name {
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      max-width: 200px;
    }
  }

  .number-cell {
    text-align: right;
    font-variant-numeric: tabular-nums;
  }

  .actions-cell {
    display: flex;
    gap: $spacing-xs;
    justify-content: flex-end;

    .icon-btn {
      width: 32px;
      height: 32px;
      border: none;
      background: transparent;
      border-radius: $radius-sm;
      cursor: pointer;
      font-size: 16px;
      transition: all $transition-fast;

      &:hover {
        background: $bg-light;
      }

      &.danger:hover {
        background: rgba(255, 0, 0, 0.1);
      }
    }
  }
}

// Modal Styles
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  z-index: 1000;
  @include flex-center;
}

.modal {
  width: 800px;
  max-width: 90vw;
  max-height: 80vh;
  background: $bg-white;
  border-radius: $radius-xl;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  @include flex-between;
  padding: $spacing-lg $spacing-xl;
  border-bottom: 1px solid $border-light;

  .modal-title {
    display: flex;
    align-items: center;
    gap: $spacing-sm;
    font-size: 18px;
    font-weight: 600;

    .modal-icon {
      font-size: 24px;
    }
  }

  .modal-close {
    width: 32px;
    height: 32px;
    border: none;
    background: transparent;
    cursor: pointer;
    font-size: 20px;
    opacity: 0.5;
    transition: all $transition-fast;

    &:hover {
      opacity: 1;
      transform: rotate(90deg);
    }
  }
}

.modal-body {
  flex: 1;
  padding: $spacing-lg $spacing-xl;
  overflow-y: auto;

  .modal-meta {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: $spacing-md;
    margin-bottom: $spacing-lg;

    .meta-item {
      font-size: $font-size-sm;

      .meta-label {
        color: $text-muted;
        margin-right: $spacing-xs;
      }
    }
  }
}

.chunks-list {
  display: flex;
  flex-direction: column;
  gap: $spacing-md;
}

.chunk-card {
  background: $bg-light;
  border-radius: $radius-md;
  padding: $spacing-md;

  .chunk-header {
    font-weight: 600;
    margin-bottom: $spacing-sm;
    font-size: $font-size-sm;
  }

  .chunk-text {
    font-size: $font-size-sm;
    line-height: 1.6;
    color: $text-dark;
  }

  .chunk-actions {
    margin-top: $spacing-sm;
    display: flex;
    gap: $spacing-sm;

    .chunk-btn {
      padding: $spacing-xs $spacing-sm;
      border: 1px solid $border-light;
      border-radius: $radius-sm;
      background: $bg-white;
      font-size: $font-size-xs;
      cursor: pointer;
      transition: all $transition-fast;

      &:hover {
        background: $primary-color;
        color: $text-white;
        border-color: $primary-color;
      }
    }
  }
}

.no-chunks {
  text-align: center;
  color: $text-muted;
  padding: $spacing-lg;
}

.modal-footer {
  padding: $spacing-md $spacing-xl;
  border-top: 1px solid $border-light;
  text-align: right;

  .modal-btn {
    @include button-secondary;
    padding: $spacing-sm $spacing-lg;
  }
}
</style>

