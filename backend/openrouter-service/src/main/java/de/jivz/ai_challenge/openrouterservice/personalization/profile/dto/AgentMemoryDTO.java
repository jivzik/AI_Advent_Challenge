package de.jivz.ai_challenge.openrouterservice.personalization.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for AgentMemory responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentMemoryDTO {
    private Long id;
    private String userId;
    private String memoryType;
    private String key;
    private String value;
    private Double confidence;
    private Integer usageCount;
    private LocalDateTime lastUsed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
