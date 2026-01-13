// ============================================
// TYPES.TS - All TypeScript Interfaces
// Central type definitions for the application
// ============================================

// ============================================
// MESSAGE TYPES
// ============================================

/**
 * Message in a conversation
 * Compatible with both frontend (Date) and backend (string) timestamps
 */
export interface Message {
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp?: Date | string; // ⭐ Both Date and string supported!

  // Developer Assistant fields
  isDeveloperMode?: boolean;        // Ответ от Developer Assistant
  sources?: string[];                // Источники из RAG (документы)
  gitContext?: GitContext;           // Git контекст из ответа
  codeBlocks?: CodeBlock[];          // Извлеченные code blocks
  suggestedFiles?: string[];         // Рекомендованные файлы
}

/**
 * Git context extracted from developer response
 */
export interface GitContext {
  currentBranch?: string;
  modifiedFiles?: string[];
  addedFiles?: string[];
  deletedFiles?: string[];
}

/**
 * Code block with syntax highlighting info
 */
export interface CodeBlock {
  language: string;
  code: string;
  lineNumbers?: boolean;
}

/**
 * Developer Assistant status
 */
export interface DevAssistantStatus {
  ragAvailable: boolean;
  gitAvailable: boolean;
  mcpServiceAvailable: boolean;
}

/**
 * Message with full metadata
 */
export interface MessageWithMetadata extends Message {
  id?: string;
  conversationId?: string;
  userId?: string;
  metrics?: ResponseMetrics;
  isCompressed?: boolean;
  compressedMessagesCount?: number;
  compressionTimestamp?: Date | string;
}

// ============================================
// CONVERSATION TYPES
// ============================================

/**
 * Conversation summary for sidebar list
 */
export interface ConversationSummary {
  conversationId: string;
  firstMessage: string;
  lastMessageTime: string;
  messageCount: number;
  hasCompression: boolean;
  userId: string;
  compressedMessageCount: number;
}

/**
 * Grouped conversations by date
 */
export interface GroupedConversations {
  today: ConversationSummary[];
  yesterday: ConversationSummary[];
  lastWeek: ConversationSummary[];
  older: ConversationSummary[];
}

/**
 * Response from backend when fetching conversation history
 */
export interface ConversationHistoryResponse {
  conversationId: string;
  messageCount: number;
  messages: Message[];
}

// ============================================
// CHAT API TYPES
// ============================================

/**
 * Chat request to backend
 */
export interface ChatRequest {
  message: string;
  userId?: string;
  conversationId?: string;
  jsonMode?: boolean;
  autoSchema?: boolean;
  jsonSchema?: string;
  systemPrompt?: string;
  temperature?: number;
  provider?: string;
  model?: string;
}

/**
 * Chat response from backend
 */
export interface ChatResponse {
  reply: string;
  toolName: string;
  timestamp: string;
  metrics?: ResponseMetrics;
}

/**
 * Error response from backend
 */
export interface ErrorResponse {
  error: string;
  message: string;
  timestamp: string;
}

// ============================================
// METRICS TYPES
// ============================================

/**
 * Response metrics (tokens, cost, time)
 */
export interface ResponseMetrics {
  inputTokens: number | null;
  outputTokens: number | null;
  totalTokens: number | null;
  cost: number | null;
  responseTimeMs: number | null;
  model: string | null;
  provider: string | null;
}

/**
 * Compression information
 */
export interface CompressionInfo {
  compressed: boolean;
  fullHistorySize: number;
  compressedHistorySize: number;  // ⭐ Added
  reductionPercentage: number;
  compressionRatio: string;
  messagesSaved: number;          // ⭐ Added
  tokensSaved: number;
  estimatedTokensSaved: number;   // ⭐ Added
  costSaved: number;
}

// ============================================
// UTILITY FUNCTIONS
// ============================================

/**
 * Group conversations by date (Today, Yesterday, Last Week, Older)
 */
export function groupConversationsByDate(conversations: ConversationSummary[]): GroupedConversations {
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const yesterday = new Date(today);
  yesterday.setDate(yesterday.getDate() - 1);
  const lastWeek = new Date(today);
  lastWeek.setDate(lastWeek.getDate() - 7);

  const grouped: GroupedConversations = {
    today: [],
    yesterday: [],
    lastWeek: [],
    older: []
  };

  conversations.forEach(conv => {
    const convDate = new Date(conv.lastMessageTime);

    if (convDate >= today) {
      grouped.today.push(conv);
    } else if (convDate >= yesterday) {
      grouped.yesterday.push(conv);
    } else if (convDate >= lastWeek) {
      grouped.lastWeek.push(conv);
    } else {
      grouped.older.push(conv);
    }
  });

  return grouped;
}

/**
 * Format relative time (e.g., "5m ago", "2h ago", "3d ago")
 */
export function formatRelativeTime(timestamp: string | Date): string {
  const date = typeof timestamp === 'string' ? new Date(timestamp) : timestamp;
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffSeconds = Math.floor(diffMs / 1000);
  const diffMinutes = Math.floor(diffSeconds / 60);
  const diffHours = Math.floor(diffMinutes / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffSeconds < 60) {
    return 'just now';
  } else if (diffMinutes < 60) {
    return `${diffMinutes}m ago`;
  } else if (diffHours < 24) {
    return `${diffHours}h ago`;
  } else if (diffDays < 7) {
    return `${diffDays}d ago`;
  } else {
    return date.toLocaleDateString();
  }
}

// ============================================
// TYPE GUARDS
// ============================================

/**
 * Check if object is a valid Message
 */
export function isMessage(obj: any): obj is Message {
  return (
      obj &&
      typeof obj === 'object' &&
      typeof obj.role === 'string' &&
      ['user', 'assistant', 'system'].includes(obj.role) &&
      typeof obj.content === 'string'
  );
}

/**
 * Check if object is a valid ConversationSummary
 */
export function isConversationSummary(obj: any): obj is ConversationSummary {
  return (
      obj &&
      typeof obj === 'object' &&
      typeof obj.conversationId === 'string' &&
      typeof obj.firstMessage === 'string' &&
      typeof obj.messageCount === 'number'
  );
}