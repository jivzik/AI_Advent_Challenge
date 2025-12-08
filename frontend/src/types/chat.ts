export interface ChatRequest {
  message: string;
  userId?: string;
  conversationId?: string;
  jsonMode?: boolean;
  autoSchema?: boolean;
  jsonSchema?: string;
  systemPrompt?: string;
  temperature?: number;
}
export interface ChatResponse {
  reply: string;
  toolName: string;
  timestamp: string;
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
