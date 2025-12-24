<template>
  <div class="rag-container">
    <div class="rag-section">
      <!-- Header -->
      <div class="rag-header">
        <div class="header-content">
          <div>
            <h1>📤 Document Upload</h1>
            <p>Upload and index your documents</p>
          </div>
          <div class="header-controls">
            <button
              @click="showSettings = !showSettings"
              class="settings-button"
              :class="{ active: showSettings }"
            >
              ⚙️ Settings
            </button>
            <button class="nav-button" @click="$emit('navigate', 'rag-library')">
              📊 View Library
            </button>
          </div>
        </div>
      </div>

      <!-- Settings Panel -->
      <div v-if="showSettings" class="settings-panel">
        <div class="settings-title">⚙️ Chunking Settings</div>
        <div class="settings-grid">
          <div class="setting-item">
            <label>Chunk Size</label>
            <input
              v-model.number="chunkingSettings.chunkSize"
              type="number"
              min="500"
              max="2000"
              step="100"
            />
            <span class="hint">(characters per chunk)</span>
          </div>
          <div class="setting-item">
            <label>Overlap Size</label>
            <input
              v-model.number="chunkingSettings.overlapSize"
              type="number"
              min="50"
              max="500"
              step="50"
            />
            <span class="hint">(overlap between chunks)</span>
          </div>
          <div class="setting-item">
            <label>Strategy</label>
            <select v-model="chunkingSettings.strategy">
              <option value="recursive">Recursive</option>
              <option value="sentence">Sentence-based</option>
              <option value="paragraph">Paragraph</option>
            </select>
          </div>
          <div class="setting-item">
            <button @click="resetSettings" class="reset-button">
              Reset to defaults
            </button>
          </div>
        </div>
      </div>

      <!-- Error Banner -->
      <div v-if="error" class="error-banner">
        <span>⚠️ {{ error }}</span>
        <button @click="error = ''" class="close-button">✕</button>
      </div>

      <!-- Content Area -->
      <div class="content-area">
        <!-- Dropzone -->
        <div
          class="dropzone"
          :class="{
            'dropzone--hover': isHover,
            'dropzone--drag-over': isDragOver,
            'dropzone--uploading': isUploading
          }"
          @dragenter.prevent="handleDragEnter"
          @dragover.prevent="handleDragOver"
          @dragleave.prevent="handleDragLeave"
          @drop.prevent="handleDrop"
          @click="triggerFileInput"
        >
          <input
            ref="fileInput"
            type="file"
            multiple
            accept=".pdf,.txt,.md,.docx,.epub"
            @change="handleFileSelect"
            hidden
          />
          <div class="dropzone-content">
            <div class="dropzone-icon">📄</div>
            <div class="dropzone-title">Drag & drop files here</div>
            <div class="dropzone-subtitle">or click to browse</div>
            <div class="dropzone-formats">
              Supported: PDF, TXT, MD, DOCX, EPUB | Max 50 MB
            </div>
          </div>
        </div>

        <!-- Uploaded Files List -->
        <div v-if="documents.length > 0" class="files-list">
          <h3>Uploaded Documents</h3>
          <div
            v-for="doc in documents"
            :key="doc.id"
            class="file-card"
            :class="{ 'file-card--error': doc.status === 'error' }"
          >
            <div class="file-icon">
              {{ getFileIcon(doc.type) }}
            </div>
            <div class="file-info">
              <div class="file-name">{{ doc.name }}</div>
              <div class="file-meta">
                {{ formatSize(doc.size) }}
                <span v-if="doc.chunksCount"> | {{ doc.chunksCount }} chunks</span>
                <span v-if="doc.timestamp"> | {{ formatTime(doc.timestamp) }}</span>
              </div>
              <!-- Progress Bar -->
              <div v-if="doc.status === 'indexing'" class="progress-container">
                <div class="progress-bar">
                  <div class="progress-fill" :style="{ width: doc.progress + '%' }"></div>
                </div>
                <span class="progress-text">{{ doc.progress }}%</span>
              </div>
              <!-- Error Message -->
              <div v-if="doc.error" class="file-error">
                {{ doc.error }}
              </div>
            </div>
            <div class="file-status">
              <span v-if="doc.status === 'ready'" class="status-ready">✅</span>
              <span v-else-if="doc.status === 'indexing'" class="status-indexing">
                <span class="spinner"></span>
              </span>
              <span v-else-if="doc.status === 'uploading'" class="status-uploading">
                <span class="spinner"></span>
              </span>
              <span v-else-if="doc.status === 'error'" class="status-error">❌</span>
            </div>
            <div class="file-actions">
              <button
                v-if="doc.status === 'ready'"
                @click="viewChunks(doc)"
                class="action-button"
                title="View chunks"
              >
                👁️
              </button>
              <button
                @click="deleteDocument(doc.id)"
                class="action-button action-delete"
                title="Delete"
              >
                🗑️
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ragDocumentService, type ChunkingSettings, type UploadedDocument } from '../services/ragDocumentService';

