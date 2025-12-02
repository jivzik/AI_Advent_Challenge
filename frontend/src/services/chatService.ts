import type { ChatRequest, ChatResponse, ErrorResponse } from '../types/chat';
const API_BASE_URL = 'http://localhost:8080/api/chat';
export class ChatService {
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
}
