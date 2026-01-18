package de.jivz.agentservice.cli.domain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerStatus {
    private String name;
    private String status;
    private String uptime;
    private String memoryUsage;
    private String cpuUsage;
    private String image;
    private boolean healthy;
    public String getStatusEmoji() {
        if (!healthy) return "❌";
        if ("running".equalsIgnoreCase(status)) return "✅";
        if ("restarting".equalsIgnoreCase(status)) return "🔄";
        return "⚠️";
    }
}
