import type { Message } from '../types/types';

const OPENROUTER_API_BASE_URL = 'http://localhost:8084/api/v1/openrouter/chat';

/**
 * Response from GET /api/v1/openrouter/chat/conversations
 */
interface GetConversationsResponse {
  conversations: ConversationSummary[];
  count: number;
  status: string;
}

/**
 * Response from GET /api/v1/openrouter/chat/conversations/{id}/history
 */
interface GetConversationHistoryResponse {
  conversationId: string;
  messages: Message[];
  messageCount: number;
  status: string;
}

/**
 * Conversation summary for sidebar
 */
interface ConversationSummary {
  conversationId: string;
  firstMessage: string;
  lastMessageTime: string;
  messageCount: number;
  hasCompression?: boolean;
}

/**
 * OpenRouter Chat Service - для управления конверсациями в Tools
 *
 * Аналогично ChatService но для OpenRouter Tools эндпоинтов
 */

interface SendMessageOptions {
  message: string;
  conversationId: string;
  systemPrompt?: string;
  temperature?: number;
  model?: string;
}

interface SendMessageResponse {
  reply: string;
  timestamp: string;
  model: string;
}

export class OpenRouterChatService {
  /**
   * Отправить сообщение и получить ответ
   */
  static async sendMessage(options: SendMessageOptions): Promise<SendMessageResponse> {
    try {
      const request = {
        message: options.message,
        conversationId: options.conversationId,
        systemPrompt: options.systemPrompt,
        temperature: options.temperature,
        model: options.model
      };

      const response = await fetch(`${OPENROUTER_API_BASE_URL}/full`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(request)
      });

      if (!response.ok) {
        throw new Error(`Failed to send message: ${response.status}`);
      }

      const data = await response.json();
      return {
        reply: data.reply || data.message || '',
        timestamp: data.timestamp || new Date().toISOString(),
        model: data.model || ''
      };
    } catch (error) {
      console.error('Error sending message:', error);
      throw error;
    }
  }

  /**
   * Получить список всех конверсаций (для сайдбара)
   */
  static async getConversations(): Promise<GetConversationsResponse> {
    try {
      const response = await fetch(`${OPENROUTER_API_BASE_URL}/conversations`);

      if (!response.ok) {
        console.warn(`OpenRouter API returned ${response.status}, returning empty conversations`);
        // Return empty conversations instead of throwing
        return {
          conversations: [],
          count: 0,
          status: 'error'
        };
      }

      return await response.json();
    } catch (error) {
      console.error('Error fetching conversations:', error);
      // Return empty conversations on error instead of throwing
      return {
        conversations: [],
        count: 0,
        status: 'error'
      };
    }
  }

  /**
   * Получить полную историю конверсации
   */
  static async getConversationHistory(conversationId: string): Promise<Message[]> {
    try {
      const response = await fetch(
        `${OPENROUTER_API_BASE_URL}/conversations/${conversationId}/history`
      );
      if (!response.ok) {
        console.warn(`Failed to fetch history: ${response.status}`);
        return [];
      }
      const data: GetConversationHistoryResponse = await response.json();
      return data.messages || [];
    } catch (error) {
      console.error('Error fetching conversation history:', error);
      return [];
    }
  }

  /**
   * Удалить конверсацию
   */
  static async deleteConversation(conversationId: string): Promise<void> {
    try {
      const response = await fetch(
        `${OPENROUTER_API_BASE_URL}/conversations/${conversationId}`,
        { method: 'DELETE' }
      );
      if (!response.ok) {
        console.warn(`Failed to delete conversation: ${response.status}`);
      }
    } catch (error) {
      console.error('Error deleting conversation:', error);
    }
  }

  /**
   * Очистить историю конверсации
   */
  static async clearConversationHistory(conversationId: string): Promise<void> {
    try {
      const response = await fetch(
        `${OPENROUTER_API_BASE_URL}/conversations/${conversationId}/clear`,
        { method: 'POST' }
      );
      if (!response.ok) {
        console.warn(`Failed to clear history: ${response.status}`);
      }
    } catch (error) {
      console.error('Error clearing conversation history:', error);
    }
  }

  /**
   * Health check
   */
  static async checkHealth(): Promise<boolean> {
    try {
      const response = await fetch(`${OPENROUTER_API_BASE_URL}/health`);
      return response.ok;
    } catch (error) {
      console.error('Health check failed:', error);
      return false;
    }
  }
}

