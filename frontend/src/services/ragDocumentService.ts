/**
 * RAG Document Service - API calls for RAG system
 */

const RAG_API_URL = 'http://localhost:8086/api';

// Types
export interface UploadedDocument {
  id: string;
  name: string;
  size: number;
  type: string;
  status: 'uploading' | 'indexing' | 'ready' | 'error';
  progress: number;
  chunksCount?: number;
  tokensCount?: number;
  error?: string;
  timestamp: Date;
}

export interface Document {
  id: string;
  fileName: string;
  fileType: string;
  fileSize: number;
  chunkCount: number;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface Chunk {
  index: number;
  text: string;
  tokensCount: number;
}

export interface SearchResult {
  documentName: string;
  chunkIndex: number;
  chunkText: string;
  /** Semantic similarity score (0-1) */
  similarity: number;
  /** Keyword relevance score (0-1) - используется для keyword search */
  relevance?: number;
  tokensCount?: number;
  keywordScore?: number;
  metadata?: Record<string, unknown>;
}

export interface SearchResponse {
  results: SearchResult[];
  processingTime: string;
}

export interface LibraryStats {
  totalDocuments: number;
  totalChunks: number;
  totalSearches: number;
  todayDocuments: number;
  todayChunks: number;
  todaySearches: number;
}

export interface ChunkingSettings {
  chunkSize: number;
  overlapSize: number;
  strategy: 'recursive' | 'sentence' | 'paragraph';
}

export interface IndexResponse {
  documentId: string;
  chunksCount: number;
  status: string;
  processingTime: string;
}

class RagDocumentService {
  /**
   * Upload and index a document
   */
  async uploadDocument(file: File, settings?: ChunkingSettings): Promise<IndexResponse> {
    const formData = new FormData();
    formData.append('file', file);

    if (settings) {
      formData.append('chunkSize', settings.chunkSize.toString());
      formData.append('overlap', settings.overlapSize.toString());
      formData.append('strategy', settings.strategy);
    }

    const response = await fetch(`${RAG_API_URL}/index/document`, {
      method: 'POST',
      body: formData,
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error?.message || 'Upload failed');
    }

    return response.json();
  }

  /**
   * Get all indexed documents
   */
  async getDocuments(): Promise<Document[]> {
    const response = await fetch(`${RAG_API_URL}/documents`);

    if (!response.ok) {
      throw new Error('Failed to load documents');
    }

    return response.json();
  }

  /**
   * Get indexed documents list (simplified)
   */
  async getIndexedDocuments(): Promise<Array<{documentName: string; chunksCount: number; createdAt: string}>> {
    const response = await fetch(`${RAG_API_URL}/index/documents`);

    if (!response.ok) {
      throw new Error('Failed to load documents');
    }

    return response.json();
  }

  /**
   * Get document by ID
   */
  async getDocument(id: string): Promise<Document> {
    const response = await fetch(`${RAG_API_URL}/documents/${id}`);

    if (!response.ok) {
      throw new Error('Document not found');
    }

    return response.json();
  }

  /**
   * Delete document by ID
   */
  async deleteDocument(id: string): Promise<void> {
    const response = await fetch(`${RAG_API_URL}/documents/${id}`, {
      method: 'DELETE',
    });

    if (!response.ok) {
      throw new Error('Failed to delete document');
    }
  }

  /**
   * Delete document by name
   */
  async deleteDocumentByName(name: string): Promise<void> {
    const response = await fetch(`${RAG_API_URL}/index/document/${encodeURIComponent(name)}`, {
      method: 'DELETE',
    });

    if (!response.ok) {
      throw new Error('Failed to delete document');
    }
  }

  /**
   * Search with support for semantic, keyword, and hybrid modes
   */
  async search(
    query: string,
    topK: number = 5,
    threshold: number = 0.7,
    documents?: string[],
    searchMode: 'semantic' | 'keyword' | 'hybrid' = 'semantic',
    semanticWeight: number = 0.5
  ): Promise<SearchResponse> {
    const response = await fetch(`${RAG_API_URL}/search`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        query,
        topK,
        threshold,
        documents,
        searchMode,
        semanticWeight,
      }),
    });

    if (!response.ok) {
      throw new Error('Search failed');
    }

    const data = await response.json();

    // Нормализуем результаты - бэкенд может возвращать relevance вместо similarity
    if (data.results) {
      data.results = data.results.map((r: any) => ({
        ...r,
        // Используем similarity если есть, иначе relevance
        similarity: r.similarity ?? r.relevance ?? 0
      }));
    }

    return data;
  }

  /**
   * Get library statistics
   */
  async getStats(): Promise<LibraryStats> {
    // For now, calculate from documents
    const docs = await this.getDocuments();

    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const todayDocs = docs.filter(d => new Date(d.createdAt) >= today);

    return {
      totalDocuments: docs.length,
      totalChunks: docs.reduce((sum, d) => sum + (d.chunkCount || 0), 0),
      totalSearches: 0, // Would need backend tracking
      todayDocuments: todayDocs.length,
      todayChunks: todayDocs.reduce((sum, d) => sum + (d.chunkCount || 0), 0),
      todaySearches: 0,
    };
  }

  /**
   * Get file type icon emoji
   */
  getFileIcon(type: string): string {
    const icons: Record<string, string> = {
      pdf: '📄',
      txt: '📝',
      md: '📋',
      markdown: '📋',
      docx: '📘',
      doc: '📘',
      epub: '📖',
      fb2: '📖',
      code: '💻',
      html: '🌐',
      xml: '📰',
    };
    return icons[type.toLowerCase()] || '📄';
  }

  /**
   * Format file size
   */
  formatSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  /**
   * Format relative time
   */
  formatRelativeTime(date: Date | string): string {
    const now = new Date();
    const d = typeof date === 'string' ? new Date(date) : date;
    const diff = now.getTime() - d.getTime();

    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (minutes < 1) return 'just now';
    if (minutes < 60) return `${minutes} min ago`;
    if (hours < 24) return `${hours} hours ago`;
    if (days < 7) return `${days} days ago`;

    return d.toLocaleDateString();
  }
}

export const ragDocumentService = new RagDocumentService();

