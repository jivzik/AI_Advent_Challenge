package de.jivz.rag.dto;

import de.jivz.rag.repository.entity.Document;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO для информации о документе.
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
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}

