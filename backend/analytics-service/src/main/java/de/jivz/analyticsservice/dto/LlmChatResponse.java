package de.jivz.analyticsservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmChatResponse {
    private String response;
    private String model;
    private Integer tokensUsed;
}
