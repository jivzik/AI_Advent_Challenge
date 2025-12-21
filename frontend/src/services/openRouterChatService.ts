
const API_BASE_URL = 'http://localhost:8084/api/v1/openrouter/tools/chat';

/**
 * Request for OpenRouter tools chat
 */
export interface OpenRouterChatRequest {
  message: string;
  conversationId?: string;
  systemPrompt?: string;
  temperature?: number;
  model?: string;
}

/**
 * Response from OpenRouter tools chat
 */
export interface OpenRouterChatResponse {
  reply: string;
  model?: string;
  timestamp: string;
  toolsUsed?: string[];
}

/**
 * Service for OpenRouter tools chat API
 */
export class OpenRouterChatService {
  /**
   * Send message to OpenRouter tools chat
   */
  static async sendMessage(options: OpenRouterChatRequest): Promise<OpenRouterChatResponse> {
    try {
      const response = await fetch(API_BASE_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(options),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
      }

      const data: OpenRouterChatResponse = await response.json();
      return data;
    } catch (error) {
      if (error instanceof Error) {
        throw error;
      }
      throw new Error('An unexpected error occurred');
    }
  }
}

