/**
 * Voice Agent Service - Audio Transcription with LLM Processing
 */

const VOICE_API_BASE_URL = 'http://localhost:8084/api/voice';

export interface VoiceAgentResponse {
    transcription: string;
    response: string;
    timestamp: string;
    language: string;
    transcriptionTimeMs: number;
    llmProcessingTimeMs: number;
    totalTimeMs: number;
    model: string;
    userId: string;
}

export interface VoiceProcessOptions {
    audioFile: File;
    userId: string;
    language?: string;
    model?: string;
    temperature?: number;
    systemPrompt?: string;
}

export const VoiceAgentService = {
    /**
     * Process audio file: Transcribe + LLM Response
     */
    async processVoiceCommand(options: VoiceProcessOptions): Promise<VoiceAgentResponse> {
        const formData = new FormData();
        formData.append('audio', options.audioFile);
        formData.append('userId', options.userId);

        if (options.language) {
            formData.append('language', options.language);
        }
        if (options.model) {
            formData.append('model', options.model);
        }
        if (options.temperature !== undefined) {
            formData.append('temperature', options.temperature.toString());
        }
        if (options.systemPrompt) {
            formData.append('systemPrompt', options.systemPrompt);
        }

        const response = await fetch(`${VOICE_API_BASE_URL}/process`, {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Voice processing failed: ${errorText}`);
        }

        return await response.json();
    },

    /**
     * Only transcribe audio (no LLM)
     */
    async transcribeAudio(audioFile: File, language?: string): Promise<{ text: string; language: string; model: string }> {
        const formData = new FormData();
        formData.append('audio', audioFile);

        if (language) {
            formData.append('language', language);
        }

        const response = await fetch(`${VOICE_API_BASE_URL}/transcribe`, {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Transcription failed: ${errorText}`);
        }

        return await response.json();
    },

    /**
     * Health check
     */
    async healthCheck(): Promise<boolean> {
        try {
            const response = await fetch(`${VOICE_API_BASE_URL}/health`);
            return response.ok;
        } catch {
            return false;
        }
    }
};
