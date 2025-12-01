import type { ChatRequest, ChatResponse, ErrorResponse } from '../types/chat';
const API_BASE_URL = 'http://localhost:8080/api/chat';
export class ChatService {
  static async sendMessage(message: string, userId?: string): Promise<ChatResponse> {
    const request: ChatRequest = {
      message,
      userId
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
}