defineEmits<{
  navigate: [mode: string]
}>();

// State
const documents = ref<UploadedDocument[]>([]);
const isUploading = ref(false);
const isDragOver = ref(false);
const isHover = ref(false);
const error = ref('');
const showSettings = ref(false);
const fileInput = ref<HTMLInputElement | null>(null);

const chunkingSettings = ref<ChunkingSettings>({
  chunkSize: 800,
  overlapSize: 100,
  strategy: 'recursive'
});

// Methods
const resetSettings = () => {
  chunkingSettings.value = {
    chunkSize: 800,
    overlapSize: 100,
    strategy: 'recursive'
  };
};

const handleDragEnter = () => {
  isDragOver.value = true;
  isHover.value = true;
};

const handleDragOver = (event: DragEvent) => {
  event.dataTransfer!.dropEffect = 'copy';
  isDragOver.value = true;
};

const handleDragLeave = (event: DragEvent) => {
  if (event.relatedTarget === null || !event.currentTarget) {
    isDragOver.value = false;
    isHover.value = false;
  }
};

const handleDrop = (event: DragEvent) => {
  isDragOver.value = false;
  isHover.value = false;
  const files = event.dataTransfer?.files;
  if (files) handleFiles(files);
};

const triggerFileInput = () => {
  fileInput.value?.click();
};

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement;
  if (target.files) handleFiles(target.files);
  target.value = ''; // Reset for same file selection
};

const handleFiles = async (files: FileList) => {
  for (const file of Array.from(files)) {
    if (!validateFile(file)) continue;
    await uploadFile(file);
  }
};

const validateFile = (file: File): boolean => {
  const maxSize = 50 * 1024 * 1024; // 50 MB
  const allowedTypes = ['pdf', 'txt', 'md', 'docx', 'epub', 'fb2'];

  const ext = file.name.split('.').pop()?.toLowerCase() || '';

  if (!allowedTypes.includes(ext)) {
    error.value = `Unsupported file type: ${ext}`;
    return false;
  }

  if (file.size > maxSize) {
    error.value = `File too large: ${file.name} (max 50 MB)`;
    return false;
  }

  return true;
};

const uploadFile = async (file: File) => {
  const doc: UploadedDocument = {
    id: Date.now().toString(),
    name: file.name,
    size: file.size,
    type: file.name.split('.').pop() || 'unknown',
    status: 'uploading',
    progress: 0,
    timestamp: new Date()
  };

  documents.value.unshift(doc);
  isUploading.value = true;
  error.value = '';

  try {
    doc.status = 'indexing';

    // Simulate progress
    const progressInterval = setInterval(() => {
      if (doc.progress < 90) {
        doc.progress += 10;
      }
    }, 500);

    const result = await ragDocumentService.uploadDocument(file, chunkingSettings.value);

    clearInterval(progressInterval);
    doc.progress = 100;
    doc.status = 'ready';
    doc.chunksCount = result.chunksCount;
    doc.id = result.documentId;

  } catch (err: unknown) {
    doc.status = 'error';
    doc.error = err instanceof Error ? err.message : 'Upload failed';
    error.value = doc.error;
  } finally {
    isUploading.value = false;
  }
};

const deleteDocument = async (id: string) => {
  if (!confirm('Delete this document?')) return;

  try {
    await ragDocumentService.deleteDocument(id);
    documents.value = documents.value.filter(d => d.id !== id);
  } catch (err) {
    error.value = 'Failed to delete document';
  }
};

const viewChunks = (doc: UploadedDocument) => {
  // Navigate to library with document selected
  console.log('View chunks for:', doc.name);
};

const getFileIcon = (type: string): string => {
  return ragDocumentService.getFileIcon(type);
};

const formatSize = (bytes: number): string => {
  return ragDocumentService.formatSize(bytes);
};

const formatTime = (date: Date): string => {
  return ragDocumentService.formatRelativeTime(date);
};

