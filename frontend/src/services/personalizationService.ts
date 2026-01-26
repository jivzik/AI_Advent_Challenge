/**
 * Personalization API - Minimal für Demo
 */

import type {
  PersonalizedChatRequest,
  PersonalizedChatResponse,
  UserProfile
} from '../types/personalization.types';

const API_BASE_URL = 'http://localhost:8084/api';

/**
 * Get user profile
 */
export async function getProfile(userId: string): Promise<UserProfile> {
  try {
    const response = await fetch(`${API_BASE_URL}/profile?userId=${userId}`);

    if (!response.ok) {
      throw new Error(`Failed to fetch profile: ${response.statusText}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Error fetching profile:', error);
    throw error;
  }
}

/**
 * Send personalized chat message
 */
export async function sendMessage(
  userId: string,
  message: string,
  useProfile: boolean
): Promise<PersonalizedChatResponse> {
  try {
    const response = await fetch(
      `${API_BASE_URL}/chat/personalized?userId=${userId}`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          message,
          useProfile
        } as PersonalizedChatRequest)
      }
    );

    if (!response.ok) {
      throw new Error(`Failed to send message: ${response.statusText}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Error sending message:', error);
    throw error;
  }
}

/**
 * Submit feedback for interaction
 */
export async function submitFeedback(
  interactionId: number,
  feedback: number
): Promise<void> {
  try {
    const response = await fetch(`${API_BASE_URL}/chat/feedback`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        interactionId,
        feedback
      })
    });

    if (!response.ok) {
      throw new Error(`Failed to submit feedback: ${response.statusText}`);
    }
  } catch (error) {
    console.error('Error submitting feedback:', error);
    throw error;
  }
}
