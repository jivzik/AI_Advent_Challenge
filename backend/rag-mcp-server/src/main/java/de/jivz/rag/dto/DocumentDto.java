package de.jivz.rag.dto;

import de.jivz.rag.repository.entity.Document;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO für информации о документе.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentDto {

    private Long id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Integer chunkCount;
    private String status;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DocumentDto fromEntity(Document doc) {
        return DocumentDto.builder()
                .id(doc.getId())
                .fileName(doc.getFileName())
                .fileType(doc.getFileType())
                .fileSize(doc.getFileSize())
                .chunkCount(doc.getChunkCount())
                .status(doc.getStatus().name())
                .metadata(doc.getMetadata())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
