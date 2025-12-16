package de.jivz.ai_challenge.googleservice.dto;

import com.google.api.client.util.DateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {
    private String title;
    private String notes;
    private DateTime due;  // RFC 3339 timestamp
}
