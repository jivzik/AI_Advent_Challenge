/**
 * Personalization Types - Minimal für Demo
 */

export interface PersonalizedChatRequest {
  message: string;
  useProfile: boolean;
}

export interface PersonalizedChatResponse {
  response: string;
  usedProfile: boolean;
  processingTimeMs: number;
  interactionId: number;
}

export interface UserProfile {
  userId: string;
  name: string;
  expertiseLevel: string;
  techStack: string[];
  preferredLanguage: string;
}
