package de.jivz.agentservice.dto.github;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowRunsResponse {
    @JsonProperty("total_count")
    private Integer totalCount;
    @JsonProperty("workflow_runs")
    private List<WorkflowRun> workflowRuns;
}
