package de.jivz.agentservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PRInfo {
    private Integer number;
    private String title;
    private String description;
    private String author;
    private String baseBranch;
    private String headBranch;
    private String baseSha;
    private String headSha;
    private String repository;
    private Integer filesCount;
    private Integer additions;
    private Integer deletions;
}