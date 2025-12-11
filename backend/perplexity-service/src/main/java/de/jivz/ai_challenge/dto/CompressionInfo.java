package de.jivz.ai_challenge.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for compression information response
 */
@Data
@Builder
public class CompressionInfo {
    private String conversationId;
    private int fullHistorySize;
    private int compressedHistorySize;
    private boolean isCompressed;
    private int messagesSaved;
    private String compressionRatio;
    private int estimatedTokensSaved;
    private String timestamp;
}