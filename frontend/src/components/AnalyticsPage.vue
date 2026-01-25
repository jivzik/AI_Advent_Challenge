<template>
  <div class="analytics-container">
    <div class="analytics-header">
      <h1>📊 Local Data Analytics</h1>
      <p class="subtitle">Upload CSV/JSON/log files and ask questions to get AI-powered insights</p>
    </div>

    <div class="analytics-content">
      <!-- Upload Section -->
      <div class="upload-section">
        <div class="file-upload-area" :class="{ 'drag-over': isDragging }" @drop.prevent="handleDrop"
          @dragover.prevent="isDragging = true" @dragleave.prevent="isDragging = false">
          <input type="file" ref="fileInput" @change="handleFileSelect" accept=".csv,.json,.txt,.log" hidden />
          <div v-if="!selectedFile" class="upload-prompt" @click="handleFileClick">
            <div class="upload-icon">📁</div>
            <p>Click to upload or drag & drop</p>
            <p class="file-types">CSV, JSON, TXT, LOG (max 50MB)</p>
          </div>
          <div v-else class="file-selected">
            <div class="file-info">
              <span class="file-icon">📄</span>
              <span class="file-name">{{ selectedFile.name }}</span>
              <span class="file-size">({{ formatFileSize(selectedFile.size) }})</span>
            </div>
            <button @click="clearFile" class="clear-button">✕</button>
          </div>
        </div>

        <!-- Query Input -->
        <div class="query-section">
          <label for="query">Your Question:</label>
          <textarea id="query" v-model="query" placeholder="e.g., What errors occur most frequently?" rows="3"
            :disabled="isAnalyzing"></textarea>
        </div>

        <!-- Options -->
        <div class="options-section">
          <label class="checkbox-label">
            <input type="checkbox" v-model="detailedRecommendations" :disabled="isAnalyzing" />
            <span>Detailed recommendations</span>
          </label>
        </div>

        <!-- Submit Button -->
        <button @click="analyzeData" :disabled="!canAnalyze" class="analyze-button"
          :class="{ analyzing: isAnalyzing }">
          <span v-if="!isAnalyzing">🔍 Analyze Data</span>
          <span v-else>
            <span class="spinner"></span> Analyzing...
          </span>
        </button>

        <!-- Error Display -->
        <div v-if="error" class="error-message">
          <strong>Error:</strong> {{ error }}
        </div>
      </div>

      <!-- Results Section -->
      <div v-if="results" class="results-section">
        <h2>📈 Analysis Results</h2>

        <!-- Answer -->
        <div class="result-card answer-card">
          <h3>💡 AI Insights</h3>
          <div class="answer-content markdown-content" v-html="renderMarkdown(results.answer)"></div>
        </div>

        <!-- Key Insights -->
        <div v-if="results.insights && results.insights.length > 0" class="result-card insights-card">
          <h3>🎯 Key Insights</h3>
          <ul class="insights-list">
            <li v-for="(insight, index) in results.insights" :key="index">{{ insight }}</li>
          </ul>
        </div>

        <!-- Recommendations -->
        <div v-if="results.recommendations && results.recommendations.length > 0" class="result-card recommendations-card">
          <h3>✅ Recommendations</h3>
          <ul class="recommendations-list">
            <li v-for="(recommendation, index) in results.recommendations" :key="index">{{ recommendation }}</li>
          </ul>
        </div>

        <!-- Raw Data -->
        <div class="result-card raw-data-card">
          <h3>📊 Raw Analysis Data</h3>
          <button @click="toggleRawData" class="toggle-button">
            {{ showRawData ? '▼ Hide' : '▶ Show' }}
          </button>
          <pre v-if="showRawData" class="raw-data">{{ JSON.stringify(results.rawData, null, 2) }}</pre>
        </div>

        <!-- Metadata -->
        <div class="metadata">
          <span>📄 Format: {{ results.metadata.fileFormat }}</span>
          <span>📊 Rows: {{ results.metadata.rowsAnalyzed }}</span>
          <span>⏱️ Time: {{ results.metadata.processingTimeMs }}ms</span>
          <span>🕐 {{ formatTimestamp(results.metadata.timestamp) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import axios from 'axios';
import { marked } from 'marked';
import DOMPurify from 'dompurify';

// Configure marked for better code highlighting
marked.setOptions({
  breaks: true,
  gfm: true,
});

interface AnalyticsResponse {
  answer: string;
  rawData: Record<string, any>;
  insights: string[];
  recommendations: string[];
  metadata: {
    fileFormat: string;
    rowsAnalyzed: number;
    processingTimeMs: number;
    timestamp: string;
  };
}

const fileInput = ref<HTMLInputElement | null>(null);
const selectedFile = ref<File | null>(null);
const query = ref('');
const detailedRecommendations = ref(false);
const isAnalyzing = ref(false);
const isDragging = ref(false);
const error = ref('');
const results = ref<AnalyticsResponse | null>(null);
const showRawData = ref(false);

const canAnalyze = computed(() => {
  return selectedFile.value && query.value.trim() && !isAnalyzing.value;
});

const handleFileClick = () => {
  if (fileInput.value) {
    fileInput.value.click();
  }
};

const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement;
  if (target.files && target.files[0]) {
    const file = target.files[0];
    if (file.size > 50 * 1024 * 1024) {
      error.value = 'File must be < 50MB';
      return;
    }
    selectedFile.value = file;
    error.value = '';
  }
};

const handleDrop = (event: DragEvent) => {
  isDragging.value = false;
  if (event.dataTransfer?.files && event.dataTransfer.files[0]) {
    const file = event.dataTransfer.files[0];
    if (file.size > 50 * 1024 * 1024) {
      error.value = 'File must be < 50MB';
      return;
    }
    selectedFile.value = file;
    error.value = '';
  }
};

const clearFile = () => {
  selectedFile.value = null;
  if (fileInput.value) {
    fileInput.value.value = '';
  }
};

const analyzeData = async () => {
  if (!selectedFile.value || !query.value.trim()) {
    return;
  }

  isAnalyzing.value = true;
  error.value = '';
  results.value = null;

  try {
    const formData = new FormData();
    formData.append('file', selectedFile.value);
    formData.append('query', query.value);
    if (detailedRecommendations.value) {
      formData.append('detailedRecommendations', 'true');
    }

    const response = await axios.post<AnalyticsResponse>(
      'http://localhost:8091/api/analytics/analyze',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
        timeout: 120000, // 2 minutes
      }
    );

    results.value = response.data;
  } catch (err: any) {
    console.error('Analysis failed:', err);
    if (err.response?.data?.error) {
      error.value = err.response.data.error;
    } else if (err.code === 'ECONNABORTED') {
      error.value = 'Request timed out. Please try with a smaller file.';
    } else {
      error.value = err.message || 'Analysis failed. Please try again.';
    }
  } finally {
    isAnalyzing.value = false;
  }
};

