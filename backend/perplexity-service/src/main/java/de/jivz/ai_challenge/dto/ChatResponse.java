package de.jivz.ai_challenge.dto;

import lombok.*;

import java.util.Date;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ChatResponse {
    private String reply;
    private String toolName;
    private Date timestamp;
    private ResponseMetrics metrics;
}
