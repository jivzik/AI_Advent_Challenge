import type { ConversationSummary, Message, CompressionInfo, ChatRequest, ChatResponse, ErrorResponse } from '../types/types';

const API_BASE_URL = 'http://localhost:8080/api/chat';
const MEMORY_API_BASE_URL = 'http://localhost:8080/api/memory';

interface SendMessageOptions {
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
 * Response from GET /api/memory/conversations
 */
interface GetConversationsResponse {
  totalConversations: number;
  conversations: ConversationSummary[];
  timestamp: string;
}

export class ChatService {
  // =========================================
  // CHAT METHODS (original)
  // =========================================

  /**
   * Send message with options object (new method for better ergonomics)
   */
  static async sendMessageWithOptions(options: SendMessageOptions): Promise<ChatResponse> {
    const request: ChatRequest = {
      message: options.message,
      userId: options.userId,
      conversationId: options.conversationId,
      jsonMode: options.jsonMode || false,
      autoSchema: options.autoSchema || false,
      jsonSchema: options.jsonSchema,
      systemPrompt: options.systemPrompt,
      temperature: options.temperature,
      provider: options.provider,
      model: options.model
    };

    try {
      const response = await fetch(API_BASE_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        const errorData: ErrorResponse = await response.json();
        throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
      }

      const data: ChatResponse = await response.json();
      return data;
    } catch (error) {
      if (error instanceof Error) {
        throw error;
      }
      throw new Error('An unexpected error occurred');
    }
  }

  /**
   * Original sendMessage method (kept for backward compatibility)
   */
  static async sendMessage(
      message: string,
      userId: string | undefined,
      conversationId: string | undefined,
      jsonMode: boolean,
      autoSchema: boolean = false
  ): Promise<ChatResponse> {
    const request: ChatRequest = {
      message,
      userId,
      conversationId,
      jsonMode,
      autoSchema
    };
    try {
      const response = await fetch(API_BASE_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      });
      if (!response.ok) {
        const errorData: ErrorResponse = await response.json();
        throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
      }
      const data: ChatResponse = await response.json();
      return data;
    } catch (error) {
      if (error instanceof Error) {
        throw error;
      }
      throw new Error('An unexpected error occurred');
    }
  }

  /**
   * Check backend health
   */
  static async checkHealth(): Promise<{ status: string; timestamp: string }> {
    try {
      const response = await fetch(`${API_BASE_URL}/health`);
      if (!response.ok) {
        throw new Error('Health check failed');
      }
      return await response.json();
    } catch (error) {
      throw new Error('Backend is not reachable');
    }
  }

  /**
   * Clear conversation history
   */
  static async clearConversation(conversationId: string): Promise<void> {
    try {
      const response = await fetch(`${API_BASE_URL}/conversation/${conversationId}`, {
        method: 'DELETE',
      });
      if (!response.ok) {
        throw new Error('Failed to clear conversation');
      }
    } catch (error) {
      throw new Error('Failed to clear conversation history');
    }
  }

  /**
   * Get conversation stats
   */
  static async getStats(): Promise<{ activeConversations: number; timestamp: string }> {
    try {
      const response = await fetch(`${API_BASE_URL}/stats`);
      if (!response.ok) {
        throw new Error('Failed to get stats');
      }
      return await response.json();
    } catch (error) {
      throw new Error('Failed to get conversation stats');
    }
  }

  /**
   * Get compression info for a conversation
   */
  static async getCompressionInfo(conversationId: string): Promise<CompressionInfo | null> {
    try {
      const response = await fetch(`${API_BASE_URL}/compression-info/${conversationId}`);
      if (!response.ok) {
        console.warn('Compression info not available');
        return null;  // ⭐ Kein Error!
      }
      return await response.json();
    } catch (error) {
      console.warn('Failed to fetch compression info:', error);
      return null;  // ⭐ Kein Error!
    }
  }

  // =========================================
  // MEMORY/PERSISTENCE METHODS (new)
  // =========================================

  /**
   * Get list of all conversations
   */
  static async getConversations(): Promise<GetConversationsResponse> {
    try {
      const response = await fetch(`${MEMORY_API_BASE_URL}/conversations`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        console.warn(`Failed to fetch conversations: ${response.status}`);
        // Return empty conversations instead of throwing
        return {
          totalConversations: 0,
          conversations: [],
          timestamp: new Date().toISOString()
        };
      }

      return response.json();
    } catch (error) {
      console.error('Error fetching conversations:', error);
      // Return empty conversations on error instead of throwing
      return {
        totalConversations: 0,
        conversations: [],
        timestamp: new Date().toISOString()
      };
    }
  }

  /**
   * Get full message history for a conversation
   */
  static async getConversationHistory(conversationId: string): Promise<Message[]> {
    try {
      const response = await fetch(`${MEMORY_API_BASE_URL}/conversation/${conversationId}`);
      if (!response.ok) {
        console.warn(`Failed to fetch conversation history: ${response.status}`);
        return [];
      }
      const data = await response.json();
      return data.messages || [];
    } catch (error) {
      console.error('Error fetching conversation history:', error);
      return [];
    }
  }

  /**
   * Delete a conversation from memory
   */
  static async deleteConversation(conversationId: string): Promise<void> {
    try {
      const response = await fetch(`${MEMORY_API_BASE_URL}/conversation/${conversationId}`, {
        method: 'DELETE',
      });
      if (!response.ok) {
        console.warn(`Failed to delete conversation: ${response.status}`);
      }
    } catch (error) {
      console.error('Error deleting conversation:', error);
    }
  }
}