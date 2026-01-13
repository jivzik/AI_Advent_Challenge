import { ref, computed } from 'vue';
import { DevAssistantService } from '../services/devAssistantService';
import type { Message } from '../types/types';

/**
 * Composable для работы с Developer Assistant
 *
 * Функции:
 * - Детекция команды /help
 * - Извлечение источников из ответа
 * - Проверка статуса RAG/Git
 * - Подсветка кода
 */
export function useDevAssistant() {
    const ragAvailable = ref(false);
    const gitAvailable = ref(false);
    const mcpServiceAvailable = ref(false);

    /**
     * Проверить доступность всех сервисов
     */
    const checkAvailability = async () => {
        try {
            const [rag, git, status] = await Promise.all([
                DevAssistantService.checkRAGAvailability(),
                DevAssistantService.checkGitAvailability(),
                DevAssistantService.isAvailable()
            ]);

            ragAvailable.value = rag;
            gitAvailable.value = git;
            mcpServiceAvailable.value = status;

            console.log('🔍 Dev Assistant Status:', { rag, git, status });
        } catch (error) {
            console.error('Failed to check availability:', error);
        }
    };

    /**
     * Проверить является ли сообщение командой /help
     */
    const isHelpCommand = (message: string): boolean => {
        return message.trim().startsWith('/help');
    };

    /**
     * Извлечь query из команды /help
     *
     * "/help How to create MCP?" → "How to create MCP?"
     */
    const extractHelpQuery = (message: string): string => {
        return message.substring(5).trim();
    };

    /**
     * Отправить вопрос Developer Assistant
     */
    const askDeveloperAssistant = async (
        query: string,
        conversationId: string,
        userId: string = 'user'
    ): Promise<Message> => {
        try {
            const response = await DevAssistantService.askHelp({
                query,
                userId,
                conversationId,
                includeGitContext: gitAvailable.value,
                maxDocuments: 5
            });

            // Извлечь источники из ответа
            const sources = DevAssistantService.extractSources(response.reply);

            // Удалить секцию источников из основного текста
            const contentWithoutSources = sources.length > 0
                ? DevAssistantService.removeSources(response.reply)
                : response.reply;

            // Создать сообщение с метаданными
            const message: Message = {
                role: 'assistant',
                content: contentWithoutSources,
                timestamp: new Date(),
                isDeveloperMode: true,
                sources: sources.length > 0 ? sources : undefined
            };

            return message;
        } catch (error) {
            console.error('Error asking developer assistant:', error);

            // Вернуть сообщение об ошибке
            return {
                role: 'assistant',
                content: `❌ Developer Assistant error: ${error instanceof Error ? error.message : 'Unknown error'}`,
                timestamp: new Date(),
                isDeveloperMode: true
            };
        }
    };

    /**
     * Извлечь code blocks из Markdown текста
     */
    const extractCodeBlocks = (content: string) => {
        const codeBlockRegex = /```(\w+)?\n([\s\S]*?)```/g;
        const blocks = [];
        let match;

        while ((match = codeBlockRegex.exec(content)) !== null) {
            blocks.push({
                language: match[1] || 'text',
                code: match[2]?.trim()
            });
        }

        return blocks;
    };

    /**
     * Проверить есть ли в массиве сообщений хотя бы одно от Developer Assistant
     */
    const hasDevMessages = computed(() => (messages: Message[]) => {
        return messages.some(msg => msg.isDeveloperMode === true);
    });

    /**
     * Получить список доступных команд
     */
    const availableCommands = [
        {
            command: '/help',
            description: 'Ask developer assistant (with RAG + Git)',
            example: '/help How to create MCP Provider?',
            icon: '🧑‍💻'
        },
        {
            command: '/status',
            description: 'Check Developer Assistant status',
            example: '/status',
            icon: '📊'
        },
        {
            command: '/git',
            description: 'Show current Git context',
            example: '/git status',
            icon: '🔧'
        }
    ];

    return {
        // State
        ragAvailable,
        gitAvailable,
        mcpServiceAvailable,
        availableCommands,

        // Methods
        checkAvailability,
        isHelpCommand,
        extractHelpQuery,
        askDeveloperAssistant,
        extractCodeBlocks,
        hasDevMessages
    };
}