// ============================================
// TEAM ASSISTANT TYPES
// TypeScript interfaces for Team Assistant
// ============================================

/**
 * Team Assistant message with metadata
 */
export interface TeamAssistantMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  metadata?: TeamAssistantMetadata;
}

/**
 * Metadata for Team Assistant responses
 */
export interface TeamAssistantMetadata {
  sources?: string[];
  toolsUsed?: string[];
  confidenceScore?: number;
  queryType?: QueryType;
  responseTimeMs?: number;
  actions?: string[];
}

/**
 * Query types supported by Team Assistant
 */
export type QueryType =
  | 'ANSWER_QUESTION'
  | 'SHOW_TASKS'
  | 'CREATE_TASK'
  | 'ANALYZE_PRIORITY'
  | 'EXPLAIN_ARCHITECTURE'
  | 'LIST_BUGS'
  | 'SEARCH_DOCS'
  | 'GENERAL';

/**
 * User context for Team Assistant
 */
export interface UserContext {
  email: string;
  role: 'developer' | 'pm' | 'designer' | 'qa';
  team: 'backend' | 'frontend' | 'devops' | 'mobile';
}

/**
 * Team Assistant API request
 */
export interface TeamAssistantRequest {
  userEmail: string;
  query: string;
  sessionId?: string;
}

/**
 * Team Assistant API response
 */
export interface TeamAssistantResponse {
  answer: string;
  queryType: QueryType;
  sources: string[];
  toolsUsed: string[];
  actions: string[];
  confidenceScore: number;
  responseTimeMs: number;
  timestamp: string;
}

/**
 * Confidence level helper
 */
export function getConfidenceLevel(score: number): {
  label: string;
  color: string;
  icon: string;
} {
  if (score >= 0.9) {
    return { label: 'High confidence', color: 'success', icon: '●' };
  } else if (score >= 0.7) {
    return { label: 'Medium confidence', color: 'warning', icon: '●' };
  } else {
    return { label: 'Low confidence', color: 'info', icon: '●' };
  }
}

/**
 * Format response time
 */
export function formatResponseTime(ms: number): string {
  if (ms < 1000) {
    return `${ms}ms`;
  }
  return `${(ms / 1000).toFixed(1)}s`;
}

/**
 * Get tool color based on tool name
 */
export function getToolColor(tool: string): string {
  if (tool.includes('rag:')) return 'purple';
  if (tool.includes('git:')) return 'cyan';
  if (tool.includes('search:')) return 'blue';
  if (tool.includes('task:')) return 'green';
  return 'gray';
}