onMounted(() => {
  console.log('DocumentUpload initialized');
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
    gap: $spacing-sm;
  }

  .settings-button,
  .nav-button {
    @include button-glass;
    text-decoration: none;

    &.active {
      background: rgba(255, 255, 255, 0.4);
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
  }

  .settings-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: $spacing-md;
  }

  .setting-item {
    display: flex;
    flex-direction: column;
    gap: $spacing-xs;

    label {
      font-size: $font-size-sm;
      font-weight: 600;
      color: $text-dark;
    }

    input, select {
      padding: $spacing-sm;
      border: 1px solid $border-light;
      border-radius: $radius-md;
      font-size: $font-size-base;
    }

    .hint {
      font-size: $font-size-xs;
      color: $text-muted;
    }
  }

  .reset-button {
    @include button-secondary;
    padding: $spacing-sm $spacing-md;
    font-size: $font-size-sm;
  }
}

.error-banner {
  background: rgba(255, 0, 0, 0.1);
  border-left: 4px solid #c33;
  padding: $spacing-sm $spacing-md;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: $error-text;

  .close-button {
    background: none;
    border: none;
    cursor: pointer;
    font-size: $font-size-lg;
    opacity: 0.7;

    &:hover {
      opacity: 1;
    }
  }
}

.content-area {
  flex: 1;
  padding: $spacing-lg;
  overflow-y: auto;
}

.dropzone {
  min-height: 300px;
  border: 3px dashed $border-light;
  border-radius: $radius-lg;
  background: rgba($primary-color, 0.03);
  cursor: pointer;
  transition: all $transition-base;
  @include flex-center;

  &:hover,
  &--hover {
    border-color: $primary-color;
    background: rgba($primary-color, 0.08);
    transform: scale(1.01);
  }

  &--drag-over {
    border-color: $primary-color;
    border-style: solid;
    background: rgba($primary-color, 0.12);
    transform: scale(1.02);
    box-shadow: 0 0 20px rgba($primary-color, 0.2);
  }

  &--uploading {
    opacity: 0.7;
    pointer-events: none;
  }
}

.dropzone-content {
  text-align: center;
  padding: $spacing-xl;

  .dropzone-icon {
    font-size: 72px;
    margin-bottom: $spacing-md;
  }

  .dropzone-title {
    font-size: 24px;
    font-weight: 600;
    color: $text-dark;
    margin-bottom: $spacing-sm;
  }

  .dropzone-subtitle {
    font-size: 16px;
    color: $text-muted;
    margin-bottom: $spacing-md;
  }

  .dropzone-formats {
    font-size: 14px;
    color: $text-light;
  }
}

.files-list {
  margin-top: $spacing-xl;

  h3 {
    font-size: $font-size-lg;
    margin-bottom: $spacing-md;
    color: $text-dark;
  }
}

.file-card {
  display: flex;
  align-items: center;
  padding: $spacing-md;
  margin-bottom: $spacing-sm;
  background: $bg-white;
  border: 1px solid $border-light;
  border-radius: $radius-md;
  transition: all $transition-base;

  &:hover {
    transform: translateY(-2px);
    box-shadow: $shadow-md;
    border-color: $primary-color;
  }

  &--error {
    border-color: $error-text;
    background: rgba(255, 0, 0, 0.02);
  }
}

.file-icon {
  font-size: 32px;
  margin-right: $spacing-md;
  flex-shrink: 0;
}

.file-info {
  flex: 1;
  min-width: 0;

  .file-name {
    font-size: 16px;
    font-weight: 600;
    color: $text-dark;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .file-meta {
    font-size: 14px;
    color: $text-muted;
    margin-top: $spacing-xs;
  }

  .file-error {
    font-size: 13px;
    color: $error-text;
    margin-top: $spacing-xs;
  }
}

.progress-container {
  display: flex;
  align-items: center;
  gap: $spacing-sm;
  margin-top: $spacing-sm;
}

.progress-bar {
  flex: 1;
  height: 6px;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: $primary-gradient;
  transition: width 0.5s ease;
}

.progress-text {
  font-size: 12px;
  color: $primary-color;
  font-weight: 600;
}

.file-status {
  margin: 0 $spacing-md;
  font-size: 20px;

  .spinner {
    display: inline-block;
    width: 20px;
    height: 20px;
    border: 2px solid $border-light;
    border-top-color: $primary-color;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }
}

.file-actions {
  display: flex;
  gap: $spacing-xs;
}

.action-button {
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

  &.action-delete:hover {
    background: rgba(255, 0, 0, 0.1);
  }
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>

