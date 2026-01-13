package de.jivz.agentservice.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class PRDiff {
    private Integer prNumber;
    private String repository;
    private String baseSha;
    private String headSha;

    private List<PRFile> files;

    private int totalAdditions;
    private int totalDeletions;
    private int totalChanges;

    @Data
    @Builder
    public static class PRFile {
        private String filename;
        private String status;
        private int additions;
        private int deletions;
        private String patch;
    }
}