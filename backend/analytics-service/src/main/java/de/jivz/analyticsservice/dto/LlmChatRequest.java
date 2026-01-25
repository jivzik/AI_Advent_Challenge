package de.jivz.analyticsservice.dto;


import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data

public class LlmChatRequest {

    private Integer maxTokens;
    private Double temperature;
    private String model;
    private String message;

}



