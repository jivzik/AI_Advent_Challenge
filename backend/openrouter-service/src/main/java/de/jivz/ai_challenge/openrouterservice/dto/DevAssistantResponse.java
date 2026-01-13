package de.jivz.ai_challenge.openrouterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO für den Developer Assistant.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevAssistantResponse {

    /**
     * Die Antwort in Markdown Format
     */
    private String answer;

    /**
     * Quellen aus der Dokumentation
     */
    private List<SourceReference> sources;

    /**
     * Git Kontext Informationen
     */
    private GitContextInfo gitContext;

    /**
     * Vorgeschlagene Dateien zum Anschauen
     */
    private List<String> suggestedFiles;

    /**
     * Code Beispiele
     */
    private List<CodeExample> codeExamples;

    /**
     * Antwortzeit in Millisekunden
     */
    private Long responseTimeMs;

    /**
     * Verwendetes AI Modell
     */
    private String model;

    /**
     * Fehler falls vorhanden
     */
    private String error;

    /**
     * Quellreferenz aus der Dokumentation
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceReference {
        private String documentId;
        private String filePath;
        private String title;
        private Double relevanceScore;
        private String excerpt;
    }

    /**
     * Git Kontext Informationen
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GitContextInfo {
        private String currentBranch;
        private String gitStatus;
        private List<String> recentCommits;
        private Boolean hasUncommittedChanges;
    }

    /**
     * Code Beispiel
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeExample {
        private String language;
        private String code;
        private String description;
        private String filePath;
    }
}