const toggleRawData = () => {
  showRawData.value = !showRawData.value;
};

const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
};

const formatTimestamp = (timestamp: string): string => {
  return new Date(timestamp).toLocaleString();
};

const renderMarkdown = (text: string): string => {
  const rawHtml = marked(text) as string;
  return DOMPurify.sanitize(rawHtml);
};
</script>

<style scoped>
.analytics-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.analytics-header {
  text-align: center;
  margin-bottom: 30px;
}

.analytics-header h1 {
  font-size: 2rem;
  margin-bottom: 10px;
  color: #333;
}

.subtitle {
  color: #666;
  font-size: 1rem;
}

.analytics-content {
  display: grid;
  grid-template-columns: 1fr;
  gap: 30px;
}

@media (min-width: 768px) {
  .analytics-content {
    grid-template-columns: 400px 1fr;
  }
}

/* Upload Section */
.upload-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.file-upload-area {
  border: 2px dashed #ccc;
  border-radius: 8px;
  padding: 30px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
}

.file-upload-area:hover,
.file-upload-area.drag-over {
  border-color: #4CAF50;
  background-color: #f0f8f0;
}

.upload-icon {
  font-size: 3rem;
  margin-bottom: 10px;
}

.upload-prompt p {
  margin: 5px 0;
}

.file-types {
  color: #999;
  font-size: 0.875rem;
}

.file-selected {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px;
  background-color: #f5f5f5;
  border-radius: 8px;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.file-icon {
  font-size: 1.5rem;
}

.file-name {
  font-weight: 500;
}

.file-size {
  color: #666;
  font-size: 0.875rem;
}

.clear-button {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #999;
  transition: color 0.3s;
}

.clear-button:hover {
  color: #f44336;
}

/* Query Section */
.query-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.query-section label {
  font-weight: 500;
  color: #333;
}

.query-section textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
  font-family: inherit;
  resize: vertical;
}

/* Options */
.options-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}

.checkbox-label input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

