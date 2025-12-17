const API_BASE_URL = 'http://localhost:8080/api/reminder';

export interface ReminderSummary {
  id: number;
  userId: string;
  summaryType: 'TASKS' | 'CALENDAR' | 'GENERAL' | 'DAILY_DIGEST' | 'WEEKLY_DIGEST';
  title: string;
  content: string;
  rawData?: string;
  createdAt: string;
  notifiedAt?: string;
  notified: boolean;
  itemsCount?: number;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  nextReminderAt?: string;
}

export interface ReminderStatus {
  schedulerEnabled: boolean;
  pendingNotifications: number;
  latestSummaryId: number | string;
  latestSummaryTime: string;
}

/**
 * Service für Reminder API Kommunikation
 */
export const reminderService = {
  /**
   * Manueller Trigger des Reminder-Workflows
   */
  async triggerReminder(userId: string = 'manual-trigger'): Promise<ReminderSummary> {
    const response = await fetch(
      `${API_BASE_URL}/trigger?userId=${encodeURIComponent(userId)}`,
      { method: 'POST' }
    );
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return response.json();
  },

  /**
   * Holt alle Summaries für einen Benutzer
   */
  async getSummaries(userId: string): Promise<ReminderSummary[]> {
    const response = await fetch(
      `${API_BASE_URL}/summaries?userId=${encodeURIComponent(userId)}`
    );
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return response.json();
  },

  /**
   * Holt die neueste Summary
   */
  async getLatestSummary(): Promise<ReminderSummary | null> {
    const response = await fetch(`${API_BASE_URL}/latest`);
    if (response.status === 204) {
      return null;
    }
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return response.json();
  },

  /**
   * Holt ausstehende Benachrichtigungen
   */
  async getPendingNotifications(): Promise<ReminderSummary[]> {
    const response = await fetch(`${API_BASE_URL}/pending`);
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return response.json();
  },

  /**
   * Holt den Scheduler-Status
   */
  async getStatus(): Promise<ReminderStatus> {
    const response = await fetch(`${API_BASE_URL}/status`);
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return response.json();
  }
};

