const DEV_ASSISTANT_API_BASE_URL = 'http://localhost:8084/api/dev';

/**
 * Request для Developer Assistant
 */
interface DevHelpRequest {
    query: string;
    userId: string;
    conversationId?: string;
    includeGitContext?: boolean;
    maxDocuments?: number;
    autoReadFiles?: boolean;
}

/**
 * Response от Developer Assistant
 */
interface DevHelpResponse {
    reply: string;
    model: string;
    responseTimeMs: number;
    finishReason: string;
}

/**
 * Status проверка
 */
interface DevAssistantStatus {
    service: string;
    status: string;
    features: string[];
    prompts_loaded: boolean;
}

/**
 * Developer Assistant Service
 *
 * Специализированный сервис для помощи разработчикам.
 * Автоматически использует RAG (документация) и Git tools.
 */
export class DevAssistantService {

    /**
     * Отправить вопрос Developer Assistant
     *
     * LLM автоматически вызовет нужные tools:
     * - rag:search_documents - поиск в документации
     * - git:get_current_branch - текущая ветка
     * - git:get_git_status - измененные файлы
     * - git:get_git_log - коммиты
     * - git:read_project_file - чтение файлов
     */
    static async askHelp(request: DevHelpRequest): Promise<DevHelpResponse> {
        try {
            const response = await fetch(`${DEV_ASSISTANT_API_BASE_URL}/help`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(request)
            });

            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Developer Assistant API error: ${response.status} - ${errorText}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error calling Developer Assistant:', error);
            throw error;
        }
    }

    /**
     * Быстрый вопрос без conversationId (GET endpoint)
     */
    static async quickHelp(query: string): Promise<DevHelpResponse> {
        try {
            const encodedQuery = encodeURIComponent(query);
            const response = await fetch(`${DEV_ASSISTANT_API_BASE_URL}/quick-help?query=${encodedQuery}`);

            if (!response.ok) {
                throw new Error(`Quick help failed: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error with quick help:', error);
            throw error;
        }
    }

    /**
     * Проверить статус Developer Assistant
     */
    static async checkStatus(): Promise<DevAssistantStatus> {
        try {
            const response = await fetch(`${DEV_ASSISTANT_API_BASE_URL}/status`);

            if (!response.ok) {
                throw new Error(`Status check failed: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error checking status:', error);
            throw error;
        }
    }

    /**
     * Health check (простая проверка доступности)
     */
    static async isAvailable(): Promise<boolean> {
        try {
            const status = await this.checkStatus();
            return status.status === 'operational';
        } catch {
            return false;
        }
    }

    /**
     * Извлечь источники из ответа (если есть секция 📚 Источники)
     */
    static extractSources(reply: string): string[] {
        const sourcesMatch = reply.match(/📚 Источники информации:\s*([\s\S]*?)(?:\n\n|$)/);
        if (!sourcesMatch) {
            return [];
        }

        const sourcesSection = sourcesMatch[1];
        const sources: string[] = [];

        // Парсим строки вида "1. `filename.md`"
        const lines = sourcesSection?.split('\n');
        if (lines) {
            for (const line of lines) {
                const match = line.match(/\d+\.\s*`([^`]+)`/);
                if (match && match[1]) {
                    sources.push(match[1]);
                }
            }
        }

        return sources;
    }

    /**
     * Удалить секцию источников из ответа (для отдельного отображения)
     */
    static removeSources(reply: string): string {
        return reply.replace(/\n\n---\n\n\*\*📚 Источники информации:\*\*\n[\s\S]*$/, '');
    }

    /**
     * Проверить доступность RAG MCP Server
     */
    static async checkRAGAvailability(): Promise<boolean> {
        try {
            // Проверяем через MCP Service
            const response = await fetch('http://localhost:8083/mcp/tools');
            if (!response.ok) return false;

            const tools = await response.json();
            // Ищем rag:search_documents tool
            return tools.some((tool: any) => tool.name === 'search_documents' || tool.name?.includes('rag'));
        } catch {
            return false;
        }
    }

    /**
     * Проверить доступность Git MCP Server
     */
    static async checkGitAvailability(): Promise<boolean> {
        try {
            const response = await fetch('http://localhost:8083/mcp/tools');
            if (!response.ok) return false;

            const tools = await response.json();
            // Ищем git tools
            return tools.some((tool: any) => tool.name?.includes('git') || tool.name === 'get_current_branch');
        } catch {
            return false;
        }
    }
}