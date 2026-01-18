package de.jivz.agentservice.dto.github;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowRun {
    private Long id;
    private String name;
    private String status;
    private String conclusion;
    @JsonProperty("html_url")
    private String htmlUrl;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("updated_at")
    private String updatedAt;
    @JsonProperty("run_number")
    private Integer runNumber;
    @JsonProperty("workflow_id")
    private Long workflowId;
    public boolean isCompleted() {
        return "completed".equalsIgnoreCase(status);
    }
    public boolean isSuccess() {
        return "success".equalsIgnoreCase(conclusion);
    }
    public boolean isFailed() {
        return "failure".equalsIgnoreCase(conclusion);
    }
}
