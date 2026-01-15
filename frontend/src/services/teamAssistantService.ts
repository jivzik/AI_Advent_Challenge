import type {
  TeamAssistantRequest,
  TeamAssistantResponse,
} from '../types/team-assistant';

const BASE_URL = 'http://localhost:8089/api/team-assistant';

export class TeamAssistantService {
  /**
   * Send a query to the Team Assistant API
   */
  static async sendQuery(request: TeamAssistantRequest): Promise<TeamAssistantResponse> {
    try {
      const response = await fetch(`${BASE_URL}/query`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(
          errorData.message || `HTTP error! status: ${response.status}`
        );
      }

      return await response.json();
    } catch (error: any) {
      console.error('❌ Team Assistant API error:', error);
      throw new Error(
        error.message || 'Failed to get response from Team Assistant'
      );
    }
  }

  /**
   * Health check for Team Assistant service
   */
  static async healthCheck(): Promise<boolean> {
    try {
      const response = await fetch(`${BASE_URL}/health`);
      return response.ok;
    } catch (error) {
      console.error('❌ Team Assistant health check failed:', error);
      return false;
    }
  }
}

