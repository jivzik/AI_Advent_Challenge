package de.jivz.ai_challenge.dto;

import lombok.*;

import java.time.Instant;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ChatResponse {
    private String reply;
    private String toolName;
    private Instant timestamp;
    private ResponseMetrics metrics;
}
