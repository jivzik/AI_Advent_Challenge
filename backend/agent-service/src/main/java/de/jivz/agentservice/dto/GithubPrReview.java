package de.jivz.agentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GithubPrReview {
    private String repository;
    private Integer prNumber;
    private String reviewBody;
    ReviewDecision decision;
    private String commitSha;

}
