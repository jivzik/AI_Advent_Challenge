package de.jivz.agentservice.cli.domain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeploymentInfo {
    private String serviceName;
    private String version;
    private DeploymentStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String workflowRunUrl;
    private String currentStep;
    private int totalSteps;
    private int completedSteps;
    public enum DeploymentStatus {
        STARTING, RUNNING_TESTS, BUILDING_IMAGE, DEPLOYING, SUCCESS, FAILED, CANCELLED
    }
    public String getProgressPercentage() {
        if (totalSteps == 0) return "0%";
        return String.format("%d%%", (completedSteps * 100) / totalSteps);
    }
}
