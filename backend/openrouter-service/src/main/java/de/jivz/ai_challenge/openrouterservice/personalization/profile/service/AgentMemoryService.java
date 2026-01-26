package de.jivz.ai_challenge.openrouterservice.personalization.profile.service;

import de.jivz.ai_challenge.openrouterservice.personalization.profile.dto.AgentMemoryDTO;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.dto.AgentMemoryRequestDTO;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.entity.AgentMemory;
import de.jivz.ai_challenge.openrouterservice.personalization.profile.repository.AgentMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing agent memory and learned patterns
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentMemoryService {

    private final AgentMemoryRepository agentMemoryRepository;

    /**
     * Store or update a memory entry
     * @param requestDTO The memory request
     * @return The stored memory DTO
     */
    @Transactional
    public AgentMemoryDTO storeMemory(AgentMemoryRequestDTO requestDTO) {
        log.debug("Storing memory for user: {}, key: {}", requestDTO.getUserId(), requestDTO.getKey());

        Optional<AgentMemory> existingMemory = agentMemoryRepository
                .findByUserIdAndKey(requestDTO.getUserId(), requestDTO.getKey());

        AgentMemory memory;
        if (existingMemory.isPresent()) {
            // Update existing memory
            memory = existingMemory.get();
            memory.setValue(requestDTO.getValue());
            memory.setMemoryType(requestDTO.getMemoryType());
            if (requestDTO.getConfidence() != null) {
                memory.updateConfidence(requestDTO.getConfidence());
            }
            memory.incrementUsage();
            log.info("Updated existing memory: userId={}, key={}", requestDTO.getUserId(), requestDTO.getKey());
        } else {
            // Create new memory
            memory = AgentMemory.builder()
                    .userId(requestDTO.getUserId())
                    .memoryType(requestDTO.getMemoryType())
                    .key(requestDTO.getKey())
                    .value(requestDTO.getValue())
                    .confidence(requestDTO.getConfidence() != null ? requestDTO.getConfidence() : 0.5)
                    .build();
            log.info("Created new memory: userId={}, key={}", requestDTO.getUserId(), requestDTO.getKey());
        }

        AgentMemory saved = agentMemoryRepository.save(memory);
        return toDTO(saved);
    }

    /**
     * Get a specific memory entry
     * @param userId The user ID
     * @param key The memory key
     * @return Optional containing the memory DTO if found
     */
    @Transactional(readOnly = true)
    public Optional<AgentMemoryDTO> getMemory(String userId, String key) {
        log.debug("Getting memory for user: {}, key: {}", userId, key);
        return agentMemoryRepository.findByUserIdAndKey(userId, key)
                .map(this::toDTO);
    }

    /**
     * Get all memory entries for a user
     * @param userId The user ID
     * @return List of memory DTOs
     */
    @Transactional(readOnly = true)
    public List<AgentMemoryDTO> getAllMemories(String userId) {
        log.debug("Getting all memories for user: {}", userId);
        return agentMemoryRepository.findByUserId(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get memory entries by type
     * @param userId The user ID
     * @param memoryType The memory type
     * @return List of memory DTOs
     */
    @Transactional(readOnly = true)
    public List<AgentMemoryDTO> getMemoriesByType(String userId, String memoryType) {
        log.debug("Getting {} memories for user: {}", memoryType, userId);
        return agentMemoryRepository.findByUserIdAndMemoryType(userId, memoryType).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get high-confidence memory entries
     * @param userId The user ID
     * @param minConfidence Minimum confidence level (0.0 to 1.0)
     * @return List of high-confidence memory DTOs
     */
    @Transactional(readOnly = true)
    public List<AgentMemoryDTO> getHighConfidenceMemories(String userId, Double minConfidence) {
        log.debug("Getting high-confidence memories for user: {} (min: {})", userId, minConfidence);
        return agentMemoryRepository.findHighConfidenceMemories(userId, minConfidence).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get most used memory entries
     * @param userId The user ID
     * @param limit Maximum number of results
     * @return List of most used memory DTOs
     */
    @Transactional(readOnly = true)
    public List<AgentMemoryDTO> getMostUsedMemories(String userId, int limit) {
        log.debug("Getting top {} most used memories for user: {}", limit, userId);
        return agentMemoryRepository.findMostUsedMemories(userId, limit).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Increment usage count for a memory entry
     * @param userId The user ID
     * @param key The memory key
     * @return Updated memory DTO
     */
    @Transactional
    public Optional<AgentMemoryDTO> incrementUsage(String userId, String key) {
        log.debug("Incrementing usage for memory: userId={}, key={}", userId, key);

        Optional<AgentMemory> memoryOpt = agentMemoryRepository.findByUserIdAndKey(userId, key);
        if (memoryOpt.isPresent()) {
            AgentMemory memory = memoryOpt.get();
            memory.incrementUsage();
            AgentMemory saved = agentMemoryRepository.save(memory);
            log.info("Incremented usage count: userId={}, key={}, count={}",
                     userId, key, saved.getUsageCount());
            return Optional.of(toDTO(saved));
        }

        log.warn("Memory not found for incrementing usage: userId={}, key={}", userId, key);
        return Optional.empty();
    }

    /**
     * Update confidence level for a memory entry
     * @param userId The user ID
     * @param key The memory key
     * @param confidence New confidence level (0.0 to 1.0)
     * @return Updated memory DTO
     */
    @Transactional
    public Optional<AgentMemoryDTO> updateConfidence(String userId, String key, Double confidence) {
        log.debug("Updating confidence for memory: userId={}, key={}, confidence={}",
                  userId, key, confidence);

        Optional<AgentMemory> memoryOpt = agentMemoryRepository.findByUserIdAndKey(userId, key);
        if (memoryOpt.isPresent()) {
            AgentMemory memory = memoryOpt.get();
            memory.updateConfidence(confidence);
            AgentMemory saved = agentMemoryRepository.save(memory);
            log.info("Updated confidence: userId={}, key={}, confidence={}",
                     userId, key, saved.getConfidence());
            return Optional.of(toDTO(saved));
        }

        log.warn("Memory not found for updating confidence: userId={}, key={}", userId, key);
        return Optional.empty();
    }

    /**
     * Delete a memory entry
     * @param userId The user ID
     * @param key The memory key
     */
    @Transactional
    public void deleteMemory(String userId, String key) {
        log.info("Deleting memory: userId={}, key={}", userId, key);
        agentMemoryRepository.findByUserIdAndKey(userId, key)
                .ifPresent(memory -> {
                    agentMemoryRepository.delete(memory);
                    log.info("Memory deleted: userId={}, key={}", userId, key);
                });
    }

    /**
     * Delete all memory entries for a user
     * @param userId The user ID
     */
    @Transactional
    public void deleteAllMemories(String userId) {
        log.info("Deleting all memories for user: {}", userId);
        agentMemoryRepository.deleteByUserId(userId);
        log.info("All memories deleted for user: {}", userId);
    }

    /**
     * Convert AgentMemory entity to DTO
     * @param memory The memory entity
     * @return The memory DTO
     */
    private AgentMemoryDTO toDTO(AgentMemory memory) {
        return AgentMemoryDTO.builder()
                .id(memory.getId())
                .userId(memory.getUserId())
                .memoryType(memory.getMemoryType())
                .key(memory.getKey())
                .value(memory.getValue())
                .confidence(memory.getConfidence())
                .usageCount(memory.getUsageCount())
                .lastUsed(memory.getLastUsed())
                .createdAt(memory.getCreatedAt())
                .updatedAt(memory.getUpdatedAt())
                .build();
    }
}
