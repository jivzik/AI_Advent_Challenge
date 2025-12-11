export interface ChatRequest {
  message: string;
  userId?: string;
  conversationId?: string;
  jsonMode?: boolean;
  autoSchema?: boolean;
  jsonSchema?: string;
  systemPrompt?: string;
  temperature?: number;
  provider?: string; // 'perplexity' or 'openrouter'
  model?: string; // Optional model selection for OpenRouter
}

export interface ResponseMetrics {
  inputTokens: number | null;
  outputTokens: number | null;
  totalTokens: number | null;
  cost: number | null;
  responseTimeMs: number | null;
  model: string | null;
  provider: string | null;
}

export interface CompressionInfo {
  conversationId: string;
  fullHistorySize: number;
  compressedHistorySize: number;
  compressed: boolean;
  messagesSaved: number;
  compressionRatio: string;
  estimatedTokensSaved: number;
  timestamp: string;
}

export interface ChatResponse {
  reply: string;
  toolName: string;
  timestamp: string;
  metrics?: ResponseMetrics; // Optional metrics from API response
}
export interface ChatMessage {
  id: string;
  content: string;
  isUser: boolean;
  timestamp: Date;
  toolName?: string;
}
export interface ErrorResponse {
  error: string;
  message: string;
  timestamp: string;
}
