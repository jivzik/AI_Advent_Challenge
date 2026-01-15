const SUPPORT_API_BASE_URL = 'http://localhost:8088/api/support';

/**
 * Request to send message
 */
interface SendMessageRequest {
    userEmail: string;
    message: string;
    ticketNumber?: string;
    category?: string;
    priority?: string;
    orderId?: string;
    productId?: string;
    errorCode?: string;
}

/**
 * Response from support chat
 */
interface SendMessageResponse {
    ticketNumber: string;
    status: string;
    answer: string;
    isAiGenerated: boolean;
    confidenceScore?: number;
    sources?: string[];
    needsHumanAgent?: boolean;
    escalationReason?: string;
    timestamp: string;
    messageCount?: number;
    firstResponseAt?: string;
    slaBreached?: boolean;
}

/**
 * Ticket message from history
 */
interface TicketMessage {
    senderType: string;
    senderName: string;
    message: string;
    isAiGenerated: boolean;
    confidenceScore?: number;
    sources?: string[];
    createdAt: string;
}

/**
 * Ticket info
 */
interface TicketInfo {
    ticketNumber: string;
    subject: string;
    category: string;
    priority: string;
    status: string;
    orderId?: string;
    productId?: string;
    createdAt: string;
    firstResponseAt?: string;
    slaBreached?: boolean;
    messageCount: number;
}

/**
 * Support Chat Service
 */
export class SupportChatService {
    /**
     * Send message to support chat
     */
    static async sendMessage(request: SendMessageRequest): Promise<SendMessageResponse> {
        try {
            const response = await fetch(`${SUPPORT_API_BASE_URL}/chat`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(request)
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(
                    errorData.message || `Failed to send message: ${response.status}`
                );
            }

            return await response.json();
        } catch (error) {
            console.error('Error sending message:', error);
            throw error;
        }
    }

    /**
     * Get ticket information
     */
    static async getTicket(ticketNumber: string): Promise<TicketInfo> {
        try {
            const response = await fetch(
                `${SUPPORT_API_BASE_URL}/tickets/${ticketNumber}`
            );

            if (!response.ok) {
                throw new Error(`Failed to get ticket: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error getting ticket:', error);
            throw error;
        }
    }

    /**
     * Get ticket message history
     */
    static async getTicketHistory(ticketNumber: string): Promise<TicketMessage[]> {
        try {
            const response = await fetch(
                `${SUPPORT_API_BASE_URL}/tickets/${ticketNumber}/messages`
            );

            if (!response.ok) {
                console.warn(`Failed to get history: ${response.status}`);
                return [];
            }

            return await response.json();
        } catch (error) {
            console.error('Error getting ticket history:', error);
            return [];
        }
    }

    /**
     * Health check
     */
    static async checkHealth(): Promise<boolean> {
        try {
            const response = await fetch(`${SUPPORT_API_BASE_URL}/health`);
            return response.ok;
        } catch (error) {
            console.error('Health check failed:', error);
            return false;
        }
    }
}