/* Analyze Button */
.analyze-button {
  padding: 15px 30px;
  font-size: 1rem;
  font-weight: 600;
  background: linear-gradient(135deg, #4CAF50, #45a049);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.analyze-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #45a049, #3d8b40);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);
}

.analyze-button:disabled {
  background: #ccc;
  cursor: not-allowed;
  transform: none;
}

.analyze-button.analyzing {
  background: #2196F3;
}

.spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid #fff;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
  margin-right: 8px;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* Error Message */
.error-message {
  padding: 15px;
  background-color: #ffebee;
  color: #c62828;
  border-radius: 8px;
  border-left: 4px solid #c62828;
}

/* Results Section */
.results-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.results-section h2 {
  font-size: 1.5rem;
  color: #333;
  margin-bottom: 10px;
}

.result-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.result-card h3 {
  font-size: 1.25rem;
  margin-bottom: 15px;
  color: #333;
}

.answer-card {
  border-left: 4px solid #2196F3;
}

.answer-content {
  white-space: pre-wrap;
  line-height: 1.6;
  color: #555;
}

/* Markdown Content Styling */
.markdown-content {
  white-space: normal;
}

.markdown-content h1,
.markdown-content h2,
.markdown-content h3,
.markdown-content h4 {
  color: #333;
  margin-top: 1em;
  margin-bottom: 0.5em;
  font-weight: 600;
}

.markdown-content h1 {
  font-size: 1.75rem;
  border-bottom: 2px solid #e0e0e0;
  padding-bottom: 0.3em;
}

.markdown-content h2 {
  font-size: 1.5rem;
  border-bottom: 1px solid #e0e0e0;
  padding-bottom: 0.3em;
}

.markdown-content h3 {
  font-size: 1.25rem;
}

.markdown-content h4 {
  font-size: 1.1rem;
}

.markdown-content p {
  margin: 0.8em 0;
  line-height: 1.6;
}

.markdown-content ul,
.markdown-content ol {
  margin: 0.8em 0;
  padding-left: 2em;
}

.markdown-content li {
  margin: 0.4em 0;
  line-height: 1.6;
}

.markdown-content code {
  background-color: #f5f5f5;
  padding: 0.2em 0.4em;
  border-radius: 3px;
  font-family: 'Courier New', Courier, monospace;
  font-size: 0.9em;
  color: #d63384;
}

.markdown-content pre {
  background-color: #f5f5f5;
  padding: 1em;
  border-radius: 6px;
  overflow-x: auto;
  margin: 1em 0;
}

.markdown-content pre code {
  background-color: transparent;
  padding: 0;
  color: #333;
  font-size: 0.875rem;
}

.markdown-content blockquote {
  border-left: 4px solid #2196F3;
  padding-left: 1em;
  margin: 1em 0;
  color: #666;
  font-style: italic;
}

.markdown-content table {
  border-collapse: collapse;
  width: 100%;
  margin: 1em 0;
}

.markdown-content th,
.markdown-content td {
  border: 1px solid #ddd;
  padding: 8px 12px;
  text-align: left;
}

.markdown-content th {
  background-color: #f5f5f5;
  font-weight: 600;
}

.markdown-content a {
  color: #2196F3;
  text-decoration: none;
}

.markdown-content a:hover {
  text-decoration: underline;
}

.markdown-content strong {
  font-weight: 600;
  color: #333;
}

.markdown-content em {
  font-style: italic;
}

.markdown-content hr {
  border: none;
  border-top: 2px solid #e0e0e0;
  margin: 1.5em 0;
}

.insights-card {
  border-left: 4px solid #FF9800;
}

.insights-list {
  list-style: none;
  padding: 0;
}

.insights-list li {
  padding: 10px;
  margin-bottom: 8px;
  background-color: #fff3e0;
  border-radius: 6px;
  color: #e65100;
}

.recommendations-card {
  border-left: 4px solid #4CAF50;
}

.recommendations-list {
  list-style: none;
  padding: 0;
}

.recommendations-list li {
  padding: 10px;
  margin-bottom: 8px;
  background-color: #e8f5e9;
  border-radius: 6px;
  color: #2e7d32;
}

.raw-data-card {
  border-left: 4px solid #9C27B0;
}

.toggle-button {
  padding: 8px 16px;
  background-color: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.875rem;
  margin-bottom: 10px;
  transition: background-color 0.3s;
}

.toggle-button:hover {
  background-color: #e0e0e0;
}

.raw-data {
  background-color: #f5f5f5;
  padding: 15px;
  border-radius: 6px;
  overflow-x: auto;
  font-size: 0.875rem;
  line-height: 1.4;
}

.metadata {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  padding: 15px;
  background-color: #f9f9f9;
  border-radius: 8px;
  font-size: 0.875rem;
  color: #666;
}

.metadata span {
  display: flex;
  align-items: center;
  gap: 5px;
}
</style>
